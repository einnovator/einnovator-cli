package org.einnovator.cli;

import static  org.einnovator.util.MappingUtils.convert;
import static org.einnovator.util.MappingUtils.updateObjectFromNonNull;

import java.net.URI;
import java.util.Map;

import org.einnovator.social.client.SocialClient;
import org.einnovator.social.client.config.SocialClientConfiguration;
import org.einnovator.social.client.model.Channel;
import org.einnovator.social.client.modelx.ChannelFilter;
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

	private static final String SOCIALS_DEFAULT_SERVER = "http://localhost:2051";

	@Autowired
	Sso sso;

	OAuth2AccessToken token;
	
	private SocialClient socialClient;

	private SocialClientConfiguration config = new SocialClientConfiguration();

	
	
	@Override
	public String getPrefix() {
		return "social";
	}

	String[] SOCIALS_COMMANDS = new String[] { 
		"channels", "channel", "ch",
		};

	protected String[] getCommands() {
		return SOCIALS_COMMANDS;
	}



	public void init(Map<String, Object> args) {
		super.init(args, template);
		config.setServer(SOCIALS_DEFAULT_SERVER);
		updateObjectFromNonNull(config, convert(args, SocialClientConfiguration.class));

		ResourceOwnerPasswordResourceDetails resource = sso.getRequiredResourceDetails();
		DefaultOAuth2ClientContext context = new DefaultOAuth2ClientContext();
		OAuth2RestTemplate template = new OAuth2RestTemplate(resource, context);
		template.setRequestFactory(config.getConnection().makeClientHttpRequestFactory());
		
		socialClient = new SocialClient(template, config);
	}

	public void run(String type, String op, Map<String, Object> argsMap, String[] args) {

		switch (type) {

		case "channel": case "channels": case "m":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getChannel(argsMap);
				break;
			case "list": case "l": case "":
				listChannels(argsMap);
				break;
			case "create": case "c":
				createChannel(argsMap);
				break;
			case "update": case "u":
				updateChannel(argsMap);
				break;
			case "delete": case "del": case "d":
				deleteChannel(argsMap);
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
	
	public void listChannels(Map<String, Object> args) {
		Pageable pageable = convert(args, PageOptions.class).toPageRequest();
		ChannelFilter filter = convert(args, ChannelFilter.class);
		Page<Channel> channels = socialClient.listChannels(filter, pageable);
		printLine("Listing Channels...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("Channels:");
		print(channels);
	}
	
	public void getChannel(Map<String, Object> args) {
		String channelId = (String)get(new String[] {"id", "uuid"}, args);
		Channel channel = null; socialClient.getChannel(channelId, null);
		printLine("Get Channel...");
		printLine("ID:", channelId);
		printLine("Channel:");
		print(channel);
	}
	
	public void createChannel(Map<String, Object> args) {
		Channel channel = convert(args, Channel.class);
		printLine("Creating Channel...");
		print(channel);
		URI uri = null; socialClient.createChannel(channel, null);
		printLine("URI:", uri);
		String channelId = UriUtils.extractId(uri);
		Channel channel2 = null; socialClient.getChannel(channelId, null);
		print("Created Channel:");
		print(channel2);
	}

	public void updateChannel(Map<String, Object> args) {
		String channelId = (String)get(new String[] {"id", "uuid"}, args);
		Channel channel = convert(args, Channel.class);
		printLine("Updating Channel...");
		print(channel);
		socialClient.updateChannel(channel, null);
		Channel channel2 = null; socialClient.getChannel(channelId, null);
		print("Updated Channel:");
		print(channel2);

	}
	
	public void deleteChannel(Map<String, Object> args) {
		String channelId = (String)get(new String[] {"id", "uuid"}, args);
		printLine("Deleting Channel...");
		printLine("ID:", channelId);		
		socialClient.deleteChannel(channelId, null);		
	}
	
	
	
}