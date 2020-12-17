package org.einnovator.cli;

import static org.springframework.util.StringUtils.hasText;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class CliRunner {

	public static String CLI_NAME = "ei";

	private final Log logger = LogFactory.getLog(getClass());

	@Autowired
	private List<CommandRunner> runners;
	
	DefaultOAuth2ClientContext context;
	OAuth2RestTemplate template;
	
	public static void main(String[] args) {
		new SpringApplicationBuilder(CliRunner.class).bannerMode(Mode.OFF).logStartupInfo(false).web(false).run(args);
	}
	
	@PostConstruct
	public void init() {
		List<CommandRunner> runners2 = new ArrayList<CommandRunner>();
		for (CommandRunner runner: runners) {
			String prefix = runner.getPrefix();
			if (!hasText(prefix)) {
				logger.warn(CommandRunner.class.getSimpleName() + " missing prefix");
				continue;
			}
			runners2.add(runner);
		}
		runners = runners2;
	}
	
	@Component
	@Profile("!test")
	public class CommandLiner implements CommandLineRunner {

		@Override
		public void run(String... args) throws Exception {
			dispatch(args);
		}

	}
	
	public void dispatch(String... args) {
		if (args.length==0) {
			printUsage();
			System.exit(-1);
		}
		List<String> cmds = new ArrayList<>();
		options = makeArgsMap(args, cmds);
		if (cmds.size()==0) {
			System.err.println("Missing arguments...");
			printUsage();
			System.exit(-1);
			return;
		}
		String prefix = cmds.get(0);
		CommandRunner runner = getRunnerByName(prefix);
		if (runner!=null) {
			if (cmds.size()==1) {
				System.err.println("Missing command...");
				printUsage();
				System.exit(-1);
			}
			cmds.remove(0);
		} else {
			runner = getRunnerByCommand(prefix);
		}
		if (runner==null) {
			System.err.println("Unknow service: " + prefix);
			printUsage();
			System.exit(-1);
		}
		if (args.length==0) {
			System.err.println("Missing arguments...");
			runner.printUsage();
			System.exit(-1);
			return;
		}
		String type = cmds.get(0).toLowerCase();
		cmds.remove(0);
		String op = "";
		if (cmds.size()>0) {
			op = cmds.get(0).toLowerCase();
			cmds.remove(0);
		}

		debug("Type: " + type + " ; Op: " + op + " ; Args:" + options + " ; Runner:" + runner.getClass().getSimpleName());

		String[] cmds_ = cmds.toArray(new String[cmds.size()]);

		OAuth2RestTemplate template = null;

		Sso sso = (Sso)getRunnerByName(Sso.SSO_PREFIX);
		sso.setup(options);
		setupEndpoints(sso.getAllEndpoints());
		
		if (!(runner instanceof Sso)) {
			sso.init(cmds_, options, null);
			template = sso.getTemplate();			
		}
		runner.init(cmds_, options, template);		

		runner.run(type, op, cmds_, options);

	}
	
	public CommandRunner getRunnerByName(String name) {
		for (CommandRunner runner: runners) {
			String rprefix = runner.getPrefix();
			if (name.equalsIgnoreCase(rprefix)) {
				return runner;
			}
		}
		return null;
	}

	public CommandRunner getRunnerByCommand(String prefix) {
			for (CommandRunner runner: runners) {
			if (runner.supports(prefix)) {
				return runner;
			}
		}
		return null;
	}
	
	public void printUsage() {
		StringBuilder sb = new StringBuilder();
		sb.append("usage: ");
		sb.append(CLI_NAME);
		int i = 0;
		for (CommandRunner runner: runners) {
			String prefix = runner.getPrefix();
			sb.append(" ");								
			if (i>0) {
				sb.append("| ");				
			}
			sb.append(prefix);
			i++;
		}
		sb.append(" args... [-option value]* [--options==value]*");				
		System.err.println(sb.toString());
	}


	Map<String, Object> options;

	
	private Map<String, Object> makeArgsMap(String[] args, List<String> cmds) {
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		for (int i=0; i<args.length; i++) {
			String a = args[i];
			if (a.startsWith("--")) {
				if (a.length()>2) {
					a = a.substring(2);
					int j = a.indexOf("=");
					if (j>0) {
						map.put(a.substring(0,j), j<a.length()-1 ? a.substring(j+1) : "");
					} else if (j<0) {
						map.put(a, "");						
					} else {
						System.err.println("ERROR: missing name before =");
						System.exit(1);
					}
				}
			} else if (a.startsWith("-")) {
				if (a.length()>1) {
					a = a.substring(1);
					//if (a.length()>1) {
					//	map.put(a.substring(0,1), a.substring(1));
					//} else
					if (i<args.length-1) {
						map.put(a, args[i+1]);
						i++;
					} else {
						map.put(a, "");
					}
				} else {
					System.err.println("ERROR: missing name after -");
					System.exit(1);					
				}
			} else {
				cmds.add(a);
			}
		}
		return map;
	}
	
	private void debug(String... s) {
		if (isDebug()) {
			System.out.println(s);
		}
	}
	
	private boolean isDebug() {
		String s = (String)options.get("debug");
		return s!=null;
	}
	

	public void setupEndpoints(Map<String, Object> endpoints) {
		if (endpoints!=null) {
			for (CommandRunner runner: runners) {
				String name = runner.getPrefix();
				@SuppressWarnings("unchecked")
				Map<String, Object> endpoints1 = (Map<String, Object>)endpoints.get(name);
				if (endpoints1!=null) {
					runner.setEndpoints(endpoints1);
				}
			}			
		}
	}
	
}
