package org.einnovator.cli;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;

import org.einnovator.devops.client.model.NamedEntity;
import org.einnovator.util.MapUtil;
import org.einnovator.util.MappingUtils;
import org.einnovator.util.StringUtil;
import org.einnovator.util.config.ConnectionConfiguration;
import org.einnovator.util.meta.MetaUtil;
import org.einnovator.util.model.EntityBase;
import org.springframework.data.domain.Page;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;


public abstract class CommandRunnerBase  extends RunnerBase implements CommandRunner {


	protected String[] cmds;
	protected Map<String, Object> options;
	protected OAuth2RestTemplate template;
	protected boolean interactive;
	protected boolean init;
	
	protected ResourceBundle bundle;

	protected static YAMLFactory yamlFactory = new YAMLFactory();

	
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


	protected String[][] getCommands() {
		return null;
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
	public void init(String[] cmds, Map<String, Object> options, OAuth2RestTemplate template, boolean interactive, ResourceBundle bundle) {
		super.init(interactive, bundle);
		this.cmds = cmds;
		this.options = options;
		this.template = template;
	}

	protected OAuth2RestTemplate makeOAuth2RestTemplate(ResourceOwnerPasswordResourceDetails resource,
			DefaultOAuth2ClientContext context, ConnectionConfiguration connection) {
		OAuth2RestTemplate template = new OAuth2RestTemplate(resource, context);
		if (connection!=null) {			
			template.setRequestFactory(connection.makeClientHttpRequestFactory());			
		}
		return template;
	}


	
	public boolean isHelp() {
		if (options.get("h")!=null) {
			return true;
		}
		return false;
	}
	public boolean isHelp(String type) {
		if (isHelp()) {
			printUsage(type);
			return true;
		}
		return false;		
	}

	public boolean isHelp(String type, String op) {
		if (isHelp()) {
			printUsage(type, op);
			return true;
		}
		return false;		
	}
	public boolean isHelp(String type, String op, String subcmd) {
		if (isHelp()) {
			printUsage(type, op, subcmd);
			return true;
		}
		return false;		
	}

	@Override
	public void printUsage() {
		String descr = resolve(getName());
		System.out.println(String.format("[%s] %s", getName(), descr));			
		String[][] cmds = getCommands();
		if (cmds!=null) {
			int width = 0;
			for (String[] cmds_: cmds) {
				if (cmds_[0].length()>width) {
					width = cmds_[0].length();
				}
			}
			for (String[] cmds_: cmds) {
				printUsage(cmds_[0], cmds_, width, false, true);	
			}

		}
		if (interactive) {
			System.out.println(String.format("\nUsage: [%s] %s", getName(), getUsage(false)));			
		} else {
			System.out.println(String.format("\nUsage: %s [%s] %s", CliRunner.CLI_NAME, getName(), getUsage(false)));			
		}
		exit(0);
	}

	public void printUsage(String cmd) {
		String[] alias = findCommand(cmd);
		printUsage(cmd, alias, 0, true, false);
	}

	public void printUsage(String cmd, String subcmd) {
		String[] alias = findSubCommand(cmd, subcmd);
		printUsage(cmd, subcmd, 0, alias, true, false);
	}

	protected void printUsage(String cmd, String[] alias, int width, boolean sub, boolean indent) {
		String[] alias0 = findCommand(cmd);
		String kcmd = cmd.replaceAll(" ", ".");
		if (alias0!=null && alias0.length>0) {
			kcmd = alias0[0];
		}
		String key = getName() + "." + kcmd;
		String descr = resolve(key);
		System.out.println(String.format((!sub && indent ? "  " : "") + (sub ? "[" + getName() + "] ":"") + (width>1 ? "%-"+width+"s" : "%s") + (!sub ? "   %s" : "  --  %s"), cmd, descr));	

		if (sub) {
			printUsageDetails(key, alias);			
			if (alias!=null && alias.length>0) {
				cmd = alias[0];
			}
			String[][] subcmds = getSubCommands(cmd);
			printCmds(cmd, subcmds, true);
		}
	}
	
	protected void printCmds(String prefix, String[][] subcmds, boolean indent) {
		if (subcmds!=null) {
			int width = 0;
			for (String[] subcmd: subcmds) {
				if (prefix.length() + 1 + subcmd[0].length()>width) {
					width = prefix.length() + 1 + subcmd[0].length();
				}
			}
			for (String[] subcmd: subcmds) {
				printUsage(prefix, subcmd[0], width, subcmd, false, true);
			}
		}
	}


	protected void printUsage(String cmd, String subcmd, String subsubcmd) {
		String key = getName() + "." + cmd.replaceAll(" ", ".") + "." + subcmd.replaceAll(" ", ".") + "." + subsubcmd.replaceAll(" ", ".");
		String descr = resolve(key);
		String qname = cmd + (!subcmd.isEmpty() ? " " + subcmd : "") + (!subsubcmd.isEmpty() ? " " + subsubcmd : "");
		System.out.println(String.format("[" + getName() + "] " + "%s" + "  --  %s", qname, descr));	
		printUsageDetails(key, null);
	}

	protected void printUsage(String cmd, String subcmd, int width, String[] alias, boolean sub, boolean indent) {
		String[] alias0 = findCommand(cmd);
		String kcmd = cmd.replaceAll(" ", ".");
		if (alias0!=null && alias0.length>0) {
			kcmd = alias0[0];
		}
		String ksubcmd = subcmd.replaceAll(" ", ".");
		if (alias!=null && alias.length>0) {
			ksubcmd = alias[0];
		}
		String key = getName() + "." + kcmd + "." + ksubcmd;
		String descr = resolve(key);
		String qname = cmd + (!subcmd.isEmpty() ? " " + subcmd : "");
		System.out.println(String.format((!sub && indent ? "  " : "") + (sub ? "[" + getName() + "] ":"") + (width>1 ? "%-"+width+"s" : "%s") + (!sub ? "   %s" : "  --  %s"), qname, descr));	

		if (sub) {
			printUsageDetails(key, alias);
			if (alias!=null && alias.length>0) {
				cmd = alias[0];
			}
			String[][] subcmds = findSubSubCommands(kcmd, subcmd);
			printCmds(kcmd + " " + ksubcmd, subcmds, true);
		}
	}
	
	protected void printUsageDetails(String key, String[] alias) {
		if (alias!=null && alias.length>1) {
			String salias = String.join(" | ", alias);
			System.out.println(String.format("  Alias: %s", salias));								
		}
		String descr2 = resolve(key + ".descr", false);
		if (descr2!=null) {
			System.out.println(String.format("\n  %s", descr2));
		}
		String options = resolve(key + ".options", false);
		if (options!=null) {
			StringBuilder sb = new StringBuilder();
			String[] a = options.split(",");
			Map<String, String> optionsDescr = new LinkedHashMap<>();
			int width = 0;
			for (String option: a) {
				int i = options.indexOf("=");
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
					sb.append(" | ");
				}
				if (option.length()==1) {
					sb.append("-");
				} else {
					sb.append("--");						
				}
				if (option.length()>width) {
					width = option.length();
				}
				sb.append(option);
				if (values!=null && !values.trim().isEmpty()) {
					sb.append("=");
					sb.append(values);
				}
				String descr3 = resolve(key + ".options." + option, false);
				if (descr3!=null) {
					optionsDescr.put(option, descr3);
				}

			}
			if (sb.length()>0) {
				System.out.println(String.format("\n  Options: %s", sb));					
			}
			if (optionsDescr.size()>0) {
				System.out.println();
				for (Map.Entry<String, String> e: optionsDescr.entrySet()) {
					String option = e.getKey();
					if (option.length()==1) {
						option = "-" + option;
					} else {
						option = "--" + option;
					}
					System.out.println(String.format("  %" + (width>1 ? "-" + width : "") + "s   %s", option, e.getValue()));
				}				
			}
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
			for (Map.Entry<String, String[][]> e: map.entrySet()) {
				String[][] cmds = e.getValue();
				if (cmds!=null) {
					for (String[] alias: cmds) {
						if (StringUtil.contains(alias, subcmd)) {
							return alias;
						}
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
		System.err.println(String.format("ERROR: " + msg, args));
	}

	protected void noresources(String type, Map<String, Object> args) {
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

	@SuppressWarnings("rawtypes")
	void print(Object obj, int n) {
		if (obj instanceof Iterable) {
			print((Iterable)obj);
			return;
		}
		if (n>0) {
			System.out.print(String.format("%" + (n+1) + "s%s", "", format(obj)));			
		} else {
			System.out.print(format(obj));
		}
	}

	void printW(Object obj, int n) {
		System.out.print(String.format("%" + (n>1 ? "-" + n : "") + "s", formatSimple(obj)));		
	}

	void print(Page<?> page, Class<?> type) {
		print(page, type.getSimpleName());
	}

	void print(Page<?> page, String type) {
		if (page==null) {
			operationFailed(type, "list", options);
			System.exit(-1);
			return;
		}
		if (page.getContent()==null || page.getContent().isEmpty()) {
			noresources(type, options);
			System.exit(0);
			return;
		}

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
			for (Object obj: it) {
				String s = format(obj);
				print(s);
			}
		}

	}

	void printObj(Object obj) {
		if (obj==null) {
			return;
		}
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
			String s = toJson(obj);
			return s;
		} else if ("yaml".equals(fmt)) {
			String s = toYaml(obj);
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
					sb.append(formatSimple(value));						
				}
			}
			return sb.toString();
		} else {
			return formatSimple(obj);			
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
	
	List<String> getFields(Object obj, String fmt) {
		List<String> values = new ArrayList<>();
		Map<String, Object> map = MappingUtils.toMap(obj);
		if (map==null) {
		} else if ("all".equals(fmt)) {
			for (Map.Entry<String, Object> e: map.entrySet()) {
				Object value = getPropertyValue(obj, e.getKey());
				values.add(formatSimple(value));						
			}
		} else {
			String[] props = getFormatProps(fmt);
			for (String s: props) {
				Object value = getPropertyValue(obj, s);
				values.add(formatSimple(value));						
			}
		}
		return values;
	}

	public Object getPropertyValue(Object obj, String name) {
		if (obj==null) {
			return null;
		}
		int i = name.indexOf(".");
		String prop = i<0 ? name : name.substring(0, i);
		Member member = MetaUtil.getPropertyMember(obj.getClass(), prop, false);
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
				Object value = getPropertyValue(it, name.substring(i+1)); //consume and recurse		
				String s = formatSimple(value);
				if (sb.length()>0) {
					sb.append(",");
				}
				sb.append(s);
			}
			return sb.toString();
		}
		return getPropertyValue(obj_, name.substring(i+1)); //consume and recurse
	}

	public static final int MAX_COL_SIZE = 32;
	
	private String formatSimple(Object value) {
		if (value==null) {
			return "";
		}
		if (value instanceof String) {
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
				String s = formatSimple(item);
				if (i>0 && sb.length()+s.length()+3>=MAX_COL_SIZE) {
					sb.append("...");
					break;
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
				String s = formatSimple(item);
				if (j>0 && sb.length()+s.length()+3>=MAX_COL_SIZE) {
					sb.append("...");
					break;
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

	protected void info(String s, Object... args) {
		System.out.println(String.format(s, args));
	}

	protected boolean isDebug() {
		String s = (String)options.get("debug");
		if (s!=null) {
			return true;
		}
		s = (String)options.get("v");
		if (s!=null) {
			return true;
		}
		return false;
	}
	
	//
	// Format
	//

	protected boolean isEcho() {
		return hasFormat();
	}
	
	protected boolean hasFormat() {
		String o = (String)options.get("o");
		if (o==null) {
			return false;
		}
		return true;
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
		String fmt = getFormat();
		if (obj!=null) {
			if (fmt==null || fmt.isEmpty()) {
				fmt = getDefaultFormat(obj.getClass());
			} else if ("wide".equals(fmt)) {
				fmt = getWideFormat(obj.getClass());
			}
			String fmt0 = getFormat(fmt, obj.getClass());
			if (fmt0!=null) {
				fmt = fmt0;
			}			
		}
		return fmt;
	}
	
	protected String getDefaultFormat(Class<? extends Object> type) {
		return "id,name";
	}

	protected String getWideFormat(Class<? extends Object> type) {
		return getDefaultFormat(type);
	}
	
	protected <T> T convert(Object obj, Class<T> type) {
		return MappingUtils.convert(obj, type);
	}

	protected <T> T convert(Map<String, Object> map, Class<T> type) {
		Map<String, Object> map2 = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e: map.entrySet()) {
			String prop = e.getKey();
			Object value = e.getValue();
			if (value!=null) {
				Class<?> propType = MetaUtil.getPropertyType(type, prop);
				if (propType!=null && propType.isEnum()) {
					String s = value.toString();
					Method parse = MetaUtil.getMethod(propType, "parse");
					if (parse!=null) {
						Object out = MetaUtil.invoke(value, parse, new Object[] {s});
						if (out!=null) {
							value = out;
						}
					} else {
						Object out = parseEnum2(propType, s);
						if (out!=null) {
							value = out;
						}
					}
				}

			}
			map2.put(e.getKey(), value);
		}
		return MappingUtils.convert(map2, type);
	}

	
	protected Object parseEnum(Class<?> type, String name){
	    for(Object e: type.getEnumConstants())
	        if(((Enum<?>)e).name().equals(name)){
	            return e;
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


	protected String getFormat(String fmt, Class<? extends Object> type) {
		return null;
	}
	
	protected Integer parseInt(String s) {
		try {
			return Integer.parseInt(s);			
		} catch (RuntimeException e) {
			return null;
		}
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
		
	protected String argId(String op, String[] cmds, boolean required) {
		return argn(op, cmds, 0, required);
	}
	protected String argId1(String op, String[] cmds, boolean required) {
		return argn(op, cmds, 1, required);
	}
	protected String argId2(String op, String[] cmds, boolean required) {
		return argn(op, cmds, 2, required);
	}

	protected String argName(String op, String[] cmds, boolean required) {
		return argn(op, cmds, 0, required);
	}

	protected String argName(String op, String[] cmds) {
		return argName(op, cmds, true);
	}

	protected String argId(String op, String[] cmds) {
		return argId(op, cmds, true);
	}
	protected String argId1(String op, String[] cmds) {
		return argId1(op, cmds, true);
	}
	protected String argId2(String op, String[] cmds) {
		return argId2(op, cmds, true);
	}
	
	protected String argPID(Map<String, Object> options) {
		return null;
	}
	
	protected String argIdx(String op, String[] cmds) {
		return argIdx(op, cmds, 0);
	}

	protected String argIdx1(String op, String[] cmds) {
		return argIdx(op, cmds, 1);
	}

	protected String argIdx(String op, String[] cmds, int index) {
		String id = cmds.length > index ? cmds[index] : null;
		if (id==null) {
			error(String.format("missing resource id"));
			exit(-1);
			return null;
		}
		return makeIdx(id);
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

	
	protected void writeConfig() {
		Sso sso = (Sso)getRunnerByName(Sso.SSO_NAME);
		sso.writeConfig();
	}
	
	
	public static String[] c(String... ss) {
		String[] a = new String[ss.length];
		int i = 0;
		for (String s: ss) {
			a[i] = s;
			i++;
		}
		return a;
	}

	public static String[][] c(String[]... sss) {
		String[][] a = new String[sss.length][];
		int i = 0;
		for (String[] ss: sss) {
			a[i] = ss;
			i++;
		}
		return a;
	}
	
	public static Map<String, String[][]> m(String key, Map<String, Map<String, String[][]>> parent) {
		Map<String, String[][]> map = new LinkedHashMap<>();
		parent.put(key, map);
		return map;
	}

}