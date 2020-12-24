package org.einnovator.cli;

import static org.einnovator.util.MappingUtils.updateObjectFromNonNull;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.einnovator.social.client.SocialClient;
import org.einnovator.social.client.config.SocialClientConfiguration;
import org.einnovator.social.client.model.Attachment;
import org.einnovator.social.client.model.Channel;
import org.einnovator.social.client.model.Message;
import org.einnovator.social.client.model.Reaction;
import org.einnovator.social.client.modelx.ChannelFilter;
import org.einnovator.social.client.modelx.ChannelOptions;
import org.einnovator.social.client.modelx.MessageFilter;
import org.einnovator.social.client.modelx.MessageOptions;
import org.einnovator.social.client.modelx.ReactionFilter;
import org.einnovator.social.client.modelx.ReactionOptions;
import org.einnovator.util.PageOptions;
import org.einnovator.util.StringUtil;
import org.einnovator.util.UriUtils;
import org.einnovator.util.web.RequestOptions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


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

	OAuth2AccessToken token;
	
	private SocialClient socialClient;

	private String server = SOCIAL_DEFAULT_SERVER;

	private SocialClientConfiguration config = new SocialClientConfiguration();

	
	
	@Override
	public String getName() {
		return "social";
	}

	@Override
	protected String getServer() {
		return server;
	}

	String[][] SOCIALS_COMMANDS = c( 
		c("channel", "channels"),
		c("message", "messages", "msg"),
		c("reaction", "reactions")
	);

	@Override
	protected String[][] getCommands() {
		return SOCIALS_COMMANDS;
	}

	static Map<String, String[][]> subcommands;

	static {
		Map<String, String[][]> map = new LinkedHashMap<>();
		subcommands = map;
		map.put("channel", c(c("ls", "list"), c("get"), c("schema", "meta"), 
			c("create", "add"), c("update"), c("delete", "del", "rm"),
			c("help")));
		map.put("message", c(c("ls", "list"), c("get"), c("schema", "meta"), 
			c("create", "add"), c("update"), c("delete", "del", "remove", "rm"), 
			c("help")));
		map.put("reaction", c(c("ls", "list"), c("get"), c("schema", "meta"), 
			c("create", "add"), c("update"), c("delete", "del", "remove", "rm"), c("help")));
	}

	@Override
	protected Map<String, String[][]> getSubCommands() {
		return subcommands;
	}
	
	@Override
	public void init(String[] cmds, Map<String, Object> options, RestTemplate template, boolean interactive, ResourceBundle bundle) {
		if (!init) {
			super.init(cmds, options, template, interactive, bundle);
			config.setServer(server);
			updateObjectFromNonNull(config, convert(options, SocialClientConfiguration.class));
			if (template instanceof OAuth2RestTemplate) {
				socialClient = new SocialClient((OAuth2RestTemplate)template, config);				
			} else {
				singleuserNotSupported();
				exit(-1);
			}
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
		setLine(type, op, cmds, options);
		switch (type) {
		case "help": case "":
			printUsage();
			return;
		}
		if (!setupToken()) {
			return;
		}
		switch (type) {
		case "channel": case "channels":
			switch (op) {
			case "help": case "":
				printUsage1();
				break;
			case "get": 
				getChannel(cmds, options);
				break;
			case "ls": case "list":
				listChannels(cmds, options);
				break;
			case "create": case "add":
				createChannel(cmds, options);
				break;
			case "update":
				updateChannel(cmds, options);
				break;
			case "delete": case "del": case "rm": case "remove":
				deleteChannel(cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "message": case "msg":
			switch (op) {
			case "help": case "":
				printUsage1();
				break;
			case "get": 
				getMessage(cmds, options);
				break;
			case "ls": case "list":
				listMessages(cmds, options);
				break;
			case "post": case "create": case "add":
				postMessage(cmds, options);
				break;
			case "update":
				updateMessage(cmds, options);
				break;
			case "delete": case "del": case "rm": case "remove":
				deleteMessage(cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "reaction":
			switch (op) {
			case "help": case "":
				printUsage1();
				break;
			case "get": 
				getReaction(cmds, options);
				break;
			case "ls": case "list":
				listReactions(cmds, options);
				break;
			case "post": case "create": case "add":
				postReaction(cmds, options);
				break;
			case "update":
				updateReaction(cmds, options);
				break;
			case "delete": case "del": case "rm": case "remove":
				deleteReaction(cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}		
			break;
		default:
			invalidOp(type);
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
	
	public void listChannels(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		ChannelFilter filter = convert(options, ChannelFilter.class);
		debug("Channel: %s %s", filter, pageable);
		Page<Channel> channels = socialClient.listChannels(filter, pageable);
		print(channels);
	}
	
	public void getChannel(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String channelId = argId(op, cmds);
		debug("Channel: %s", channelId);
		Channel channel = socialClient.getChannel(channelId, null);
		printObj(channel);
	}
	
	public void createChannel(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		Channel channel = convert(options, Channel.class);
		channel.setName(argName(op, cmds));
		ChannelOptions options_ = convert(options, ChannelOptions.class);
		debug("Creating Channel: %s %s", channel, options_);
		URI uri = socialClient.createChannel(channel, new RequestOptions());
		if (isEcho()) {
			printLine("Channel URI:", uri);
			String id = UriUtils.extractId(uri);
			Channel channel2 = socialClient.getChannel(id, options_);
			printObj(channel2);			
		}
	}

	public void updateChannel(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String channelId = argId(op, cmds);
		Channel channel = convert(options, Channel.class);
		ChannelOptions options_ = convert(options, ChannelOptions.class);
		debug("Updating Channel: %s %s %s", channelId, channel, options_);
		socialClient.updateChannel(channel, null);
		if (isEcho()) {
			Channel channel2 = socialClient.getChannel(channelId, options_);
			printObj(channel2);			
		}
	}
	
	public void deleteChannel(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String channelId = argId(op, cmds);
		ChannelOptions options_ = convert(options, ChannelOptions.class);
		debug("Deleting Channel: %s %s", channelId, options_);
		socialClient.deleteChannel(channelId, options_);
		if (isEcho()) {
			listChannels(cmds, options);
		}
	}
	
	//
	// Channels
	//
	
	public void listMessages(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		MessageFilter filter = convert(options, MessageFilter.class);
		String channelId = argId(op, cmds);
		debug("Message: %s %s %s", channelId, filter, pageable);
		Page<Message> messages = socialClient.listMessages(channelId, filter, pageable);
		print(messages);
	}
	
	public void getMessage(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String channelId = argId(op, cmds);
		String messageId = argId1(op, cmds);
		debug("Message: %s %s", channelId, messageId);
		Message message = socialClient.getMessage(channelId, messageId, null);
		printObj(message);
	}
	
	public void postMessage(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String channelId = argId(op, cmds);
		Message message = convert(options, Message.class);
		processMessageOptions(message, cmds, options);
		MessageOptions options_ = convert(options, MessageOptions.class);
		debug("Creating Message: %s %s %s", channelId, message, options_);
		URI uri = socialClient.postMessage(channelId, message, new RequestOptions());
		if (isEcho()) {
			printLine("Message URI:", uri);
			String id = UriUtils.extractId(uri);
			Message message2 = socialClient.getMessage(channelId, id, options_);
			printObj(message2);			
		}
	}

	public void updateMessage(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String channelId = argId(op, cmds);
		String messageId = argId1(op, cmds);
		Message message = convert(options, Message.class);
		processMessageOptions(message, cmds, options);
		MessageOptions options_ = convert(options, MessageOptions.class);
		debug("Updating Message: %s %s %s %s", channelId, messageId, message, options_);
		socialClient.updateMessage(channelId, message, options_);
		if (isEcho()) {
			Message message2 = socialClient.getMessage(channelId, messageId, null);
			printObj(message2);			
		}
	}
	
	private void processMessageOptions(Message message, String[] cmds, Map<String, Object> options) {
		String content = argn(op, cmds, 1, false);
		if (StringUtil.hasText(content)) {
			message.setContent(content);
		}
		content = (String)options.get("m");
		if (StringUtil.hasText(content)) {
			message.setContent(content);			
		}
	}
	
	public void deleteMessage(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String channelId = argId(op, cmds);
		String messageId = argId1(op, cmds);
		MessageOptions options_ = convert(options, MessageOptions.class);
		debug("Deleting Message: %s %s %s", channelId, messageId, options_);
		socialClient.deleteMessage(channelId, messageId, options_);
		if (isEcho()) {
			listMessages(cmds, options);
		}
	}

	//
	// Reaction
	//
	
	public void listReactions(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		ReactionFilter filter = convert(options, ReactionFilter.class);
		String channelId = argId(op, cmds);
		String messageId = argId1(op, cmds);
		debug("Reactions: %s %s %s %s", channelId, messageId, filter, pageable);
		Page<Reaction> reactions = socialClient.listReactions(channelId, messageId, filter, pageable);
		print(reactions);
	}
	
	public void getReaction(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String channelId = argId(op, cmds);
		String messageId = argId1(op, cmds);
		String reactionId = argId2(op, cmds);
		ReactionOptions options_ = convert(options, ReactionOptions.class);
		debug("Reaction: %s %s %s", channelId, messageId, reactionId);
		Reaction reaction = socialClient.getReaction(channelId, messageId, reactionId, options_);
		printObj(reaction);
	}
	
	public void postReaction(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String channelId = argId(op, cmds);
		String messageId = argId1(op, cmds);
		Reaction reaction = convert(options, Reaction.class);
		processReactionOptions(reaction, cmds, options);
		ReactionOptions options_ = convert(options, ReactionOptions.class);
		debug("Creating Reaction: %s %s %s", channelId, messageId, reaction);
		URI uri = socialClient.postReaction(channelId, messageId, reaction, options_);
		if (isEcho()) {
			printLine("Reaction URI:", uri);
			String id = UriUtils.extractId(uri);
			Reaction reaction2 = socialClient.getReaction(channelId, messageId, id, null);
			printObj(reaction2);			
		}
	}

	public void updateReaction(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String channelId = argId(op, cmds);
		String messageId = argId1(op, cmds);
		String reactionId = argId2(op, cmds);
		Reaction reaction = convert(options, Reaction.class);
		processReactionOptions(reaction, cmds, options);
		debug("Updating Reaction: %s %s %s", channelId, reactionId, reaction);
		ReactionOptions options_ = convert(options, ReactionOptions.class);
		socialClient.updateReaction(channelId, messageId, reaction, options_);
		if (isEcho()) {
			Reaction reaction2 = socialClient.getReaction(channelId, messageId, reactionId, options_);
			printObj(reaction2);			
		}
	}
	
	private void processReactionOptions(Reaction reaction, String[] cmds, Map<String, Object> options) {
	}
	
	public void deleteReaction(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String channelId = argId(op, cmds);
		String messageId = argId1(op, cmds);
		String reactionId = argId1(op, cmds);
		debug("Deleting Reaction: %s %s %s", channelId, messageId, reactionId);
		socialClient.deleteReaction(channelId, messageId, reactionId, null);
		if (isEcho()) {
			listReactions(cmds, options);
		}
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