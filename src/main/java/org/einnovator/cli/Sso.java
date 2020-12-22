package org.einnovator.cli;

import static  org.einnovator.util.MappingUtils.updateObjectFromNonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.einnovator.sso.client.SsoClient;
import org.einnovator.sso.client.config.SsoClientConfiguration;
import org.einnovator.sso.client.model.Client;
import org.einnovator.sso.client.model.Group;
import org.einnovator.sso.client.model.Invitation;
import org.einnovator.sso.client.model.Member;
import org.einnovator.sso.client.model.Role;
import org.einnovator.sso.client.model.User;
import org.einnovator.sso.client.modelx.ClientFilter;
import org.einnovator.sso.client.modelx.GroupFilter;
import org.einnovator.sso.client.modelx.InvitationFilter;
import org.einnovator.sso.client.modelx.InvitationOptions;
import org.einnovator.sso.client.modelx.MemberFilter;
import org.einnovator.sso.client.modelx.RoleFilter;
import org.einnovator.sso.client.modelx.UserFilter;
import org.einnovator.util.PageOptions;
import org.einnovator.util.StringUtil;
import org.einnovator.util.config.ConnectionConfiguration;
import org.einnovator.util.web.RequestOptions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;


@Component
public class Sso extends CommandRunnerBase {

	
	private static final String SSO_DEFAULT_SERVER = "http://localhost:2000";
	private static final String SSO_MONITOR_SERVER = "http://localhost:2001";

	public static final String SSO_NAME = "sso";

	public static final String CONFIG_FOLDER = ".ei";
	public static final String CONFIG_FILE = "config.json";
	public static final String KEY_TOKEN = "token";
	public static final String KEY_API = "api";
	public static final String KEY_ENDPOINTS = "endpoints";
	public static final String KEY_USERNAME = "username";
	public static final String KEY_SETTINGS = "settings";
	public static final String KEY_LASTMODIFIED = "lastModified";

	public String DEFAULT_CLIENT = "application";
	public String DEFAULT_SECRET = "application$123";


	private static final String USER_DEFAULT_FORMAT = "id,username,email,status";
	private static final String USER_WIDE_FORMAT = "id,username,email,firstName,lastName,title,address.country,phone.formatted,status,enabled";

	private static final String GROUP_DEFAULT_FORMAT = "id,name,type,owner";
	private static final String GROUP_WIDE_FORMAT = "id,name,type,owner,address.country";

	private static final String CLIENT_DEFAULT_FORMAT = "id,clientId,clientSecret";
	private static final String CLIENT_WIDE_FORMAT = "id,clientId,clientSecret,scopes";

	private static final String MEMBER_DEFAULT_FORMAT = "id,user.username,group.name,title";
	private static final String MEMBER_WIDE_FORMAT = "id,user.username,group.name,title,enabled";

	private static final String ROLE_DEFAULT_FORMAT = "id,name,type,group.name,userCount";
	private static final String ROLE_WIDE_FORMAT = "id,name,displayName,type,group.name,userCount";

	private static final String INVITATION_DEFAULT_FORMAT ="id,invitee,type,owner,status";
	private static final String INVITATION_WIDE_FORMAT ="id,invitee,type,owner,status,subject";

	OAuth2AccessToken token;
	
	private SsoClient ssoClient;

	private SsoClientConfiguration config = new SsoClientConfiguration();

	String tokenUsername;
	String tokenPassword;
	String clientId = DEFAULT_CLIENT;
	String clientSecret = DEFAULT_SECRET;
	
	private String server = SSO_DEFAULT_SERVER;
	private String api = API_DEFAULT;

	
	private Map<String, Object> allEndpoints;
	 
	@Override
	public String getName() {
		return SSO_NAME;
	}

	String[][] SSO_COMMANDS = c(
		c("login", "l"),
		c("api", "a"),
		c("token"),
		c("users", "user"),
		c("groups", "group"),
		c("member", "members"),
		c("role", "roles"),
		c("invitation", "invitations", "invites", "inv"),
		c("clients", "client")
	);

	protected String[][] getCommands() {
		return SSO_COMMANDS;
	}

	
	static Map<String, String[][]> subcommands;
	
	static {
		Map<String, String[][]> map = new LinkedHashMap<>();
		subcommands = map;
		map.put("user", c(c("ls", "list"), c("get"), c("schema", "meta"), 
			c("create", "add"), c("update"), c("delete", "del", "rm"),
			c("help")));
		map.put("group", c(c("ls", "list"), c("get"), c("schema", "meta"), 
			c("create", "add"), c("update"), c("delete", "del", "remove", "rm"), 
			c("help")));
		map.put("invitation", c(c("ls", "list"), c("get"), c("schema", "meta"), 
			c("create", "add"), c("update"), c("delete", "del", "remove", "rm"), c("help")));
		map.put("member", c(c("ls", "list"), c("get"), c("schema", "meta"), 
			c("add", "create"), c("update"), c("delete", "del", "remove", "rm"), c("help")));
		map.put("role", c(c("ls", "list"), c("get"), c("schema", "meta"), 
			c("create", "add"), c("update"), c("delete", "del", "remove", "rm"), c("help")));
		map.put("client", c(c("ls", "list"), c("get"), c("schema", "meta"), 
			c("create", "add"), c("update"), c("delete", "del", "remove", "rm"), 
			c("help")));
	}
	
	@Override
	protected Map<String, String[][]> getSubCommands() {
		return subcommands;
	}

	@Override
	public void init(String[] cmds, Map<String, Object> options, OAuth2RestTemplate template, boolean interactive, ResourceBundle bundle) {
		if (!init) {
			super.init(cmds, options, template, interactive, bundle);
			if (token!=null && token.isExpired()) {
				error("Token expired! Login again...");
				exit(-1);
				token = null;
			}
			if (template==null) {
				ResourceOwnerPasswordResourceDetails resource = getRequiredResourceDetails();
				DefaultOAuth2ClientContext context = new DefaultOAuth2ClientContext();
				if (token!=null) {
					context.setAccessToken(token);					
				}
				template = new OAuth2RestTemplate(resource, context);
				template.setRequestFactory(config.getConnection().makeClientHttpRequestFactory());			
			}
			setTemplate(template);
			ssoClient = new SsoClient(template, config, false);			
			init = true;
		}
	}
	
	@Override
	public void setEndpoints(Map<String, Object> endpoints) {
		String server = (String)endpoints.get("server");
		if (server!=null) {
			this.server = server;
		}
	}
	
	public Map<String, Object> getAllEndpoints() {
		return allEndpoints;
	}

	public ResourceOwnerPasswordResourceDetails getRequiredResourceDetails() {
		if (!init) {
			setup(options);
			init = true;
		}
		if (token==null) {
			if (!StringUtil.hasText(tokenUsername)) {
				error("missing username");
				exit(-1);
				return null;
			}
			if (!StringUtil.hasText(tokenPassword)) {
				error("missing password");
				exit(-1);
				return null;
			}			
		} else {
			if (tokenPassword==null) {
			}		
		}

		ResourceOwnerPasswordResourceDetails resource = SsoClient.makeResourceOwnerPasswordResourceDetails(tokenUsername, tokenPassword, config);
		return resource;
	}

	
	public void run(String type, String op, String[] cmds, Map<String, Object> options) {
		setLine(type, op, cmds, options);
		if (isHelp()) {
			printUsage();
		}
		switch (type) {
		case "help": case "":
			printUsage();
			break;
		case "login": case "l":
			login(type, op, cmds, options);
			return;
		case "api": case "a":
			api(type, op, cmds, options);
			return;
		}
		
		getToken(type, op, cmds, options);
		
		switch (type) {
		case "token":
			switch (op) {
				case "show": case "s": case "":
					showToken();
					break;
				default: 
					invalidOp(type, op);
					break;
			}
			break;
		case "user": case "users":
			switch (op) {
			case "help": case "":
				printUsage("user");
				break;
			case "get": 
				getUser(cmds, options);
				break;
			case "ls": case "list":
				listUsers(cmds, options);
				break;
			case "create": case "add":
				createUser(cmds, options);
				break;
			case "update":
				updateUser(cmds, options);
				break;
			case "delete": case "del": case "rm": case "remove":
				deleteUser(cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "group": case "groups":
			switch (op) {
			case "help":
				printUsage("group");
				break;
			case "get": 
				getGroup(cmds, options);
				break;
			case "ls": case "list":
				listGroups(cmds, options);
				break;
			case "create": case "add":
				createGroup(cmds, options);
				break;
			case "update":
				updateGroup(cmds, options);
				break;
			case "delete": case "del": case "rm": case "remove":
				deleteGroup(cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "member": case "members":
			listGroupMembers(cmds, options);
			switch (op) {
			case "help": case "":
				printUsage("member");
				break;
			case "get": 
				//getGroupMember(cmds, options);
				break;
			case "ls": case "list":
				listGroupMembers(cmds, options);
				break;
			case "create": case "add":
				addMember(cmds, options);
				break;
			case "update":
				//updateMember(cmds, options);
				break;
			case "delete": case "del": case "rm": case "remove":
				removeMember(cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "role": case "roles":
			switch (op) {
			case "help": case "":
				printUsage("role");
				break;
			case "get": 
				getRole(cmds, options);
				break;
			case "ls": case "list":
				listRoles(cmds, options);
				break;
			case "create": case "add":
				createRole(cmds, options);
				break;
			case "update":
				updateRole(cmds, options);
				break;
			case "delete": case "del": case "rm": case "remove":
				deleteRole(cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "invitation": case "invitations": case "invites": case "inv":
			switch (op) {
			case "help": case "":
				printUsage("invitation");
				break;
			case "get": 
				getInvitation(cmds, options);
				break;
			case "ls": case "list":
				listInvitations(cmds, options);
				break;
			case "create": case "add":
				invite(cmds, options);
				break;
			case "update":
				updateInvitation(cmds, options);
				break;
			case "delete": case "del": case "rm": case "remove":
				deleteInvitation(cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "client": case "clients":
			switch (op) {
			case "help": case "":
				printUsage("client");
				break;
			case "get": 
				getClient(cmds, options);
				break;
			case "ls": case "list":
				listClients(cmds, options);
				break;
			case "create": case "add":
				createClient(cmds, options);
				break;
			case "update":
				updateClient(cmds, options);
				break;
			case "delete": case "del": case "rm": case "remove":
				deleteClient(cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		default:
			System.err.println("Invalid command: " + type + " " + op);
			printUsage();
			break;
		}
	}



	public String getUsage() {
		StringBuilder sb = new StringBuilder();
		return sb.toString();
	}

	//
	// Login, API, Token
	//

	public void login(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("login")) {
			return;
		}
		String api = (String)get("a", options, null);
		if (api==null) {
			api = getCurrentApi();
		}
		if (api==null) {
			error("missing api");
			exit(-1);
			return;
		}
		Map<String, Object> endpoints = getEndpoints(api, cmds, options);
		if (endpoints==null) {
			error("unable to get endpoints from api: %s", api);
			exit(-1);
			return;
		}
		allEndpoints = endpoints;
		if (endpoints!=null) {
			writeConfig(api, endpoints, null, options);
		}
		getToken(type, op, cmds, options);	
		System.out.println(String.format("Logged in at: %s", api));
	}

	private static String API_LOCALHOST = "localhost";
	private static String API_EI = "https://sso.einnovator.org";
	private static String API_DOCKER = "https://localhost:5050";
	private static String API_DEFAULT = API_LOCALHOST;

	public String getCurrentApi() {
		return api;
	}
	
	public void api(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("api")) {
			return;
		}
		String api = (String)get("a", options, null);
		if (api==null) {
			api = getCurrentApi();
		}
		if (api==null) {
			error("missing api");
			exit(-1);
			return;
		}
		Map<String, Object> endpoints = getEndpoints(api, cmds, options);
		if (endpoints==null) {
			error("unable to get endpoints from api: %s", api);
			exit(-1);
			return;
		}
		allEndpoints = endpoints;
		if (endpoints!=null) {
			writeConfig(api, endpoints, null, options);
		}		
		getToken(type, op, cmds, options);	
		System.out.println(String.format("Api set to: %s", api));
	}

	public Map<String, Object> getEndpoints(String api, String[] cmds, Map<String, Object> options) {
		
		if (api.equals(API_LOCALHOST)) {
			return getEndpointsLocalhost(cmds, options);			
		}
		if (api.equals(API_EI)) {
			return getEndpointsEI(cmds, options);			
		}
		if (api.equals(API_DOCKER)) {
			return getEndpointsDocker(cmds, options);			
		}
		if (!api.startsWith("http://") && !api.startsWith("https://")) {
			if (api.startsWith("localhost")) {
				api = "http://" + api;
			} else {
				api = "https://" + api;
			}
		}
		Map<String, Object> endpoints = getEndpointsRemote(api, cmds, options);
		return endpoints;
	}

	private Map<String, Object> getEndpointsRemote(String api, String[] cmds, Map<String, Object> options) {
		return null;
	}


	private Map<String, Object> makeEndpointsStub(String[] cmds, Map<String, Object> options) {
		Map<String, Object> endpoints = new LinkedHashMap<>();
		Map<String, Object> sso = new LinkedHashMap<>();
		Map<String, Object> devops = new LinkedHashMap<>();
		Map<String, Object> documents = new LinkedHashMap<>();
		Map<String, Object> notifications = new LinkedHashMap<>();
		Map<String, Object> payments = new LinkedHashMap<>();
		Map<String, Object> social = new LinkedHashMap<>();
		endpoints.put("sso", sso);
		endpoints.put("devops", devops);
		endpoints.put("documents", documents);
		endpoints.put("notifications", notifications);
		endpoints.put("payments", payments);
		endpoints.put("social", social);
		return endpoints;
	}


	@SuppressWarnings("unchecked")
	public Map<String, Object> getEndpointsLocalhost(String[] cmds, Map<String, Object> options) {
		Map<String, Object> endpoints = makeEndpointsStub(cmds, options);
		Map<String, Object> sso = (Map<String, Object>)endpoints.get("sso");
		Map<String, Object> devops = (Map<String, Object>)endpoints.get("devops");
		Map<String, Object> documents = (Map<String, Object>)endpoints.get("documents");
		Map<String, Object> notifications = (Map<String, Object>)endpoints.get("notifications");
		Map<String, Object> payments = (Map<String, Object>)endpoints.get("payments");
		Map<String, Object> social = (Map<String, Object>)endpoints.get("social");
		sso.put("server", SSO_DEFAULT_SERVER);
		devops.put("devops", Devops.DEVOPS_DEFAULT_SERVER);
		documents.put("documents", Documents.DOCUMENTS_DEFAULT_SERVER);
		notifications.put("notifications", Notifications.NOTIFICATIONS_DEFAULT_SERVER);
		payments.put("payments", Payments.PAYMENTS_DEFAULT_SERVER);
		social.put("server", Social.SOCIAL_DEFAULT_SERVER);
		return endpoints;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getEndpointsEI(String[] cmds, Map<String, Object> options) {
		Map<String, Object> endpoints = makeEndpointsStub(cmds, options);
		Map<String, Object> sso = (Map<String, Object>)endpoints.get("sso");
		Map<String, Object> devops = (Map<String, Object>)endpoints.get("devops");
		Map<String, Object> documents = (Map<String, Object>)endpoints.get("documents");
		Map<String, Object> notifications = (Map<String, Object>)endpoints.get("notifications");
		Map<String, Object> payments = (Map<String, Object>)endpoints.get("payments");
		Map<String, Object> social = (Map<String, Object>)endpoints.get("social");
		sso.put("server", "https://sso.einnovator.org");
		devops.put("devops", "https://cloud.einnovator.org");
		documents.put("documents", "https://documents.einnovator.org");
		notifications.put("notifications", "https://notifications.einnovator.org");
		payments.put("payments", "https://payments.einnovator.org");
		social.put("server", "https://social.einnovator.org");
		return endpoints;
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, Object> getEndpointsDocker(String[] cmds, Map<String, Object> options) {
		Map<String, Object> endpoints = makeEndpointsStub(cmds, options);
		Map<String, Object> sso = (Map<String, Object>)endpoints.get("sso");
		Map<String, Object> devops = (Map<String, Object>)endpoints.get("devops");
		Map<String, Object> documents = (Map<String, Object>)endpoints.get("documents");
		Map<String, Object> notifications = (Map<String, Object>)endpoints.get("notifications");
		Map<String, Object> payments = (Map<String, Object>)endpoints.get("payments");
		Map<String, Object> social = (Map<String, Object>)endpoints.get("social");
		sso.put("server", SSO_DEFAULT_SERVER);
		devops.put("devops", "http://localhost:5050");
		documents.put("documents", Documents.DOCUMENTS_DEFAULT_SERVER);
		notifications.put("notifications", Notifications.NOTIFICATIONS_DEFAULT_SERVER);
		payments.put("payments", Payments.PAYMENTS_DEFAULT_SERVER);
		social.put("server", Social.SOCIAL_DEFAULT_SERVER);
		return endpoints;
	}

	public void getToken(String type, String op, String[] cmds, Map<String, Object> options) {
		debug("Credentials: %s %s", tokenUsername, tokenPassword);
		debug("Config: %s", config);
		token = ssoClient.getToken(tokenUsername, tokenPassword);
		debug("Token: %s", token);
		if (token!=null) {
			writeConfig(api, allEndpoints, token.toString(), options);			
		}
	}


	private void writeConfig(String api, Map<String, Object> endpoints,  String token, Map<String, Object> options) {
		File file = getConfigFile(options);
		Map<String, Object> config = makeConfig(api, endpoints, token);
		try (PrintWriter writer = new PrintWriter(new FileOutputStream(file))) {
			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);
			writer.write(json);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
	}
	
	@Override
	protected void writeConfig() {
		writeConfig(this.api, this.allEndpoints, this.token!=null ? this.token.toString() : null, this.options);
	}

	public void setup(Map<String, Object> options) {
		Map<String, Object> config = readConfig(options);
		if (config!=null) {
			String current = (String)config.get("current");
			@SuppressWarnings("unchecked")
			Map<String, Object> context = findContext(current, (List<Map<String, Object>>)config.get("contexts"));
			setupForContext(context);
			if (current!=null) {
				this.api = current;				
			}
		}
				
		this.config.setServer(server);
		this.config.setClientId(clientId);
		this.config.setClientSecret(clientSecret);
		tokenUsername = (String)get("u", options, tokenUsername);
		tokenPassword = (String)get("p", options, tokenPassword);
		updateObjectFromNonNull(this.config, convert(options, SsoClientConfiguration.class));

		
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> readConfig(Map<String, Object> options) {
		File file = getConfigFile(options);
		try (Reader reader = new FileReader(file)) {
			ObjectMapper mapper = new ObjectMapper();
			mapper.readTree(reader);
			Map<String, Object> config = (Map<String, Object>)mapper.readValue(file, Map.class);
			if (config==null) {
				return null;
			}
			return config;
		} catch (IOException e) {
			return null;
		}		
	}
	
	private Map<String, Object> makeConfig(String api, Map<String, Object> endpoints, String token) {
		Map<String, Object> config = new LinkedHashMap<>();
		config.put("current", api);
		List<Map<String, Object>> contexts = new ArrayList<>();
		config.put("contexts", contexts);
		Map<String, Object> context = makeContext(api, endpoints, token);
		contexts.add(context);
		return config;
	}
	
	private Map<String, Object> makeContext(String api, Map<String, Object> endpoints, String token) {
		Map<String, Object> context = new LinkedHashMap<>();	
		context.put(KEY_API, api);
		context.put(KEY_TOKEN, token);
		context.put(KEY_ENDPOINTS, endpoints);
		context.put(KEY_USERNAME, tokenUsername);
		context.put(KEY_LASTMODIFIED, new Date().toInstant().toString());
		Map<String, Object> settings = getAllSettings();
		context.put(KEY_SETTINGS, settings);
		return context;
	}

	public Map<String, Object> getAllSettings() {
		if (runners!=null) {
			Map<String, Object> settings = new LinkedHashMap<>();
			for (CommandRunner runner: runners) {
				Map<String, Object> settings1 = runner.getSettings();
				if (settings1!=null) {
					settings.put(runner.getName(), settings1);
				}
			}
			return settings;
		}
		return getSettings();
	}
	
	public void loadAllSettings(Map<String, Object> settings) {
		if (runners!=null) {
			for (CommandRunner runner: runners) {
				@SuppressWarnings("unchecked")
				Map<String, Object> settings1 = (Map<String, Object>)settings.get(runner.getName());
				if (settings1!=null) {
					runner.loadSettings(settings1);
				}
			}
			return;
		}
		loadSettings(settings);
	}

	@Override
	public Map<String, Object> getSettings() {
		Map<String, Object> settings = new LinkedHashMap<>();
		settings.put("connection", this.config.getConnection());
		settings.put("clientId", this.config.getClientId());
		settings.put("clientSecret", this.config.getClientSecret());
		return settings;
	}

	@Override
	public void loadSettings(Map<String, Object> settings) {
		this.config.setClientId(get("clientId", settings, this.config.getClientId()));
		this.config.setClientSecret(get("clientSecret", settings, this.config.getClientId()));
		this.config.setConnection(get("connection", settings, this.config.getConnection(), ConnectionConfiguration.class));
	}
	
	@SuppressWarnings("unchecked")
	private void setupForContext(Map<String, Object> context) {
		if (context==null) {
			return;
		}
		this.api = (String)context.get(KEY_API);
		this.allEndpoints = (Map<String, Object>) context.get(KEY_ENDPOINTS);
		Map<String, Object> settings = (Map<String, Object>) context.get(KEY_SETTINGS);
		String token = (String)context.get(KEY_TOKEN);
		if (token!=null) {
			this.token = new DefaultOAuth2AccessToken(token);
		}
		tokenUsername = (String)context.get(KEY_USERNAME);

		if (settings!=null) {
			loadAllSettings(settings);
		}
	}


	private Map<String, Object> findContext(String name, List<Map<String, Object>> contexts) {
		if (name!=null && contexts!=null) {
			for (Map<String, Object> context: contexts) {
				String api = (String)context.get(KEY_API);
				if (name.equals(api)) {
					return context;
				}
			}			
		}
		return null;
	}
	
	private File getConfigFile(Map<String, Object> options) {
		String home = System.getProperty("user.home");
		if (home==null) {
			home = ".";
		}
		String dir =  home + File.separator + CONFIG_FOLDER;
		File fdir = new File(dir);
		fdir.mkdirs();
		String path = dir + File.separator + CONFIG_FILE;
		return new File(path);
	}


	public void showToken() {
		System.out.println(String.format("Token: %s", token));
	}

	
	//
	// User
	//
	
	public void listUsers(String[] cmds, Map<String, Object> options) {
		if (isHelp("user", "ls")) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		UserFilter filter = convert(options, UserFilter.class);
		debug("Users: %s %s", filter, pageable);
		Page<User> users = ssoClient.listUsers(filter, pageable);
		print(users);
	}

	public void getUser(String[] cmds, Map<String, Object> options) {
		if (isHelp("user", "get")) {
			return;
		}
		String userId = argId(op, cmds);
		debug("User: %s", userId);
		User user = ssoClient.getUser(userId, null);
		printObj(user);
	}

	public void createUser(String[] cmds, Map<String, Object> options) {
		if (isHelp("user", "create")) {
			return;
		}
		User user = convert(options, User.class);
		user.setUsername(argName(op, cmds));
		debug("Creating User: %s", user);
		URI uri = ssoClient.createUser(user, new RequestOptions());
		if (isEcho()) {
			printLine("User URI:", uri);
			String id = extractId(uri);
			User user2 = ssoClient.getUser(id, null);
			printObj(user2);			
		}
	}

	
	public void updateUser(String[] cmds, Map<String, Object> options) {
		if (isHelp("user", "update")) {
			return;
		}
		String userId = argId(op, cmds);
		User user = convert(options, User.class);
		debug("Updating User: %s %s", userId, user);
		ssoClient.updateUser(user, null);
		if (isEcho()) {
			User user2 = ssoClient.getUser(userId, null);
			printObj(user2);			
		}
	}

	public void deleteUser(String[] cmds, Map<String, Object> options) {
		if (isHelp("user", "delete")) {
			return;
		}
		String userId = argId(op, cmds);
		debug("Deleting User: %s", userId);
		ssoClient.deleteUser(userId, null);	
		if (isEcho()) {
			listUsers(cmds, options);
		}
	}

	//
	// Groups
	//
	
	public void listGroups(String[] cmds, Map<String, Object> options) {
		if (isHelp("group", "ls")) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		GroupFilter filter = convert(options, GroupFilter.class);
		debug("Groups: %s %s", filter, pageable);
		Page<Group> groups = ssoClient.listGroups(filter, pageable);
		print(groups);
	}
	
	public void getGroup(String[] cmds, Map<String, Object> options) {
		if (isHelp("group", "get")) {
			return;
		}
		String groupId = argId(op, cmds);
		debug("Group: %s", groupId);
		Group group = ssoClient.getGroup(groupId, null);
		printObj(group);
	}
	
	public void createGroup(String[] cmds, Map<String, Object> options) {
		if (isHelp("group", "create")) {
			return;
		}
		Group group = convert(options, Group.class);
		group.setName(argName(op, cmds));
		debug("Creating Group: %s", group);
		URI uri = ssoClient.createGroup(group, new RequestOptions());
		if (isEcho()) {
			printLine("Group URI:", uri);
			String id = extractId(uri);
			Group group2 = ssoClient.getGroup(id, null);
			printObj(group2);			
		}
	}

	public void updateGroup(String[] cmds, Map<String, Object> options) {
		if (isHelp("group", "update")) {
			return;
		}
		String groupId = argId(op, cmds);
		Group group = convert(options, Group.class);
		debug("Updating Group: %s %s", groupId, group);
		ssoClient.updateGroup(group, null);
		if (isEcho()) {
			Group group2 = ssoClient.getGroup(groupId, null);
			printObj(group2);			
		}
	}
	
	public void deleteGroup(String[] cmds, Map<String, Object> options) {
		if (isHelp("group", "delete")) {
			return;
		}
		String groupId = argId(op, cmds);
		debug("Deleting Group: %s", groupId);
		ssoClient.deleteGroup(groupId, null);
		if (isEcho()) {
			listGroups(cmds, options);
		}
	}



	//
	// Group Members
	//
	
	public void addMember(String[] cmds, Map<String, Object> options) {
		if (isHelp("member", "add")) {
			return;
		}
		String userId = (String)get("user", options);
		String groupId = (String)get("group", options);
		printLine("Adding Member...");
		printLine("User:", userId);		
		printLine("Group:", groupId);	
		ssoClient.addMemberToGroup(userId, groupId, null);
	}

	
	public void removeMember(String[] cmds, Map<String, Object> options) {
		if (isHelp("member", "remove")) {
			return;
		}
		String userId = (String)get("user", options);
		String groupId = (String)get("group", options);
		printLine("Removing Member...");
		printLine("User:", userId);		
		printLine("Group:", groupId);		
		ssoClient.removeMemberFromGroup(userId, groupId, null);
		if (isEcho()) {
			listGroupMembers(groupId, options);
		}
	}

	public void listGroupMembers(String[] cmds, Map<String, Object> options) {
		if (isHelp("member", "ls")) {
			return;
		}
		String groupId = (String)get("group", options);
		listGroupMembers(groupId, options);
	}
	
	public void listGroupMembers(String groupId, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		MemberFilter filter = convert(options, MemberFilter.class);
		debug("Members: %s %s %s", groupId, filter, pageable);
		Page<Member> members = ssoClient.listGroupMembers(groupId, null, null);
		print(members);
	}

	
	//
	// Invitation
	//
	

	public void invite(String[] cmds, Map<String, Object> options) {
		if (isHelp("invitation", "create")) {
			return;
		}
		Invitation invitation = convert(options, Invitation.class);
		InvitationOptions options_ = convert(options, InvitationOptions.class);
		Boolean sendMail = get("sendMail", options, true);
		options_.setSendMail(sendMail);
		invitation.setInvitee(argName(op, cmds));
		debug((Boolean.TRUE.equals(sendMail) ? "Sending Invitation:" : "Creating Invitation:") + "%s", invitation);
		URI uri = ssoClient.invite(invitation, options_);
		if (isEcho()) {
			printLine("Invitation URI:", uri);
			String id = extractId(uri);
			Invitation invitation2 = ssoClient.getInvitation(id, null);
			printObj(invitation2);		
			URI tokenUri = ssoClient.getInvitationToken(id, new InvitationOptions().withSendMail(false));
			printLine(String.format("Token URI: %s", tokenUri));
		}
	}
	
	
	public void listInvitations(String[] cmds, Map<String, Object> options) {
		if (isHelp("invitation", "ls")) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		InvitationFilter filter = convert(options, InvitationFilter.class);
		debug("Invitations: %s %s", filter, pageable);
		Page<Invitation> invitations = ssoClient.listInvitations(filter, pageable);
		print(invitations);
	}

	public void getInvitation(String[] cmds, Map<String, Object> options) {
		if (isHelp("invitation", "get")) {
			return;
		}
		String invitationId = argId(op, cmds);
		debug("Invitation: %s", invitationId);
		Invitation invitation = ssoClient.getInvitation(invitationId, null);
		printObj(invitation);
	}


	
	public void updateInvitation(String[] cmds, Map<String, Object> options) {
		if (isHelp("invitation", "update")) {
			return;
		}
		String invitationId = argId(op, cmds);
		Invitation invitation = convert(options, Invitation.class);
		debug("Updating Invitation: %s %s", invitationId, invitation);
		ssoClient.updateInvitation(invitation, null);
		if (isEcho()) {
			Invitation invitation2 = ssoClient.getInvitation(invitationId, null);
			printObj(invitation2);			
		}
	}

	public void deleteInvitation(String[] cmds, Map<String, Object> options) {
		if (isHelp("invitation", "delete")) {
			return;
		}
		String invitationId = argId(op, cmds);
		debug("Deleting Invitation: %s", invitationId);
		ssoClient.deleteInvitation(invitationId, null);	
		if (isEcho()) {
			listInvitations(cmds, options);
		}
	}


	//
	// Roles
	//
	
	public void listRoles(String[] cmds, Map<String, Object> options) {
		if (isHelp("role", "ls")) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		RoleFilter filter = convert(options, RoleFilter.class);
		debug("Role: %s %s", filter, pageable);
		Page<Role> roles = ssoClient.listRoles(filter, pageable);
		print(roles);
	}
	
	public void getRole(String[] cmds, Map<String, Object> options) {
		if (isHelp("role", "get")) {
			return;
		}
		String roleId = argId(op, cmds);
		debug("Role: %s", roleId);
		Role role = ssoClient.getRole(roleId, null);
		printObj(role);
	}
	
	public void createRole(String[] cmds, Map<String, Object> options) {
		if (isHelp("role", "create")) {
			return;
		}
		Role role = convert(options, Role.class);
		role.setName(argName(op, cmds));
		debug("Creating Role: %s", role);
		URI uri = ssoClient.createRole(role, new RequestOptions());
		if (isEcho()) {
			printLine("Role URI:", uri);
			String id = extractId(uri);
			Role role2 = ssoClient.getRole(id, null);
			printObj(role2);			
		}
	}

	public void updateRole(String[] cmds, Map<String, Object> options) {
		if (isHelp("role", "update")) {
			return;
		}
		String roleId = argId(op, cmds);
		Role role = convert(options, Role.class);
		debug("Updating Role: %s %s", roleId, role);
		ssoClient.updateRole(role, null);
		if (isEcho()) {
			Role role2 = ssoClient.getRole(roleId, null);
			printObj(role2);			
		}
	}
	
	public void deleteRole(String[] cmds, Map<String, Object> options) {
		if (isHelp("role", "delete")) {
			return;
		}
		String roleId = argId(op, cmds);
		debug("Deleting Role: %s", roleId);
		ssoClient.deleteRole(roleId, null);
		if (isEcho()) {
			listRoles(cmds, options);
		}
	}


	//
	// Clients
	//
	

	public void listClients(String[] cmds, Map<String, Object> options) {
		if (isHelp("client", "ls")) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		ClientFilter filter = convert(options, ClientFilter.class);
		debug("Client: %s %s", filter, pageable);
		Page<Client> clients = ssoClient.listClients(filter, pageable);
		print(clients);
	}

	public void getClient(String[] cmds, Map<String, Object> options) {
		if (isHelp("client", "get")) {
			return;
		}
		String clientId = argId(op, cmds);
		debug("Client: %s", clientId);
		Client client = ssoClient.getClient(clientId, null);
		printObj(client);
	}

	public void createClient(String[] cmds, Map<String, Object> options) {
		if (isHelp("client", "create")) {
			return;
		}
		Client client = convert(options, Client.class);
		client.setClientId(argName(op, cmds));
		debug("Creating Client: %s", client);
		URI uri = ssoClient.createClient(client, new RequestOptions());
		if (isEcho()) {
			printLine("Client URI:", uri);
			String id = extractId(uri);
			Client client2 = ssoClient.getClient(id, null);
			printObj(client2);			
		}
	}

	
	public void updateClient(String[] cmds, Map<String, Object> options) {
		if (isHelp("client", "update")) {
			return;
		}
		String clientId = argId(op, cmds);
		Client client = convert(options, Client.class);
		debug("Updating Client: %s %s", clientId, client);
		ssoClient.updateClient(client, null);
		if (isEcho()) {
			Client client2 = ssoClient.getClient(clientId, null);
			printObj(client2);			
		}
	}

	public void deleteClient(String[] cmds, Map<String, Object> options) {
		if (isHelp("client", "delete")) {
			return;
		}
		String clientId = argId(op, cmds);
		debug("Deleting Client: %s", clientId);
		ssoClient.deleteClient(clientId, null);
		if (isEcho()) {
			listClients(cmds, options);
		}
	}

	@Override
	protected String getDefaultFormat(Class<? extends Object> type) {
		if (User.class.equals(type)) {
			return USER_DEFAULT_FORMAT;
		}
		if (Group.class.equals(type)) {
			return GROUP_DEFAULT_FORMAT;
		}
		if (Client.class.equals(type)) {
			return CLIENT_DEFAULT_FORMAT;
		}
		if (Member.class.equals(type)) {
			return MEMBER_DEFAULT_FORMAT;
		}
		if (Invitation.class.equals(type)) {
			return INVITATION_DEFAULT_FORMAT;
		}
		if (Role.class.equals(type)) {
			return ROLE_DEFAULT_FORMAT;
		}
		return null;
	}

	@Override
	protected String getWideFormat(Class<? extends Object> type) {
		if (User.class.equals(type)) {
			return USER_WIDE_FORMAT;
		}
		if (Group.class.equals(type)) {
			return GROUP_WIDE_FORMAT;
		}
		if (Client.class.equals(type)) {
			return CLIENT_WIDE_FORMAT;
		}
		if (Member.class.equals(type)) {
			return MEMBER_WIDE_FORMAT;
		}
		if (Invitation.class.equals(type)) {
			return INVITATION_WIDE_FORMAT;
		}
		if (Role.class.equals(type)) {
			return ROLE_WIDE_FORMAT;
		}
		return null;
	}
	
	
}