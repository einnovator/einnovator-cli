package org.einnovator.cli;

import static  org.einnovator.util.MappingUtils.convert;
import static org.einnovator.util.MappingUtils.updateObjectFromNonNull;

import java.net.URI;
import java.util.Map;

import org.einnovator.notifications.client.NotificationsClient;
import org.einnovator.notifications.client.config.NotificationsClientConfiguration;
import org.einnovator.notifications.client.model.Event;
import org.einnovator.notifications.client.model.Job;
import org.einnovator.notifications.client.model.Notification;
import org.einnovator.notifications.client.model.NotificationType;
import org.einnovator.notifications.client.model.Template;
import org.einnovator.notifications.client.modelx.JobFilter;
import org.einnovator.notifications.client.modelx.NotificationFilter;
import org.einnovator.notifications.client.modelx.NotificationTypeFilter;
import org.einnovator.notifications.client.modelx.TemplateFilter;
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
public class Notifications extends CommandRunnerBase {

	private static final String NOTIFICATIONS_DEFAULT_SERVER = "http://localhost:2011";

	@Autowired
	Sso sso;

	OAuth2AccessToken token;
	
	private NotificationsClient notificationsClient;

	private NotificationsClientConfiguration config = new NotificationsClientConfiguration();

	
	
	@Override
	public String getPrefix() {
		return "notifications";
	}

	String[] NOTIFICATIONS_COMMANDS = new String[] { 
		"events", "event", "ev", "e",
		"notifications", "notification", "notific", "n",
		"notification-types", "notific-type", "ntype", "nt",
		"templates", "template", "templ", 
		"jobs", "job", "j",
		};

	protected String[] getCommands() {
		return NOTIFICATIONS_COMMANDS;
	}


	public void init(String[] cmds, Map<String, Object> args, OAuth2RestTemplate template) {
		config.setServer(NOTIFICATIONS_DEFAULT_SERVER);
		updateObjectFromNonNull(config, convert(args, NotificationsClientConfiguration.class));

		template = makeOAuth2RestTemplate(sso.getRequiredResourceDetails(), config.getConnection());
		super.init(cmds, args, template);
		notificationsClient = new NotificationsClient(template, config);
	}

	public void run(String type, String op, Map<String, Object> argsMap, String[] args) {

		switch (type) {
		case "events": case "event": case "ev": case "e":
			switch (op) {
			case "publish": case "pub": case "p":
				publish(argsMap);
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "notifications": case "notification": case "notific": case "n":
			switch (op) {
			//case "get": case "g": case "show": case "s": case "view": case "v":
			//	getNotification(argsMap);
			//	break;
			case "list": case "l": case "":
				listNotifications(argsMap);
				break;
			case "count": case "c":
				countNotifications(argsMap);
				break;
			case "delete": case "del": case "d":
				deleteNotification(argsMap);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "job": case "jobs": case "m":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getJob(argsMap);
				break;
			case "list": case "l": case "":
				listJobs(argsMap);
				break;
			case "create": case "c":
				createJob(argsMap);
				break;
			case "update": case "u":
				updateJob(argsMap);
				break;
			case "delete": case "del": case "d":
				deleteJob(argsMap);
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
	// Notifications
	//
	
	public void listNotifications(Map<String, Object> args) {
		Pageable pageable = convert(args, PageOptions.class).toPageRequest();
		NotificationFilter filter = convert(args, NotificationFilter.class);
		Page<Notification> notifications = notificationsClient.listNotifications(filter, pageable);
		printLine("Listing Notifications...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("Notifications:");
		print(notifications);
	}
	
	public void countNotifications(Map<String, Object> args) {
		NotificationFilter filter = convert(args, NotificationFilter.class);
		Long n = notificationsClient.countNotifications(filter);
		printLine("Count Notification...");
		printLine(n);
	}
	
	public void publish(Map<String, Object> args) {
		Event event = convert(args, Event.class);
		printLine("Publish Event...");
		print(event);
		notificationsClient.publishEvent(event);
	}

	
	public void deleteNotification(Map<String, Object> args) {
		String notificationId = (String)get(new String[] {"id", "uuid"}, args);
		printLine("Deleting Notification...");
		printLine("ID:", notificationId);		
		notificationsClient.deleteNotification(notificationId, null);		
	}

	//
	// NotificationTypes
	//
	
	public void listNotificationTypes(Map<String, Object> args) {
		Pageable pageable = convert(args, PageOptions.class).toPageRequest();
		NotificationTypeFilter filter = convert(args, NotificationTypeFilter.class);
		Page<NotificationType> notificationTypes = notificationsClient.listNotificationTypes(filter, pageable);
		printLine("Listing NotificationTypes...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("NotificationTypes:");
		print(notificationTypes);
	}
	
	public void getNotificationType(Map<String, Object> args) {
		String notificationTypeId = (String)get(new String[] {"id", "uuid"}, args);
		NotificationType notificationType = null; notificationsClient.getNotificationType(notificationTypeId, null);
		printLine("Get NotificationType...");
		printLine("ID:", notificationTypeId);
		printLine("NotificationType:");
		print(notificationType);
	}
	
	public void createNotificationType(Map<String, Object> args) {
		NotificationType notificationType = convert(args, NotificationType.class);
		printLine("Creating NotificationType...");
		print(notificationType);
		URI uri = null; notificationsClient.createNotificationType(notificationType, null);
		printLine("URI:", uri);
		String notificationTypeId = UriUtils.extractId(uri);
		NotificationType notificationType2 = null; notificationsClient.getNotificationType(notificationTypeId, null);
		print("Created NotificationType:");
		print(notificationType2);
	}

	public void updateNotificationType(Map<String, Object> args) {
		String notificationTypeId = (String)get(new String[] {"id", "uuid"}, args);
		NotificationType notificationType = convert(args, NotificationType.class);
		printLine("Updating NotificationType...");
		print(notificationType);
		notificationsClient.updateNotificationType(notificationType, null);
		NotificationType notificationType2 = null; notificationsClient.getNotificationType(notificationTypeId, null);
		print("Updated NotificationType:");
		print(notificationType2);

	}
	
	public void deleteNotificationType(Map<String, Object> args) {
		String notificationTypeId = (String)get(new String[] {"id", "uuid"}, args);
		printLine("Deleting NotificationType...");
		printLine("ID:", notificationTypeId);		
		notificationsClient.deleteNotificationType(notificationTypeId, null);		
	}

	
	//
	// Templates
	//
	
	public void listTemplates(Map<String, Object> args) {
		Pageable pageable = convert(args, PageOptions.class).toPageRequest();
		TemplateFilter filter = convert(args, TemplateFilter.class);
		Page<Template> templates = notificationsClient.listTemplates(filter, pageable);
		printLine("Listing Templates...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("Templates:");
		print(templates);
	}
	
	public void getTemplate(Map<String, Object> args) {
		String templateId = (String)get(new String[] {"id", "uuid"}, args);
		Template template = null; notificationsClient.getTemplate(templateId, null);
		printLine("Get Template...");
		printLine("ID:", templateId);
		printLine("Template:");
		print(template);
	}
	
	public void createTemplate(Map<String, Object> args) {
		Template template = convert(args, Template.class);
		printLine("Creating Template...");
		print(template);
		URI uri = null; notificationsClient.createTemplate(template, null);
		printLine("URI:", uri);
		String templateId = UriUtils.extractId(uri);
		Template template2 = null; notificationsClient.getTemplate(templateId, null);
		print("Created Template:");
		print(template2);
	}

	public void updateTemplate(Map<String, Object> args) {
		String templateId = (String)get(new String[] {"id", "uuid"}, args);
		Template template = convert(args, Template.class);
		printLine("Updating Template...");
		print(template);
		notificationsClient.updateTemplate(template, null);
		Template template2 = null; notificationsClient.getTemplate(templateId, null);
		print("Updated Template:");
		print(template2);

	}
	
	public void deleteTemplate(Map<String, Object> args) {
		String templateId = (String)get(new String[] {"id", "uuid"}, args);
		printLine("Deleting Template...");
		printLine("ID:", templateId);		
		notificationsClient.deleteTemplate(templateId, null);		
	}

	//
	// Jobs
	//
	
	public void listJobs(Map<String, Object> args) {
		Pageable pageable = convert(args, PageOptions.class).toPageRequest();
		JobFilter filter = convert(args, JobFilter.class);
		Page<Job> jobs = notificationsClient.listJobs(filter, pageable);
		printLine("Listing Jobs...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("Jobs:");
		print(jobs);
	}
	
	public void getJob(Map<String, Object> args) {
		String jobId = (String)get(new String[] {"id", "uuid"}, args);
		Job job = null; notificationsClient.getJob(jobId, null);
		printLine("Get Job...");
		printLine("ID:", jobId);
		printLine("Job:");
		print(job);
	}
	
	public void createJob(Map<String, Object> args) {
		Job job = convert(args, Job.class);
		printLine("Creating Job...");
		print(job);
		URI uri = null; notificationsClient.createJob(job, null);
		printLine("URI:", uri);
		String jobId = UriUtils.extractId(uri);
		Job job2 = null; notificationsClient.getJob(jobId, null);
		print("Created Job:");
		print(job2);
	}

	public void updateJob(Map<String, Object> args) {
		String jobId = (String)get(new String[] {"id", "uuid"}, args);
		Job job = convert(args, Job.class);
		printLine("Updating Job...");
		print(job);
		notificationsClient.updateJob(job, null);
		Job job2 = null; notificationsClient.getJob(jobId, null);
		print("Updated Job:");
		print(job2);

	}
	
	public void deleteJob(Map<String, Object> args) {
		String jobId = (String)get(new String[] {"id", "uuid"}, args);
		printLine("Deleting Job...");
		printLine("ID:", jobId);		
		notificationsClient.deleteJob(jobId, null);		
	}
	
	
	
}