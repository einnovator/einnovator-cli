package org.einnovator.cli;

import java.io.IOException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;

import org.bouncycastle.util.Arrays;
import org.einnovator.devops.client.model.NamedEntity;
import org.einnovator.util.MapUtil;
import org.einnovator.util.MappingUtils;
import org.einnovator.util.StringUtil;
import org.einnovator.util.config.ConnectionConfiguration;
import org.einnovator.util.meta.MetaUtil;
import org.einnovator.util.model.EntityBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;


public abstract class CommandRunnerBase implements CommandRunner {


	protected String[] cmds;
	protected Map<String, Object> options;
	protected OAuth2RestTemplate template;
	protected boolean interactive;
	protected boolean init;
	
	protected ResourceBundle bundle;

	protected static YAMLFactory yamlFactory = new YAMLFactory();

	@Autowired
	protected List<CommandRunner> runners;
	
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

	@Override
	public void init(String[] cmds, Map<String, Object> options, OAuth2RestTemplate template, boolean interactive, ResourceBundle bundle) {
		this.cmds = cmds;
		this.options = options;
		this.template = template;
		this.interactive = interactive;
		this.bundle = bundle;
	}


	@Override
	public void printUsage() {
		String[][] cmds = getCommands();
		if (cmds!=null) {
			String descr = resolve(getPrefix());
			System.out.println(String.format("[%s] %s", descr));			
			for (String[] cmds_: cmds) {
				descr = resolve(cmds_);
				System.out.println(String.format("  %s %s", cmds_[0], descr));	
				if (cmds_.length>1) {
					String alias = String.join("|", cmds_);
					alias = alias.substring(alias.indexOf("|"));
					System.out.println(String.format("  %s", alias));								
				}
	
			}
		}
		if (interactive) {
			System.out.println(String.format("usage: %s %s", getPrefix(), getUsage()));			
		} else {
			System.out.println(String.format("usage: %s %s %s", CliRunner.CLI_NAME, getPrefix(), getUsage()));			
		}
		exit(0);
	}
	
	private String resolve(String[] cmds_) {
		for (String cmd: cmds_) {
			String s = resolve(cmd);
			if (StringUtil.hasText(s)) {
				return s.trim();
			}
		}
		return "";
	}


	private String resolve(String key) {
		if (bundle!=null) {
			try {
				String s = bundle.getString(key);						
				return s;
			} catch (RuntimeException e) {
			}			
		}
		return "?" + key + "?";
	}


	protected String getUsage() {
		StringBuilder sb = new StringBuilder();
		String[][] cmds = getCommands();
		if (cmds!=null) {
			int i = 0;
			for (String[] cmds_: cmds) {
				int j = 0;
				for (String cmd: cmds_) {
					sb.append(cmd);
					if (sb.length()>0) {
						sb.append(" | ");
					}
					j++;
				}
				i++;
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
		return s!=null;
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
		if (fmt==null || fmt.isEmpty()) {
			fmt = getDefaultFormat(obj.getClass());
		} else if ("wide".equals(fmt)) {
			fmt = getWideFormat(obj.getClass());
		}
		String fmt0 = getFormat(fmt, obj.getClass());
		if (fmt0!=null) {
			fmt = fmt0;
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
		String pid = argPID(options);
		if (pid!=null) {
			if (id.indexOf("/")<0) {
				id = pid + "/" + id;
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

	
}