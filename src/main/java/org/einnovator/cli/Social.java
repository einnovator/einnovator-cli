package org.einnovator.cli;

import static org.einnovator.util.MappingUtils.updateObjectFromNonNull;

import java.net.URI;
import java.util.Map;
import java.util.ResourceBundle;

import org.einnovator.social.client.SocialClient;
import org.einnovator.social.client.config.SocialClientConfiguration;
import org.einnovator.social.client.model.Attachment;
import org.einnovator.social.client.model.Channel;
import org.einnovator.social.client.model.Message;
import org.einnovator.social.client.model.Reaction;
import org.einnovator.social.client.modelx.ChannelFilter;
import org.einnovator.util.PageOptions;
import org.einnovator.util.UriUtils;
import org.einnovator.util.web.RequestOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
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

	String[][] SOCIALS_COMMANDS = new String[][] { 
		new String[] {"channels", "channel", "ch"},
	};

	@Override
	protected String[][] getCommands() {
		return SOCIALS_COMMANDS;
	}


	@Override
	public void init(String[] cmds, Map<String, Object> options, OAuth2RestTemplate template, boolean interactive, ResourceBundle bundle) {
		if (!init) {
			super.init(cmds, options, template, interactive, bundle);
			config.setServer(server);
			updateObjectFromNonNull(config, convert(options, SocialClientConfiguration.class));

			template = makeOAuth2RestTemplate(sso.getRequiredResourceDetails(), config.getConnection());
			super.init(cmds, options, template, interactive, bundle);
			
			socialClient = new SocialClient(template, config);
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

	public void run(String type, String op, String[] cmds, Map<String, Object> options) {

		switch (type) {
		case "help":
			printUsage();
			break;
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
		debug("Channel: %s %s", filter, pageable);
		Page<Channel> channels = socialClient.listChannels(filter, pageable);
		print(channels);
	}
	
	public void getChannel(String type, String op, String[] cmds, Map<String, Object> options) {
		String channelId = argId(op, cmds);
		debug("Channel: %s", channelId);
		Channel channel = socialClient.getChannel(channelId, null);
		printObj(channel);
	}
	
	public void createChannel(String type, String op, String[] cmds, Map<String, Object> options) {
		Channel channel = convert(options, Channel.class);
		channel.setName(argName(op, cmds));
		debug("Creating Channel: %s", channel);
		URI uri = socialClient.createChannel(channel, new RequestOptions());
		if (isEcho()) {
			printLine("Channel URI:", uri);
			String id = UriUtils.extractId(uri);
			Channel channel2 = socialClient.getChannel(id, null);
			printObj(channel2);			
		}
	}

	public void updateChannel(String type, String op, String[] cmds, Map<String, Object> options) {
		String channelId = argId(op, cmds);
		Channel channel = convert(options, Channel.class);
		debug("Updating Channel: %s %s", channelId, channel);
		socialClient.updateChannel(channel, null);
		if (isEcho()) {
			Channel channel2 = socialClient.getChannel(channelId, null);
			printObj(channel2);			
		}
	}
	
	public void deleteChannel(String type, String op, String[] cmds, Map<String, Object> options) {
		String channelId = argId(op, cmds);
		debug("Deleting Channel: %s", channelId);
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