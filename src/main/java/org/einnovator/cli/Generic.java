package org.einnovator.cli;

import java.util.Map;


public class Generic extends CommandRunnerBase {

	private static final String APP_NAME = "ei cli";
	private static final String APP_VERSION = "1.0";
	private static final String APP_COPYRIGHT = "(c) EInnovator 2020";
	private static final String APP_LICENSE = "Apache License";
	private static final String APP_SUPPORT = "support@einnovator.org";

	public static final String GENERIC_NAME = "generic";

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
		c("help")
	);
		
	@Override
	protected String[][] getCommands() {
		return DEVOPS_COMMANDS;
	}

	@Override
	public void run(String type, String op, String[] args, Map<String, Object> options) {
		setLine(type, op, cmds, options);
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
		case "version": 
			version(cmds, options);
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
		run("pwd", cmds, options, false);
	}

	private void cd(String[] cmds, Map<String, Object> options) {
		run("cd", cmds, options, false);
		if (isEcho()) {
			run("pwd", cmds, options, false);
		}
	}

	
	private void run(String cmd, String[] cmds, Map<String, Object> options, boolean nl) {
		boolean b = false;
		boolean first = true;
		for (CommandRunner runner: runners) {
			if (!runner.getClass().getSimpleName().equals(this.getClass().getSimpleName())) {
				if (runner.supports(cmd, options)) {
					if (!(runner instanceof Sso)) {
						runner.init(cmds, options, template, interactive, bundle);		
					}
					runner.run(cmd, op, cmds, options);
					b = true;
					first = false;
					if (!first && nl) {
						System.out.println();								
					}
				}
			}
		}
		if (!b) {
			for (CommandRunner runner: runners) {
				if (!runner.getClass().getSimpleName().equals(this.getClass().getSimpleName())) {
					if (runner.supports(cmd, null)) {
						if (!(runner instanceof Sso)) {
							runner.init(cmds, options, template, interactive, bundle);		
						}
						runner.run(cmd, op, cmds, options);
						first = false;
						if (!first && nl) {
							System.out.println();								
						}
					}
				}
			}
		}
	}


	private void version(String[] cmds, Map<String, Object> options) {
		System.out.println(String.format("%s - %s", APP_NAME, APP_VERSION));
		System.out.println(String.format("%s - %s - %s", APP_COPYRIGHT, APP_LICENSE, APP_SUPPORT));
	}

}
