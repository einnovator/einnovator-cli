package org.einnovator.cli;

import static  org.einnovator.util.MappingUtils.convert;
import static org.einnovator.util.MappingUtils.updateObjectFromNonNull;

import java.net.URI;
import java.util.Date;
import java.util.Map;

import org.einnovator.notifications.client.NotificationsClient;
import org.einnovator.notifications.client.config.NotificationsClientConfiguration;
import org.einnovator.notifications.client.model.Event;
import org.einnovator.notifications.client.model.EventType;
import org.einnovator.notifications.client.model.Job;
import org.einnovator.notifications.client.model.Notification;
import org.einnovator.notifications.client.model.NotificationType;
import org.einnovator.notifications.client.model.ObjectInfo;
import org.einnovator.notifications.client.model.Template;
import org.einnovator.notifications.client.model.TrackedEvent;
import org.einnovator.notifications.client.modelx.JobFilter;
import org.einnovator.notifications.client.modelx.NotificationFilter;
import org.einnovator.notifications.client.modelx.NotificationTypeFilter;
import org.einnovator.notifications.client.modelx.TemplateFilter;
import org.einnovator.util.PageOptions;
import org.einnovator.util.UriUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Component;


@Component
public class Notifications extends CommandRunnerBase {

	public static final String NOTIFICATIONS_DEFAULT_SERVER = "http://localhost:2010";
	public static final String NOTIFICATIONS_MONITOR_SERVER = "http://localhost:2011";


	private static final String NOTIFICATIONTYPE_DEFAULT_FORMAT = "id,typeId,category,subcategory,app,admin";
	private static final String NOTIFICATIONTYPE_WIDE_FORMAT = "id,typeId,category,subcategory,app,admin,label";

	private static final String NOTIFICATION_DEFAULT_FORMAT = "id,targets,source.id,source.type,action.id,app,principal.id:principal,date";
	private static final String NOTIFICATION_WIDE_FORMAT = "id,targets,source.id,source.type,action.id,app,principal.id:principal,date,formattedDate:date,subject";

	private static final String TEMPLATE_DEFAULT_FORMAT = "id,name,category,medium,contentType,app";
	private static final String TEMPLATE_WIDE_FORMAT = "id,name,category,medium,contentType,app,subject";

	private static final String JOB_DEFAULT_FORMAT ="id,name,receiverType:type,system,status,dryRun,submitDate:submitted,startDate:started,completedDate:completed,readCount:read,processCount:proc,writeCount:write,skipCount:skip";
	private static final String JOB_WIDE_FORMAT ="id,name,receiverType:type,system,status,templateId,submitDate:submitted,startDate:started,completedDate:completed,readCount:read,processCount:proc,writeCount:write,skipCount:skip,skip:offset,max";

	private static final String TRACKEDEVENT_DEFAULT_FORMAT = "id,username,type,app,xid,xname,xtype,source.id,source.type,action.id,date";
	private static final String TRACKEDEVENT_WIDE_FORMAT = "id,username,type,app,xid,xname,xtype,source.id,source.type,action.id,date,dateFormatted:date";

	@Autowired
	Sso sso;

	OAuth2AccessToken token;
	
	private NotificationsClient notificationsClient;

	private String server = NOTIFICATIONS_DEFAULT_SERVER;

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

	@Override
	protected String[] getCommands() {
		return NOTIFICATIONS_COMMANDS;
	}


	@Override
	public void init(String[] cmds, Map<String, Object> options, OAuth2RestTemplate template) {
		config.setServer(server);
		updateObjectFromNonNull(config, convert(options, NotificationsClientConfiguration.class));

		template = makeOAuth2RestTemplate(sso.getRequiredResourceDetails(), config.getConnection());
		super.init(cmds, options, template);
		notificationsClient = new NotificationsClient(template, config);
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
		case "events": case "event": case "ev": case "e":
			switch (op) {
			case "publish": case "pub": case "p":
				publish(type, op, cmds, options);
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "notifications": case "notification": case "notific": case "n":
			switch (op) {
			//case "get": case "g": case "show": case "s": case "view": case "v":
			//	getNotification(type, op, cmds, options);
			//	break;
			case "list": case "l": case "":
				listNotifications(type, op, cmds, options);
				break;
			case "count": case "c":
				countNotifications(type, op, cmds, options);
				break;
			case "delete": case "del": case "d":
				deleteNotification(type, op, cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "job": case "jobs": case "m":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getJob(type, op, cmds, options);
				break;
			case "list": case "l": case "":
				listJobs(type, op, cmds, options);
				break;
			case "create": case "c":
				createJob(type, op, cmds, options);
				break;
			case "update": case "u":
				updateJob(type, op, cmds, options);
				break;
			case "delete": case "del": case "d":
				deleteJob(type, op, cmds, options);
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
	
	public void listNotifications(String type, String op, String[] cmds, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		NotificationFilter filter = convert(options, NotificationFilter.class);
		Page<Notification> notifications = notificationsClient.listNotifications(filter, pageable);
		printLine("Listing Notifications...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("Notifications:");
		print(notifications);
	}
	
	public void countNotifications(String type, String op, String[] cmds, Map<String, Object> options) {
		NotificationFilter filter = convert(options, NotificationFilter.class);
		Long n = notificationsClient.countNotifications(filter);
		printLine("Count Notification...");
		printLine(n);
	}
	
	public void publish(String type, String op, String[] cmds, Map<String, Object> options) {
		Event event = convert(options, Event.class);
		printLine("Publish TrackedEvent...");
		print(event);
		notificationsClient.publishEvent(event);
	}

	
	public void deleteNotification(String type, String op, String[] cmds, Map<String, Object> options) {
		String notificationId = (String)get(new String[] {"id", "uuid"}, options);
		printLine("Deleting Notification...");
		printLine("ID:", notificationId);		
		notificationsClient.deleteNotification(notificationId, null);		
	}

	//
	// NotificationTypes
	//
	
	public void listNotificationTypes(String type, String op, String[] cmds, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		NotificationTypeFilter filter = convert(options, NotificationTypeFilter.class);
		Page<NotificationType> notificationTypes = notificationsClient.listNotificationTypes(filter, pageable);
		printLine("Listing NotificationTypes...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("NotificationTypes:");
		print(notificationTypes);
	}
	
	public void getNotificationType(String type, String op, String[] cmds, Map<String, Object> options) {
		String notificationTypeId = (String)get(new String[] {"id", "uuid"}, options);
		NotificationType notificationType = null; notificationsClient.getNotificationType(notificationTypeId, null);
		printLine("Get NotificationType...");
		printLine("ID:", notificationTypeId);
		printLine("NotificationType:");
		print(notificationType);
	}
	
	public void createNotificationType(String type, String op, String[] cmds, Map<String, Object> options) {
		NotificationType notificationType = convert(options, NotificationType.class);
		printLine("Creating NotificationType...");
		print(notificationType);
		URI uri = null; notificationsClient.createNotificationType(notificationType, null);
		printLine("URI:", uri);
		String notificationTypeId = UriUtils.extractId(uri);
		NotificationType notificationType2 = null; notificationsClient.getNotificationType(notificationTypeId, null);
		print("Created NotificationType:");
		print(notificationType2);
	}

	public void updateNotificationType(String type, String op, String[] cmds, Map<String, Object> options) {
		String notificationTypeId = (String)get(new String[] {"id", "uuid"}, options);
		NotificationType notificationType = convert(options, NotificationType.class);
		printLine("Updating NotificationType...");
		print(notificationType);
		notificationsClient.updateNotificationType(notificationType, null);
		NotificationType notificationType2 = null; notificationsClient.getNotificationType(notificationTypeId, null);
		print("Updated NotificationType:");
		print(notificationType2);

	}
	
	public void deleteNotificationType(String type, String op, String[] cmds, Map<String, Object> options) {
		String notificationTypeId = (String)get(new String[] {"id", "uuid"}, options);
		printLine("Deleting NotificationType...");
		printLine("ID:", notificationTypeId);		
		notificationsClient.deleteNotificationType(notificationTypeId, null);		
	}

	
	//
	// Templates
	//
	
	public void listTemplates(String type, String op, String[] cmds, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		TemplateFilter filter = convert(options, TemplateFilter.class);
		Page<Template> templates = notificationsClient.listTemplates(filter, pageable);
		printLine("Listing Templates...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("Templates:");
		print(templates);
	}
	
	public void getTemplate(String type, String op, String[] cmds, Map<String, Object> options) {
		String templateId = (String)get(new String[] {"id", "uuid"}, options);
		Template template = null; notificationsClient.getTemplate(templateId, null);
		printLine("Get Template...");
		printLine("ID:", templateId);
		printLine("Template:");
		print(template);
	}
	
	public void createTemplate(String type, String op, String[] cmds, Map<String, Object> options) {
		Template template = convert(options, Template.class);
		printLine("Creating Template...");
		print(template);
		URI uri = null; notificationsClient.createTemplate(template, null);
		printLine("URI:", uri);
		String templateId = UriUtils.extractId(uri);
		Template template2 = null; notificationsClient.getTemplate(templateId, null);
		print("Created Template:");
		print(template2);
	}

	public void updateTemplate(String type, String op, String[] cmds, Map<String, Object> options) {
		String templateId = (String)get(new String[] {"id", "uuid"}, options);
		Template template = convert(options, Template.class);
		printLine("Updating Template...");
		print(template);
		notificationsClient.updateTemplate(template, null);
		Template template2 = null; notificationsClient.getTemplate(templateId, null);
		print("Updated Template:");
		print(template2);

	}
	
	public void deleteTemplate(String type, String op, String[] cmds, Map<String, Object> options) {
		String templateId = (String)get(new String[] {"id", "uuid"}, options);
		printLine("Deleting Template...");
		printLine("ID:", templateId);		
		notificationsClient.deleteTemplate(templateId, null);		
	}

	//
	// Jobs
	//
	
	public void listJobs(String type, String op, String[] cmds, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		JobFilter filter = convert(options, JobFilter.class);
		Page<Job> jobs = notificationsClient.listJobs(filter, pageable);
		printLine("Listing Jobs...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("Jobs:");
		print(jobs);
	}
	
	public void getJob(String type, String op, String[] cmds, Map<String, Object> options) {
		String jobId = (String)get(new String[] {"id", "uuid"}, options);
		Job job = null; notificationsClient.getJob(jobId, null);
		printLine("Get Job...");
		printLine("ID:", jobId);
		printLine("Job:");
		print(job);
	}
	
	public void createJob(String type, String op, String[] cmds, Map<String, Object> options) {
		Job job = convert(options, Job.class);
		printLine("Creating Job...");
		print(job);
		URI uri = null; notificationsClient.createJob(job, null);
		printLine("URI:", uri);
		String jobId = UriUtils.extractId(uri);
		Job job2 = null; notificationsClient.getJob(jobId, null);
		print("Created Job:");
		print(job2);
	}

	public void updateJob(String type, String op, String[] cmds, Map<String, Object> options) {
		String jobId = (String)get(new String[] {"id", "uuid"}, options);
		Job job = convert(options, Job.class);
		printLine("Updating Job...");
		print(job);
		notificationsClient.updateJob(job, null);
		Job job2 = null; notificationsClient.getJob(jobId, null);
		print("Updated Job:");
		print(job2);

	}
	
	public void deleteJob(String type, String op, String[] cmds, Map<String, Object> options) {
		String jobId = (String)get(new String[] {"id", "uuid"}, options);
		printLine("Deleting Job...");
		printLine("ID:", jobId);		
		notificationsClient.deleteJob(jobId, null);		
	}
	
	@Override
	protected String getDefaultFormat(Class<? extends Object> type) {
		if (NotificationType.class.equals(type)) {
			return NOTIFICATIONTYPE_DEFAULT_FORMAT;
		}
		if (Notification.class.equals(type)) {
			return NOTIFICATION_DEFAULT_FORMAT;
		}
		if (Template.class.equals(type)) {
			return TEMPLATE_DEFAULT_FORMAT;
		}
		if (Job.class.equals(type)) {
			return JOB_DEFAULT_FORMAT;
		}
		if (TrackedEvent.class.equals(type)) {
			return TRACKEDEVENT_DEFAULT_FORMAT;
		}
		return null;
	}

	@Override
	protected String getWideFormat(Class<? extends Object> type) {
		if (NotificationType.class.equals(type)) {
			return NOTIFICATIONTYPE_WIDE_FORMAT;
		}
		if (Notification.class.equals(type)) {
			return NOTIFICATION_WIDE_FORMAT;
		}
		if (Template.class.equals(type)) {
			return TEMPLATE_WIDE_FORMAT;
		}
		if (Job.class.equals(type)) {
			return JOB_WIDE_FORMAT;
		}
		if (TrackedEvent.class.equals(type)) {
			return TRACKEDEVENT_WIDE_FORMAT;
		}
		return null;
	}
}