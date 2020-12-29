package org.einnovator.cli;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Map.Entry;

import org.einnovator.util.script.TextTemplates;
import org.springframework.web.client.RestTemplate;


public class Generic extends CommandRunnerBase {

	private static final String APP_NAME = "ei cli";
	private static final String APP_VERSION = "1.0";
	private static final String APP_COPYRIGHT = "(c) EInnovator 2020";
	private static final String APP_LICENSE = "Apache License";
	private static final String APP_SUPPORT = "support@einnovator.org";

	public static final String GENERIC_NAME = "generic";

	private Map<String, Object> env = new LinkedHashMap<>();
	
	@Override
	public String getName() {
		return GENERIC_NAME;
	}

	protected boolean isNamed() {
		return false;
	}

	static String[][] DEVOPS_COMMANDS = c(
		c("ls", "list"),
		c("pwd"),
		c("cd"),
		c("version"),
		c("set"),
		c("echo"),
		c("exit"),
		c("help")
	);
		
	@Override
	protected String[][] getCommands() {
		return DEVOPS_COMMANDS;
	}

	@Override
	public void init(Map<String, Object> options, RestTemplate template, boolean interactive, ResourceBundle bundle) {
		super.init(options, template, interactive, bundle);
		if (env==null) {
			env = makeEnv(null);			
		}
	}
	

	@Override
	public void run(String type, String op, String[] cmds, String[] extra, Map<String, Object> options) {
		setLine(type, op, cmds, extra, options);
		switch (type) {
		case "help": case "":
			printUsageGlobal();
			return;
		}
		setupToken();

		switch (type) {
		case "ls": case "list":
			ls(cmds, options);
			break;
		case "pwd": 
			pwd(cmds, options);
			break;
		case "cd": 
			cd(cmds, options);
			break;
		case "set": 
			set(c(op,cmds), options);
			break;
		case "echo": 
			echo(c(op,cmds), options);
			break;
		case "version": 
			version(cmds, options);
			break;
		case "exit": 
			exit(c(op,cmds), options);
			break;
		default: 
			invalidOp(type, op);
			break;
		}

	}

	private void ls(String[] cmds, Map<String, Object> options) {
		run("ls", cmds, options, isHelp());
	}

	private void pwd(String[] cmds, Map<String, Object> options) {
		run("pwd", cmds, options, isHelp());
	}

	private void cd(String[] cmds, Map<String, Object> options) {
		run("cd", cmds, options, isHelp());
		if (options.get("w")!=null) {
			run("pwd", cmds, options, false);
		}
	}

	private void set(String[] cmds, Map<String, Object> options) {
		if (cmds.length==0) {
			error("missing variable name in set");
			exit(-1);
			return;
		}
		String name = cmds[0];
		String value = cmds.length>1 ? cmds[1] : "";
		env.put(name, value);
	}

	private void echo(String[] cmds, Map<String, Object> options) {
		if (cmds.length==0) {
			error("missing variable name in set");
			exit(-1);
			return;
		}
		StringBuilder sb = new StringBuilder();
		for (String s: cmds) {
			if (sb.length()>0) {
				sb.append(" ");
			}
			s = s.trim();
			s = expand(s, env);
			sb.append(s);
		}
		System.out.println(sb.toString());
	}

	private void exit(String[] cmds, Map<String, Object> options) {
		System.exit(0);
	}

	private void run(String cmd, String[] cmds, Map<String, Object> options, boolean nl) {
		boolean b = false;
		boolean first = true;
		for (CommandRunner runner: runners) {
			if (!runner.getClass().getSimpleName().equals(this.getClass().getSimpleName())) {
				if (runner.supports(cmd, options) || (options.get("h")!=null && runner.supports(cmd))) {
					if (!(runner instanceof Sso)) {
						runner.init(options, template, interactive, bundle);		
					}
					if (!first && nl) {
						System.out.println();								
					}
					runner.run(cmd, op, cmds, null, options);
					b = true;
					first = false;
				}
			}
		}
		if (!b) {
			for (CommandRunner runner: runners) {
				if (!runner.getClass().getSimpleName().equals(this.getClass().getSimpleName())) {
					if (runner.supports(cmd, null)) {
						if (!(runner instanceof Sso)) {
							runner.init(options, template, interactive, bundle);		
						}
						if (!first && nl) {
							System.out.println();								
						}
						runner.run(cmd, op, cmds, null, options);
						first = false;
					}
				}
			}
		}
	}


	private void version(String[] cmds, Map<String, Object> options) {
		System.out.println(String.format("%s - %s", APP_NAME, APP_VERSION));
		System.out.println(String.format("%s - %s - %s", APP_COPYRIGHT, APP_LICENSE, APP_SUPPORT));
	}

	private TextTemplates templates;

	private Map<String, Object> makeEnv(Map<String, Object> env) {
		if (env==null) {
			env = new LinkedHashMap<>();
		}
		for (Map.Entry<String, String> e: System.getenv().entrySet()) {
			env.put(e.getKey(), e.getValue());
		}
		for (Entry<Object, Object> e: System.getProperties().entrySet()) {
			env.put(e.getKey().toString(), e.getValue());
		}
		
		return env;
	}

	@Override
	public String expand(String s, Map<String, Object> env) {
		if (templates==null) {
			templates = new TextTemplates();			
		}
		String out = templates.expand(s, env);
		return out;
	}

	@Override
	protected Map<String, Object> getEnv() {
		return env;
	}

}
