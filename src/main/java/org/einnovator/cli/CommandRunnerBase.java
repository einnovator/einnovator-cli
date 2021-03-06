package org.einnovator.cli;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.function.Function;

import org.einnovator.devops.client.model.NamedEntity;
import org.einnovator.util.MapUtil;
import org.einnovator.util.MappingUtils;
import org.einnovator.util.PathUtil;
import org.einnovator.util.StringUtil;
import org.einnovator.util.UriUtils;
import org.einnovator.util.config.ConnectionConfiguration;
import org.einnovator.util.meta.MetaUtil;
import org.einnovator.util.model.EntityBase;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.domain.Page;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;


public abstract class CommandRunnerBase  extends RunnerBase implements CommandRunner {

	private static final String PROPINFO_DEFAULT_FORMAT = "name,type,declaredIn,description";
	
	protected static final String DEFAULT_FORMAT = "id,name";

	protected static final String EMPTY_VALUE = "<none>";
	
	protected String[] cmds;
	protected String[] extra;
	protected Map<String, Object> options;
	protected RestTemplate template;
	protected boolean interactive;
	protected boolean init;
	protected String type;
	protected String op;
	protected boolean singleuser;
	protected boolean admin;
	protected boolean server;

	protected ResourceBundle bundle;

	protected static YAMLFactory yamlFactory = new YAMLFactory();

	protected ConversionService conversionService = new DefaultConversionService();
	
	@Override
	public boolean supports(String cmd) {
		String[][] cmds = getCommands();
		if (cmds!=null) {
			for (String[] cmds2: cmds) {
				if (StringUtil.containsIgnoreCase(cmds2, cmd)) {
					return true;
				}				
			}
		}
		return false;
	}

	@Override
	public boolean supports(String cmd, Map<String, Object> options) {
		if (!supports(cmd)) {
			return false;
		}
		String[] options2 =  getOptions(cmd);
		if (options2!=null) {
			if (options==null) {
				if (StringUtil.contains(options2, "")) {
					return true;					
				}
			} else {
				for (String option: options2) {
					if (option!=null && !option.isEmpty() && options.get(option)!=null) {
						return true;
					}
				}
				
			}
		}
		return false;
	}

	protected String[] getOptions(String cmd) {
		return null;
	}

	protected String[][] getCommands() {
		return null;
	}

	protected String[][] getRootCommands() {
		String[][] cmds = getCommands();
		List<String[]> root = new ArrayList<>();
		if (cmds!=null) {
			for (String[] cmd: cmds) {
				if (isRootCommand(cmd[0])) {
					root.add(cmd);
				}
			}
		}
		return root.toArray(new String[root.size()][]);
	}
	
	protected boolean isRootCommand(String cmd) {
		return supports(cmd, null);
	}
	
	protected Map<String, String[][]> getSubCommands() {
		return null;
	}
	
	protected Map<String, Map<String, String[][]>> getSubSubCommands() {
		return null;
	}


	protected String[][] getSubCommands(String cmd) {
		Map<String, String[][]> subcmds = getSubCommands();
		if (subcmds!=null) {
			return subcmds.get(cmd);
		}
		return null;
	}
	

	@Override
	public void init(Map<String, Object> options, RestTemplate template, boolean interactive, ResourceBundle bundle) {
		super.init(interactive, bundle);
		this.options = options;
		this.template = template;
	}

	protected void setLine(String type, String op, String[] cmds, String[] extra, Map<String, Object> options) {	
		this.type = type;
		this.op = op;
		this.cmds = cmds;
		this.extra = extra;
		this.options = options;
	}
		
	protected OAuth2RestTemplate makeOAuth2RestTemplate(ResourceOwnerPasswordResourceDetails resource,
			DefaultOAuth2ClientContext context, ConnectionConfiguration connection) {
		OAuth2RestTemplate template = new OAuth2RestTemplate(resource, context);
		if (connection!=null) {			
			template.setRequestFactory(connection.makeClientHttpRequestFactory());			
		}
		return template;
	}


	protected RestTemplate makeRestTemplate(ConnectionConfiguration connection) {
		RestTemplate template = new RestTemplate();
		if (connection!=null) {			
			template.setRequestFactory(connection.makeClientHttpRequestFactory());			
		}
		return template;
	}

	protected boolean isDryrun() {
		if (options.get("dryrun")!=null) {
			return true;
		}
		return false;
	}

	protected boolean isDump() {
		if (options.get("dump")!=null) {
			return true;
		}
		return false;
	}

	protected boolean isHelp() {
		if (options.get("h")!=null) {
			return true;
		}
		return false;
	}
	protected boolean isHelp(String type) {
		if (isHelp()) {
			printUsage(type);
			return true;
		}
		return false;		
	}

	protected boolean isHelp(String type, String op) {
		if (isHelp()) {
			printUsage(type, op);
			return true;
		}
		return false;		
	}
	
	protected boolean isHelp(String type, String op, String subcmd) {
		if (isHelp()) {
			printUsage(type, op, subcmd);
			return true;
		}
		return false;		
	}

	protected boolean isHelp1() {
		return isHelp(type);
	}

	protected boolean isHelp2() {
		return isHelp(type, op);
	}

	protected boolean isHelp3(String subcmd) {
		return isHelp(type, op, subcmd);
	}

	
	@Override
	public void printUsage() {
		printCmds();
		if (interactive) {
			System.out.println(String.format("\nUsage: [%s] %s", getName(), getUsage(false)));			
		} else {
			System.out.println(String.format("\nUsage: %s [%s] %s", CliRunner.CLI_NAME, getName(), getUsage(false)));			
		}
	}
	
	public void printCmds() {
		String descr = resolve(getName());
		if (isNamed()) {
			System.out.println(String.format("[%s] %s", getName(), descr));						
		} else {
			System.out.println(descr);						
		}
		String[][] cmds = getCommands();
		if (cmds!=null) {
			printCmds(null, null, cmds, true);
		}
	}

	protected boolean isNamed() {
		return true;
	}

	public void printUsage1() {
		printUsage(type);
	}

	public void printUsage2() {
		printUsage(type, op);
	}

	public void printUsage3(String op2) {
		printUsage(type, op, op2);
	}

	public void printUsage(String cmd) {
		String[] alias = findCommand(cmd);
		String kcmd = cmd;
		if (alias!=null && alias.length>0) {
			kcmd = alias[0];
		}
		String[][] subcmds = getSubCommands(kcmd);
		printUsageDetails(cmd, kcmd, alias, subcmds!=null && subcmds.length>0);			
		printCmds(cmd, kcmd, subcmds, true);
	}

	private static String[] addPrefix(String prefix, String[] ss) {
		if (prefix==null || ss==null) {
			return null;
		}
		String[] ss2 = new String[ss.length];
		for (int i=0; i<ss.length; i++) {
			ss2[i] = prefix + (ss[i]!=null ? ss[i] : "");
		}
		return ss2;
	}
	public void printUsage(String cmd, String subcmd) {
		String kcmd = cmd, ksubcmd = subcmd;
		String[] alias0 = findCommand(cmd);
		if (alias0!=null && alias0.length>0) {
			kcmd = alias0[0];
		}
		String[] alias = findSubCommand(cmd, subcmd);
		if (alias!=null && alias.length>0) {
			ksubcmd = alias[0];
		}
		String qname = cmd + (!subcmd.isEmpty() ? " " + ksubcmd : "");	
		String key = kcmd + (!ksubcmd.isEmpty() ? "." + ksubcmd : "");		

		String[][] subsubcmds = findSubSubCommands(kcmd, ksubcmd);
		alias = addPrefix(cmd + " ", alias);
		
		printUsageDetails(qname, key, alias, subsubcmds!=null && subsubcmds.length>0);			
		printCmds(qname, key, subsubcmds, true);
	}
	
	protected void printUsage(String cmd, String subcmd, String subsubcmd) {
		String kcmd = cmd;
		String[] alias = findCommand(cmd);
		if (alias!=null && alias.length>0) {
			kcmd = alias[0];
		}
		String ksubcmd = subcmd;
		alias = findSubCommand(kcmd, ksubcmd);
		if (alias!=null && alias.length>0) {
			ksubcmd = alias[0];
		}
		String ksubsubcmd = subsubcmd;
		alias = findSubSubCommand(kcmd, ksubcmd, subsubcmd);
		if (alias!=null && alias.length>0) {
			ksubsubcmd = alias[0];
		}
		alias = findSubSubCommand(kcmd, ksubcmd, ksubsubcmd);
		String qname = cmd + " " + subcmd + (!subsubcmd.isEmpty() ? " " + subsubcmd : "");
		String key = kcmd + "." + ksubcmd + "." + ksubsubcmd;
		
		alias = addPrefix(cmd + " " + subcmd + " ", alias);
		printUsageDetails(qname, key, alias, false);
	}

	protected void printCmds(String qname, String qkey, String[][] subcmds, boolean indent) {
		if (subcmds!=null && subcmds.length>0) {
			int width = 0;
			System.out.println();
			for (String[] subcmd: subcmds) {
				if ((qname!=null ? qname.length() + 1 : 0) + subcmd[0].length()>width) {
					width = (qname!=null ? qname.length() + 1 : 0) + subcmd[0].length();
				}
			}
			for (String[] subcmd: subcmds) {
				String qname1 = (qname!=null ? qname + " " : "") + subcmd[0];
				String qkey1 = (qkey!=null ? qkey + "."  : "") + subcmd[0];
				String descr = resolve(getName() + "." + qkey1);
				System.out.println(String.format("  %" + (width>1 ? "-" + width : "") + "s    %s", qname1, descr));
			}
		}		
	}


	protected Map<String, String> getCmdsDescription(String[][] cmds, boolean indent) {
		return getCmdsDescription(null, null, cmds, indent);
	}

	protected Map<String, String> getCmdsDescription(String qname, String qkey, String[][] subcmds, boolean indent) {
		Map<String, String> map = new LinkedHashMap<>();
		if (subcmds!=null && subcmds.length>0) {
			for (String[] subcmd: subcmds) {
				String qname1 = (qname!=null ? qname + " " : "") + subcmd[0];
				String qkey1 = (qkey!=null ? qkey + "."  : "") + subcmd[0];
				String descr = resolve(getName() + "." + qkey1);
				map.put(qname1, descr);
			}
		}
		return map;
	}

	
	protected void printUsageDetails(String qcmd, String key, String[] alias, boolean sub) {
		String xkey = getName() + "." + key;
		String descr = resolve(xkey);
		System.out.println(String.format("[%s]  %s  --  %s", getName(), qcmd, descr));	

		if (alias!=null && alias.length>1) {
			String salias = String.join(" | ", alias);
			System.out.println(String.format("\n  Alias: %s", salias));								
		}		
		String descr2 = resolve(xkey + ".descr", false);
		if (descr2!=null) {
			System.out.println();
			print(2, descr2);
		}
		int width = 0;
		String args = resolve(xkey + ".args", false);
		StringBuilder asb = new StringBuilder();
		asb.append(qcmd);
		Map<String, String> argsDescr = new LinkedHashMap<>();
		if (args!=null) {
			String[] a = args.split(",");
			for (String arg: a) {
				arg = arg.trim();
				boolean optional = false;
				boolean rep = false;
				if (arg.endsWith("++")) {
					optional = true;
					rep = true;
					if (arg.length()==2) {
						continue;
					}
					arg = arg.substring(0, arg.length()-2);
				} else if (arg.endsWith("+")) {
					optional = true;
					if (arg.length()==1) {
						continue;
					}
					arg = arg.substring(0, arg.length()-1);
				}
				if (asb.length()>0) {
					asb.append(" ");
				}
				if (optional) {
					asb.append("[");
				} else {
					asb.append("<");
				}
				asb.append(arg);
				if (optional) {
					asb.append("]");
				} else {
					asb.append(">");					
				}
				if (rep) {
					asb.append("*");
				}
				if (arg.length()>width) {
					width = arg.length();
				}
				String karg = arg;
				int i = karg.indexOf("|");
				if (i>0) {
					karg = karg.substring(0, i).trim();
				}
				String descr3 = resolve(xkey + ".args." + karg, false);
				if (descr3!=null) {
					argsDescr.put(arg, descr3);
				}
			}
		}
		
		String options = resolve(xkey + ".options", false);
		Map<String, String> optionsDescr = new LinkedHashMap<>();
		StringBuilder sb = new StringBuilder();
		if (options!=null) {
			String[] a = options.split(",");
			for (String option: a) {
				boolean optional = false;
				boolean rep = false;
				if (option.endsWith("++")) {
					optional = true;
					rep = true;
					if (option.length()==2) {
						continue;
					}
					option = option.substring(0, option.length()-2);
				} else if (option.endsWith("+")) {
					optional = true;
					if (option.length()==1) {
						continue;
					}
					option = option.substring(0, option.length()-1);
				}
				int i = option.indexOf("=");
				String values = null;
				if (i>0) {
					if (i<option.length()-1) {
						values = option.substring(i+1);						
					}
					option = option.substring(0, i);
				}
				if (options.isEmpty()) {
					continue;
				}
				if (sb.length()>0) {
					sb.append("  |  ");
				}
				String[] aoption = option.split("\\|");
				StringBuilder sb2 = new StringBuilder();
				for (String option_: aoption) {
					option_ = option_.trim();
					if (option_.length()==1) {
						option_ = "-" + option_;
					} else {
						option_ = "--" + option_;
					}			
					if (sb2.length()>0) {
						sb2.append(" | ");
					}
					sb2.append(option_);						
				}
				if (sb2.length()>width) {
					width = sb2.length();
				}
				sb.append(sb2);
				if (values!=null && !values.trim().isEmpty()) {
					sb.append("=");
					sb.append(values);
				}
				if (optional) {
					
				}
				if (rep) {
					sb.append("*");
				}
				String koption = option;
				i = koption.indexOf("|");
				if (i>0) {
					koption = koption.substring(0, i).trim();
				}

				String descr3 = resolve(xkey + ".options." + koption, false);
				if (descr3!=null) {
					optionsDescr.put(option, descr3);
				}

			}
		}
		String extra = resolve(xkey + ".extra", false);
		System.out.println(String.format("\n  Usage: %s%s%s", asb, (sub ? " ..." : ""), extra!=null ? (" -- " + extra) : ""));					
		if (argsDescr.size()>0) {
			System.out.println("\n  Args:");
			for (Map.Entry<String, String> e: argsDescr.entrySet()) {
				String arg = e.getKey();
				System.out.println(String.format("  %" + (width>1 ? "-" + width : "") + "s   %s", arg, e.getValue()));
			}				
		}

		if (sb.length()>0) {
			System.out.println(String.format("\n  Options: %s", sb));					
		}
		if (optionsDescr.size()>0) {
			System.out.println();
			for (Map.Entry<String, String> e: optionsDescr.entrySet()) {
				String option = e.getKey();
				String[] aoption = option.split("\\|");
				StringBuilder sb2 = new StringBuilder();
				for (String option_: aoption) {
					option_ = option_.trim();
					if (option_.length()==1) {
						option_ = "-" + option_;
					} else {
						option_ = "--" + option_;
					}			
					if (sb2.length()>0) {
						sb2.append(" | ");
					}
					sb2.append(option_);						
				}
				System.out.println(String.format("  %" + (width>1 ? "-" + width : "") + "s   %s", sb2.toString(), e.getValue()));
			}				
		}

	}

	protected void print(int indent, String s) {
		if (indent<=0) {
			System.out.println(s);
			return;
		}
		String[] lines = s.split("\n");
		for (String line: lines) {
			System.out.println(String.format("%-" + indent + "s%s", "", line));
		}
	}
	protected String[] findCommand(String cmd) {
		String[][] cmds = getCommands();
		if (cmds!=null) {
			for (String[] alias: cmds) {
				if (StringUtil.contains(alias, cmd)) {
					return alias;
				}
			}
		}
		return null;
	}

	protected String[] findSubCommand(String cmd, String subcmd) {
		Map<String, String[][]> map = getSubCommands();
		if (map!=null) {
			String[] alias = findCommand(cmd);
			if (alias!=null && alias.length>0) {
				cmd = alias[0];
			}
			String[][] cmds = map.get(cmd);
			if (cmds!=null) {
				for (String[] alias2: cmds) {
					if (StringUtil.contains(alias2, subcmd)) {
						return alias2;
					}
				}
			}				
		}
		return null;
	}

	protected String[][] findSubSubCommands(String cmd, String subcmd) {
		Map<String, Map<String, String[][]>> map = getSubSubCommands();
		if (map!=null) {
			Map<String, String[][]> map2 = map.get(cmd);
			if (map2!=null) {
				String[][] cmds = map2.get(subcmd);
				return cmds;
			}
		}
		return null;
	}

	protected String[] findSubSubCommand(String cmd, String subcmd, String subsubcmd) {
		String[][] subsubcmds = findSubSubCommands(cmd, subcmd);
		if (subsubcmds!=null) {
			for (String[] alias: subsubcmds) {
				if (StringUtil.contains(alias, subsubcmd)) {
					return alias;
				}
			}
		}
		return null;
	}
	
	protected String getUsage(boolean alias) {
		StringBuilder sb = new StringBuilder();
		String[][] cmds = getCommands();
		if (cmds!=null) {
			for (String[] cmds_: cmds) {
				if (!alias) {
					if (sb.length()>0) {
						sb.append(" | ");
					}
					sb.append(cmds_[0]);
				} else {
					for (String cmd: cmds_) {
						if (sb.length()>0) {
							sb.append(" | ");
						}
						sb.append(cmd);
					}
				}
			}
		}
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
	public RestTemplate getTemplate() {
		return template;
	}


	/**
	 * Set the value of property {@code template}.
	 *
	 * @param template the value of {@code template}
	 */
	public void setTemplate(RestTemplate template) {
		this.template = template;
	}

	/**
	 * Get the value of property {@code runners}.
	 *
	 * @return the value of {@code runners}
	 */
	public List<CommandRunner> getRunners() {
		return runners;
	}


	/**
	 * Set the value of property {@code runners}.
	 *
	 * @param runners the value of {@code runners}
	 */
	@Override
	public void setRunners(List<CommandRunner> runners) {
		this.runners = runners;
	}

	@Override
	public Map<String, Object> getSettings() {
		return null;
	}

	@Override
	public void loadSettings(Map<String, Object> settings) {
	}

	protected void error(String msg, Object... args) {
		System.err.println(String.format(String.format("ERROR: [%s] %s", getName(), msg), args));
	}
	
	protected void singleuserNotSupported() {
		error("Single user mode not supported!");
	}

	protected void noresources(String type, Map<String, Object> args) {
		error("Resources not found: %s", type);		
	}

	protected void noresources(Map<String, Object> args) {
		noresources(type, args);
	}

	protected void operationFailed(String type, String op, Map<String, Object> args) {
		error("operation faield: %s %s", type, op);
	}

	protected void operationFailed(Map<String, Object> args) {
		operationFailed(args);
	}

	protected void invalidOp(String type, String op) {
		error("invalid operation: %s %s", type, op);
	}

	protected void invalidOp(String type) {
		error("invalid operation: %s", type);
	}

	protected void invalidOp() {
		invalidOp(type, op);
	}

	protected void invalidType(String type) {
		error("invalid resource type: %s", type);
	}

	protected void invalidType() {
		invalidType(type);
	}

	protected void missingArg(String name, String type, String opS) {
		error("missing argument %s in %s", name, type);
	}
	
	protected void missingArg(String name) {
		missingArg(name, type, op);
	}
	
	protected Object get(String name, Map<String, Object> map) {
		Object value = map.get(name);
		return value;
	}
	
	protected String getAsString(String name, Map<String, Object> map) {
		Object value = map.get(name);
		return (String)value;
	}
	
	
	//
	// Util
	//

	protected OAuth2RestTemplate makeOAuth2RestTemplate(ResourceOwnerPasswordResourceDetails resource, ConnectionConfiguration conncConfig) {
		DefaultOAuth2ClientContext context = new DefaultOAuth2ClientContext();
		OAuth2RestTemplate template = new OAuth2RestTemplate(resource, context);
		template.setRequestFactory(conncConfig.makeClientHttpRequestFactory());
		return template;
	}

	protected OAuth2RestTemplate makeOAuth2RestTemplate(ConnectionConfiguration conncConfig) {
		Sso sso = getSso();
		return makeOAuth2RestTemplate(sso.getRequiredResourceDetails(), conncConfig);
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

	@SuppressWarnings("unchecked")
	protected <T> T get(String name, Map<String, Object> map, T defaultValue, Class<T> type) {
		T value = (T)map.get(name);
		if (value==null) {
			value = defaultValue;
		} else if (!type.isAssignableFrom(value.getClass())) {
			if (value instanceof Map) {
				Object obj = MetaUtil.newInstance(type);
				MappingUtils.fromMap(obj, (Map<String,Object>)value);
				value = (T)obj;
			} else if (value instanceof String) {
				value = (T)MappingUtils.fromJson((String)value, type);
			}
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
		print(obj);
		System.out.println();
	}

	void println() {
		System.out.println();
	}

	void println(String s) {
		System.out.println(s);
	}

	@SuppressWarnings("rawtypes")
	void print(Object obj, int n) {
		if (obj instanceof Iterable) {
			print((Iterable)obj, null);
			return;
		}
		if (n>0) {
			System.out.print(String.format("%" + (n+1) + "s%s", "", format(obj, isBlock())));			
		} else {
			System.out.print(format(obj, isBlock()));
		}
	}

	void printW(Object obj, int n) {
		System.out.print(String.format("%" + (n>1 ? "-" + n : "") + "s", formatSimple(obj, isBlock())));		
	}

	void print(Page<?> page, Class<?> type) {
		print(page, type, isBlock());
	}


	void print(Page<?> page, Class<?> type, boolean silent) {
		if (page==null) {
			operationFailed(type.getSimpleName().toLowerCase(), "list", options);
			System.exit(-1);
			return;
		}
		if (page.getContent()==null || page.getContent().isEmpty()) {
			if (!silent) {
				noresources(type.getSimpleName().toLowerCase(), options);
				System.exit(0);				
			}
			return;
		}

		print(page.getContent(), type);
	}

	protected int size(Page<?> page) {
		if (page==null || page.getContent()==null) {
			return 0;
		}
		return page.getContent().size();
	}

	void print(Iterable<?> it, Class<?> type) {
		if (isBlock()) {
			if (type!=null) {
				printBlock(it, type);
			}
		} else if (isTabular()) {
			printTabular(it);
		} else {
			for (Object obj: it) {
				String s = format(obj, isBlock());
				print(s);
			}
		}

	}

	void printTabular(Iterable<?> it) {
		boolean ln = false;
		boolean hrule = false;
		String fmt = null;
		List<List<String>> table = new ArrayList<>();
		for (Object o: it) {
			if (fmt==null) {
				fmt = getCols(o);
			}
			List<String> row = getFields(o, fmt, false);
			table.add(row);
		}
		int[] widths = getColsWidth(table);
		String[] cols = getFormatCols(fmt);
		if (cols==null) {
			return;
		}
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
	}

	
	void printBlock(Iterable<?> it, Class<?> type) {
		String cols_ = getCols(type);
		String[] cols = getFormatCols(cols_);
		if (cols==null) {
			return;
		}
		int width = 0;
		for (int i=0; i<cols.length; i++) {
			if (cols[i].length()>width) {
				width = cols[i].length();
			}
		}
		int i = 0;
		for (Object o: it) {
			if (i>0) {
				System.out.println();				
			}
			List<String> row = getFields(o, cols_, true);
			for (int j = 0; j<cols.length; j++) {
				String col = formatColName(cols[j]);
				printW(col, width+3);
				printW(row.get(j), 0);
				System.out.println();
			}
			i++;
		}
	}

	protected void printObj(Object obj) {
		printObj(obj, true);
	}
	
	protected void printObj(Object obj, boolean header) {
		if (obj==null) {
			return;
		}
		if (isBlock()) {
			printBlock(obj);
		}  else if (isTabular()) {
			printTabular(obj, header);
		} else {
			print(obj, 0);
		}	
	}

	protected void printTabular(Object obj) {
		printTabular(obj, true);
	}

	protected void printTabular(Object obj, boolean header) {
		String cols = getCols(obj);
		List<String> values = getFields(obj, cols, false);
		String[] cols2 = getFormatCols(cols);
		int[] widths = new int[Math.max(values.size(), cols2.length)];
		for (int i=0; i<widths.length; i++) {
			widths[i] = i<cols2.length && i<values.size() ? Math.max(values.get(i).length(), cols2[i].length()) :
				i<cols2.length ? cols2[i].length() : values.get(i).length();
		}
		if (header) {
			for (int j = 0; j<cols2.length; j++) {
				String col = formatColName(cols2[j]);
				printW(col, widths[j]+3);
			}			
			System.out.println();
		}
		int j = 0;
		for (String value: values) {
			printW(value, widths[j]+3);
			j++;
		}
		System.out.println();
	}

	protected void printBlock(Object obj) {
		String cols = getCols(obj);
		List<String> values = getFields(obj, cols, true);
		String[] cols2 = getFormatCols(cols);
		int width = 0;
		for (int i=0; i<cols2.length; i++) {
			if (cols2[i].length()>width) {
				width = cols2[i].length();
			}
		}
		for (int j = 0; j<cols2.length; j++) {
			String col = formatColName(cols2[j]);
			printW(col, width+3);
			String value = values.get(j);
			printW(value, 0);
			System.out.println();
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
			ww[ww.length-1] = 1;
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
	
	protected boolean isBlock() {
		String o = getFormat();
		if (o==null) {
			return false;
		}
		switch (o) {
		case "block":
			return true;
		default: 
			break;
		}
		if (options.get("b")!=null) {
			return true;
		}
		return false;
	}

	String[] getFormatCols(String fmt) {
		if (!StringUtils.hasText(fmt)) {
			return null;
		}
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
		if (!StringUtils.hasText(fmt)) {
			return null;
		}
		String[] cols = fmt.split(",");
		for (int i =0; i<cols.length; i++) {
			String col = cols[i].trim();
			int j = col.indexOf(":");
			if (j>0 && j<col.length()-1) {
				cols[i] = col.substring(0, j);
			}
		}
		return cols;
	}

	String format(Object obj, boolean block) {
		if (obj==null) {
			return "";
		}
		String fmt = getFormat();
		if ("raw".equals(fmt)) {
			return obj.toString();
		} else if ("json".equals(fmt)) {
			String s = toJson(obj);
			return s;
		} else if ("yaml".equals(fmt)) {
			String s = toYaml(obj);
			return s;
		}
		String cols = getCols(obj);
		
		if (cols!=null && !cols.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			Map<String, Object> map = MappingUtils.toMap(obj);
			if (map==null) {
				sb.append("???");
			} else if (cols=="+") {
				for (Map.Entry<String, Object> e: map.entrySet()) {
					if (sb.length()>0) {
						sb.append(" ");						
					}
					sb.append(e.getValue());
				}
			} else {
				String[] props = getFormatProps(cols);
				for (String s: props) {
					if (!s.isEmpty()) {
						sb.append(" ");						
					}
					Object value = MapUtil.resolve(s, map);
					sb.append(formatSimple(value, block));						
				}
			}
			return sb.toString();
		} else {
			return formatSimple(obj, block);			
		}
	}

	String formatCols(Object obj, String fmt) {
		StringBuilder sb = new StringBuilder();
		Map<String, Object> map = MappingUtils.toMap(obj);
		if (map==null) {
			sb.append("???");
		} else if ("all".equals(fmt)) {
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
	
	List<String> getFields(Object obj, String cols, boolean block) {
		List<String> values = new ArrayList<>();
		Map<String, Object> map = MappingUtils.toMap(obj);
		if (map==null) {
		} else if ("all".equals(cols)) {
			for (Map.Entry<String, Object> e: map.entrySet()) {
				Object value = getPropertyValue(obj, e.getKey(), block);
				values.add(formatSimple(value, block));						
			}
		} else {
			String[] props = getFormatProps(cols);
			if (props!=null) {
				for (String s: props) {
					String ss[] = s.split("/");
					StringBuilder sb = new StringBuilder();
					for (String s1: ss) {
						Object value = getPropertyValue(obj, s1, block);
						String svalue = formatSimple(value, ss.length>1 ? "0" : EMPTY_VALUE, block);
						if (svalue!=null && !svalue.isEmpty()) {
							svalue = svalue.trim();
							if (!svalue.isEmpty()) {
								if (sb.length()>0) {
									sb.append("/");
								}
								sb.append(svalue);								
							}
						}
					}
					values.add(sb.toString());												
				}				
			}
		}
		return values;
	}

	public Object getPropertyValue(Object obj, String name, boolean block) {
		if (obj==null) {
			return null;
		}
		int i = name.indexOf(".");
		String prop = i<0 ? name : name.substring(0, i);
		Member member = MetaUtil.getPropertyMember(obj.getClass(), prop, false, false);
		if (member==null) {
			return null;
		}
		if (i<0) {
			return MetaUtil.getPropertyValue(obj, member);
		}
		Object obj_ = MetaUtil.getPropertyValue(obj, member);
		if (obj_==null) {
			return null;
		}
		if (obj_ instanceof Iterable) {
			StringBuilder sb = new StringBuilder();
			for (Object it: (Iterable<?>)obj_) {
				Object value = getPropertyValue(it, name.substring(i+1), block); //consume and recurse		
				String s = formatSimple(value, block);
				if (sb.length()>0) {
					sb.append(",");
				}
				sb.append(s);
			}
			return sb.toString();
		}
		return getPropertyValue(obj_, name.substring(i+1), block); //consume and recurse
	}

	public static final int MAX_COL_SIZE = 32;

	private String formatSimple(Object value, boolean block) {
		return formatSimple(value, EMPTY_VALUE, block);
	}

	private String formatSimple(Object value, String defaultValue, boolean block) {
		if (value==null) {
			return defaultValue;
		}
		if (value instanceof String) {
			if (((String) value).isEmpty()) {
				return defaultValue;	
			}
			return (String)value;
		}
		if (value.getClass().isEnum()) {
			Method method = MetaUtil.getGetter(value.getClass(), "displayValue");
			if (method==null) {
				method = MetaUtil.getGetter(value.getClass(), "displayName");
			}
			if (method!=null) {
				Object out = MetaUtil.invoke(value, method);
				if (out!=null) {
					value = out;
				}
			}
			return value.toString();
		}
		if (value instanceof Collection) {
			StringBuilder sb = new StringBuilder();
			int i = 0;
			for (Object item: (Collection<?>)value) {
				if (sb.length()>0) {
					sb.append(",");
				}
				String s = formatSimple(item, null, block);
				if (s==null || s.isEmpty()) {
					continue;
				}
				if (!block) {
					if (i>0 && sb.length()+s.length()+3>=MAX_COL_SIZE) {
						sb.append("...");
						break;
					}					
				}
				i++;
				if (s!=null && !s.isEmpty()) {
					sb.append(s);
				}
			}
			return sb.toString();
		}
		if (value.getClass().isArray()) {
			StringBuilder sb = new StringBuilder();
			Object[] a = (Object[])value;
			int length = Array.getLength(a);
			int j = 0;
			for (int i = 0; i < length; i ++) {
				if (sb.length()>0) {
					sb.append(",");
				}
				Object item = Array.get(a, i);
				String s = formatSimple(item, null, block);
				if (s==null || s.isEmpty()) {
					continue;
				}
				if (!block) {
					if (j>0 && sb.length()+s.length()+3>=MAX_COL_SIZE) {
						sb.append("...");
						break;
					}					
				}
				j++;
				if (s!=null && !s.isEmpty()) {
					sb.append(s);
				}
			}
			return sb.toString();
		}
		return value.toString();
	}

	protected void debug(String s, Object... args) {
		if (isDebug()) {
			System.out.println(String.format(s, args));
		}
	}

	protected void debug(int level, String s, Object... args) {
		if (isDebug(level)) {
			System.out.println(String.format(s, args));
		}
	}

	protected void info(String msg, Object... args) {
		System.out.println(String.format(String.format("[%s] %s", getName(), msg), args));
	}

	protected void println(String s, Object... args) {
		System.out.println(String.format(s, args));
	}

	protected boolean isDebug() {
		return isDebug(options);
	}
	
	protected boolean isDebug(Integer level) {
		return isDebug(level, options);
	}
	
	//
	// Format
	//

	protected boolean isEcho() {
		return hasFormat();
	}
	
	protected boolean hasFormat() {
		String o = (String)options.get("o");
		if (o!=null) {
			return true;
		}
		String b = (String)options.get("b");
		if (b!=null) {
			return true;
		}
		o = (String)options.get("O");
		if (o!=null) {
			return true;
		}
		return false;
	}

	protected String getFormat() {
		String o = (String)options.get("o");
		if (o!=null) {
			return o.toLowerCase();
		}
		return "";
	}

	protected String getCols() {
		String o = (String)options.get("O");
		if (o!=null) {
			return o.toLowerCase();
		}
		return "";
	}

	protected String getCols(Class<?> type) {
		String fmt = getFormat();
		String cols = getCols(fmt, getCols(), type);
		if (StringUtil.hasText(cols)) {
			return cols.trim();
		}	
		if ("wide".equals(fmt)) {
			fmt = getWideFormat(type);
		} 
		if (!StringUtil.hasText(fmt)) {
			fmt = getDefaultFormat(type);
		} 
		return fmt;
	}

	protected String getCols(Object obj) {
		if (obj==null) {
			return "";
		}
		return getCols(obj.getClass());
	}
	
	protected String getDefaultFormat(Class<? extends Object> type) {
		if (type.equals(PropertyInfo.class)) {
			return PROPINFO_DEFAULT_FORMAT;
		}
		return DEFAULT_FORMAT;
	}

	protected String getWideFormat(Class<? extends Object> type) {
		return getDefaultFormat(type);
	}
	
	protected <T> T convert(Object obj, Class<T> type) {
		Map<String, Object> map = MappingUtils.toMap(obj);
		return convert(map, type);
	}

	protected <T> T updateFrom(T obj, Map<String, Object> map) {
		if (obj!=null && map!=null) {
			Map<String, Object> props = extractProps(options, obj.getClass());
			MappingUtils.updateObjectFrom(obj, props);
		}
		return obj;
	}

	public <T> T convert(Map<String, Object> options, Class<T> type) {
		Map<String, Object> props = extractProps(options, type);
		return MappingUtils.convert(props, type);
	}
	
	@SuppressWarnings("unchecked")
	protected Map<String, Object> extractProps(Map<String, Object> map, Class<?> type) {
		Map<String, Object> props = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e: map.entrySet()) {
			String prop = e.getKey();
			int i = prop.indexOf(".");
			if (i>0) {
				String base = prop.substring(0, i);		
				Class<?> baseType = MetaUtil.getPropertyType(type, base);
				if (baseType!=null) {					
					Map<String, Object> map2 = null;
					Object obj = props.get(base);
					if (obj instanceof Map) {
						map2 = (Map<String, Object>) obj;
					}
					Map<String, Object> map3 = extractProps(removeKeyPrefix(map, base+"."), baseType);
					if (map2==null) {
						props.put(base, map3);
					} else {
						map2.putAll(map3);
					}
				}
			} else {
				Object value = e.getValue();
				Member member = MetaUtil.getPropertyMember(type, prop, false, false);
				if (member!=null) {
					prop = MetaUtil.getPropertyName(member);
					if (value!=null && !value.toString().isEmpty()) {
						Class<?> propType = MetaUtil.getPropertyType(type, prop);
						if (propType!=null) {
							if (propType.isEnum()) {
								String s = value.toString();
								Method parse = MetaUtil.getMethod(propType, "parse", 1, true);
								if (parse!=null) {
									Object out = MetaUtil.invoke(null, parse, new Object[] {s});
									if (out!=null) {
										value = out;
									}
								} else {
									value = parseEnum2(propType, s);
								}
							} else {
								if (conversionService.canConvert(value.getClass(), propType)) {
									try {
										value = conversionService.convert(value, propType);											
									} catch (ConversionException e_) {											
									}
								}
							}
						}
					} else {
						Class<?> propType = MetaUtil.getPropertyType(type, prop);
						if (propType!=null && (Boolean.TYPE.equals(propType) || Boolean.class.equals(propType))) {
							value = true;
						}
					}
				}
				props.put(prop, value);
			}

		}
		return props;
	}

	
	private Map<String, Object> removeKeyPrefix(Map<String, Object> map, String prefix) {
		Map<String, Object> map2 = new LinkedHashMap<>();
		if (map!=null) {
			for (Map.Entry<String, Object> e: map.entrySet()) {
				String key = e.getKey();
				if (key.startsWith(prefix)) {
					key = key.length()>prefix.length() ? key.substring(prefix.length()) : "";
					map2.put(key, e.getValue());
				}			
			}			
		}
		return map2;
	}

	protected Object parseEnum(Class<?> type, String name){
		for(Object e: type.getEnumConstants()) {
			if(((Enum<?>)e).name().equalsIgnoreCase(name)){
				return e;
			}
		}
		String name2 = name.replaceAll("-", "_");
		if (!name.equals(name2)) {
			Object value = parseEnum(type, name2);
			if (value!=null) {
				return value;
			}
		}
		return null;
	}
	
	protected Object parseEnum2(Class<?> type, String name){
		Object value = parseEnum(type, name);
		if (value==null) {
			name = name.toUpperCase();
			value = parseEnum(type, name);
		}
		return value;
	}
	
	protected String toYaml(Object obj) {
		if (obj==null) {
			return "";
		}
		if (obj instanceof String) {
			return (String)obj;
		}
		ObjectMapper mapper = new ObjectMapper(yamlFactory);
		String s;
		try {
			s = mapper.writeValueAsString(obj);
		} catch (IOException e) {
			s = null;
		}
		if (s==null) {
			s = "";
		}
		return s;
	}
	protected String toJson(Object obj) {
		if (obj==null) {
			return "";
		}
		if (obj instanceof String) {
			return (String)obj;
		}
		ObjectMapper mapper = new ObjectMapper();
		String s;
		try {
			s = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			s = null;
		}
		if (s==null) {
			s = "";
		}
		return s;
	}


	protected String getCols(String fmt, String cols, Class<? extends Object> type) {
		return cols;
	}
	
	protected void exit(int code) {
		if (interactive) {
			throw new InteractiveException();
		}
		System.exit(code);		
	}

	protected String argn(String op, String[] cmds, int index, boolean required) {
		String id = cmds.length > index ? cmds[index] : null;
		if (required && id==null) {
			error(String.format("missing argument"));
			exit(-1);
			return null;
		}
		return id;
	}
		

	protected String arg0(String op, String[] cmds, boolean required) {
		return argn(op, cmds, 0, required);
	}
	protected String argId(String op, String[] cmds, boolean required) {
		return arg0(op, cmds, required);
	}

	protected String argId(String op, String[] cmds, String defaultValue) {
		String value = argn(op, cmds, 0, false);
		if (value==null) {
			value = defaultValue;
		}
		if (value==null) {
			error(String.format("missing argument"));
			exit(-1);
		}
		return value;
	}

	protected String arg1(String op, String[] cmds, boolean required) {
		return argn(op, cmds, 1, required);
	}
	protected String arg2(String op, String[] cmds, boolean required) {
		return argn(op, cmds, 2, required);
	}
	protected String arg3(String op, String[] cmds, boolean required) {
		return argn(op, cmds, 3, required);
	}

	protected String argId(String op, String[] cmds) {
		return argId(op, cmds, true);
	}
	protected String arg0(String op, String[] cmds) {
		return arg0(op, cmds, true);
	}
	protected String arg1(String op, String[] cmds) {
		return arg1(op, cmds, true);
	}
	protected String arg2(String op, String[] cmds) {
		return arg2(op, cmds, true);
	}
	
	protected String argPID(Map<String, Object> options) {
		return null;
	}
	
	protected String argIdx(String op, String[] cmds) {
		return argIdx(op, cmds, 0);
	}

	protected String argIdx(String op, String[] cmds, boolean required) {
		return argIdx(op, cmds, 0, required);
	}
	
	protected String argIdx1(String op, String[] cmds) {
		return argIdx(op, cmds, 1);
	}


	protected String argIdx1(String op, String[] cmds, boolean required) {
		return argIdx(op, cmds, 1, required);
	}

	protected String argIdx(String op, String[] cmds, int index) {
		return argIdx(op, cmds, index, true);
	}
	
	protected String argIdx(String op, String[] cmds, int index, boolean required) {
		String id = cmds.length > index ? cmds[index] : null;
		if (id==null) {
			if (required) {
				missingResourceId(op);
			}
			return null;
		}
		return makeIdx(id);
	}

	protected void missingResourceId(String op) {
		if (StringUtil.hasText(op)) {
			error(String.format("missing resource id for %s", op));				
		} else {
			error(String.format("missing resource id"));		
		}
		exit(-1);
	}

	protected void missingResourceId() {
		error(String.format("missing resource id"));
		exit(-1);
	}

	protected String makeIdx(String id) {
		try {
			Long.parseLong(id);			
			return id;
		} catch (IllegalArgumentException e) {			
		}
		try {
			UUID.fromString(id);
			return id;
		} catch (IllegalArgumentException e) {			
		}
		if (id.indexOf("/")<0) {
			String pid = argPID(options);
			if (pid!=null) {
				id = pid.trim() + "/" + id.trim();
			}
		}
		return id;
	}

	
	protected void setId(EntityBase entity, String id) {
		try {
			Long.parseLong(id);			
			entity.setId(id);
			return;
		} catch (IllegalArgumentException e) {			
		}
		try {
			UUID.fromString(id);
			entity.setUuid(id);
		} catch (IllegalArgumentException e) {			
		}
		if (entity instanceof NamedEntity) {
			((NamedEntity)entity).setName(id);
			return;
		}
	}

	protected String extractId(URI uri) {
		return UriUtils.extractId(uri);
	}
	
	protected void writeConfig() {
		Sso sso = (Sso)getRunnerByName(Sso.SSO_NAME);
		sso.writeConfig();
	}
	
	protected boolean setupToken(String[] cmds, Map<String, Object> options) {
		Sso sso = (Sso)getRunnerByName(Sso.SSO_NAME);
		if (sso!=null && sso!=this) {
			return sso.setupToken(cmds, options);			
		}
		return false;
	}

	protected boolean setupToken() {
		if (options.get("h")!=null) {
			return true;
		}
		return setupToken(cmds, options);
	}

	protected Sso getSso() {
		return (Sso)getRunnerByName(Sso.SSO_NAME);
	}
	
	public static Map<String, String[][]> m(String key, Map<String, Map<String, String[][]>> parent) {
		Map<String, String[][]> map = new LinkedHashMap<>();
		parent.put(key, map);
		return map;
	}


	@Override
	public void setEndpoints(Map<String, Object> endpoints) {		
	}

	protected boolean view(String type, String id) {
		String url = makeUrl(false, type, id);
		if (url==null) {
			error("failed to build resource url: %s %s", type, id);
		}		
		return openBrowser(url);
	}

	protected boolean view(String ptype, String pid, String type, String id) {
		String url = makeUrl(false, ptype, pid, type, id);
		if (url==null) {
			error("failed to build resource url: %s %s %s %s", ptype, pid, type, id);
		}		
		return openBrowser(url);
	}

	protected String makeUrl(boolean api, String type, String id) {
		String server = getServer();
		if (server==null) {
			return null;
		}
		return PathUtil.concat(server, (api ? "/api" : "") + (isAdmin() ? "/admin" : "") + type + "/" + id);
	}
	
	protected String makeUrl(boolean api, String ptype, String pid, String type, String id) {
		String server = getServer();
		if (server==null) {
			return null;
		}
		return PathUtil.concat(server, (api ? "/api" : "") + (isAdmin() ? "/admin" : "") + ptype + "/" + pid + "/" + type + "/" + id);
	}


	protected String getServer() {
		return null;
	}

	protected boolean isAdmin() {
		return admin;
	}

	//
	// Schema
	//

	public void schema(Class<?> type) {
		if (isEcho()) {
			debug("Schema for: %s", type.getSimpleName());
		}
		List<Member> props = MetaUtil.collectAllPropertyMember(type, false);
		List<PropertyInfo> infos = new ArrayList<>();
		if (props!=null) {
			for (Member prop: props) {
				String name = MetaUtil.getPropertyName(prop);
				if (name!=null) {
					if (name.equals("class")) {
						continue;
					}
					Class<?> propType = MetaUtil.getPropertyType(prop);
					String descr = "";
					Class<?> declaredId = prop.getDeclaringClass();
					PropertyInfo info = new PropertyInfo(name, propType!=null ? propType.getSimpleName() : "", declaredId!=null ? declaredId.getSimpleName() :  "", descr);
					infos.add(info);
				}
			}
		}
		printTabular(infos);
	}
	

	public void schema(Class<?> type, Class<?> filterType, Class<?> optionsType, Map<String, Object> options) {
		if (options.get("f")!=null) {
			schema(filterType);
			return;
		}
		if (options.get("g")!=null) {
			schema(optionsType);
			return;
		}
		schema(type);
	}

	protected boolean isTrack() {
		return options.get("track")!=null;
	}
	
	protected <T> boolean track(T obj, TrackPredicate<T> predicate, int n, long initialDelay, long delay) {
		sleep(initialDelay);
		for (int i=0; i<n; i++) {
			if (predicate.test(obj, i)) {
				return true;
			}
			sleep(delay);
		}
		return false;
	}

	public static final int TRACK_MAX_STEPS = 10;
	public static final long TRACK_INITIAL_DELAY = 3*1000;
	public static final long TRACK_DELAY = 3*1000;
	
	public static interface TrackPredicate<T> {
		boolean test(T obj, int n);
	}
	
	protected <T> boolean track(T obj, TrackPredicate<T> predicate) {
		return track(obj, predicate, TRACK_MAX_STEPS, TRACK_INITIAL_DELAY, TRACK_DELAY);
	}

	protected <T> boolean track(T obj, TrackPredicate<T> predicate, long initialDelay) {
		return track(obj, predicate, TRACK_MAX_STEPS, initialDelay, TRACK_DELAY);
	}
	
	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
		}
	}

	public List<String> readIds(String file, int field) {
		return readFieldFromFile(file, field, new Function<String, String>() {
			public String apply(String value) {
				return normalizeValue(value);
			}
		});
	}
	
	public List<String> readFieldFromFile(String file, int field, Function<String, String> processor) {
		try {
			List<String> out = new ArrayList<>();
			List<String> lines = Files.readAllLines(Paths.get(file), StandardCharsets.UTF_8);
			if (field<0) {
				field = 0;
			}
			for (String line: lines) {
				line = line.trim();
				if (line.startsWith("#")) {
					continue;
				}
				String value = line;
				String[] fields = line.split(",");
				if (field>=fields.length) {
					continue;
				}
				value = fields[field];
				if (value==null) {
					continue;
				}
				if (processor!=null) {
					value = processor.apply(value);
				}
				if (value==null || value.isEmpty()) {
					continue;
				}
				out.add(value);
			}
			return out;
		} catch (IOException e) {
			error("Error reading file: %s", file);
			exit(-1);
			return null;
		}
	}
	

	public static String normalizeValue(String value) {
		if (value==null || value.isEmpty()) {
			return null;
		}
		value = value.trim();
		if (value.isEmpty()) {
			return null;
		}
		return value;
	}
}