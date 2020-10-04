package org.einnovator.cli;

import static org.springframework.util.StringUtils.hasText;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
		new SpringApplicationBuilder(CliRunner.class).web(false).run(args);
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
		String prefix = args[0];
		CommandRunner runner = getRunner(prefix);
		if (runner==null) {
			System.err.println("Unknow service: " + prefix);
			printUsage();
			System.exit(-1);
		}
		if (args.length==1) {
			System.err.println("Missing arguments...");
			runner.printUsage();
			System.exit(-1);
			return;
		}
		String type = args[1];
		type = type.toLowerCase();
		String op = args.length>1 && !args[2].startsWith("-")? args[2] : "";
		argsMap = makeArgsMap(args);


		System.out.println("Type: " + type + " ; Op: " + op + " ; Args:" + argsMap + " ; Runner:" + runner.getClass().getSimpleName());
		OAuth2RestTemplate template = null;
		if (!(runner instanceof Sso)) {
			CommandRunner runner2 = getRunner(Sso.SSO_PREFIX);
			runner2.init(argsMap, null);
			template = ((Sso)runner2).getTemplate();			
		}
		runner.init(argsMap, template);		

		runner.run(type, op, argsMap, args);

	}
	
	public CommandRunner getRunner(String prefix) {
		for (CommandRunner runner: runners) {
			String rprefix = runner.getPrefix();
			if (prefix.equalsIgnoreCase(rprefix)) {
				return runner;
			}
		}
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


	Map<String, Object> argsMap;

	
	private Map<String, Object> makeArgsMap(String[] args) {
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		int i0 = args.length>1 && !args[1].startsWith("-")? 2 : 1;
		for (int i=i0; i<args.length; i++) {
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
			}
		}
		return map;
	}
	
}
