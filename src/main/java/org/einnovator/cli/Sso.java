package org.einnovator.cli;

import static  org.einnovator.util.MappingUtils.convert;
import static  org.einnovator.util.MappingUtils.updateObjectFromNonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import org.einnovator.sso.client.modelx.RoleFilter;
import org.einnovator.sso.client.modelx.UserFilter;
import org.einnovator.util.MappingUtils;
import org.einnovator.util.PageOptions;
import org.einnovator.util.StringUtil;
import org.einnovator.util.UriUtils;
import org.einnovator.util.config.ConnectionConfiguration;
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

	public static final String SSO_PREFIX = "sso";

	public static String CONFIG_FOLDER = ".ei";
	public static String CONFIG_FILE = "config.json";
	public static String KEY_TOKEN = "token";
	public static String KEY_API = "api";
	public static String KEY_ENDPOINTS = "endpoints";
	public static String KEY_USERNAME = "username";
	public static String KEY_SETTINGS = "settings";

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

	boolean init;
	
	private Map<String, Object> allEndpoints;
	 
	@Override
	public String getPrefix() {
		return SSO_PREFIX;
	}

	String[] SSO_COMMANDS = new String[] { 
		"login",
		"api",
		"token", "tk", "t",
		"users", "user", "u",
		"groups", "group", "g",
		"member", "members", "m",
		"role", "roles", "r",
		"invitation", "invitations", "invites", "inv", "i",
		"clients", "client", "c",
		};

	protected String[] getCommands() {
		return SSO_COMMANDS;
	}


	@Override
	public void init(String[] cmds, Map<String, Object> options, OAuth2RestTemplate template) {
		super.init(cmds, options, template);

		init = true;
		
		if (token!=null && token.isExpired()) {
			error("Token expired! Login again...");
			System.exit(-1);
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
				System.exit(-1);
				return null;
			}
			if (!StringUtil.hasText(tokenPassword)) {
				error("missing password");
				System.exit(-1);
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

		switch (type) {
		case "login": case "l":
			login(type, op, cmds, options);
			return;
		case "api": case "a":
			api(type, op, cmds, options);
			return;
		}
		
		getToken(type, op, cmds, options);
		
		switch (type) {
		case "token": case "tk": case "t":
			switch (op) {
				case "show": case "s": case "":
					showToken();
					break;
				default: 
					invalidOp(type, op);
					break;
			}
			break;
		case "user": case "users": case "u":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getUser(type, op, cmds, options);
				break;
			case "list": case "l": case "":
				listUsers(type, op, cmds, options);
				break;
			case "create": case "c":
				createUser(type, op, cmds, options);
				break;
			case "update": case "u":
				updateUser(type, op, cmds, options);
				break;
			case "delete": case "del": case "rm": case "d":
				deleteUser(type, op, cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "group": case "groups": case "g":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getGroup(type, op, cmds, options);
				break;
			case "list": case "l": case "":
				listGroups(type, op, cmds, options);
				break;
			case "create": case "c":
				createGroup(type, op, cmds, options);
				break;
			case "update": case "u":
				updateGroup(type, op, cmds, options);
				break;
			case "delete": case "del": case "rm": case "d":
				deleteGroup(type, op, cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "member": case "members": case "m":
			listGroupMembers(type, op, cmds, options);
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				//getMember(type, op, cmds, options);
				break;
			case "list": case "l": case "":
				//listMembers(type, op, cmds, options);
				break;
			case "create": case "c":
				addMember(type, op, cmds, options);
				break;
			case "update": case "u":
				//updateMember(type, op, cmds, options);
				break;
			case "delete": case "del": case "rm": case "d":
				removeMember(type, op, cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "role": case "roles": case "r":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getRole(type, op, cmds, options);
				break;
			case "list": case "l": case "":
				listRoles(type, op, cmds, options);
				break;
			case "create": case "c":
				createRole(type, op, cmds, options);
				break;
			case "update": case "u":
				updateRole(type, op, cmds, options);
				break;
			case "delete": case "del": case "rm": case "d":
				deleteRole(type, op, cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "invitation": case "invitations": case "invites": case "inv": case "i": 
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getInvitation(type, op, cmds, options);
				break;
			case "list": case "l": case "":
				listInvitations(type, op, cmds, options);
				break;
			case "create": case "c":
				invite(type, op, cmds, options);
				break;
			case "update": case "u":
				updateInvitation(type, op, cmds, options);
				break;
			case "delete": case "del": case "rm": case "d":
				deleteInvitation(type, op, cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "client": case "clients": case "c":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getClient(type, op, cmds, options);
				break;
			case "list": case "l": case "":
				listClients(type, op, cmds, options);
				break;
			case "create": case "c":
				createClient(type, op, cmds, options);
				break;
			case "update": case "u":
				updateClient(type, op, cmds, options);
				break;
			case "delete": case "del": case "rm": case "d":
				deleteClient(type, op, cmds, options);
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
		String api = (String)get("a", options, null);
		if (api==null) {
			api = getCurrentApi();
		}
		if (api==null) {
			error("missing api");
			System.exit(-1);
			return;
		}
		Map<String, Object> endpoints = getEndpoints(api, cmds, options);
		if (endpoints==null) {
			error("unable to get endpoints from api: %s", api);
			System.exit(-1);
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
		String api = (String)get("a", options, null);
		if (api==null) {
			api = getCurrentApi();
		}
		if (api==null) {
			error("missing api");
			System.exit(-1);
			return;
		}
		Map<String, Object> endpoints = getEndpoints(api, cmds, options);
		if (endpoints==null) {
			error("unable to get endpoints from api: %s", api);
			System.exit(-1);
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
		info("Credentials: %s %s", tokenUsername, tokenPassword);
		debug("Config: %s", config);
		token = ssoClient.getToken(tokenUsername, tokenPassword);
		debug("Token: %s", token);
		if (token!=null) {
			writeConfig(api, allEndpoints, token.getValue(), options);			
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
	
	public void setup(Map<String, Object> options) {
		Map<String, Object> config = readConfig(options);
		if (config!=null) {
			String current = (String)config.get("current");
			@SuppressWarnings("unchecked")
			Map<String, Object> context = findContext(current, (List<Map<String, Object>>)config.get("contexts"));
			setupForContext(context);
		}
				
		this.config.setServer(server);
		this.config.setClientId(clientId);
		this.config.setClientSecret(clientSecret);
		tokenUsername = (String)get("u", options, tokenUsername);
		tokenPassword = (String)get("p", options, tokenPassword);
		updateObjectFromNonNull(config, convert(options, SsoClientConfiguration.class));

		
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
		Map<String, Object> settings = new LinkedHashMap<>();
		context.put(KEY_SETTINGS, settings);
		settings.put("connection", this.config.getConnection());
		settings.put("clientId", this.config.getClientId());
		settings.put("clientSecret", this.config.getClientSecret());
		return context;
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
			clientId = (String)settings.get("clientId");
			clientSecret = (String)settings.get("clientSecret");
			Map<String, Object> connectionMap = (Map<String, Object>) settings.get("connection"); 
			if (connectionMap!=null) {
				ConnectionConfiguration connection = new ConnectionConfiguration();
				MappingUtils.fromMap(connection, connectionMap);
				this.config.setConnection(connection);
			}
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
	
	public void listUsers(String type, String op, String[] cmds, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		UserFilter filter = convert(options, UserFilter.class);
		Page<User> users = ssoClient.listUsers(filter, pageable);
		printLine("Listing Users...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("Users:");
		print(users);
	}

	public void getUser(String type, String op, String[] cmds, Map<String, Object> options) {
		String userId = (String)get(new String[] {"id", "uuid", "username", "email"}, options);
		User user = ssoClient.getUser(userId, null);
		printLine("Get User...");
		printLine("ID:", userId);
		printLine("User:");
		print(user);
	}

	public void createUser(String type, String op, String[] cmds, Map<String, Object> options) {
		User user = convert(options, User.class);
		printLine("Creating User...");
		print(user);
		URI uri = ssoClient.createUser(user, null);
		printLine("URI:", uri);
		print("Created User:");
		String id = UriUtils.extractId(uri);
		User user2 = ssoClient.getUser(id, null);
		print(user2);

	}

	
	public void updateUser(String type, String op, String[] cmds, Map<String, Object> options) {
		String userId = (String)get("user", options);
		User user = convert(options, User.class);
		printLine("Updating User...");
		print(user);
		ssoClient.updateUser(user, null);
		print("Updated User:");
		User user2 = ssoClient.getUser(userId, null);
		print(user2);
	}

	public void deleteUser(String type, String op, String[] cmds, Map<String, Object> options) {
		String userId = (String)get(new String[] {"id", "username"}, options);
		printLine("Deleting User...");
		printLine("ID:", userId);		
		ssoClient.deleteUser(userId, null);	
	}

	//
	// Groups
	//
	
	public void listGroups(String type, String op, String[] cmds, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		GroupFilter filter = convert(options, GroupFilter.class);
		Page<Group> groups = ssoClient.listGroups(filter, pageable);
		printLine("Listing Groups...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("Groups:");
		print(groups);
	}
	
	public void getGroup(String type, String op, String[] cmds, Map<String, Object> options) {
		String groupId = (String)get(new String[] {"id", "uuid"}, options);
		Group group = ssoClient.getGroup(groupId, null);
		printLine("Get Group...");
		printLine("ID:", groupId);
		printLine("Group:");
		print(group);
	}
	
	public void createGroup(String type, String op, String[] cmds, Map<String, Object> options) {
		Group group = convert(options, Group.class);
		printLine("Creating Group...");
		print(group);
		URI uri = ssoClient.createGroup(group, null);
		printLine("URI:", uri);
		String groupId = UriUtils.extractId(uri);
		Group group2 = ssoClient.getGroup(groupId, null);
		print("Created Group:");
		print(group2);
	}

	public void updateGroup(String type, String op, String[] cmds, Map<String, Object> options) {
		String groupId = (String)get(new String[] {"id", "uuid"}, options);
		Group group = convert(options, Group.class);
		printLine("Updating Group...");
		print(group);
		ssoClient.updateGroup(group, null);
		Group group2 = ssoClient.getGroup(groupId, null);
		print("Updated Group:");
		print(group2);

	}
	
	public void deleteGroup(String type, String op, String[] cmds, Map<String, Object> options) {
		String groupId = (String)get(new String[] {"id", "uuid"}, options);
		printLine("Deleting Group...");
		printLine("ID:", groupId);		
		ssoClient.deleteGroup(groupId, null);		
	}



	//
	// Group Members
	//
	
	public void addMember(String type, String op, String[] cmds, Map<String, Object> options) {
		String userId = (String)get("user", options);
		String groupId = (String)get("group", options);
		printLine("Adding Member...");
		printLine("User:", userId);		
		printLine("Group:", groupId);		
		ssoClient.addMemberToGroup(userId, groupId, null);
	}

	
	public void removeMember(String type, String op, String[] cmds, Map<String, Object> options) {
		String userId = (String)get("user", options);
		String groupId = (String)get("group", options);
		printLine("Removing Member...");
		printLine("User:", userId);		
		printLine("Group:", groupId);		
		ssoClient.removeMemberFromGroup(userId, groupId, null);
	}
	
	public void listGroupMembers(String type, String op, String[] cmds, Map<String, Object> options) {
		String groupId = (String)get("group", options);
		printLine("Listing Group Members...");
		printLine("Group:", groupId);
		Page<Member> members = ssoClient.listGroupMembers(groupId, null, null);
		printLine("Members:", groupId);
		print(members);
	}

	
	//
	// Invitation
	//
	

	public void invite(String type, String op, String[] cmds, Map<String, Object> options) {
		Invitation invitation = convert(options, Invitation.class);
		Boolean sendMail = get("sendMail", options, true);
		printLine(Boolean.TRUE.equals(sendMail) ? "Sending Invitation..." : "Creating Invitation...");
		printLine("Invitation", invitation);
		URI uri = ssoClient.invite(invitation, new InvitationOptions().withSendMail(sendMail));
		printLine("URI:", uri);
		String id = UriUtils.extractId(uri);
		Invitation invitation2 = ssoClient.getInvitation(id, null);
		print("Created Invitation:");
		print(invitation2);
		print("Token URI:");
		URI tokenUri = ssoClient.getInvitationToken(id, new InvitationOptions().withSendMail(false));
		print(tokenUri);
	}
	
	
	public void listInvitations(String type, String op, String[] cmds, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		InvitationFilter filter = convert(options, InvitationFilter.class);
		Page<Invitation> invitations = ssoClient.listInvitations(filter, pageable);
		printLine("Listing Invitations...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("Invitations:");
		print(invitations);
	}

	public void getInvitation(String type, String op, String[] cmds, Map<String, Object> options) {
		String invitationId = (String)get(new String[] {"id", "uuid"}, options);
		Invitation invitation = ssoClient.getInvitation(invitationId, null);
		printLine("Get Invitation...");
		printLine("ID:", invitationId);
		printLine("Invitation:");
		print(invitation);
	}


	
	public void updateInvitation(String type, String op, String[] cmds, Map<String, Object> options) {
		String invitationId = (String)get("invitation", options);
		Invitation invitation = convert(options, Invitation.class);
		printLine("Updating Invitation...");
		print(invitation);
		ssoClient.updateInvitation(invitation, null);
		print("Updated Invitation:");
		Invitation invitation2 = ssoClient.getInvitation(invitationId, null);
		print(invitation2);
	}

	public void deleteInvitation(String type, String op, String[] cmds, Map<String, Object> options) {
		String invitationId = (String)get(new String[] {"id", "uuid"}, options);
		printLine("Deleting Invitation...");
		printLine("ID:", invitationId);		
		ssoClient.deleteInvitation(invitationId, null);	
	}


	//
	// Roles
	//
	
	public void listRoles(String type, String op, String[] cmds, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		RoleFilter filter = convert(options, RoleFilter.class);
		Page<Role> roles = ssoClient.listRoles(filter, pageable);
		printLine("Listing Roles...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("Roles:");
		print(roles);
	}
	
	public void getRole(String type, String op, String[] cmds, Map<String, Object> options) {
		String roleId = (String)get(new String[] {"id", "uuid"}, options);
		Role role = ssoClient.getRole(roleId, null);
		printLine("Get Role...");
		printLine("ID:", roleId);
		printLine("Role:");
		print(role);
	}
	
	public void createRole(String type, String op, String[] cmds, Map<String, Object> options) {
		Role role = convert(options, Role.class);
		printLine("Creating Role...");
		print(role);
		URI uri = ssoClient.createRole(role, null);
		printLine("URI:", uri);
		String roleId = UriUtils.extractId(uri);
		Role role2 = ssoClient.getRole(roleId, null);
		print("Created Role:");
		print(role2);
	}

	public void updateRole(String type, String op, String[] cmds, Map<String, Object> options) {
		String roleId = (String)get(new String[] {"id", "uuid"}, options);
		Role role = convert(options, Role.class);
		printLine("Updating Role...");
		print(role);
		ssoClient.updateRole(role, null);
		Role role2 = ssoClient.getRole(roleId, null);
		print("Updated Role:");
		print(role2);

	}
	
	public void deleteRole(String type, String op, String[] cmds, Map<String, Object> options) {
		String roleId = (String)get(new String[] {"id", "uuid"}, options);
		printLine("Deleting Role...");
		printLine("ID:", roleId);		
		ssoClient.deleteRole(roleId, null);		
	}



	//
	// Clients
	//
	

	public void listClients(String type, String op, String[] cmds, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		ClientFilter filter = convert(options, ClientFilter.class);
		Page<Client> clients = ssoClient.listClients(filter, pageable);
		printLine("Listing Clients...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("Clients:");
		print(clients);
	}

	public void getClient(String type, String op, String[] cmds, Map<String, Object> options) {
		String clientId = (String)get(new String[] {"id", "uuid"}, options);
		Client client = ssoClient.getClient(clientId, null);
		printLine("Get Client...");
		printLine("ID:", clientId);
		printLine("Client:");
		print(client);
	}

	public void createClient(String type, String op, String[] cmds, Map<String, Object> options) {
		Client client = convert(options, Client.class);
		printLine("Creating Client...");
		print(client);
		URI uri = ssoClient.createClient(client, null);
		printLine("URI:", uri);
		print("Created Client:");
		String id = UriUtils.extractId(uri);
		Client client2 = ssoClient.getClient(id, null);
		print(client2);

	}

	
	public void updateClient(String type, String op, String[] cmds, Map<String, Object> options) {
		String clientId = (String)get("client", options);
		Client client = convert(options, Client.class);
		printLine("Updating Client...");
		print(client);
		ssoClient.updateClient(client, null);
		print("Updated Client:");
		Client client2 = ssoClient.getClient(clientId, null);
		print(client2);
	}

	public void deleteClient(String type, String op, String[] cmds, Map<String, Object> options) {
		String clientId = (String)get(new String[] {"id", "uuid"}, options);
		printLine("Deleting Client...");
		printLine("ID:", clientId);		
		ssoClient.deleteClient(clientId, null);	
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