package org.einnovator.cli;

import java.util.List;
import java.util.Map;

import org.einnovator.util.MappingUtils;
import org.einnovator.util.StringUtil;
import org.einnovator.util.config.ConnectionConfiguration;
import org.einnovator.util.meta.MetaUtil;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;


public abstract class CommandRunnerBase implements CommandRunner {


	protected Map<String, Object> argsMap;
	protected OAuth2RestTemplate template;
	protected String[] cmds;
	
	@Override
	public boolean supports(String cmd) {
		return StringUtil.containsIgnoreCase(getCommands(), cmd);
	}


	protected String[] getCommands() {
		return null;
	}

	@Override
	public void init(String[] cmds, Map<String, Object> argsMap, OAuth2RestTemplate template) {
		this.cmds = cmds;
		this.argsMap = argsMap;
		this.template = template;
	}


	@Override
	public void printUsage() {
		System.err.println("usage: " + CliRunner.CLI_NAME + " " + getPrefix() + " " + getUsage());
		System.exit(-1);
	}
	
	protected String getUsage() {
		StringBuilder sb = new StringBuilder();
		return sb.toString();
	}

	
	//
	// Util
	//
	
	
	/**
	 * Get the value of property {@code argsMap}.
	 *
	 * @return the value of {@code argsMap}
	 */
	public Map<String, Object> getArgsMap() {
		return argsMap;
	}


	/**
	 * Set the value of property {@code argsMap}.
	 *
	 * @param argsMap the value of {@code argsMap}
	 */
	public void setArgsMap(Map<String, Object> argsMap) {
		this.argsMap = argsMap;
	}


	/**
	 * Get the value of property {@code template}.
	 *
	 * @return the value of {@code template}
	 */
	public OAuth2RestTemplate getTemplate() {
		return template;
	}


	/**
	 * Set the value of property {@code template}.
	 *
	 * @param template the value of {@code template}
	 */
	public void setTemplate(OAuth2RestTemplate template) {
		this.template = template;
	}

	protected void error(String msg, Object... args) {
		System.err.println(String.format("ERROR: " + msg, args));
	}

	protected void noresources(String type, String op, Map<String, Object> args) {
		System.err.println(String.format("Resources not found: %s", type));		
	}

	protected void operationFailed(String type, String op, Map<String, Object> args) {
		System.err.println(String.format("ERROR: operation faield: %s %s", type, op));
	}

	protected void invalidOp(String type, String op) {
		System.err.println(String.format("ERROR: invalid operation: %s %s", type, op));
	}

	protected void invalidType(String type) {
		System.err.println(String.format("ERROR: invalid resource type: %s", type));
	}
	
	protected void missingArg(String type, String op, String name) {
		System.err.println(String.format("ERROR: missing argument %s: %s %s", name, type, op));
	}
	
	protected <T> T get(String name, Map<String, Object> map, T defaultValue) {
		@SuppressWarnings("unchecked")
		T value = (T)map.get(name);
		if (value==null) {
			value = defaultValue;
		}
		return value;
	}
	
	protected Object get(String name, Map<String, Object> map) {
		Object value = map.get(name);
		return value;
	}
	
	protected <T> T get(String[] names, Map<String, Object> map, Class<T> type) {
		for (String name: names) {
			@SuppressWarnings("unchecked")
			T value = (T)map.get(name);
			if (value!=null) {
				return value;
			}
		}
		return null;
	}

	protected Object get(String[] names, Map<String, Object> map) {
		for (String name: names) {
			Object value = map.get(name);
			if (value!=null) {
				return value;
			}
		}
		return null;
	}
	//
	// Print
	//
	
	void print(Object obj) {
		print(obj, 0);
	}

	@SuppressWarnings("rawtypes")
	void print(Object obj, int n) {
		if (obj instanceof Iterable) {
			for (Object o: (Iterable)obj) {
				print(o, n+1);
			}
			return;
		}
		System.out.println(String.format("%" + (n+1) + "s%s", "", format(obj)));
	}
	
	void printLine(Object... objs) {
		boolean first = true;
		for (Object obj: objs) {
			if (!first) {
				System.out.print(" ");						
			}
			System.out.print(obj);		
			first = false;
		}
		System.out.println();		
	}
	
	String format(Object obj) {
		String o = (String)argsMap.get("o");
		if (o!=null && !o.isEmpty()) {
			String[] a = o.split(",");
			StringBuilder sb = new StringBuilder();
			Map<String, Object> map = MappingUtils.toMap(obj);
			for (String s: a) {
				if (!s.isEmpty()) {
					sb.append(" ");						
				}
				sb.append(map.get(s));
			}
			return sb.toString();
		} else {
			return obj.toString();			
		}
	}

	
	public String schemaToString(Class<?> type) {
		return schemaToString(type, " ");
	}

	public String schemaToString(Class<?> type, String separator) {
		List<String> props = MetaUtil.collectAllPropertyNames(type);
		return String.join(separator, props.toArray(new String[props.size()]));
	}
	
	OAuth2RestTemplate makeOAuth2RestTemplate(ResourceOwnerPasswordResourceDetails resource, ConnectionConfiguration conncConfig) {
		DefaultOAuth2ClientContext context = new DefaultOAuth2ClientContext();
		OAuth2RestTemplate template = new OAuth2RestTemplate(resource, context);
		template.setRequestFactory(conncConfig.makeClientHttpRequestFactory());
		return template;
	}
}