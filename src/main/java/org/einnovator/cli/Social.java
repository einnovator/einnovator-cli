package org.einnovator.cli;

import static  org.einnovator.util.MappingUtils.convert;
import static org.einnovator.util.MappingUtils.updateObjectFromNonNull;

import java.net.URI;
import java.util.Map;

import org.einnovator.social.client.SocialClient;
import org.einnovator.social.client.config.SocialClientConfiguration;
import org.einnovator.social.client.model.Attachment;
import org.einnovator.social.client.model.Channel;
import org.einnovator.social.client.model.Message;
import org.einnovator.social.client.model.Reaction;
import org.einnovator.social.client.modelx.ChannelFilter;
import org.einnovator.sso.client.model.Client;
import org.einnovator.sso.client.model.Role;
import org.einnovator.util.PageOptions;
import org.einnovator.util.UriUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Component;


@Component
public class Social extends CommandRunnerBase {

	public static final String SOCIAL_DEFAULT_SERVER = "http://localhost:2030";
	public static final String SOCIAL_MONITOR_SERVER = "http://localhost:2031";

	private static final String CHANNEL_DEFAULT_FORMAT = "id,username,email,status";
	private static final String CHANNEL_WIDE_FORMAT = "id,username,email,firstName,lastName,title,address.country,phone.formatted,status,enabled";

	private static final String MESSAGE_DEFAULT_FORMAT = "id,name,type,owner";
	private static final String MESSAGE_WIDE_FORMAT = "id,name,type,owner,address.country";

	private static final String REACTION_DEFAULT_FORMAT = "id,user.username,group.name,title";
	private static final String REACTION_WIDE_FORMAT = "id,user.username,group.name,title,enabled";

	private static final String ATTACHMENT_DEFAULT_FORMAT ="id,invitee,type,owner,status";
	private static final String ATTACHMENT_WIDE_FORMAT ="id,invitee,type,owner,status,subject";
	
	@Autowired
	Sso sso;

	OAuth2AccessToken token;
	
	private SocialClient socialClient;

	private String server = SOCIAL_DEFAULT_SERVER;

	private SocialClientConfiguration config = new SocialClientConfiguration();

	
	
	@Override
	public String getPrefix() {
		return "social";
	}

	String[] SOCIALS_COMMANDS = new String[] { 
		"channels", "channel", "ch",
		};

	@Override
	protected String[] getCommands() {
		return SOCIALS_COMMANDS;
	}


	@Override
	public void init(String[] cmds, Map<String, Object> options, OAuth2RestTemplate template) {
		config.setServer(server);
		updateObjectFromNonNull(config, convert(options, SocialClientConfiguration.class));

		template = makeOAuth2RestTemplate(sso.getRequiredResourceDetails(), config.getConnection());
		super.init(cmds, options, template);
		
		socialClient = new SocialClient(template, config);
	}

	@Override
	public void setEndpoints(Map<String, Object> endpoints) {
		String server = (String)endpoints.get("server");
		if (server!=null) {
			this.server = server;
		}
	}

	public void run(String type, String op, String[] cmds, Map<String, Object> options) {

		switch (type) {
		case "channel": case "channels": case "m":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getChannel(type, op, cmds, options);
				break;
			case "list": case "l": case "":
				listChannels(type, op, cmds, options);
				break;
			case "create": case "c":
				createChannel(type, op, cmds, options);
				break;
			case "update": case "u":
				updateChannel(type, op, cmds, options);
				break;
			case "delete": case "del": case "rm": case "d":
				deleteChannel(type, op, cmds, options);
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
	// Channels
	//
	
	public void listChannels(String type, String op, String[] cmds, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		ChannelFilter filter = convert(options, ChannelFilter.class);
		Page<Channel> channels = socialClient.listChannels(filter, pageable);
		printLine("Listing Channels...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("Channels:");
		print(channels);
	}
	
	public void getChannel(String type, String op, String[] cmds, Map<String, Object> options) {
		String channelId = (String)get(new String[] {"id", "uuid"}, options);
		Channel channel = null; socialClient.getChannel(channelId, null);
		printLine("Get Channel...");
		printLine("ID:", channelId);
		printLine("Channel:");
		print(channel);
	}
	
	public void createChannel(String type, String op, String[] cmds, Map<String, Object> options) {
		Channel channel = convert(options, Channel.class);
		printLine("Creating Channel...");
		print(channel);
		URI uri = null; socialClient.createChannel(channel, null);
		printLine("URI:", uri);
		String channelId = UriUtils.extractId(uri);
		Channel channel2 = null; socialClient.getChannel(channelId, null);
		print("Created Channel:");
		print(channel2);
	}

	public void updateChannel(String type, String op, String[] cmds, Map<String, Object> options) {
		String channelId = (String)get(new String[] {"id", "uuid"}, options);
		Channel channel = convert(options, Channel.class);
		printLine("Updating Channel...");
		print(channel);
		socialClient.updateChannel(channel, null);
		Channel channel2 = null; socialClient.getChannel(channelId, null);
		print("Updated Channel:");
		print(channel2);

	}
	
	public void deleteChannel(String type, String op, String[] cmds, Map<String, Object> options) {
		String channelId = (String)get(new String[] {"id", "uuid"}, options);
		printLine("Deleting Channel...");
		printLine("ID:", channelId);		
		socialClient.deleteChannel(channelId, null);		
	}
	
	@Override
	protected String getDefaultFormat(Class<? extends Object> type) {
		if (Channel.class.equals(type)) {
			return CHANNEL_DEFAULT_FORMAT;
		}
		if (Message.class.equals(type)) {
			return MESSAGE_DEFAULT_FORMAT;
		}
		if (Reaction.class.equals(type)) {
			return REACTION_DEFAULT_FORMAT;
		}
		if (Attachment.class.equals(type)) {
			return ATTACHMENT_DEFAULT_FORMAT;
		}
		return null;
	}

	@Override
	protected String getWideFormat(Class<? extends Object> type) {
		if (Channel.class.equals(type)) {
			return CHANNEL_WIDE_FORMAT;
		}
		if (Message.class.equals(type)) {
			return MESSAGE_WIDE_FORMAT;
		}
		if (Reaction.class.equals(type)) {
			return REACTION_WIDE_FORMAT;
		}
		if (Attachment.class.equals(type)) {
			return ATTACHMENT_WIDE_FORMAT;
		}
		return null;
	}
	
	
	
	
}