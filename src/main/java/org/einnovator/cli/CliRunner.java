package org.einnovator.cli;

import java.io.Console;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.einnovator.util.ResourceUtils;
import org.einnovator.util.StringUtil;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
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
	private static boolean AUTOCOMPLETE = true;

	
	static long t0, t1;
	static boolean tcli;
	
	public CliRunner() {
	}
	
	//@Autowired
	public CliRunner(List<CommandRunner> runners) {
		this.runners = runners;
	}

	
	public static void main(String[] args) {
		JavaUtil.disableAccessWarnings();
		Map<String, Object> options = makeArgsMap(args, null, null, false);
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
		List<String> extra = new ArrayList<>();		
		options = makeArgsMap(args, cmds, extra);
	
		Sso sso = (Sso)getRunnerByName(Sso.SSO_NAME);
		sso.setup(options);
		
		if (options.get("i")!=null) {
			printUsageGlobal(true, true, false);
			runConsole(args);
			return;
		}
		String source = (String)options.get("e"); 
		if (source!=null) {
			runScript(source);
			return;
		}
		String file = (String)options.get("E"); 
		if (file!=null) {
			runScriptFile(file);
			return;
		}

		if (cmds.size()==0) {
			System.err.println("Missing arguments...");
			if (!interactive) {
				printUsageGlobal();
			}
			exit(-1);
			return;
		}
		run(args);
	}
	
	public void run(String... args) {
		List<String> cmds = new ArrayList<>();
		List<String> extra = new ArrayList<>();
		options = makeArgsMap(args, cmds, extra);
		if (cmds.size()==0) {
			System.err.println("Missing arguments...");
			if (!interactive) {
				printUsageGlobal();
				exit(-1);
			}
			return;
		}
		String name = cmds.get(0);
		if (name.startsWith("!")) {
			if (name.length()>1) {
				name = name.substring(1);
				args[0] = name;
			}
			exec(args);
			return;
		}
		CommandRunner runner = getRunnerByName(name);
		if (runner!=null) {
			if (cmds.size()==1) {
				System.err.println(String.format("Missing command for %s!", runner.getName()));
				if (!interactive) {
					runner.printUsage();
					exit(-1);
				}
				return;
			}
			cmds.remove(0);
		} else {
			runner = getRunnerByCommand(name);
		}
		if (runner==null) {
			System.err.println("Unknow service or command: " + name);
			if (!interactive) {
				printUsageGlobal();				
				exit(-1);
			}
			return;
		}
		if (args.length==0) {
			System.err.println("Missing arguments...");
			if (!interactive) {
				runner.printUsage();
				exit(-1);
			}
			return;
		}
		String op = "";
		String type = cmds.get(0).toLowerCase();
		cmds.remove(0);
		if (cmds.size()>0) {
			op = cmds.get(0).toLowerCase();
			cmds.remove(0);
		}

		String[] cmds_ = cmds.toArray(new String[cmds.size()]);
		String[] extra_ = extra.toArray(new String[extra.size()]);

		debug(1, "Type: %s  ; Op: %s ; Args: %s ; Extra: %s ; Options: %s ; Runner: %s", type, op, Arrays.toString(cmds_), Arrays.toString(extra_), options, runner.getName());

		RestTemplate template = null;

		Sso sso = (Sso)getRunnerByName(Sso.SSO_NAME);
		bundle = getResourceBundle();
		if (!(runner instanceof Sso)) {
			sso.init(options, null, interactive, bundle);
			template = sso.getTemplate();			
		} else {
			sso.setLine(type, op, cmds_, extra_, options);
		}
		runner.init(options, template, interactive, bundle);		

		try {
			runner.run(type, op, cmds_, extra_, options);			
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
			line = line.trim();
			if (line.startsWith("#")) {
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
				System.exit(-1);
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
		if (AUTOCOMPLETE) {
			return readLineAutoComplete();			
		} else {
			return readLineConsole();
		}
	}

	public String readLineConsole() {
		String line = console.readLine(PROMPT);
		return line;
	}


	private Terminal terminal = null;
	private LineReader lnreader = null;
	
	public  String readLineAutoComplete() {
		try {
			if (terminal==null) {
				terminal = TerminalBuilder.terminal(); //builder().system(true).dumb(false).build();
				terminal.enterRawMode();	
				lnreader = LineReaderBuilder.builder()
					 .terminal(terminal)
					 .completer(makeCompleter())
					 //.highlighter(makeHighlighter())
					 //.parser(makeParser())
					 .build();
				//NonBlockingReader reader = terminal.reader();
			}
			return lnreader.readLine(PROMPT);
		} catch (IOException e) {
			System.err.println(e);
			return null;
		}		
	}


	private Completer makeCompleter() {
		List<Completer> completers = new ArrayList<>();
		if (runners!=null && runners.size()>0) {
			for (CommandRunner runner: runners) {
				if (runner instanceof CommandRunnerBase) {
					CommandRunnerBase runner_ = (CommandRunnerBase)runner;
					String[][] cmds = runner_.getCommands();
					if (cmds!=null) {
						for (String[] cmd: cmds) {
							List<Completer> ncompleters = new ArrayList<>();
							StringsCompleter completer0 = new StringsCompleter(cmd);
							ncompleters.add(completer0);
							Completer completer1 = null;
							//Completer completer2 = null;
							Map<String, String[][]> subcmds = runner_.getSubCommands();
							if (subcmds!=null) {
								Map<String, Map<String, String[][]>> subsubcmds = runner_.getSubSubCommands();
								String[][] subcmds1 = subcmds.get(cmd[0]);
								List<Completer> subcompleters = new ArrayList<>();
								if (subcmds1!=null) {
									for (String[] subcmds1_: subcmds1) {
										subcompleters.add(new StringsCompleter(subcmds1_));
									}
								}
								if (subcompleters.size()>0) {
									completer1 = new AggregateCompleter(subcompleters);								
								}
							}
							if (completer1!=null) {
								ncompleters.add(completer1);
							}
							ncompleters.add(new StringsCompleter());
							ArgumentCompleter ncompleter = new ArgumentCompleter(ncompleters);
							completers.add(ncompleter);
						}
					}
				}
			}
			
		}

		
		Completer completer = new AggregateCompleter(completers);

		return completer;
	}

	private void runScript(String source) {
		if (!StringUtil.hasText(source)) {
			error("missing script source");
			exit(-1);
			return;
		}
		runLine(source);
	}

	private void runLines(String[] lines) {
		for (int i=0; i<lines.length; i++) {
			String line = lines[i];
			runLine(line);
		}
	}

	private void runLine(String line) {
		if (line==null) {
			return;
		}
		line = line.trim();
		if (line.startsWith("#")) {
			return;
		}
		String[] parts = line.split(";");
		for (String part: parts) {
			part = part.trim();
			if (part.isEmpty()) {
				continue;
			}
			String[] cmds = part.split(" ");
			cmds = StringUtil.trim(cmds);			
			cmds = expand(cmds);
			run(cmds);
		}
	}

	private void runScriptFile(String file) {
		String content = ResourceUtils.readResource(file, false);
		String lines[] = content.split("\n");
		runLines(lines);
	}

	Map<String, Object> options;


	private Map<String, Object> makeArgsMap(String[] args, List<String> cmds, List<String> extra) {
		return makeArgsMap(args, cmds, extra, interactive);
	}

	public static Map<String, Object> makeArgsMap(String[] args, List<String> cmds, List<String> extra, boolean interactive) {
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
				} else {
					if (extra!=null) {
						i++;
						for (;i<args.length; i++) {
							extra.add(args[i]);
						}						
					}
					return map;
				}
			} else if (a.startsWith("-")) {
				if (a.length()>1) {
					a = a.substring(1);
					String key;
					String value;
					if (a.length()>1) {
						value = a.substring(1);
						if (value.startsWith("=")) {
							value = value.length()>1 ? value.substring(1) : "";
						}
						key = a.substring(0,1);
					} else if (i<args.length-1) {
						key = a;
						if (!args[i+1].startsWith("-")) {
							value = args[i+1];
							i++;							
						} else {
							value = "";
						}
					} else {
						key = a;
						value = "";
					}
					if (StringUtil.hasText(value) && concatOption(key)) {
						String current = (String)map.get(key);
						if (StringUtil.hasText(current)) {
							value = current + ";" + value;
						}
					}
					map.put(key, value);						
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
	
	private static boolean concatOption(String option) {
		return "e".equals(option);
	}
	
	
	public void debug(String s, Object... args) {
		if (isDebug(options)) {
			System.out.println(String.format(s, args));
		}
	}

	private void debug(int level, String s, Object... args) {
		if (isDebug(level, options)) {
			System.out.println(String.format(s, args));
		}
	}


}
