package org.einnovator.cli;

import java.io.Console;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

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

	private static boolean START_WITH_BOOT = false;
	
	
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
		boolean boot = START_WITH_BOOT;
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
			printUsageGlobal();
			exit(-1);
		}
		List<String> cmds = new ArrayList<>();
		options = makeArgsMap(args, cmds);
	
		Sso sso = (Sso)getRunnerByName(Sso.SSO_NAME);
		sso.setup(options);
		
		if (options.get("i")!=null) {
			runConsole(args);
			return;
		}
		if (cmds.size()==0) {
			System.err.println("Missing arguments...");
			printUsageGlobal();
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
			printUsageGlobal();
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
			printUsageGlobal();
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
						String s = a.substring(1);
						if (s.startsWith("=")) {
							s = s.length()>1 ? s.substring(1) : "";
						}
						map.put(a.substring(0,1), s);
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
	
	private void debug(String s, Object... args) {
		if (isDebug()) {
			System.out.println(String.format(s, args));
		}
	}
	
	private boolean isDebug() {
		String s = (String)options.get("v");
		return s!=null;
	}


}
