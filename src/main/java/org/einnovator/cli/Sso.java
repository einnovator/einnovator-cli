package org.einnovator.cli;

import static  org.einnovator.util.MappingUtils.convert;
import static  org.einnovator.util.MappingUtils.updateObjectFromNonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.LinkedHashMap;
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
import org.einnovator.util.UriUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;


@Component
public class Sso extends CommandRunnerBase {

	
	private static final String SSO_DEFAULT_SERVER = "http://localhost:2001";
	
	public static String CONFIG_FOLDER = ".ei";
	public static String CONFIG_FILE = "config.json";
	public static String KEY_TOKEN = "token";
	
	public String DEFAULT_CLIENT = "application";
	public String DEFAULT_SECRET = "application$123";

	public String DEFAULT_USERNAME = "jsimao71@gmail.com";
	public String DEFAULT_USERNAME2 = "tdd@gmail.com";
	public String DEFAULT_PASSWORD = "Einnovator123!!";


	OAuth2AccessToken token;
	
	private SsoClient ssoClient;

	private SsoClientConfiguration config = new SsoClientConfiguration();

	String tokenUsername = DEFAULT_USERNAME;
	String tokenPassword = DEFAULT_PASSWORD;
	
	boolean init;
	
	@Override
	public String getPrefix() {
		return "sso";
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



	public void init(Map<String, Object> args) {
		config.setServer(SSO_DEFAULT_SERVER);
		config.setClientId(DEFAULT_CLIENT);
		config.setClientSecret(DEFAULT_SECRET);
		updateObjectFromNonNull(config, convert(args, SsoClientConfiguration.class));

		tokenUsername = get("u", args, DEFAULT_USERNAME);
		tokenPassword = get("p", args, DEFAULT_PASSWORD);
		
		init = true;
		ResourceOwnerPasswordResourceDetails resource = getRequiredResourceDetails();
		DefaultOAuth2ClientContext context = new DefaultOAuth2ClientContext();
		OAuth2RestTemplate template = new OAuth2RestTemplate(resource, context);
		template.setRequestFactory(config.getConnection().makeClientHttpRequestFactory());

		ssoClient = new SsoClient(template, config, false);
	}
	


	public ResourceOwnerPasswordResourceDetails getRequiredResourceDetails() {
		if (!init) {
			//TODO: read config file
			init = true;
		}
		ResourceOwnerPasswordResourceDetails resource = SsoClient.makeResourceOwnerPasswordResourceDetails(tokenUsername, tokenPassword, config);
		return resource;
	}

	
	public void run(String type, String op, Map<String, Object> argsMap, String[] args) {

		getToken(argsMap);
		
		switch (type) {
		case "login": case "l":
			login(argsMap);
			break;
		case "api": case "a":
			api(argsMap);
			break;
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
				getUser(argsMap);
				break;
			case "list": case "l": case "":
				listUsers(argsMap);
				break;
			case "create": case "c":
				createUser(argsMap);
				break;
			case "update": case "u":
				updateUser(argsMap);
				break;
			case "delete": case "del": case "d":
				deleteUser(argsMap);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "group": case "groups": case "g":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getGroup(argsMap);
				break;
			case "list": case "l": case "":
				listGroups(argsMap);
				break;
			case "create": case "c":
				createGroup(argsMap);
				break;
			case "update": case "u":
				updateGroup(argsMap);
				break;
			case "delete": case "del": case "d":
				deleteGroup(argsMap);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "member": case "members": case "m":
			listGroupMembers(argsMap);
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				//getMember(argsMap);
				break;
			case "list": case "l": case "":
				//listMembers(argsMap);
				break;
			case "create": case "c":
				addMember(argsMap);
				break;
			case "update": case "u":
				//updateMember(argsMap);
				break;
			case "delete": case "del": case "d":
				removeMember(argsMap);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "role": case "roles": case "r":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getRole(argsMap);
				break;
			case "list": case "l": case "":
				listRoles(argsMap);
				break;
			case "create": case "c":
				createRole(argsMap);
				break;
			case "update": case "u":
				updateRole(argsMap);
				break;
			case "delete": case "del": case "d":
				deleteRole(argsMap);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "invitation": case "invitations": case "invites": case "inv": case "i": 
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getInvitation(argsMap);
				break;
			case "list": case "l": case "":
				listInvitations(argsMap);
				break;
			case "create": case "c":
				invite(argsMap);
				break;
			case "update": case "u":
				updateInvitation(argsMap);
				break;
			case "delete": case "del": case "d":
				deleteInvitation(argsMap);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "client": case "clients": case "c":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getClient(argsMap);
				break;
			case "list": case "l": case "":
				listClients(argsMap);
				break;
			case "create": case "c":
				createClient(argsMap);
				break;
			case "update": case "u":
				updateClient(argsMap);
				break;
			case "delete": case "del": case "d":
				deleteClient(argsMap);
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

	public void login(Map<String, Object> args) {
		getToken(args);	
	}

	public void api(Map<String, Object> args) {
		api(args);	
	}

	
	public void getToken(Map<String, Object> args) {
		printLine("Credentials: ", tokenUsername, tokenPassword);
		printLine("Config:", config);
		token = ssoClient.getToken(tokenUsername, tokenPassword);
		printLine("Token: ", token);
		if (token!=null) {
			writeConfig(args, token.getValue());			
		}
	}


	private void writeConfig(Map<String, Object> args, String token) {
		File file = getConfigFile(args);
		Map<String, Object> config = makeConfig(args, token);
		try (PrintWriter writer = new PrintWriter(new FileOutputStream(file))) {
			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);
			writer.write(json);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
	}
	
	private Map<String, Object> makeConfig(Map<String, Object> args, String token) {
		Map<String, Object> config = new LinkedHashMap<>();
		config.putAll(MappingUtils.toMap(this.config));
		config.put(KEY_TOKEN, token);
		return config;

	}
	
	private File getConfigFile(Map<String, Object> args) {
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
		System.out.println(token);
	}

	
	//
	// User
	//
	
	public void listUsers(Map<String, Object> args) {
		Pageable pageable = convert(args, PageOptions.class).toPageRequest();
		UserFilter filter = convert(args, UserFilter.class);
		Page<User> users = ssoClient.listUsers(filter, pageable, null);
		printLine("Listing Users...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("Users:");
		print(users);
	}

	public void getUser(Map<String, Object> args) {
		String userId = get(new String[] {"id", "uuid", "username", "email"}, args, null);
		User user = ssoClient.getUser(userId, null, null);
		printLine("Get User...");
		printLine("ID:", userId);
		printLine("User:");
		print(user);
	}

	public void createUser(Map<String, Object> args) {
		User user = convert(args, User.class);
		printLine("Creating User...");
		print(user);
		URI uri = ssoClient.createUser(user, null, null);
		printLine("URI:", uri);
		print("Created User:");
		String id = UriUtils.extractId(uri);
		User user2 = ssoClient.getUser(id, null, null);
		print(user2);

	}

	
	public void updateUser(Map<String, Object> args) {
		String userId = get("user", args, null);
		User user = convert(args, User.class);
		printLine("Updating User...");
		print(user);
		ssoClient.updateUser(user, null, null);
		print("Updated User:");
		User user2 = ssoClient.getUser(userId, null, null);
		print(user2);
	}

	public void deleteUser(Map<String, Object> args) {
		String userId = get(new String[] {"id", "username"}, args, null);
		printLine("Deleting User...");
		printLine("ID:", userId);		
		ssoClient.deleteUser(userId, null, null);	
	}

	//
	// Groups
	//
	
	public void listGroups(Map<String, Object> args) {
		Pageable pageable = convert(args, PageOptions.class).toPageRequest();
		GroupFilter filter = convert(args, GroupFilter.class);
		Page<Group> groups = ssoClient.listGroups(filter, pageable, null);
		printLine("Listing Groups...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("Groups:");
		print(groups);
	}
	
	public void getGroup(Map<String, Object> args) {
		String groupId = get(new String[] {"id", "uuid"}, args, null);
		Group group = ssoClient.getGroup(groupId, null, null);
		printLine("Get Group...");
		printLine("ID:", groupId);
		printLine("Group:");
		print(group);
	}
	
	public void createGroup(Map<String, Object> args) {
		Group group = convert(args, Group.class);
		printLine("Creating Group...");
		print(group);
		URI uri = ssoClient.createGroup(group, null, null);
		printLine("URI:", uri);
		String groupId = UriUtils.extractId(uri);
		Group group2 = ssoClient.getGroup(groupId, null, null);
		print("Created Group:");
		print(group2);
	}

	public void updateGroup(Map<String, Object> args) {
		String groupId = get(new String[] {"id", "uuid"}, args, null);
		Group group = convert(args, Group.class);
		printLine("Updating Group...");
		print(group);
		ssoClient.updateGroup(group, null, null);
		Group group2 = ssoClient.getGroup(groupId, null, null);
		print("Updated Group:");
		print(group2);

	}
	
	public void deleteGroup(Map<String, Object> args) {
		String groupId = get(new String[] {"id", "uuid"}, args, null);
		printLine("Deleting Group...");
		printLine("ID:", groupId);		
		ssoClient.deleteGroup(groupId, null, null);		
	}



	//
	// Group Members
	//
	
	public void addMember(Map<String, Object> args) {
		String userId = get("user", args, null);
		String groupId = get("group", args, null);
		printLine("Adding Member...");
		printLine("User:", userId);		
		printLine("Group:", groupId);		
		ssoClient.addMemberToGroup(userId, groupId, null, null);
	}

	
	public void removeMember(Map<String, Object> args) {
		String userId = get("user", args, null);
		String groupId = get("group", args, null);
		printLine("Removing Member...");
		printLine("User:", userId);		
		printLine("Group:", groupId);		
		ssoClient.removeMemberFromGroup(userId, groupId, null, null);
	}
	
	public void listGroupMembers(Map<String, Object> args) {
		String groupId = get("group", args, null);
		printLine("Listing Group Members...");
		printLine("Group:", groupId);
		Page<Member> members = ssoClient.listGroupMembers(groupId, null, null, null);
		printLine("Members:", groupId);
		print(members);
	}

	
	//
	// Invitation
	//
	

	public void invite(Map<String, Object> args) {
		Invitation invitation = convert(args, Invitation.class);
		Boolean sendMail = get("sendMail", args, null);
		printLine(Boolean.TRUE.equals(sendMail) ? "Sending Invitation..." : "Creating Invitation...");
		printLine("Invitation", invitation);
		URI uri = ssoClient.invite(invitation, new InvitationOptions().withSendMail(sendMail), null);
		printLine("URI:", uri);
		String id = UriUtils.extractId(uri);
		Invitation invitation2 = ssoClient.getInvitation(id, null, null);
		print("Created Invitation:");
		print(invitation2);
		print("Token URI:");
		URI tokenUri = ssoClient.getInvitationToken(id, new InvitationOptions().withSendMail(false), null);
		print(tokenUri);
	}
	
	
	public void listInvitations(Map<String, Object> args) {
		Pageable pageable = convert(args, PageOptions.class).toPageRequest();
		InvitationFilter filter = convert(args, InvitationFilter.class);
		Page<Invitation> invitations = ssoClient.listInvitations(filter, pageable, null);
		printLine("Listing Invitations...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("Invitations:");
		print(invitations);
	}

	public void getInvitation(Map<String, Object> args) {
		String invitationId = get(new String[] {"id", "uuid"}, args, null);
		Invitation invitation = ssoClient.getInvitation(invitationId, null, null);
		printLine("Get Invitation...");
		printLine("ID:", invitationId);
		printLine("Invitation:");
		print(invitation);
	}


	
	public void updateInvitation(Map<String, Object> args) {
		String invitationId = get("invitation", args, null);
		Invitation invitation = convert(args, Invitation.class);
		printLine("Updating Invitation...");
		print(invitation);
		ssoClient.updateInvitation(invitation, null, null);
		print("Updated Invitation:");
		Invitation invitation2 = ssoClient.getInvitation(invitationId, null, null);
		print(invitation2);
	}

	public void deleteInvitation(Map<String, Object> args) {
		String invitationId = get(new String[] {"id", "uuid"}, args, null);
		printLine("Deleting Invitation...");
		printLine("ID:", invitationId);		
		ssoClient.deleteInvitation(invitationId, null, null);	
	}


	//
	// Roles
	//
	
	public void listRoles(Map<String, Object> args) {
		Pageable pageable = convert(args, PageOptions.class).toPageRequest();
		RoleFilter filter = convert(args, RoleFilter.class);
		Page<Role> roles = ssoClient.listRoles(filter, pageable, null);
		printLine("Listing Roles...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("Roles:");
		print(roles);
	}
	
	public void getRole(Map<String, Object> args) {
		String roleId = get(new String[] {"id", "uuid"}, args, null);
		Role role = ssoClient.getRole(roleId, null, null);
		printLine("Get Role...");
		printLine("ID:", roleId);
		printLine("Role:");
		print(role);
	}
	
	public void createRole(Map<String, Object> args) {
		Role role = convert(args, Role.class);
		printLine("Creating Role...");
		print(role);
		URI uri = ssoClient.createRole(role, null, null);
		printLine("URI:", uri);
		String roleId = UriUtils.extractId(uri);
		Role role2 = ssoClient.getRole(roleId, null, null);
		print("Created Role:");
		print(role2);
	}

	public void updateRole(Map<String, Object> args) {
		String roleId = get(new String[] {"id", "uuid"}, args, null);
		Role role = convert(args, Role.class);
		printLine("Updating Role...");
		print(role);
		ssoClient.updateRole(role, null, null);
		Role role2 = ssoClient.getRole(roleId, null, null);
		print("Updated Role:");
		print(role2);

	}
	
	public void deleteRole(Map<String, Object> args) {
		String roleId = get(new String[] {"id", "uuid"}, args, null);
		printLine("Deleting Role...");
		printLine("ID:", roleId);		
		ssoClient.deleteRole(roleId, null, null);		
	}



	//
	// Clients
	//
	

	public void listClients(Map<String, Object> args) {
		Pageable pageable = convert(args, PageOptions.class).toPageRequest();
		ClientFilter filter = convert(args, ClientFilter.class);
		Page<Client> clients = ssoClient.listClients(filter, pageable, null);
		printLine("Listing Clients...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("Clients:");
		print(clients);
	}

	public void getClient(Map<String, Object> args) {
		String clientId = get(new String[] {"id", "uuid"}, args, null);
		Client client = ssoClient.getClient(clientId, null, null);
		printLine("Get Client...");
		printLine("ID:", clientId);
		printLine("Client:");
		print(client);
	}

	public void createClient(Map<String, Object> args) {
		Client client = convert(args, Client.class);
		printLine("Creating Client...");
		print(client);
		URI uri = ssoClient.createClient(client, null, null);
		printLine("URI:", uri);
		print("Created Client:");
		String id = UriUtils.extractId(uri);
		Client client2 = ssoClient.getClient(id, null, null);
		print(client2);

	}

	
	public void updateClient(Map<String, Object> args) {
		String clientId = get("client", args, null);
		Client client = convert(args, Client.class);
		printLine("Updating Client...");
		print(client);
		ssoClient.updateClient(client, null, null);
		print("Updated Client:");
		Client client2 = ssoClient.getClient(clientId, null, null);
		print(client2);
	}

	public void deleteClient(Map<String, Object> args) {
		String clientId = get(new String[] {"id", "uuid"}, args, null);
		printLine("Deleting Client...");
		printLine("ID:", clientId);		
		ssoClient.deleteClient(clientId, null, null);	
	}

	
	//
	// Util
	//
	
	public <T> T get(String name, Map<String, Object> map, T defaultValue) {
		@SuppressWarnings("unchecked")
		T value = (T)map.get(name);
		if (value==null) {
			value = defaultValue;
		}
		return value;
	}
	
	public <T> T get(String[] names, Map<String, Object> map, T defaultValue) {
		for (String name: names) {
			T value = get(name, map, null);
			if (value!=null) {
				return value;
			}
		}
		return defaultValue;
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