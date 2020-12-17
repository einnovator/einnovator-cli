package org.einnovator.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bouncycastle.util.Arrays;
import org.einnovator.devops.client.model.Binding;
import org.einnovator.devops.client.model.Catalog;
import org.einnovator.devops.client.model.Cluster;
import org.einnovator.devops.client.model.Connector;
import org.einnovator.devops.client.model.CronJob;
import org.einnovator.devops.client.model.Deployment;
import org.einnovator.devops.client.model.Domain;
import org.einnovator.devops.client.model.Job;
import org.einnovator.devops.client.model.Mount;
import org.einnovator.devops.client.model.Registry;
import org.einnovator.devops.client.model.Route;
import org.einnovator.devops.client.model.Solution;
import org.einnovator.devops.client.model.Space;
import org.einnovator.devops.client.model.Vcs;
import org.einnovator.util.MapUtil;
import org.einnovator.util.MappingUtils;
import org.einnovator.util.StringUtil;
import org.einnovator.util.config.ConnectionConfiguration;
import org.einnovator.util.meta.MetaUtil;
import org.springframework.data.domain.Page;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;


public abstract class CommandRunnerBase implements CommandRunner {


	protected String[] cmds;
	protected Map<String, Object> options;
	protected OAuth2RestTemplate template;
	
	@Override
	public boolean supports(String cmd) {
		return StringUtil.containsIgnoreCase(getCommands(), cmd);
	}


	protected String[] getCommands() {
		return null;
	}

	@Override
	public void init(String[] cmds, Map<String, Object> options, OAuth2RestTemplate template) {
		this.cmds = cmds;
		this.options = options;
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
		return options;
	}


	/**
	 * Set the value of property {@code argsMap}.
	 *
	 * @param argsMap the value of {@code argsMap}
	 */
	public void setArgsMap(Map<String, Object> argsMap) {
		this.options = argsMap;
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
	
	protected Object get(String name, Map<String, Object> map) {
		Object value = map.get(name);
		return value;
	}
	
	
	
	//
	// Util
	//

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

	protected <T> T get(String name, Map<String, Object> map, T defaultValue) {
		@SuppressWarnings("unchecked")
		T value = (T)map.get(name);
		if (value==null) {
			value = defaultValue;
		}
		return value;
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

	void println(Object obj) {
		print(obj, 0);
		System.out.println();
	}

	@SuppressWarnings("rawtypes")
	void print(Object obj, int n) {
		if (obj instanceof Iterable) {
			for (Object o: (Iterable)obj) {
				print(o, n+1);
			}
			return;
		}
		System.out.print(String.format((n>0 ? "%" + (n+1) + "s" : "") + "%s", "", format(obj)));
	}

	void printW(Object obj, int n) {
		System.out.print(String.format("%" + (n>1 ? "-" + n : "") + "s", formatSimple(obj)));		
	}

	void print(Page<?> page) {
		print(page.getContent());
	}
	
	void print(Iterable<?> it) {
		boolean ln = false;
		boolean hrule = false;
		if (isTabular()) {
			String fmt = null;
			List<List<String>> table = new ArrayList<>();
			for (Object o: it) {
				if (fmt==null) {
					fmt = getFormat(o);
				}
				List<String> row = getFields(o, fmt);
				table.add(row);
			}
			int[] widths = getColsWidth(table);
			String[] cols = getFormatCols(fmt);
			if (cols.length>widths.length) {
				widths = Arrays.copyOf(widths, cols.length);
			}
			for (int j = 0; j<cols.length; j++) {
				if (cols[j].length()>widths[j]) {
					widths[j] = cols[j].length();
				}
			}
			for (int j = 0; j<cols.length; j++) {
				String col = formatColName(cols[j]);
				printW(col, widths[j]+3);
			}
			System.out.println();
			if (hrule) {
				for (int j = 0; j<cols.length; j++) {
					System.out.print(new String(new char[widths[j]+3]).replace('\0', '-'));
				}
				System.out.println();				
			}
			int i = 0;
			for (List<String> row: table) {
				if (ln) {
					print("[%-" + digits(table.size()) + "s] ", i);
				}
				int j = 0;
				for (String value: row) {
					printW(value, widths[j]+3);
					j++;
				}
				System.out.println();
				i++;
			}
		} else {
			String fmt = null;
			for (Object o: it) {
				if (fmt==null) {
					fmt = getFormat(o);
				}
				print(o, 0);
			}			
		}

	}

	void printObj(Object obj) {
		String fmt = getFormat(obj);
		if (isTabular()) {
			List<String> values = getFields(obj, fmt);
			String[] cols = getFormatCols(fmt);
			int[] widths = new int[Math.max(values.size(), cols.length)];
			for (int i=0; i<widths.length; i++) {
				widths[i] = i<cols.length && i<values.size() ? Math.max(values.get(i).length(), cols[i].length()) :
					i<cols.length ? cols[i].length() : values.get(i).length();
			}
			for (int j = 0; j<cols.length; j++) {
				String col = formatColName(cols[j]);
				printW(col, widths[j]+3);
			}
			System.out.println();
			int j = 0;
			for (String value: values) {
				printW(value, widths[j]+3);
				j++;
			}
			System.out.println();
		} else {
			print(obj, 0);
		}		
	}
	
	private String formatColName(String col) {
		return col.toUpperCase();
	}

	private int digits(int n) {
		return (int)(Math.log10(n)+1);
	}

	private int[] getColsWidth(List<List<String>> table) {
		int[] ww = new int[getWidth(table)];
		for (List<String> row: table) {
			int i = 0;
			for (String value: row) {
				int l = value!=null ? value.length() : 0;
				if (l>ww[i]) {
					ww[i] = l;
				}
				i++;
			}				
		}
		return ww;
	}

	private int getWidth(List<List<String>> table) {
		int w = 0;
		for (List<String> row: table) {
			if (row.size()>w) {
				w = row.size();
			}
		}
		return w;
	}

	void printLine(Object... objs) {
		for (Object obj: objs) {
			System.out.print(obj);		
		}
		System.out.println();		
	}
	
	protected boolean isTabular() {
		String o = getFormat();
		if (o==null) {
			return true;
		}
		switch (o) {
		case "raw":
		case "json":
		case "yaml":
			return false;
		default: 
			return true;
		}
	}
	
	protected String getFormat() {
		String o = (String)options.get("o");
		if (o==null || o.isEmpty()) {
			return null;
		}
		o = o.toLowerCase();
		return o;
	}

	protected String getFormat(Object obj) {
		String o = getFormat();
		if (o==null || o.isEmpty()) {
			o = getDefaultFormat(obj.getClass());
		} else if ("wide".equals(o)) {
			o = getWideFormat(obj.getClass());
		}
		return o;
	}
	
	String[] getFormatCols(String fmt) {
		String[] cols = fmt.split(",");
		for (int i =0; i<cols.length; i++) {
			String col = cols[i];
			int j = col.indexOf(":");
			if (j>0 && j<col.length()-1) {
				cols[i] = col.substring(j+1);
			}
		}
		return cols;
	}

	String[] getFormatProps(String fmt) {
		String[] cols = fmt.split(",");
		for (int i =0; i<cols.length; i++) {
			String col = cols[i];
			int j = col.indexOf(":");
			if (j>0 && j<col.length()-1) {
				cols[i] = col.substring(0, j);
			}
		}
		return cols;
	}

	String format(Object obj) {
		if (obj==null) {
			return "";
		}
		String fmt = getFormat(obj);
		if ("raw".equals(fmt)) {
			return obj.toString();
		} else if ("json".equals(fmt)) {
			String s = MappingUtils.toJson(obj);
			if (s==null) {
				s = "";
			}
			return s;
		} else if ("yaml".equals(fmt)) {
			String s = MappingUtils.toJson(obj);
			if (s==null) {
				s = "";
			}
			return s;
		}
		if (fmt!=null && !fmt.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			Map<String, Object> map = MappingUtils.toMap(obj);
			if (map==null) {
				sb.append("???");
			} else if (fmt=="+") {
				for (Map.Entry<String, Object> e: map.entrySet()) {
					if (sb.length()>0) {
						sb.append(" ");						
					}
					sb.append(e.getValue());
				}
			} else {
				String[] props = getFormatProps(fmt);
				for (String s: props) {
					if (!s.isEmpty()) {
						sb.append(" ");						
					}
					Object value = MapUtil.resolve(s, map);
					if (value==null) {
						value = "";
					}
					sb.append(value);						
				}
			}
			return sb.toString();
		} else {
			return obj.toString();			
		}
	}

	String formatCols(Object obj, String fmt) {
		StringBuilder sb = new StringBuilder();
		Map<String, Object> map = MappingUtils.toMap(obj);
		if (map==null) {
			sb.append("???");
		} else if (fmt=="+") {
			for (Map.Entry<String, Object> e: map.entrySet()) {
				if (sb.length()>0) {
					sb.append(" ");						
				}
				sb.append(e.getValue());
			}
		} else {
			String[] props = getFormatProps(fmt);
			for (String s: props) {
				if (!s.isEmpty()) {
					sb.append(" ");						
				}
				Object value = MapUtil.resolve(s, map);
				if (value==null) {
					value = "";
				}
				sb.append(value);						
			}
		}
		return sb.toString();
	}
	
	List<String> getFields(Object obj, String fmt) {
		List<String> values = new ArrayList<>();
		Map<String, Object> map = MappingUtils.toMap(obj);
		if (map==null) {
		} else if (fmt=="+") {
			for (Map.Entry<String, Object> e: map.entrySet()) {
				values.add(formatSimple(e.getValue()));						
			}
		} else {
			String[] props = getFormatProps(fmt);
			for (String s: props) {
				Object value = MapUtil.resolve(s, map);
				values.add(formatSimple(value));						
			}
		}
		return values;
	}

	private String formatSimple(Object value) {
		if (value==null) {
			return "";
		}
		return value.toString();
	}

	protected void debug(Object... s) {
		if (isDebug()) {
			System.out.println(s);
		}
	}
	
	protected boolean isDebug() {
		String s = (String)options.get("debug");
		return s!=null;
	}
	
	protected String getDefaultFormat(Class<? extends Object> type) {
		return "id,name";
	}

	protected String getWideFormat(Class<? extends Object> type) {
		return getDefaultFormat(type);
	}
}