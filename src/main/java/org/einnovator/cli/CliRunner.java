package org.einnovator.cli;

import java.io.Console;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.einnovator.util.StringUtil;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

//@SpringBootApplication
@Component
public class CliRunner extends RunnerBase {

	public static String CLI_NAME = "ei";

	private final Log logger = LogFactory.getLog(getClass());

	
	static long t0, t1;
	static boolean tcli;
	
	public CliRunner() {
	}
	
	//@Autowired
	public CliRunner(List<CommandRunner> runners) {
		this.runners = runners;
	}

	
	public static void main(String[] args) {
		Map<String, Object> options = makeArgsMap(args, null, false);
		t0 = System.currentTimeMillis();
		String t = (String)options.get("t");
		if ("cli".equals(t)) {
			tcli = true;
			System.out.println("Starting...");
		}
		boolean boot = true;
		if (boot) {
			new SpringApplicationBuilder(CliRunner.class).bannerMode(Mode.OFF).logStartupInfo(false).web(false).run(args);			
		} else {
			CliRunner cli = new CliRunner();
			String[] loggers = { "org.apache.commons.beanutils.converters.ArrayConverter"};
			for (String logger : loggers) {
				org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(logger);
				log.setLevel(org.apache.log4j.Level.OFF);
			}
			cli.init();
			cli.dispatch(args);
		}
	}
	
	@PostConstruct
	public void init() {
		
		t1 = System.currentTimeMillis();
		if (tcli) {
			System.out.println(String.format("Init: %sms", t1-t0));
		}
		if (runners==null) {
			AppConfig config = new AppConfig();
			runners = config.getAllRunners();
		}
		for (CommandRunner runner: runners) {
			runner.setRunners(runners);
		}
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
		long t2 = System.currentTimeMillis();
		if (tcli) {
			System.out.println(String.format("Init: %sms %sms", t2-t0, t2-t1));
		}

		if (args.length==0) {
			printUsage();
			exit(-1);
		}
		List<String> cmds = new ArrayList<>();
		options = makeArgsMap(args, cmds);
	
		Sso sso = (Sso)getRunnerByName(Sso.SSO_NAME);
		sso.setup(options);
		setupEndpoints(sso.getAllEndpoints());
		
		if (options.get("i")!=null) {
			runConsole(args);
			return;
		}
		if (cmds.size()==0) {
			System.err.println("Missing arguments...");
			printUsage();
			exit(-1);
			return;
		}
		run(args);
	}
	
	public void run(String... args) {
		List<String> cmds = new ArrayList<>();
		options = makeArgsMap(args, cmds);
		if (cmds.size()==0) {
			System.err.println("Missing arguments...");
			printUsage();
			exit(-1);
			return;
		}
		String name = cmds.get(0);
		CommandRunner runner = getRunnerByName(name);
		if (runner!=null) {
			if (cmds.size()==1) {
				System.err.println(String.format("Missing command for %s!", runner.getName()));
				runner.printUsage();
				exit(-1);
			}
			cmds.remove(0);
		} else {
			runner = getRunnerByCommand(name);
		}
		if (runner==null) {
			System.err.println("Unknow service: " + name);
			printUsage();
			exit(-1);
		}
		if (args.length==0) {
			System.err.println("Missing arguments...");
			runner.printUsage();
			exit(-1);
			return;
		}
		String op = "";
		String type = cmds.get(0).toLowerCase();
		cmds.remove(0);
		if (cmds.size()>0) {
			op = cmds.get(0).toLowerCase();
			cmds.remove(0);
		}

		debug("Type: " + type + " ; Op: " + op + " ; Args:" + options + " ; Runner:" + runner.getClass().getSimpleName());
		String[] cmds_ = cmds.toArray(new String[cmds.size()]);

		RestTemplate template = null;

		Sso sso = (Sso)getRunnerByName(Sso.SSO_NAME);
		bundle = getResourceBundle();
		if (!(runner instanceof Sso)) {
			sso.init(cmds_, options, null, interactive, bundle);
			template = sso.getTemplate();			
		} else {
			sso.setLine(type, op, cmds_, options);
		}
		runner.init(cmds_, options, template, interactive, bundle);		

		try {
			runner.run(type, op, cmds_, options);			
		} catch (InteractiveException e) {
		} catch (HttpStatusCodeException e) {
			if (options.get("dump")!=null) {
				e.printStackTrace();
			}
			if (e.getStatusCode()==HttpStatus.NOT_FOUND) {
				error(String.format("not found!"));
			} else {
				error(String.format("%s", StringUtil.capitalize(StringUtil.toWords(e.getStatusText().toLowerCase()))));				
			}		
		} catch (RuntimeException e) {
			if (options.get("dump")!=null) {
				e.printStackTrace();
			}			
			error(e);
		}

	}
	
	public static final String PROMPT = "ei>";
	private void runConsole(String[] args) {
		interactive = true;
		initConsole(args);
        do {
            String line = readLine();		
            if (!StringUtil.hasText(line)) {
            	continue;
            }
            String[] args1 = line.split(" "); 
            if (args1.length==0) {
            	continue;
            }
            String op = args1[0];
            if (!StringUtil.hasText(op)) {
            	continue;
            }
            switch (op) {
            case "exit": case "\\q":
            	exit(-1);
            	break;
            default:
                run(args1);
            }
    	} while (true);
	}
	
	Console console = null;

	public void initConsole(String[] args) {
		console = System.console();
        if (console == null) {
            error("No console: not in interactive mode!");
            exit(0);
        }
	}
	public String readLine() {
		String line = console.readLine(PROMPT);
		return line;
	}

	
	public void printUsage() {
		StringBuilder sb = new StringBuilder();
		sb.append("usage: ");
		sb.append(CLI_NAME);
		int i = 0;
		for (CommandRunner runner: runners) {
			String name = runner.getName();
			if (name==null || name.isEmpty()) {
				continue;
			}
			sb.append(" ");								
			if (i>0) {
				sb.append("| ");				
			}
			sb.append(name);
			i++;
		}
		sb.append(" args... [-option value]* [--options==value]*");				
		System.err.println(sb.toString());
	}


	Map<String, Object> options;


	private Map<String, Object> makeArgsMap(String[] args, List<String> cmds) {
		return makeArgsMap(args, cmds, interactive);
	}

	private static Map<String, Object> makeArgsMap(String[] args, List<String> cmds, boolean interactive) {
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
						if (!interactive) {
							System.exit(-1);
						}
					}
				}
			} else if (a.startsWith("-")) {
				if (a.length()>1) {
					a = a.substring(1);
					if (a.length()>1) {
						map.put(a.substring(0,1), a.substring(1));
					} else if (i<args.length-1) {
						if (!args[i+1].startsWith("-")) {
							map.put(a, args[i+1]);
							i++;							
						} else {
							map.put(a, "");							
						}
					} else {
						map.put(a, "");
					}
				} else {
					System.err.println("ERROR: missing name after -");
					if (!interactive) {
						System.exit(-1);
					}
				}
			} else {
				if (cmds!=null) {
					cmds.add(a);					
				}
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
		String s = (String)options.get("v");
		return s!=null;
	}
	

	public void setupEndpoints(Map<String, Object> endpoints) {
		if (endpoints!=null) {
			for (CommandRunner runner: runners) {
				String name = runner.getName();
				@SuppressWarnings("unchecked")
				Map<String, Object> endpoints1 = (Map<String, Object>)endpoints.get(name);
				if (endpoints1!=null) {
					runner.setEndpoints(endpoints1);
				}
			}			
		}
	}

}
