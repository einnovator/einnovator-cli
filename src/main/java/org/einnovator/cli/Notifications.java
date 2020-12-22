package org.einnovator.cli;

import static org.einnovator.util.MappingUtils.updateObjectFromNonNull;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.einnovator.devops.client.modelx.JobOptions;
import org.einnovator.notifications.client.NotificationsClient;
import org.einnovator.notifications.client.config.NotificationsClientConfiguration;
import org.einnovator.notifications.client.model.Event;
import org.einnovator.notifications.client.model.Job;
import org.einnovator.notifications.client.model.Notification;
import org.einnovator.notifications.client.model.NotificationType;
import org.einnovator.notifications.client.model.Template;
import org.einnovator.notifications.client.model.TrackedEvent;
import org.einnovator.notifications.client.modelx.JobFilter;
import org.einnovator.notifications.client.modelx.NotificationFilter;
import org.einnovator.notifications.client.modelx.NotificationTypeFilter;
import org.einnovator.notifications.client.modelx.NotificationTypeOptions;
import org.einnovator.notifications.client.modelx.TemplateFilter;
import org.einnovator.notifications.client.modelx.TemplateOptions;
import org.einnovator.util.PageOptions;
import org.einnovator.util.UriUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


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

	private static final String JOB_DEFAULT_FORMAT ="id,name,receiverType:type,system,status,dryrun,progress,submitDate:submitted,startDate:started,completedDate:completed,readCount:read,processCount:proc,writeCount:write,skipCount:skip,totalCount:total";
	private static final String JOB_WIDE_FORMAT ="id,name,receiverType:type,system,status,dryrun,progress,templateId,submitDate:submitted,startDate:started,completedDate:completed,readCount:read,processCount:proc,writeCount:write,skipCount:skip,totalCount:total,skip:offset,max";

	private static final String TRACKEDEVENT_DEFAULT_FORMAT = "id,username,type,app,xid,xname,xtype,source.id,source.type,action.id,date";
	private static final String TRACKEDEVENT_WIDE_FORMAT = "id,username,type,app,xid,xname,xtype,source.id,source.type,action.id,date,dateFormatted:date";

	OAuth2AccessToken token;
	
	private NotificationsClient notificationsClient;

	private String server = NOTIFICATIONS_DEFAULT_SERVER;

	private NotificationsClientConfiguration config = new NotificationsClientConfiguration();

	
	
	@Override
	public String getName() {
		return "notifications";
	}

	String[][] NOTIFICATIONS_COMMANDS = c(
		c("event", "events", "ev"),
		c("notification", "notifications", "notific"),
		c("notification-type", "notification-types", "notific-type", "ntype", "nt"),
		c("template", "templates", "templ"), 
		c("mjob", "mjobs", "job", "jobs")
	);

	@Override
	protected String[][] getCommands() {
		return NOTIFICATIONS_COMMANDS;
	}

	static Map<String, String[][]> subcommands;

	static {
		Map<String, String[][]> map = new LinkedHashMap<>();
		subcommands = map;
		map.put("event", c(c("ls", "list"), c("schema", "meta"), 
			c("delete", "del", "rm"),
			c("publish"),
			c("help")));
		map.put("notification", c(c("ls", "list"), c("get"), c("schema", "meta"), 
			c("delete", "del", "rm"),
			c("help")));
		map.put("notification-type", c(c("ls", "list"), c("get"), c("schema", "meta"), 
			c("create", "add"), c("update"), c("delete", "del", "rm"),
			c("help")));
		map.put("template", c(c("ls", "list"), c("get"), c("schema", "meta"), 
			c("submit", "create", "add"), c("update"), c("delete", "del", "remove", "rm"), 
			c("help")));
		map.put("mjob", c(c("ls", "list"), c("get"), c("schema", "meta"), 
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
			updateObjectFromNonNull(config, convert(options, NotificationsClientConfiguration.class));
			if (template instanceof OAuth2RestTemplate) {
				notificationsClient = new NotificationsClient((OAuth2RestTemplate)template, config);				
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
			break;
		}
		setupToken();
		switch (type) {
		case "events": case "event": case "ev":
			switch (op) {
			case "help": case "":
				printUsage("event");
				break;
			case "publish": case "pub":
				publish(cmds, options);
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "notification": case "notifications": case "notific":
			switch (op) {
			case "help": case "":
				printUsage("notification");
				break;
			//case "get": 
			//	getNotification(cmds, options);
			//	break;
			case "ls": case "list":
				listNotifications(cmds, options);
				break;
			case "count": case "c":
				countNotifications(cmds, options);
				break;
			case "delete": case "del": case "rm": case "remove":
				deleteNotification(cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "notification-type": case "notification-types": case "notific-type": case "ntype": case "nt":
			switch (op) {
			case "help": case "":
				printUsage("notification-type");
				break;
			case "get": 
				getNotificationType(cmds, options);
				break;
			case "ls": case "list":
				listNotificationTypes(cmds, options);
				break;
			case "create": case "add":
				createNotificationType(cmds, options);
				break;
			case "update":
				updateNotificationType(cmds, options);
				break;
			case "delete": case "del": case "rm": case "remove":
				deleteNotificationType(cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "template": case "templates": case "templ": 
			switch (op) {
			case "help": case "":
				printUsage("template");
				break;
			case "get": 
				getTemplate(cmds, options);
				break;
			case "ls": case "list":
				listTemplates(cmds, options);
				break;
			case "create": case "add":
				createTemplate(cmds, options);
				break;
			case "update":
				updateTemplate(cmds, options);
				break;
			case "delete": case "del": case "rm": case "remove":
				deleteTemplate(cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "mjob": case "mjobs": case "job": case "jobs": 
			switch (op) {
			case "help": case "":
				printUsage("mjob");
				break;
			case "get": 
				getJob(cmds, options);
				break;
			case "ls": case "list":
				listJobs(cmds, options);
				break;
			case "create": case "add":
				createJob(cmds, options);
				break;
			case "update":
				updateJob(cmds, options);
				break;
			case "delete": case "del": case "rm": case "remove":
				deleteJob(cmds, options);
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
	
	public void listNotifications(String[] cmds, Map<String, Object> options) {
		if (isHelp("notification", "ls")) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		NotificationFilter filter = convert(options, NotificationFilter.class);
		debug("Notifications: %s %s", filter, pageable);
		Page<Notification> notifications = notificationsClient.listNotifications(filter, pageable);
		print(notifications);
	}
	
	public void countNotifications(String[] cmds, Map<String, Object> options) {
		if (isHelp("notification", "count")) {
			return;
		}
		NotificationFilter filter = convert(options, NotificationFilter.class);
		Long n = notificationsClient.countNotifications(filter);
		printLine("Count Notification...");
		printLine(n);
	}
	
	public void publish(String[] cmds, Map<String, Object> options) {
		if (isHelp("publish")) {
			return;
		}
		Event event = convert(options, Event.class);
		printLine("Publish TrackedEvent...");
		print(event);
		notificationsClient.publishEvent(event);
	}

	
	public void deleteNotification(String[] cmds, Map<String, Object> options) {
		if (isHelp("notification", "delete")) {
			return;
		}
		String notificationId = argId(op, cmds);
		debug("Deleting Notification: %s", notificationId);
		notificationsClient.deleteNotification(notificationId, null);		
		if (isEcho()) {
			listNotifications(cmds, options);
		}
	}

	//
	// NotificationTypes
	//
	
	public void listNotificationTypes(String[] cmds, Map<String, Object> options) {
		if (isHelp("type", "ls")) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		NotificationTypeFilter filter = convert(options, NotificationTypeFilter.class);
		Page<NotificationType> notificationTypes = notificationsClient.listNotificationTypes(filter, pageable);
		printLine("Listing NotificationTypes...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("NotificationTypes:");
		print(notificationTypes);
	}
	
	public void getNotificationType(String[] cmds, Map<String, Object> options) {
		if (isHelp("type", "get")) {
			return;
		}
		String notificationTypeId = argId(op, cmds);
		debug("NotificationType: %s", notificationTypeId);
		NotificationType notificationType = null; notificationsClient.getNotificationType(notificationTypeId, null);
		printObj(notificationType);
	}
	
	public void createNotificationType(String[] cmds, Map<String, Object> options) {
		if (isHelp("type", "create")) {
			return;
		}
		NotificationType notificationType = convert(options, NotificationType.class);
		NotificationTypeOptions options_ = convert(options, NotificationTypeOptions.class);
		debug("Creating NotificationType: %s %s", notificationType, options_);
		URI uri = notificationsClient.createNotificationType(notificationType, options_);
		if (isEcho()) {
			printLine("NotificationType URI:", uri);
			String id = UriUtils.extractId(uri);
			NotificationType notificationType2 = notificationsClient.getNotificationType(id, null);
			printObj(notificationType2);			
		}
	}

	public void updateNotificationType(String[] cmds, Map<String, Object> options) {
		if (isHelp("type", "update")) {
			return;
		}
		String notificationTypeId = argId(op, cmds);
		NotificationType account = convert(options, NotificationType.class);
		NotificationTypeOptions options_ = convert(options, NotificationTypeOptions.class);
		debug("Updating NotificationType: %s %s %s", notificationTypeId, account, options_);
		notificationsClient.updateNotificationType(account, options_);
		if (isEcho()) {
			NotificationType account2 = notificationsClient.getNotificationType(notificationTypeId, null);
			printObj(account2);			
		}
	}
	
	public void deleteNotificationType(String[] cmds, Map<String, Object> options) {
		if (isHelp("type", "delete")) {
			return;
		}
		String notificationTypeId = argId(op, cmds);
		debug("Deleting NotificationType: %s", notificationTypeId);
		notificationsClient.deleteNotificationType(notificationTypeId, null);
		if (isEcho()) {
			listNotificationTypes(cmds, options);
		}
	}

	
	//
	// Templates
	//
	
	public void listTemplates(String[] cmds, Map<String, Object> options) {
		if (isHelp("template", "ls")) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		TemplateFilter filter = convert(options, TemplateFilter.class);
		debug("Templates: %s %s", filter, pageable);
		Page<Template> templates = notificationsClient.listTemplates(filter, pageable);
		print(templates);
	}
	
	public void getTemplate(String[] cmds, Map<String, Object> options) {
		if (isHelp("template", "get")) {
			return;
		}
		String templateId = argId(op, cmds);
		debug("Template: %s", templateId);
		Template template = notificationsClient.getTemplate(templateId, null);
		printObj(template);
	}
	
	public void createTemplate(String[] cmds, Map<String, Object> options) {
		if (isHelp("template", "create")) {
			return;
		}
		Template template = convert(options, Template.class);
		TemplateOptions options_ = convert(options, TemplateOptions.class);
		debug("Creating Template: %s %s", template, options_);
		URI uri = notificationsClient.createTemplate(template, options_);
		if (isEcho()) {
			printLine("Template URI:", uri);
			String id = UriUtils.extractId(uri);
			Template template2 = notificationsClient.getTemplate(id, null);
			printObj(template2);			
		}
	}

	public void updateTemplate(String[] cmds, Map<String, Object> options) {
		if (isHelp("template", "update")) {
			return;
		}
		String templateId = argId(op, cmds);
		Template account = convert(options, Template.class);
		TemplateOptions options_ = convert(options, TemplateOptions.class);
		debug("Updating Template: %s %s %s", templateId, account, options_);
		notificationsClient.updateTemplate(account, options_);
		if (isEcho()) {
			Template account2 = notificationsClient.getTemplate(templateId, null);
			printObj(account2);			
		}
	}
	
	public void deleteTemplate(String[] cmds, Map<String, Object> options) {
		if (isHelp("template", "delete")) {
			return;
		}
		String templateId = argId(op, cmds);
		debug("Deleting Template: %s", templateId);
		notificationsClient.deleteTemplate(templateId, null);	
		if (isEcho()) {
			listTemplates(cmds, options);
		}
	}

	//
	// Jobs
	//
	
	public void listJobs(String[] cmds, Map<String, Object> options) {
		if (isHelp("mjob", "ls")) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		JobFilter filter = convert(options, JobFilter.class);
		debug("Jobs: %s %s", filter, pageable);
		Page<Job> jobs = notificationsClient.listJobs(filter, pageable);
		print(jobs);
	}
	
	public void getJob(String[] cmds, Map<String, Object> options) {
		if (isHelp("mjob", "get")) {
			return;
		}
		String jobId = argId(op, cmds);
		debug("Job: %s", jobId);
		Job job = notificationsClient.getJob(jobId, null);
		printObj(job);
	}
	
	public void createJob(String[] cmds, Map<String, Object> options) {
		if (isHelp("mjob", "create")) {
			return;
		}
		Job job = convert(options, Job.class);
		JobOptions option_ = convert(options, JobOptions.class);
		debug("Creating Job: %s %s", job, option_);
		URI uri = notificationsClient.createJob(job, option_);
		if (isEcho()) {
			printLine("Job URI:", uri);
			String id = UriUtils.extractId(uri);
			Job job2 = notificationsClient.getJob(id, null);
			printObj(job2);			
		}
	}

	public void updateJob(String[] cmds, Map<String, Object> options) {
		if (isHelp("mjob", "update")) {
			return;
		}
		String jobId = argId(op, cmds);
		Job account = convert(options, Job.class);
		JobOptions option_ = convert(options, JobOptions.class);
		debug("Updating Job: %s %s %s", jobId, account, option_);
		notificationsClient.updateJob(account, option_);
		if (isEcho()) {
			Job account2 = notificationsClient.getJob(jobId, null);
			printObj(account2);			
		}
	}
	
	public void deleteJob(String[] cmds, Map<String, Object> options) {
		if (isHelp("mjob", "delete")) {
			return;
		}
		String jobId = argId(op, cmds);
		debug("Deleting Job: %s", jobId);
		notificationsClient.deleteJob(jobId, null);
		if (isEcho()) {
			listJobs(cmds, options);
		}
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