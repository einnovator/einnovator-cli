package org.einnovator.cli;

import java.util.Map;

import org.einnovator.util.MappingUtils;
import org.einnovator.util.StringUtil;


public abstract class CommandRunnerBase implements CommandRunner {


	protected Map<String, Object> argsMap;


	@Override
	public boolean supports(String cmd) {
		return StringUtil.containsIgnoreCase(getCommands(), cmd);
	}


	protected String[] getCommands() {
		return null;
	}

	@Override
	public void init0(Map<String, Object> argsMap) {
		this.argsMap = argsMap;
	}

	@Override
	public void init(Map<String, Object> argsMap) {
		this.argsMap = argsMap;
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
	
	
	protected void invalidOp(String type, String op) {
		System.err.println(String.format("ERROR: invalid operation: %s %s", type, op));
	}

	protected void invalidType(String type) {
		System.err.println(String.format("ERROR: invalid resource type: %s", type));
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

	
	
}