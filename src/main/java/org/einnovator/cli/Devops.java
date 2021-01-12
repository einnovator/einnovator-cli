package org.einnovator.cli;

import static  org.einnovator.util.MappingUtils.updateObjectFrom;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.einnovator.devops.client.DevopsClient;
import org.einnovator.devops.client.config.DevopsClientConfiguration;
import org.einnovator.devops.client.model.Binding;
import org.einnovator.devops.client.model.Build;
import org.einnovator.devops.client.model.Catalog;
import org.einnovator.devops.client.model.Certificate;
import org.einnovator.devops.client.model.Cluster;
import org.einnovator.devops.client.model.Connector;
import org.einnovator.devops.client.model.CredentialsType;
import org.einnovator.devops.client.model.CronJob;
import org.einnovator.devops.client.model.CronJobStatus;
import org.einnovator.devops.client.model.Deployable;
import org.einnovator.devops.client.model.Deployment;
import org.einnovator.devops.client.model.DeploymentKind;
import org.einnovator.devops.client.model.DeploymentStatus;
import org.einnovator.devops.client.model.Domain;
import org.einnovator.devops.client.model.Event;
import org.einnovator.devops.client.model.Image;
import org.einnovator.devops.client.model.Job;
import org.einnovator.devops.client.model.JobStatus;
import org.einnovator.devops.client.model.KeyPath;
import org.einnovator.devops.client.model.Mount;
import org.einnovator.devops.client.model.MountType;
import org.einnovator.devops.client.model.Pod;
import org.einnovator.devops.client.model.Port;
import org.einnovator.devops.client.model.Registry;
import org.einnovator.devops.client.model.ReplicaSet;
import org.einnovator.devops.client.model.Repository;
import org.einnovator.devops.client.model.Resources;
import org.einnovator.devops.client.model.Route;
import org.einnovator.devops.client.model.Solution;
import org.einnovator.devops.client.model.Space;
import org.einnovator.devops.client.model.VarCategory;
import org.einnovator.devops.client.model.Variable;
import org.einnovator.devops.client.model.Vcs;
import org.einnovator.devops.client.model.VolumeClaim;
import org.einnovator.devops.client.model.Webhook;
import org.einnovator.devops.client.model.Workspace;
import org.einnovator.devops.client.modelx.BuildFilter;
import org.einnovator.devops.client.modelx.BuildOptions;
import org.einnovator.devops.client.modelx.CatalogFilter;
import org.einnovator.devops.client.modelx.CatalogOptions;
import org.einnovator.devops.client.modelx.ClusterFilter;
import org.einnovator.devops.client.modelx.ClusterOptions;
import org.einnovator.devops.client.modelx.CronJobFilter;
import org.einnovator.devops.client.modelx.CronJobOptions;
import org.einnovator.devops.client.modelx.DeploymentFilter;
import org.einnovator.devops.client.modelx.DeploymentOptions;
import org.einnovator.devops.client.modelx.DomainFilter;
import org.einnovator.devops.client.modelx.DomainOptions;
import org.einnovator.devops.client.modelx.EventFilter;
import org.einnovator.devops.client.modelx.ExecOptions;
import org.einnovator.devops.client.modelx.InstallOptions;
import org.einnovator.devops.client.modelx.JobFilter;
import org.einnovator.devops.client.modelx.JobOptions;
import org.einnovator.devops.client.modelx.LogOptions;
import org.einnovator.devops.client.modelx.PodFilter;
import org.einnovator.devops.client.modelx.PodOptions;
import org.einnovator.devops.client.modelx.RegistryFilter;
import org.einnovator.devops.client.modelx.RegistryOptions;
import org.einnovator.devops.client.modelx.ReplicaSetFilter;
import org.einnovator.devops.client.modelx.SolutionFilter;
import org.einnovator.devops.client.modelx.SolutionOptions;
import org.einnovator.devops.client.modelx.SpaceFilter;
import org.einnovator.devops.client.modelx.SpaceOptions;
import org.einnovator.devops.client.modelx.VcsFilter;
import org.einnovator.devops.client.modelx.VcsOptions;
import org.einnovator.devops.client.modelx.VolumeClaimFilter;
import org.einnovator.devops.client.modelx.VolumeClaimOptions;
import org.einnovator.util.MappingUtils;
import org.einnovator.util.PageOptions;
import org.einnovator.util.PathUtil;
import org.einnovator.util.ResourceUtils;
import org.einnovator.util.StringUtil;
import org.einnovator.util.security.Authority;
import org.einnovator.util.web.RequestOptions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;


@Component
public class Devops extends CommandRunnerBase {
	public static final String DEVOPS_NAME = "devops";

	public static final String DEVOPS_DEFAULT_SERVER = "http://localhost:2500";
	public static final String DEVOPS_MONITOR_SERVER = "http://localhost:2501";

	private static final String CLUSTER_DEFAULT_FORMAT = "id,name,displayName,provider,region";
	private static final String CLUSTER_WIDE_FORMAT = "id,name,displayName,provider,region,enabled,master";

	private static final String SPACE_DEFAULT_FORMAT = "id,name,displayName,cluster.name:cluster,cluster.provider:provider,cluster.region:region";
	private static final String SPACE_WIDE_FORMAT = "id,name,displayName,cluster.name:cluster,cluster.provider:provider,cluster.region:region";

	private static final String DEPLOYMENT_DEFAULT_FORMAT = "id,name,displayName,kind,status,availableReplicas:available,desiredReplicas:desired,readyReplicas/replicas:ready,age";
	private static final String DEPLOYMENT_WIDE_FORMAT = "id,name,displayName,kind,type,category,status,availableReplicas:available,desiredReplicas:desired,readyReplicas/replicas:ready,image.name:image,age";
	private static final String DEPLOYMENT_CICD_FORMAT = "id,name,builder,builderKind,repository.url:repo,repository.vcs.name:vcs,buildImage.name:buildimage,buildImage.registry.name:registry,workspace.type:workspace,webhook.enabled:webhook";
	private static final String DEPLOYMENT_RESOURCES_FORMAT = "id,name,displayName,status,resources.memory:Memory,resources.disk:Disk,resources.cpu:Cpu";

	private static final String JOB_DEFAULT_FORMAT = "id,name,displayName,kind,status,age";
	private static final String JOB_WIDE_FORMAT = "id,name,displayName,kind,status,completions,parallelism,age";
	private static final String JOB_CICD_FORMAT = DEPLOYMENT_CICD_FORMAT;
	private static final String JOB_RESOURCES_FORMAT = DEPLOYMENT_RESOURCES_FORMAT;
	
	private static final String CRONJOB_DEFAULT_FORMAT = "id,name,displayName,kind,schedule,status,suspend,active,lastScheduleAge:last scheduled,age";
	private static final String CRONJOB_WIDE_FORMAT = "id,name,displayName,kind,schedule,status,suspend,active,lastScheduleAge:last scheduled,age";
	private static final String CRONJOB_CICD_FORMAT = DEPLOYMENT_CICD_FORMAT;
	private static final String CRONJOB_RESOURCES_FORMAT = DEPLOYMENT_RESOURCES_FORMAT;
	
	private static final String DOMAIN_DEFAULT_FORMAT ="id,name,tls,age";
	private static final String DOMAIN_WIDE_FORMAT ="id,name,tls,cert,root,parent,enabled,age";

	private static final String REGISTRY_DEFAULT_FORMAT = "id,name,displayName,server,credentialsType:auth,username,age";
	private static final String REGISTRY_WIDE_FORMAT = "id,name,displayName,server,credentialsType:auth,username,email,age";

	private static final String VCS_DEFAULT_FORMAT = "id,name,displayName,url,credentialsType:auth,username,age";
	private static final String VCS_WIDE_FORMAT = "id,name,displayName,url,credentialsType:auth,username,age";

	private static final String CATALOG_DEFAULT_FORMAT = "id,name,displayName,type,enabled,age";
	private static final String CATALOG_WIDE_FORMAT = "id,name,displayName,type,enabled,age";

	private static final String SOLUTION_DEFAULT_FORMAT = "id,name,displayName,type,kind,category,keywords";
	private static final String SOLUTION_WIDE_FORMAT = "id,name,displayName,type,kind,category,keywords,url";

	private static final String CATALOG_SOLUTION_DEFAULT_FORMAT = "name,displayName,category,keywords";
	private static final String CATALOG_SOLUTION_WIDE_FORMAT = "name,displayName,category,keywords,url";

	private static final String BINDING_DEFAULT_FORMAT = "id,selector,spec";
	private static final String BINDING_WIDE_FORMAT = "id,selector,spec,meta";

	private static final String CONNECTOR_DEFAULT_FORMAT = "id,name,spec";
	private static final String CONNECTOR_WIDE_FORMAT = "id,name,spec,meta";

	private static final String ROUTE_DEFAULT_FORMAT = "id,host,dns,domain.dns:domain,tls";
	private static final String ROUTE_WIDE_FORMAT = "id,host,dns,domain.dns:domain,tls";

	private static final String MOUNT_DEFAULT_FORMAT = "id,name,type,mountPath,size";
	private static final String MOUNT_WIDE_FORMAT = "id,name,type,mountPath,size";

	private static final String VAR_DEFAULT_FORMAT = "name,category,value,configMap,secret";
	private static final String VAR_WIDE_FORMAT = "name,category,type,value,configMap,secret";

	private static final String POD_DEFAULT_FORMAT = "name,containers/readyContainers:ready,status,restarts,age";
	private static final String POD_WIDE_FORMAT = "name,containers/readyContainers:ready,status,restarts,age,ip,node";

	private static final String REPLICASET_DEFAULT_FORMAT = "name,status,availableReplicas:available,desiredReplicas:desired,readyReplicas/replicas:ready,age";
	private static final String REPLICASET_WIDE_FORMAT = "name,status,availableReplicas:available,desiredReplicas:desired,readyReplicas/replicas:ready,age";

	private static final String AUTHORITY_DEFAULT_FORMAT = "username,groupId,manage,write:dev,read:auditor";
	private static final String AUTHORITY_WIDE_FORMAT = "username,groupId,manage,write:dev,read:auditor";

	private static final String EVENT_DEFAULT_FORMAT = "type/reason,formattedDate:age,username,description:description";
	private static final String EVENT_WIDE_FORMAT = EVENT_DEFAULT_FORMAT;

	private static final String VOLUMECLAIM_DEFAULT_FORMAT = "name,mode,size,storageClass,age";
	private static final String VOLUMECLAIM_WIDE_FORMAT = "name,mode,size,storageClass,age";

	private static final String BUILD_DEFAULT_FORMAT = "name,status,shortMessage:message,step,startAge:age,duration";
	private static final String BUILD_WIDE_FORMAT = "name,status,shortMessage:message,step,startAge:age,duration";
	
	private DevopsClient devopsClient;

	private String server = DEVOPS_DEFAULT_SERVER;
	
	private String cluster;
	private String space;
	private String domain;
	private String registry;
	private String vcs;
	private String catalog;


	private DevopsClientConfiguration config = new DevopsClientConfiguration();


	@Override
	public void init(Map<String, Object> options, RestTemplate template, boolean interactive, ResourceBundle bundle) {
		if (!init) {
			super.init(options, template, interactive, bundle);
			DevopsClientConfiguration config0 = convert(options, DevopsClientConfiguration.class);
			config0.setServer(this.server);
			updateObjectFrom(config, config0);
			config.setServer(server);
			debug(3, "Devops Config: %s %s", config, template instanceof OAuth2RestTemplate ? "OAuth2" : "Basic");
			if (template instanceof OAuth2RestTemplate) {
				devopsClient = new DevopsClient((OAuth2RestTemplate)template, config);
			} else {
				devopsClient = new DevopsClient(template, config);				
			}
			init = true;
		}
	}
	
	@Override
	public void setEndpoints(Map<String, Object> endpoints) {
		String server = (String)endpoints.get("server");
		if (server!=null) {
			this.server = server;
			this.config.setServer(server);
		}
	}
	
	@Override
	public String getName() {
		return DEVOPS_NAME;
	}
	
	@Override
	protected String getServer() {
		return server;
	}

	@Override
	public Map<String, Object> getSettings() {
		Map<String, Object> settings = new LinkedHashMap<>();
		if (StringUtil.hasText(cluster)) {
			settings.put("cluster", cluster);			
		}
		if (StringUtil.hasText(space)) {
			settings.put("space", space);
		}
		if (StringUtil.hasText(domain)) {
			settings.put("domain", domain);			
		}
		if (StringUtil.hasText(registry)) {
			settings.put("registry", registry);
		}
		if (StringUtil.hasText(catalog)) {
			settings.put("catalog", catalog);
		}

		return settings;
	}

	@Override
	public void loadSettings(Map<String, Object> settings) {
		cluster = get("cluster", settings, cluster);
		space = get("space", settings, space);
		domain = get("domain", settings, domain);
		registry = get("registry", settings, registry);
		catalog = get("catalog", settings, catalog);
	}

	static String[][] DEVOPS_COMMANDS = c(
		c("cluster", "clusters"),
		c("space", "spaces", "namespace", "namespaces", "ns"),
		c("deployment", "deploy", "deployments", "deploys"),
		c("job", "jobs"),
		c("cronjob", "cronjobs"),
		c("build", "builds"),
		c("pod", "pods", "instance", "instances"),
		c("replicaset", "replicasets", "rs"),
		c("volumeclaim", "volumeclaims", "volc"),
		c("route", "routes"),
		c("mount", "mounts"),
		c("env", "var", "vars"),
		c("binding", "bindings"),
		c("connector", "connectors"),
		c("domain", "domains"),
		c("registry", "registries", "reg"),
		c("vcs", "git"),
		c("catalog", "catalogs"),
		c("solution", "solutions"),
		c("ls", "list"),
		c("ps"),
		c("cd"),
		c("pwd"),
		c("run"),
		c("kill"),
		c("market", "marketplace"),
		c("install")
	);
	
	@Override
	protected boolean isRootCommand(String cmd) {
		switch (cmd) {
		case "run":
		case "kill":
		case "market":
		case "install":

			return true;
		}
		return super.isRootCommand(cmd);
	}

	@Override
	protected String[][] getCommands() {
		return DEVOPS_COMMANDS;
	}

	static Map<String, String[][]> subcommands;
	
	static {
		Map<String, String[][]> map = new LinkedHashMap<>();
		subcommands = map;
		map.put("cluster", c(c("ls", "list"), c("get"), c("view"), c("schema", "meta"), 
			c("create", "add", "import"), c("update"), c("delete", "del", "remove", "rm"),
			c("set"), c("unset"),
			c("help")));
		map.put("space", c(c("ls", "list"), c("get"), c("view"), c("schema", "meta"), 
			c("create", "add"), c("update"), c("delete", "del", "remove", "rm"), 
			c("sync"), c("attach"),
			c("set"), c("unset"),
			c("auth", "collaborator"),
			c("help")));
		map.put("deployment", c(c("ls", "list", "ps"), c("get"), c("view"), c("schema", "meta"), 
			c("create", "add"), c("update"), c("delete", "del", "remove", "rm"), 
			c("scale"), c("resources", "rscale"), c("start"), c("stop"), c("restart"), c("sync"), c("attach"), c("exec"), c("logs", "log"),
			c("build"),
			c("event", "events"),
			c("route", "routes"),
			c("go"),
			c("mount", "mount"),
			c("env", "var", "vars"),
			c("binding", "bindings"),
			c("connector", "connectors"),
			c("help")));
		map.put("job", c(c("ls", "list", "ps"), c("get"), c("view"), c("schema", "meta"), 
			c("create", "add"), c("update"), 
			c("delete", "del", "remove", "rm"), 
			c("resources", "rscale"), c("start"), c("stop"), c("restart"), c("sync"), c("attach"), c("exec"), c("logs", "log"),
			c("build"),
			c("events", "event"),
			c("mount", "mount"),
			c("env", "var", "vars"),
			c("binding", "bindings"),
			c("help")));
		map.put("cronjob", c(c("ls", "list", "ps"), c("get"), c("view"), c("schema", "meta"), 
			c("create", "add"), c("update"), 
			c("delete", "del", "remove", "rm"),
			c("resources", "rscale"), c("start"), c("stop"), c("suspend"), c("restart"), c("sync"), c("attach"), 
			c("build"),
			c("events", "event"),
			c("job", "jobs"),
			c("mount", "mount"),
			c("env", "var", "vars"),
			c("binding", "bindings"),
			c("help")));
		map.put("route", c(c("ls", "list"), c("get"), c("schema", "meta"), 
				c("add", "create"), c("update"), c("delete", "del", "remove", "rm"), c("help")));
		map.put("mount", c(c("ls", "list"), c("get"), c("schema", "meta"), 
				c("add", "create"), c("update"), c("delete", "del", "remove", "rm"), c("help")));
		map.put("env", c(c("ls", "list"), c("get"), c("schema", "meta"), 
				c("add", "create"), c("update"), c("delete", "del", "remove", "rm"), c("help")));
		map.put("binding", c(c("ls", "list"), c("get"), c("schema", "meta"), 
				c("add", "create"), c("update"), c("delete", "del", "remove", "rm"), c("help")));
		map.put("connector", c(c("ls", "list"), c("get"), c("schema", "meta"), 
				c("add", "create"), c("update"), c("delete", "del", "remove", "rm"), c("help")));
		map.put("pod", c(c("ls", "list"), c("get"), c("view"),
			c("kill", "delete", "del", "remove", "rm"),
			c("schema", "meta"), c("help")));	
		map.put("replicaset", c(c("ls", "list"), c("get"), c("view"),
				c("kill", "delete", "del", "remove", "rm"),
				c("schema", "meta"), c("help")));	
		map.put("volumeclaim", c(c("ls", "list"), c("get"), c("schema", "meta"), 
				c("create", "add"), c("delete", "del", "remove", "rm"), c("help")));
		map.put("build", c(c("ls", "list"), c("get"),
				c("create"),
				c("kill", "delete", "del", "remove", "rm"),
				c("schema", "meta"), c("help")));
		map.put("domain", c(c("ls", "list"), c("get"), c("view"), c("schema", "meta"), 
			c("create", "add"), c("update"), c("delete", "del", "remove", "rm"),
			c("set"), c("unset"),
			c("help")));
		map.put("registry", c(c("ls", "list"), c("get"), c("view"), c("schema", "meta"), 
			c("create", "add"), c("update"), c("delete", "del", "remove", "rm"),
			c("set"), c("unset"),
			c("help")));
		map.put("vcs", c(c("ls", "list"), c("get"), c("view"), c("schema", "meta"), 
			c("create", "add"), c("update"), c("delete", "del", "remove", "rm"),
			c("set"), c("unset"),
			c("help")));
		map.put("catalog", c(c("ls", "list"), c("get"), c("view"), c("schema", "meta"), 
			c("create", "add"), c("update"), c("delete", "del", "remove", "rm"), 
			c("solution", "solutions"),
			c("set"), c("unset"),
			c("help")));
		map.put("solution", c(c("ls", "list"), c("get"), c("view"), c("schema", "meta"), 
			c("create", "add"), c("update"), c("delete", "del", "remove", "rm"), c("help")));
	}
	
	@Override
	protected Map<String, String[][]> getSubCommands() {
		return subcommands;
	}
	
	private static Map<String, Map<String, String[][]>> subsubcommands;
	
	static {
		Map<String, Map<String, String[][]>> map = new LinkedHashMap<>();
		subsubcommands = map;
		Map<String, String[][]> space = m("space", map);
		space.put("auth", c(c("ls", "list"), c("get"), c("schema", "meta"), 
				c("add", "create"), c("update"), c("resend"), c("delete", "del", "remove", "rm"), c("help")));

		Map<String, String[][]> deploy = m("deployment", map);
		deploy.put("route", c(c("ls", "list"), c("get"), c("schema", "meta"), 
			c("add", "create"), c("update"), c("delete", "del", "remove", "rm"), c("help")));
		deploy.put("env", c(c("ls", "list"), c("get"), c("schema", "meta"), 
			c("add", "create"), c("update"), c("delete", "del", "remove", "rm"), c("help")));
		deploy.put("event", c(c("ls", "list"), c("schema", "meta"), c("delete", "del", "remove", "rm"), c("help")));

		deploy.put("mount", c(c("ls", "list"), c("get"), c("schema", "meta"), 
			c("add", "create"), c("update"), c("delete", "del", "remove", "rm"), c("help")));
		deploy.put("binding", c(c("ls", "list"), c("get"), c("schema", "meta"), 
			c("add", "create"), c("update"), c("delete", "del", "remove", "rm"), c("help")));
		deploy.put("connector", c(c("ls", "list"), c("get"), c("schema", "meta"), 
			c("add", "create"), c("update"), c("refresh"), c("delete", "del", "remove", "rm"), c("help")));
	
		Map<String, String[][]> job = m("job", map);
		job.put("env", c(c("ls", "list"), c("get"), c("schema", "meta"), 
				c("add", "create"), c("update"), c("delete", "del", "remove", "rm"), c("help")));
		job.put("event", c(c("ls", "list"), c("schema", "meta"), c("delete", "del", "remove", "rm"), c("help")));
		job.put("mount", c(c("ls", "list"), c("get"), c("schema", "meta"), 
				c("add", "create"), c("update"), c("delete", "del", "remove", "rm"), c("help")));
		job.put("binding", c(c("ls", "list"), c("get"), c("schema", "meta"), 
				c("add", "create"), c("update"), c("delete", "del", "remove", "rm"), c("help")));
	
		Map<String, String[][]> cronjob = m("cronjob", map);
		cronjob.put("env", c(c("ls", "list"), c("get"), c("schema", "meta"), 
				c("add", "create"), c("update"), c("delete", "del", "remove", "rm"), c("help")));
		cronjob.put("event", c(c("ls", "list"), c("schema", "meta"), c("delete", "del", "remove", "rm"), c("help")));
		cronjob.put("mount", c(c("ls", "list"), c("get"), c("schema", "meta"), 
				c("add", "create"), c("update"), c("delete", "del", "remove", "rm"), c("help")));
		cronjob.put("binding", c(c("ls", "list"), c("get"), c("schema", "meta"), 
				c("add", "create"), c("update"), c("delete", "del", "remove", "rm"), c("help")));
		cronjob.put("job", c(c("ls", "list"), c("help")));
	}
	
	@Override
	protected Map<String, Map<String, String[][]>> getSubSubCommands() {
		return subsubcommands;
	}

	public void run(String type, String op, String[] cmds, String[] extra, Map<String, Object> options) {	
		setLine(type, op, cmds, extra, options);
		switch (type) {
		case "help": case "":
			printUsage();
			return;
		}
		if (!setupToken()) {
			return;
		}

		switch (type) {
		case "ps": 
			ps(cmds, options);
			break;
		case "kill": 
			kill(cmds, options);
			break;
		case "run": 
			runop(cmds, extra, options);
			break;
		case "install": 
			install(cmds, options);
			break;
		case "ls": case "list":
			ls(cmds, options);
			break;
		case "pwd": 
			pwd(cmds, options);
			break;
		case "cd": 
			cd(cmds, options);
			break;
		case "cluster": case "clusters": 
			cluster(cmds, options);
			break;
		case "space": case "spaces":
			space(cmds, options);
			break;
		case "volumeclaim": case "volumeclaims": case "volc":
			volumeclaim(cmds, options);
			break;
		case "pod": case "pods": case "instance": case "instances":
			pod(cmds, options);
			break;
		case "replicaset": case "replicasets": case "rs":
			replicaset(cmds, options);
			break;
		case "build": case "builds":
			build(cmds, options);
			break;
		case "deployment": case "deploy": case "deploys": case "deployments":
			deployment(cmds, options);
			break;
		case "job":
			job(cmds, options);
			break;
		case "cronjob":
			cronjob(cmds, options);
			break;
		case "route": case "routes":
			route(cmds, options);
			break;
		case "mount": case "mounts":
			mount(cmds, options);
			break;
		case "env": case "var": case "vars":
			env(cmds, options);
			break;
		case "binding": case "bindings":
			binding(cmds, options);
			break;
		case "connector": case "connectors":
			connector(cmds, options);
			break;
		case "domain": case "domains":
			domain(cmds, options);
			break;
		case "registry": case "registries": case "reg":
			registry(cmds, options);
			break;
		case "vcs": case "vcss": case "git":
			vcs(cmds, options);
			break;
		case "catalog": case "catalogs":
			catalog(cmds, options);
			break;
		case "solution": case "solutions":
			solution(cmds, options);
			break;
		case "marketplace": case "market":
			listMarketplace(cmds, options);
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
	// Cluster
	//
	
	public void cluster(String[] cmds, Map<String, Object> options) {
		switch (op) {
		case "help": case "":
			printUsage(type);
			break;
		case "get": 
			getCluster(cmds, options);
			break;
		case "view": 
			viewCluster(cmds, options);
			break;
		case "ls": case "list":
			listCluster(cmds, options);
			break;
		case "schema": case "meta":
			schemaCluster(cmds, options);
			break;
		case "set":
			setCluster(cmds, options);
			break;
		case "unset":
			unsetCluster(cmds, options);
			break;				
		case "create": case "add": case "import":
			createCluster(cmds, options);
			break;
		case "update": 
			updateCluster(cmds, options);
			break;
		case "delete": case "del": case "rm":
			deleteCluster(cmds, options);
			break;
		default: 
			invalidOp(type, op);
		}
	}

	
	public void listCluster(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String q = argId(op, cmds, false);
		listCluster(q, options);
	}

	public void listCluster(String q, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		ClusterFilter filter = convert(options, ClusterFilter.class);
		if (StringUtil.hasText(q)) {
			filter.setQ(q);
		}
		debug("Clusters: %s %s", filter, pageable);
		Page<Cluster> clusters = devopsClient.listClusters(filter, pageable);
		print(clusters, Cluster.class);
	}
	
	public void getCluster(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String clusterId = argId(op, cmds, this.cluster);
		if (clusterId==null) {
			return;
		}
		ClusterOptions options_ = convert(options, ClusterOptions.class);
		debug("Cluster: %s", clusterId);
		Cluster cluster = devopsClient.getCluster(clusterId, options_);
		printObj(cluster);
	}

	
	public void viewCluster(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String clusterId = argId(op, cmds, this.cluster);
		if (clusterId==null) {
			return;
		}
		ClusterOptions options_ = convert(options, ClusterOptions.class);
		debug("View Cluster: %s", clusterId);
		Cluster cluster = devopsClient.getCluster(clusterId, options_);
		view("cluster", cluster.getUuid());
	}

	public void schemaCluster(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		schema(Cluster.class, ClusterFilter.class, ClusterOptions.class, options);
	}

	public void setCluster(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String clusterId = argId(op, cmds);
		setCluster(clusterId, options);
	}

	public void setCluster(String clusterId, Map<String, Object> options) {
		debug("Set Cluster: %s", clusterId);
		ClusterOptions options_ = convert(options, ClusterOptions.class);
		Cluster cluster = devopsClient.getCluster(clusterId, options_);
		if (this.cluster!=null && !clusterId.equals(this.cluster)) {
			this.space = null;
		}
		this.cluster = clusterId;
		if (isEcho()) {
			printObj(cluster);
		}
		writeConfig();
	}
	
	public void unsetCluster(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String clusterId = argId(op, cmds);
		debug("Unset Cluster: %s", clusterId);
		this.cluster = null;
		this.space = null;
		writeConfig();
	}

	public void createCluster(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		Cluster cluster = convert(options, Cluster.class);
		boolean required = true;
		makeClusterConfigOption(cluster, options);
		if (StringUtil.hasText(cluster.getKubeconfig())) {
			required = false;
		}
		cluster.setName(argId(op, cmds, required));
		if (cluster.getDisplayName()!=null) {
			cluster.setDisplayName(cluster.getName());
		}
		debug("Creating Cluster: %s", cluster);
		if (isDryrun()) {
			return;
		}
		URI uri = devopsClient.createCluster(cluster, null);
		if (isEcho()) {
			debug("Cluster URI: %s", uri);
			String id = extractId(uri);
			Cluster cluster2 = devopsClient.getCluster(id, null);
			printObj(cluster2);
		}
	}

	void makeClusterConfigOption(Cluster cluster, Map<String, Object> options) {
		String file = (String)options.get("f");
		if (StringUtil.hasText(file)) {
			if (!file.startsWith("http:") && !file.startsWith("https:")) {
				String kubeconfig = ResourceUtils.readResource(file, false);
				if (kubeconfig==null) {
					error("unable to read kubeconfig file: %s", file);
					exit(-1);
					return;
				}
				if (!StringUtil.hasText(kubeconfig)) {
					error("kubeconfig is empty: %s", file);
					exit(-1);
					return;
				}
				cluster.setKubeconfig(kubeconfig);
			} else {
				cluster.setKubeconfig(file);				
			}
		}
	}
	public void updateCluster(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String clusterId = argId(op, cmds);
		Cluster cluster = convert(options, Cluster.class);
		ClusterOptions options_ = convert(options, ClusterOptions.class);
		makeClusterConfigOption(cluster, options);
		debug("Updating Cluster: %s %s", clusterId, cluster);
		if (isDryrun()) {
			return;
		}
		devopsClient.updateCluster(clusterId, cluster, options_);
		if (isEcho()) {
			Cluster cluster2 = devopsClient.getCluster(clusterId, null);
			printObj(cluster2);			
		}
	}

	public void deleteCluster(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String clusterId = argId(op, cmds);
		ClusterOptions options_ = convert(options, ClusterOptions.class);
		String[] ids = clusterId.split(",");
		for (String id: ids) {
			if (id.isEmpty()) {
				continue;
			}
			debug("Deleting Cluster: %s", id);		
			if (!isDryrun()) {
				devopsClient.deleteCluster(id, options_);	
			}
		}	
		if (isDryrun()) {
			return;
		}
		if (isEcho()) {
			listCluster(cmds, options);
		}
	}

	//
	// Spaces
	//

	public void space(String[] cmds, Map<String, Object> options) {
		switch (op) {
		case "help": case "":
			printUsage(type);
			break;
		case "get": 
			getSpace(cmds, options);
			break;
		case "view": 
			viewSpace(cmds, options);
			break;
		case "ls": case "list":
			listSpace(cmds, options);
			break;
		case "set":
			setSpace(cmds, options);
			break;
		case "unset":
			unsetSpace(cmds, options);
			break;
		case "schema": case "meta":
			schemaSpace(cmds, options);
			break;
		case "create": case "add": 
			createSpace(cmds, options);
			break;
		case "update": 
			updateSpace(cmds, options);
			break;
		case "delete": case "del": case "rm":
			deleteSpace(cmds, options);
			break;
		case "sync":
			syncSpace(cmds, options);
			break;
		case "attach":
			attachSpace(cmds, options);
			break;
		case "auth": case "collaborator":
			authSpace(cmds, options);
			break;
		default: 
			invalidOp(type, op);
			break;
		}		
	}


	
	public void listSpace(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String q = argId(op, cmds, false);
		listSpace(q, options);
	}
	
	public void listSpace(String q, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		SpaceFilter filter = convert(options, SpaceFilter.class);
		if (StringUtil.hasText(q)) {
			filter.setQ(q);
		}
		debug("Spaces: %s %s", filter, pageable);
		Page<Space> spaces = devopsClient.listSpaces(filter, pageable);
		print(spaces, Space.class);
	}
	
	public void getSpace(String spaceId , Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		SpaceOptions options_ = convert(options, SpaceOptions.class);
		Space space = devopsClient.getSpace(spaceId, options_);
		printObj(space);
	}
	
	public void getSpace(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String spaceId = argId(op, cmds, this.space);
		if (spaceId==null) {
			return;
		}
		SpaceOptions options_ = convert(options, SpaceOptions.class);
		debug("Space: %s", spaceId);
		Space space = devopsClient.getSpace(spaceId, options_);
		printObj(space);
	}

	public void viewSpace(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String spaceId = argId(op, cmds, this.space);
		if (spaceId==null) {
			return;
		}
		SpaceOptions options_ = convert(options, SpaceOptions.class);
		debug("View Space: %s", spaceId);
		Space space = devopsClient.getSpace(spaceId, options_);
		view("space", space.getUuid());
	}

	public void setSpace(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String spaceId = argId(op, cmds);
		setSpace(spaceId, options);
	}
	
	public void setSpace(String spaceId, Map<String, Object> options) {
		debug("Set Space: %s", spaceId);
		SpaceOptions options_ = convert(options, SpaceOptions.class);
		Space space = devopsClient.getSpace(spaceId, options_);
		this.space = spaceId;
		int i = spaceId.indexOf("/");
		if (i>0) {
			this.cluster = spaceId.substring(0, i);
		}
		if (isEcho()) {
			printObj(space);
		}
		writeConfig();
	}
	
	public void unsetSpace(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String spaceId = argId(op, cmds);
		debug("Unset Space: %s", spaceId);
		this.space = null;
		writeConfig();
	}
	
	public void schemaSpace(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		schema(Space.class, SpaceFilter.class, SpaceOptions.class, options);
	}


	public void createSpace(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		Space space = convert(options, Space.class);
		space.setName(argId(op, cmds));
		debug("Creating Space: %s", space);
		if (isDryrun()) {
			return;
		}
		URI uri = devopsClient.createSpace(space, null);
		if (isEcho()) {
			debug("Space URI: %s", uri);
			String spaceId = extractId(uri);
			Space space2 = devopsClient.getSpace(spaceId, null);
			printObj(space2);			
		}
	}

	public void updateSpace(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String spaceId = argIdx(op, cmds);
		Space space = convert(options, Space.class);
		SpaceOptions options_ = convert(options, SpaceOptions.class);
		debug("Updating Space: %s %s", spaceId, space);
		if (isDryrun()) {
			return;
		}
		devopsClient.updateSpace(spaceId, space, options_);
		if (isEcho()) {
			Space space2 = devopsClient.getSpace(spaceId, null);
			debug("Updated Space: %s", spaceId);
			printObj(space2);
		}
	}
	
	public void deleteSpace(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String spaceId = argIdx(op, cmds);
		SpaceOptions options_ = convert(options, SpaceOptions.class);
		String[] ids = spaceId.split(",");
		for (String id: ids) {
			if (id.isEmpty()) {
				continue;
			}
			debug("Deleting Space: %s", id);		
			if (!isDryrun()) {
				devopsClient.deleteSpace(id, options_);	
			}
		}
		if (isDryrun()) {
			return;
		}
		if (isEcho()) {
			listSpace(cmds, options);
		}
	}
		
	
	public void syncSpace(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String spaceId = argIdx(op, cmds);
		SpaceOptions options_ = convert(options, SpaceOptions.class);
		debug("Sync Space: %s %s", spaceId, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.syncSpace(spaceId, options_);			
		if (isEcho()) {
			getSpace(spaceId, options);
		}
	}
	
	public void attachSpace(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String spaceId = argId(op, cmds);
		String cluster = this.cluster;
		SpaceOptions options_ = convert(options, SpaceOptions.class);
		debug("Attach Space: %s %s %s", spaceId, spaceId, options_);		
		if (isDryrun()) {
			return;
		}
		URI uri = devopsClient.attachSpace(cluster, spaceId, options_);			
		if (isEcho()) {
			String id = extractId(uri);
			debug("Space URI: %s", uri);		
			getSpace(id, options);
		}
	}

	//
	// Space Pods
	//

	public void pod(String[] cmds, Map<String, Object> options) {
		switch (op) {
		case "help": case "":
			printUsage1();
			break;
		case "ls": case "list": {
			podList(cmds, options);
			break;
		}
		case "get": {
			podGet(cmds, options);
			break;
		}
		case "view": {
			podView(cmds, options);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			podDelete(cmds, options);
			break;
		}
		case "schema": case "meta":
			if (isHelp2()) {
				return;
			}
			schema(Pod.class);
		default:
			invalidOp();
			printUsage2();
			break;
		}
	}
	
	public void podList(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingSpaceId();
			exit(-1);
			return;
		}
		String id = argIdx(op, cmds, false);
		if (id!=null) {
			if (options.get("j")!=null) {
				podJobList(id, options);				
			} else {
				podDeploymentList(id, options);				
			}
		} else {
			podList(spaceId, options);			
		}
	}
	
	public void podList(String spaceId, Map<String, Object> options) {
		PodFilter filter = convert(options, PodFilter.class);
		PageOptions options_ = convert(options, PageOptions.class);
		debug("Pods: %s %s %s", spaceId,filter, options_);		
		List<Pod> pods = devopsClient.listPods(spaceId, filter, options_.toPageRequest());			
		print(pods);
	}
	
	public void podGet(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingSpaceId();
			exit(-1);
			return;
		}
		String podId = argId(op, cmds);
		podGet(spaceId, podId, options);
	}
	
	public void podGet(String spaceId, String podId, Map<String, Object> options) {
		debug("Pod: %s %s", spaceId, podId);		
		PodOptions options_ = convert(options, PodOptions.class);
		Pod pod = devopsClient.getPod(spaceId, podId, options_);			
		printObj(pod);
	}

	public void podView(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingSpaceId();
			exit(-1);
			return;
		}
		String podId = argId(op, cmds);
		PodOptions options_ = convert(options, PodOptions.class);
		debug("View Pod: %s %s", podId, spaceId);
		Pod pod = devopsClient.getPod(spaceId, podId, options_);			
		view("space", spaceId, "pod", pod.getName());
	}
	
	public void podDelete(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingSpaceId();
			exit(-1);
			return;
		}
		String podId = argId(op, cmds);
		podDelete(spaceId, podId, options);
	}

	public void podDelete(String spaceId, String podId, Map<String, Object> options) {
		debug("Delete Pod: %s %s", spaceId, podId);		
		RequestOptions options_ = convert(options, RequestOptions.class);
		if (isDryrun()) {
			return;
		}
		devopsClient.deletePod(spaceId, podId, options_);
		if (isEcho()) {
			podList(spaceId, options);
		}
	}

	//
	// Space ReplicaSets
	//
	
	public void replicaset(String[] cmds, Map<String, Object> options) {
		switch (op) {
		case "help": case "":
			printUsage1();
			break;
		case "ls": case "list": {
			replicasetList(cmds, options);
			break;
		}
		case "get": {
			replicasetGet(cmds, options);
			break;
		}
		case "view": {
			replicasetView(cmds, options);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			replicasetDelete(cmds, options);
			break;
		}
		case "schema": case "meta":
			if (isHelp2()) {
				return;
			}
			schema(ReplicaSet.class);
		default:
			invalidOp();
			printUsage2();
			break;
		}
	}

	public void replicasetList(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingSpaceId();
			exit(-1);
			return;
		}
		String id = argIdx(op, cmds, false);
		if (id!=null) {
			replicasetDeploymentList(id, options);				
		} else {
			replicasetList(spaceId, options);
		}
	}

	public void replicasetList(String spaceId, Map<String, Object> options) {
		ReplicaSetFilter filter = convert(options, ReplicaSetFilter.class);
		PageOptions options_ = convert(options, PageOptions.class);
		debug("ReplicaSets: %s %s %s", spaceId, filter, options_);		
		List<ReplicaSet> replicasets = devopsClient.listReplicaSets(spaceId, filter, options_.toPageRequest());			
		print(replicasets);
	}
	
	public void replicasetGet(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingSpaceId();
			exit(-1);
			return;
		}
		String rsId = argId(op, cmds);
		replicasetGet(spaceId, rsId, options);
	}
	
	public void replicasetGet(String spaceId, String rsId, Map<String, Object> options) {
		debug("Space ReplicaSet: %s %s", spaceId, rsId);		
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		ReplicaSet replicaset = devopsClient.getReplicaSet(spaceId, rsId, options_);			
		printObj(replicaset);
	}

	public void replicasetView(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingSpaceId();
			exit(-1);
			return;
		}
		String rsId = argId(op, cmds);
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		debug("View ReplicaSet: %s %s", rsId, spaceId);
		ReplicaSet rs = devopsClient.getReplicaSet(spaceId, rsId, options_);			
		view("space", spaceId, "replicaset", rs.getName());
	}
	
	public void replicasetDelete(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingSpaceId();
			exit(-1);
			return;
		}
		String rsId = arg0(op, cmds);
		replicasetDelete(spaceId, rsId, options);
	}

	public void replicasetDelete(String spaceId, String rsId, Map<String, Object> options) {
		if (isHelp()) {
			return;
		}
		RequestOptions options_ = convert(options, RequestOptions.class);
		debug("Remove ReplicaSet: %s %s %s", spaceId, rsId, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.deleteReplicaSet(spaceId, rsId, options_);
		if (isEcho()) {
			replicasetList(spaceId, options);
		}
	}

	//
	// Space VolumeClaims
	//
	
	public void volumeclaim(String[] cmds, Map<String, Object> options) {
		switch (op) {
		case "help": case "":
			printUsage1();
			break;
		case "ls": case "list": {
			volumeclaimList(cmds, options);
			break;
		}
		case "get": {
			volumeclaimGet(cmds, options);
			break;
		}
		case "add": case "create": {
			volumeclaimCreate(cmds, options);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			volumeclaimDelete(cmds, options);
			break;
		}
		case "schema": case "meta":
			if (isHelp2()) {
				return;
			}
			schema(VolumeClaim.class);
		default:
			invalidOp();
			printUsage2();
			break;
		}
	}

	public void volumeclaimList(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingSpaceId();
			exit(-1);
			return;
		}
		volumeclaimList(spaceId, options);
	}

	public void volumeclaimList(String spaceId, Map<String, Object> options) {
		VolumeClaimFilter filter = convert(options, VolumeClaimFilter.class);
		PageOptions options_ = convert(options, PageOptions.class);
		debug("VolumeClaims: %s %s %s", spaceId,filter, options_);		
		List<VolumeClaim> volumeclaims = devopsClient.listVolumeClaims(spaceId, filter);			
		print(volumeclaims);
	}
	
	public void volumeclaimGet(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingSpaceId();
			exit(-1);
			return;
		}
		String volcId = argId(op, cmds);
		volumeclaimGet(spaceId, volcId, options);
	}
	
	public void volumeclaimGet(String spaceId, String volcId, Map<String, Object> options) {
		debug("Space VolumeClaim: %s %s", spaceId, volcId);		
		VolumeClaimOptions options_ = convert(options, VolumeClaimOptions.class);
		VolumeClaim volc = devopsClient.getVolumeClaim(spaceId, volcId, options_);			
		printObj(volc);
	}

	public void volumeclaimCreate(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingSpaceId();
			exit(-1);
			return;
		}
		String volcId = argId(op, cmds);
		VolumeClaim volc = makeVolumeClaim(options);
		volc.setName(volcId);
		RequestOptions options_ = convert(options, RequestOptions.class);
		debug("Add VolumeClaim: %s %s %s", spaceId, volc, options_);		
		if (isDryrun()) {
			return;
		}
		URI uri = devopsClient.createVolumeClaim(spaceId, volc, options_);
		if (isEcho()) {
			String volcId2 = extractId(uri);
			volumeclaimGet(spaceId, volcId2, options);
		}
	}

	private VolumeClaim makeVolumeClaim(Map<String, Object> options) {
		VolumeClaim volc = convert(options, VolumeClaim.class);
		return volc;
	}
	

	public void volumeclaimDelete(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String spaceId = arg1(op, cmds, false);
		String volcId = arg2(op, cmds, false);
		if (!StringUtil.hasText(volcId)) {
			volcId = spaceId;
			spaceId = space;			
		}
		if (!StringUtil.hasText(spaceId) || !StringUtil.hasText(volcId)) {
			missingResourceId();
			exit(-1);
			return;
		}
		RequestOptions options_ = convert(options, RequestOptions.class);
		debug("Delete VolumeClaim: %s %s %s", spaceId, volcId, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.deleteVolumeClaim(spaceId, volcId, options_);
		if (isEcho()) {
			volumeclaimList(spaceId, options);
		}
	}

	
	//
	// Space Builds
	//

	public void build(String[] cmds, Map<String, Object> options) {
		switch (op) {
		case "help": case "":
			printUsage1();
			break;
		case "ls": case "list": {
			buildList(cmds, options);
			break;
		}
		case "get": {
			buildGet(cmds, options);
			break;
		}
		case "create": {
			buildCreate(cmds, options);
			break;
		}
		case "kill": case "remove": case "rm": case "delete": case "del": {
			buildDelete(cmds, options);
			break;
		}
		case "schema": case "meta":
			if (isHelp2()) {
				return;
			}
			schema(Build.class);
		default:
			invalidOp();
			printUsage2();
			break;
		}
	}
	
	public void buildList(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingSpaceId();
			exit(-1);
			return;
		}
		String id = argId(op, cmds, false);
		buildList(spaceId, id, options);			
	}
	
	public void buildList(String spaceId, String deployId, Map<String, Object> options) {
		BuildFilter filter = convert(options, BuildFilter.class);
		if (deployId!=null) {
			filter.setDeployId(deployId);
		}
		debug("Builds: %s %s %s", spaceId, filter);		
		List<Build> builds = devopsClient.listBuilds(spaceId, filter);			
		print(builds);
	}
	
	public void buildGet(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String spaceId = argNS(options, true);
		if (spaceId==null) {
			return;
		}
		String buildId = argId(op, cmds);
		buildGet(spaceId, buildId, options);
	}
	
	public void buildGet(String spaceId, String buildId, Map<String, Object> options) {
		debug("Build: %s %s", spaceId, buildId);		
		BuildOptions options_ = convert(options, BuildOptions.class);
		Build build = devopsClient.getBuild(spaceId, buildId, options_);			
		printObj(build);
	}

	public void buildCreate(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingSpaceId();
			exit(-1);
			return;
		}
		String deployId = argId(op, cmds);
		if (options.get("j")!=null) {
			buildJob(deployId, options);
		} else if (options.get("c")!=null) {
			buildCronJob(deployId, options);
		} else {
			buildDeployment(deployId, options);
		}
	}
	
	private BuildOptions makeBuildOptions(Map<String, Object> options) {
		BuildOptions options_ = convert(options, BuildOptions.class);
		String deploy = (String)options.get("d");
		if (deploy!=null) {
		}
		return options_;
	}


	public void buildDelete(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingSpaceId();
			exit(-1);
			return;
		}
		String buildId = argId(op, cmds);
		buildDelete(spaceId, buildId, options);
	}

	public void buildDelete(String spaceId, String buildId, Map<String, Object> options) {
		debug("Delete Build: %s %s", spaceId, buildId);		
		BuildOptions options_ = convert(options, BuildOptions.class);
		if (isDryrun()) {
			return;
		}
		devopsClient.deleteBuild(spaceId, buildId, options_);
		if (isEcho()) {
			buildList(spaceId, null, options);
		}
	}

	//
	// Space Authority
	//
	
	public void authSpace(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		switch (op2) {
		case "help": case "":
			printUsage2();
			break;
		case "ls": case "list": {
			authSpaceList(cmds, options);
			break;
		}
		case "get": {
			authSpaceGet(cmds, options);
			break;
		}
		case "add": case "create": {
			authSpaceAdd(cmds, options);
			break;
		}
		case "update": {
			authSpaceUpdate(cmds, options);
			break;
		}
		case "resend": {
			authSpaceResend(cmds, options);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			authSpaceDelete(cmds, options);
			break;
		}
		case "schema": case "meta":
			if (isHelp3(op2)) {
				return;
			}
			schema(Authority.class);
		default:
			invalidOp();
			printUsage2();
			break;
		}
	}

	public void authSpaceList(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String spaceId = arg1(op, cmds, false);
		if (!StringUtil.hasText(spaceId)) {
			spaceId = space;			
		}
		if (!StringUtil.hasText(spaceId)) {
			missingResourceId();
			exit(-1);
			return;
		}
		debug("Space Authorities: %s", spaceId);		
		SpaceOptions options_ = convert(options, SpaceOptions.class);
		List<Authority> auths = devopsClient.listAuthorities(spaceId, options_);			
		print(auths);
	}
	
	public void authSpaceGet(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String spaceId = arg1(op, cmds, false);
		String authId = arg2(op, cmds, false);
		if (!StringUtil.hasText(authId)) {
			authId = spaceId;
			spaceId = space;			
		}
		if (!StringUtil.hasText(spaceId) || !StringUtil.hasText(authId)) {
			missingResourceId();
			exit(-1);
			return;
		}
		authSpaceGet(spaceId, authId, cmds, options);
	}
	
	public void authSpaceGet(String spaceId, String authId, String[] cmds, Map<String, Object> options) {
		debug("Space Authority: %s %s", spaceId, authId);		
		SpaceOptions options_ = convert(options, SpaceOptions.class);
		Authority auth = devopsClient.getAuthority(spaceId, authId, options_);			
		printObj(auth);
	}

	public void authSpaceAdd(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String spaceId = arg1(op, cmds, false);
		if (!StringUtil.hasText(spaceId)) {
			spaceId = space;			
		}
		if (!StringUtil.hasText(spaceId)) {
			missingResourceId();
			exit(-1);
			return;
		}
		Authority auth = makeAuthority(options);
		if (auth.getUsername()==null && auth.getGroupId()==null) {
			error("username or group required");
			exit(-1);
			return;
		}
		if (auth.getWrite()==null && auth.getRead()==null && auth.getManage() && auth.getComment()==null) {
			error("missing permission");
			exit(-1);
			return;
		}

		RequestOptions options_ = convert(options, RequestOptions.class);
		debug("Add Authority: %s %s %s", spaceId, auth, options_);		
		if (isDryrun()) {
			return;
		}
		URI uri = devopsClient.addAuthority(spaceId, auth, options_);
		if (isEcho()) {
			String authId = extractId(uri);
			authSpaceGet(spaceId, authId, cmds, options);
		}
	}

	private Authority makeAuthority(Map<String, Object> options) {
		Authority auth = convert(options, Authority.class);
		String username = (String)options.get("u");
		if (username!=null) {
			auth.setUsername(username);
		}
		String groupId = (String)options.get("g");
		if (groupId!=null) {
			auth.setGroupId(groupId);
		}
		String manager = (String)options.get("manager");
		if (manager!=null) {
			auth.setManage(true);
		}
		String dev = (String)options.get("dev");
		if (dev!=null) {
			auth.setWrite(true);
		}
		String auditor = (String)options.get("auditor");
		if (auditor!=null) {
			auth.setRead(true);
		}
		return auth;
	}
	
	public void authSpaceUpdate(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";

		if (isHelp3(op2)) {
			return;
		}
		String spaceId = arg1(op, cmds, false);
		String authId = arg2(op, cmds, false);
		Authority auth = makeAuthority(options);
		if (!StringUtil.hasText(authId)) {
			authId = spaceId;
			spaceId = space;			
		}
		if (authId==null) {
			authId = auth.getUsername();
		}
		if (authId==null) {
			authId = auth.getGroupId();
		}
		if (!StringUtil.hasText(spaceId) || !StringUtil.hasText(authId)) {
			missingResourceId();
			exit(-1);
			return;
		}
		RequestOptions options_ = convert(options, RequestOptions.class);
		debug("Update Authority: %s %s %s", spaceId, authId, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.updateAuthority(spaceId, authId, auth, options_);
		if (isEcho()) {
			authSpaceGet(spaceId, authId, cmds, options);
		}

	}

	public void authSpaceResend(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";

		if (isHelp3(op2)) {
			return;
		}
		String spaceId = arg1(op, cmds, false);
		String authId = arg2(op, cmds, false);
		if (!StringUtil.hasText(authId)) {
			authId = spaceId;
			spaceId = space;			
		}
		if (!StringUtil.hasText(spaceId) || !StringUtil.hasText(authId)) {
			missingResourceId();
			exit(-1);
			return;
		}
		RequestOptions options_ = convert(options, RequestOptions.class);
		debug("Resend Authority: %s %s %s", spaceId, authId, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.resendAuthority(spaceId, authId, options_);
		if (isEcho()) {
			authSpaceGet(spaceId, authId, cmds, options);
		}

	}

	public void authSpaceDelete(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String spaceId = arg1(op, cmds, false);
		String authId = arg2(op, cmds, false);
		if (!StringUtil.hasText(authId)) {
			authId = spaceId;
			spaceId = space;			
		}
		if (!StringUtil.hasText(spaceId) || !StringUtil.hasText(authId)) {
			missingResourceId();
			exit(-1);
			return;
		}
		debug("Remove Authority: %s %s", spaceId, authId);		
		RequestOptions options_ = convert(options, RequestOptions.class);
		if (isDryrun()) {
			return;
		}
		devopsClient.removeAuthority(spaceId, authId, options_);
		if (isEcho()) {
			authSpaceList(cmds, options);
		}
	}
	
	//
	// Generic
	//

	@Override
	protected String[] getOptions(String cmd) {
		switch (cmd) {
		case "ls":
			return c("", "s", "ns", "c", "d", "r", "reg", "vcs", "git");
		case "pwd":
			return c("");
		case "cd":
			return c("");
		}
		return null;
	}
	
	public void ls(String[] cmds, Map<String, Object> options) {
		if (isHelp1()) {
			return;
		}
		boolean b = false;
		if (options.get("n")!=null) {
			listSpace(op, options);
			b = true;
		}
		if (options.get("c")!=null) {
			listCluster(op, options);
			b = true;
		}
		if (options.get("d")!=null) {
			listDomain(op, options);
			b = true;
		}
		if (options.get("r")!=null || options.get("reg")!=null) {
			listRegistry(op, options);
			b = true;
		}
		if (options.get("vcs")!=null || options.get("git")!=null) {
			listVcs(op, options);
			b = true;
		}

		if (!b) {
			listSpace(op, options);
		}
	}

	public void ps(String[] cmds, Map<String, Object> options) {
		if (isHelp1()) {
			return;
		}
		if (options.get("r")!=null) {
			options.put("status", "RUNNING");
		}
		boolean b = false;
		if (options.get("d")!=null || options.get("a")!=null) {
			listDeployment(cmds, options);
			b = true;
		}
		boolean b2 = false;
		if (options.get("j")!=null || options.get("a")!=null) {
			if (b) {
				System.out.println();
			}
			listJob(cmds, options);
			b = true;
			b2 = true;
		}
		if (options.get("c")!=null || options.get("a")!=null) {
			if (b || b2) {
				System.out.println();				
			}
			listCronJob(cmds, options);
			b = true;
		}
		if (!b) {
			listDeployment(cmds, options);
		}
	}

	public void kill(String[] cmds, Map<String, Object> options) {
		if (isHelp1()) {
			return;
		}
		if (op==null || op.isEmpty()) {
			error(String.format("missing resource id"));
			exit(-1);
			return;
		}
		String[] ops = op.split(",");
		for (String id: ops) {
			if (id==null || id.isEmpty()) {
				missingResourceId();
				return;
			}	
			if (options.get("j")!=null) {
				killJob(makeIdx(id), options);
			} else if (options.get("c")!=null) {
				killCronJob(makeIdx(id), options);
			} else {
				killDeployment(makeIdx(id), options);
			}
		}
		if (isEcho()) {
			if (options.get("j")!=null) {
				listJob(cmds, options);
			} else if (options.get("c")!=null) {
				listCronJob(cmds, options);
			} else {
				listDeployment(cmds, options);
			}			
		}
	}

	public void runop(String[] cmds, String[] extra, Map<String, Object> options) {
		if (isHelp1()) {
			return;
		}
		if (op==null || op.isEmpty()) {
			error(String.format("missing resource id"));
			exit(-1);
			return;
		}

		String id = op;
		if (id==null || id.trim().isEmpty()) {
			error(String.format("missing resource id"));
			exit(-1);
			return;
		}
		String image = arg0(op, cmds, false);

		if (options.get("j")!=null || options.get("p")!=null || options.get("completions")!=null || options.get("parallelism")!=null) {
			createJob(id, image, true, extra, options);
			return;
		}
		if (options.get("c")!=null || options.get("schedule")!=null) {
			createCronJob(id, image, true, extra, options);
			return;
		}
		createDeployment(id, image, true, extra, options);
	}
	

	public void cd(String[] cmds, Map<String, Object> options) {
		if (isHelp1()) {
			return;
		}
		if (op==null || op.isEmpty()) {
			error(String.format("missing argument cluster[/space]"));
			exit(-1);
			return;
		}
		String id = op;
		int i = id.indexOf("/");
		if (i<0) {
			if (cluster!=null && !cluster.isEmpty()) {
				id = cluster + "/" + id;
			}
			setSpace(id, options);
		} else if (i>0) {
			if (i==id.length()-1) {
				id = id.substring(0, i);
				setCluster(id, options);				
			} else {
				setSpace(id, options);				
			}
		} else if (i==0) {
			id = id.substring(1);
			setSpace(id, options);
		}
	}
	
	
	public void pwd(String[] cmds, Map<String, Object> options) {
		if (isHelp1()) {
			return;
		}
		if (StringUtil.hasText(cluster)) {
			System.out.println(String.format("Cluster: %s", cluster));			
		}
		if (StringUtil.hasText(space)) {
			System.out.println(String.format("Space: %s", space));			
		}
		if (StringUtil.hasText(domain)) {
			System.out.println(String.format("Domain: %s", domain));			
		}
		if (StringUtil.hasText(registry)) {
			System.out.println(String.format("Registry: %s", registry));			
		}
		if (StringUtil.hasText(vcs)) {
			System.out.println(String.format("Vcs: %s", vcs));			
		}
		if (StringUtil.hasText(catalog)) {
			System.out.println(String.format("Catalog: %s", catalog));			
		}
	}
	
	
	public void mount(String[] cmds, Map<String, Object> options) {
		if (options.get("j")!=null) {
			mountJob0(cmds, options);
		} else if (options.get("c")!=null) {
			mountCronJob0(cmds, options);
		} else {
			mountDeployment0(cmds, options);
		}
	}

	public void env(String[] cmds, Map<String, Object> options) {
		if (options.get("j")!=null) {
			envJob0(cmds, options);
		} else if (options.get("c")!=null) {
			envCronJob0(cmds, options);
		} else {
			envDeployment0(cmds, options);
		}
	}

	public void binding(String[] cmds, Map<String, Object> options) {
		if (options.get("j")!=null) {
			bindingJob0(cmds, options);
		} else if (options.get("c")!=null) {
			bindingCronJob0(cmds, options);
		} else {
			bindingDeployment0(cmds, options);
		}
	}


	
	//
	// Deployments
	//

	public void deployment(String[] cmds, Map<String, Object> options) {
		switch (op) {
		case "help": case "":
			printUsage(type);
			break;
		case "get": 
			getDeployment(cmds, options);
			break;
		case "view": 
			viewDeployment(cmds, options);
			break;
		case "ls": case "list": case "ps":
			listDeployment(cmds, options);
			break;
		case "schema": case "meta":
			schemaDeployment(cmds, options);
			break;
		case "create": case "add": 
			createDeployment(cmds, extra, options);
			break;
		case "update": 
			updateDeployment(cmds, extra, options);
			break;
		case "delete": case "del": case "rm": case "remove":
			deleteDeployment(cmds, options);
			break;
		case "scale":
			scaleDeployment(cmds, options);
			break;
		case "resources": case "resource": case "rscale":
			resourcesDeployment(cmds, options);
			break;
		case "start":
			startDeployment(cmds, options);
			break;
		case "stop":
			stopDeployment(cmds, options);
			break;
		case "restart":
			restartDeployment(cmds, options);
			break;
		case "sync":
			syncDeployment(cmds, options);
			break;
		case "attach":
			attachDeployment(cmds, options);
			break;
		case "exec":
			execDeployment(cmds, extra, options);
			break;
		case "logs": case "log":
			logDeployment(cmds, options);
			break;
		case "build":
			buildDeployment(cmds, options);
			break;
		case "events": case "event":
			eventDeployment(cmds, options);
			break;
		case "route": case "routes":
			routeDeployment(cmds, options);
			break;
		case "go":
			goDeployment(cmds, options);
			break;
		case "mount":
			mountDeployment(cmds, options);
			break;
		case "env": case "var": case "vars": 
			envDeployment(cmds, options);
			break;
		case "binding": case "bind":
			bindingDeployment(cmds, options);
			break;
		case "connector": case "conn":
			connectorDeployment(cmds, options);
			break;
		default: 
			if (isHelp()) {
				printUsage();
				exit(0);
				return;
			}
			invalidOp(type, op);
			break;
		}
	}

	
	public void listDeployment(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		DeploymentFilter filter = convert(options, DeploymentFilter.class);
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingArg("-n");
			return;
		}
		String q = argId(op, cmds, false);
		if (q!=null) {
			filter.setQ(q);
		}
		if (options.get("r")!=null) {
			filter.setStatus(DeploymentStatus.RUNNING);
		}		
		debug("Deployments: %s %s %s", spaceId, filter, pageable);
		Page<Deployment> deployments = devopsClient.listDeployments(spaceId, filter, pageable);
		print(deployments, Deployment.class);
	}
	
	public void getDeployment(String deployId, Map<String, Object> options) {
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		debug("Get Deployment: %s", deployId);
		Deployment deployment = devopsClient.getDeployment(deployId, options_);
		printObj(deployment);
	}

	public void getDeployment(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		debug("Get Deployment: %s", deployId);
		Deployment deployment = devopsClient.getDeployment(deployId, options_);
		printObj(deployment);
	}

	public void viewDeployment(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		debug("View Deployment: %s", deployId);
		Deployment deploy = devopsClient.getDeployment(deployId, options_);
		view("deployment", deploy.getUuid());
	}

	public void schemaDeployment(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		schema(Deployment.class, DeploymentFilter.class, DeploymentOptions.class, options);
	}

	public void createDeployment(String[] cmds, String[] extra, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String start = (String)options.get("--start");
		String name = argId(op, cmds);
		String image = arg1(op, cmds, false);
		createDeployment(name, image, start!=null, extra, options);
	}

	public void createDeployment(String name, String image, boolean start, String[] extra, Map<String, Object> options) {
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingArg("-n");
			return;
		}
		Deployment deployment = makeDeployment(name, image, extra, options);
		if (deployment==null) {
			return;
		}
		if (start) {
			deployment.setStart(true);
		}
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		debug("Creating Deployment: %s %s", deployment, options_);
		if (isDryrun()) {
			return;
		}
		URI uri = devopsClient.createDeployment(spaceId, deployment, options_);
		if (isEcho() || isTrack()) {
			debug("Deployment URI: %s", uri);
			String deployId = extractId(uri);
			Deployment deployment2 = devopsClient.getDeployment(deployId, null);
			printObj(deployment2);
			if (isTrack()) {
				if (deployment2.getStatus()!=DeploymentStatus.RUNNING) {
					track(deployId, (id, n)-> {
						Deployment deployment3 = devopsClient.getDeployment(id, null);
						printObj(deployment3, false);	
						return deployment3.getStatus()==DeploymentStatus.RUNNING;
					});					
				}
			}
		}
	}
	
	void setupDeployOptions(String image, Map<String, Object> options) {
		String image_ = (String)options.get("image");
		if (StringUtil.hasText(image_)) {
			options.remove("image");
			if (image==null) {
				image = image_;				
			}
		}
		if (image!=null) {
			options.put("image.name", image);
		}
		String buildImage = (String)options.get("buildImage");
		if (buildImage!=null) {
			options.remove("buildImage");
			options.put("buildImage.name", buildImage);
		}
		String registry = (String)options.get("registry");
		if (registry!=null) {
			options.remove("registry");			
			options.put("reg", registry);			
		}
		String workspace = (String)options.get("workspace");
		if (workspace!=null) {
			options.remove("workspace");
			MountType mountType = MountType.parse(workspace);
			if (mountType!=null) {
				options.put("workspace.type", mountType);				
			}
		}
		String webhook = (String)options.get("webhook");
		if (webhook!=null) {
			options.remove("webhook");
			boolean enabled = webhook.isEmpty() || "true".equalsIgnoreCase(webhook);
			options.put("webhook.enabled", enabled);				
		}
	}
	
	private Deployable processDeployableOptions(Deployable deploy, String[] extra, Map<String, Object> options) {
		if (deploy.getStack()!=null && deploy.getAutoEnv()==null) {
			deploy.setAutoEnv(true);
		}
		String command = (String)options.get("command");
		if (command==null) {
			command = (String)options.get("c");
		}
		if (extra!=null && extra.length>0) {
			if (command!=null) {
				deploy.setCommand(Arrays.asList(extra));
			} else {
				deploy.setArgs(Arrays.asList(extra));				
			}
		} else {
			if (command!=null) {
				error("command is missing");
				exit(-1);
				return null;
			}
		}
		String reg = (String)options.get("reg");
		if (reg!=null) {
			try {
				Registry registry = devopsClient.getRegistry(reg, null);	
				Image image = deploy.getImage();
				if (image==null) {
					image  = new Image();
					deploy.setImage(image);
				}
				image.setRegistryId(registry.getUuid());
				Image buildImage = deploy.getBuildImage();
				if (buildImage==null) {
					buildImage  = new Image();
					deploy.setBuildImage(buildImage);
				}
				buildImage.setRegistryId(registry.getUuid());
			} catch (HttpStatusCodeException e) {
				if (e.getStatusCode()==HttpStatus.NOT_FOUND) {
					error("registry not found: %s", reg);
					exit(-1);
					return null;
				} else {
					throw e;					
				}
			}
		}
		String repo = (String)options.get("repo");
		String git = (String)options.get("vcs");
		if (git==null) {
			git = (String)options.get("git"); 
		}
		Repository repository = deploy.getRepository();
		if (repo!=null || git!=null) {
			if (repository==null) {
				repository = new Repository();
				deploy.setRepository(repository);
			}						
		}
		if (repo!=null) {
			repository.setUrl(repo);
		}
		if (git!=null) {
			try {
				Vcs vcs = devopsClient.getVcs(git, null);	
				repository.setVcsId(vcs.getUuid());
			} catch (HttpStatusCodeException e) {
				if (e.getStatusCode()==HttpStatus.NOT_FOUND) {
					error("vcs not found: %s", git);
					exit(-1);
					return null;
				} else {
					throw e;					
				}
			}
		}

		
		String wsize = (String)options.get("wsize");
		if (wsize!=null && !wsize.isEmpty()) {
			Workspace workspace = deploy.getWorkspace();
			if (workspace==null) {
				workspace = new Workspace();
				deploy.setWorkspace(workspace);
			}
			workspace.setType(MountType.VOLUME_TEMPLATE);
			workspace.setSize(wsize);
		}
		String ws = (String)options.get("ws");
		if (ws!=null && !ws.isEmpty()) {
			Workspace workspace = deploy.getWorkspace();
			if (workspace==null) {
				workspace = new Workspace();
				deploy.setWorkspace(workspace);
			}
			workspace.setType(MountType.VOLUME);
			workspace.setVolc(ws);
			String subpath = (String)options.get("subpath");
			if (subpath!=null && !subpath.isEmpty()) {
				workspace.setSubPath(subpath);
			}
		}
		String wh = (String)options.get("webhook");
		if (wh==null) {
			wh = (String)options.get("webhooks");
		}
		if (wh==null) {
			wh = (String)options.get("wh");
		}
		String autodeploy = (String)options.get("wd");
		if (wh!=null || autodeploy!=null) {
			Webhook webhook = deploy.getWebhook();
			if (webhook==null) {
				webhook = new Webhook();
				deploy.setWebhook(webhook);
			}
		}
		Webhook webhook = deploy.getWebhook();
		if (webhook!=null) {
			boolean bautodeploy = autodeploy!=null && (autodeploy.isEmpty() || "true".equalsIgnoreCase(autodeploy));
			boolean enabled = bautodeploy || (wh!=null && (wh.isEmpty() || "true".equalsIgnoreCase(wh)));
			webhook.setEnabled(enabled);
			webhook.setDeploy(bautodeploy);
		}
		return deploy;
	}
	
	private Deployment makeDeployment(String name, String image, String[] extra, Map<String, Object> options) {
		setupDeployOptions(image, options);
		Deployment deploy = convert(options, Deployment.class);
		if (name!=null) {
			deploy.setName(name);
		}
		if (processDeployableOptions(deploy, extra, options)==null) {
			return null;
		}
		Integer k = parseInt((String)options.get("k"));
		if (k!=null) {
			deploy.setDesiredReplicas(k);
		}
		String s = (String)options.get("s");			
		if (s!=null) {
			deploy.setKind(DeploymentKind.STATEFUL_SET);
		}
		String host = (String)options.get("host");
		if (host==null) {
			host = (String)options.get("r");			
		}
		if (host!=null) {
			if (host.isEmpty()) {
				host = name;
			}
		}
		if (host!=null && !host.isEmpty()) {
			List<Route> routes = makeRoutes(host, options);
			if (routes!=null && !routes.isEmpty()) {
				if (deploy.getRoutes()==null) {
					deploy.setRoutes(routes);
				} else {
					deploy.getRoutes().addAll(routes);								
				}
			}
		}
		List<Port> ports = new ArrayList<>();
		if (!makePorts(options, ports)) {
			return null;
		}
		if (!ports.isEmpty()) {
			deploy.setPorts(ports);
		}
		List<Variable> env = new ArrayList<>();
		if (!makeEnv(options, env)) {
			return null;
		}
		if (!env.isEmpty()) {
			deploy.setEnv(env);
		}
		List<Mount> mount = new ArrayList<>();
		if (!makeMounts(options, mount)) {
			return null;
		}
		if (!mount.isEmpty()) {
			deploy.setMounts(mount);
		}
		return deploy;
	}
	
	private boolean makePorts(Map<String, Object> options, List<Port> ports) {
		String pp = (String)options.get("ports");
		if (pp==null) {
			pp = (String)options.get("port");
		}
		if (pp!=null && !pp.isEmpty()) {
			String[] ss = pp.split(",");
			for (String p: ss) {
				Port port = new Port();
				int i = p.indexOf(":");
				Integer portValue = null;
				Integer targetPort = null;
				if (i>0 && i<p.length()-1) {
					portValue = parseInt(p.substring(0,i));
					targetPort = parseInt(p.substring(i+1));
				} else if (i<0) {
					portValue = parseInt(p);
				}
				if (portValue==null) {
					error("Invalid port value: %s", p);
					exit(-1);
					return false;
				}
				port.setPort(portValue);
				if (targetPort==null) {
					targetPort = portValue;
				}
				port.setTargetPort(targetPort);
				ports.add(port);
			}
		}
		return true;
	}

	private Domain makeRouteDomain(Map<String, Object> options) {
		String domainId = (String)options.get("d");
		if (domainId==null || domainId.isEmpty()) {
			domainId = this.domain;
		}
		Domain domain = null;
		if (domainId!=null && !domainId.isEmpty()) {
			try {
				domain = devopsClient.getDomain(domainId, null);						
			 }catch (HttpStatusCodeException e) {
				if (e.getStatusCode()==HttpStatus.NOT_FOUND) {
					error("Domain not found: %s", domainId);
					exit(-1);
					return null;
				}
				throw e;
			}
		}
		if (domain==null) {
			return null;
		}
		Domain domain2 = new Domain();
		domain2.setId(domain.getId());
		domain2.setUuid(domain.getUuid());
		domain2.setDns(domain.getDns());
		return domain2;
	}

	private Route makeRoute(String host, Map<String, Object> options) {
		Domain domain = makeRouteDomain(options);
		return makeRoute(host, domain, options);
	}

	private Route makeRoute(String host, Domain domain, Map<String, Object> options) {
		Route route = convert(options, Route.class);
		route.setHost(host);
		route.setDomain(domain);
		return route;
	}
	
	private List<Route> makeRoutes(String name, Map<String, Object> options) {
		Domain domain = makeRouteDomain(options);
		String[] hosts = name.split(",");
		List<Route> routes = new ArrayList<>();
		for (String host: hosts) {
			Route route = makeRoute(host, domain, options);
			routes.add(route);
		}
		return routes;
	}
	
	/*
	 * var1=value,var2=^configmap.key,var3=^^secret.key
	 */
	private boolean makeEnv(Map<String, Object> options, List<Variable> env) {
		String ee = (String)options.get("env");
		if (ee!=null && !ee.isEmpty()) {
			String[] ss = ee.split(",");
			for (String e: ss) {
				Variable var = new Variable();
				int i = e.indexOf("=");
				String name = null;
				String value = null;
				if (i>0 && i<e.length()-1) {
					name = e.substring(0,i);
					value = e.substring(i+1);
				} else if (i<0) {
					name = e;
				}
				if (name==null) {
					error("Invalid environment variable: %s", ee);
					exit(-1);
					return false;
				}
				name = name.trim();
				if (name.isEmpty()) {
					error("Invalid environment variable: %s", ee);
					exit(-1);
					return false;
				}
				var.setName(name);
				if (value==null) {
					value = "";
				}
				if (value.startsWith("^^")) {
					var.setCategory(VarCategory.SECRET);
					if (value.length()==2) {
						error("Invalid environment variable: %s", ee);
						exit(-1);
						return false;
					}
					value = value.substring(2);				
					i = value.indexOf(".");
					if (i==0 || i==value.length()-1) {
						error("Invalid environment variable: %s", ee);
						exit(-1);
						return false;
					}
					String secret = value.substring(0, i);
					value = value.substring(i+1);
					var.setPath(value);
					var.setSecret(secret);
				} else if (value.startsWith("^")) {
					var.setCategory(VarCategory.CONFIGMAP);
					if (value.length()==1) {
						error("Invalid environment variable: %s", ee);
						exit(-1);
						return false;
					}
					value = value.substring(1);						
					i = value.indexOf(".");
					if (i==0 || i==value.length()-1) {
						error("Invalid environment variable: %s", ee);
						exit(-1);
						return false;
					}
					String configmap = value.substring(0, i);
					value = value.substring(i+1);
					var.setPath(value);
					var.setConfigmap(configmap);
				} else {
					var.setCategory(VarCategory.VALUE);
					var.setValue(value);
				}
				env.add(var);
			}
		}
		return true;
	}
	
	
	/*
	 * name1:/mountPath=1G1,name2:/mountPath=^configmap.key1+key2,name3:/mountPath=^^secret.key2+key2
	 */
	private boolean makeMounts(Map<String, Object> options, List<Mount> mounts) {
		String ee = (String)options.get("mounts");
		if (ee!=null && !ee.isEmpty()) {
			String[] ss = ee.split(",");
			for (String e: ss) {
				Mount mount = new Mount();
				int i = e.indexOf("=");
				String name = null;
				String value = null;
				if (i>0 && i<e.length()-1) {
					name = e.substring(0,i);
					value = e.substring(i+1);
				} else if (i<0) {
					name = e;
				}
				if (name==null) {
					error("Invalid mount: %s", ee);
					exit(-1);
					return false;
				}
				name = name.trim();
				if (name.isEmpty()) {
					error("Invalid mount: %s", ee);
					exit(-1);
					return false;
				}
				i = name.indexOf(":");
				if (i<0 || i==name.length()-1) {
					error("Invalid mount: %s", ee);
					exit(-1);
					return false;
				}
				String mountPath = name.substring(i+1);
				name = name.substring(0,i);
				mount.setName(name);
				mount.setMountPath(mountPath);
				if (value==null) {
					value = "1Gi";
				}
				if (value.startsWith("^^")) {
					mount.setType(MountType.SECRET);
					if (value.length()==2) {
						error("Invalid mount: %s", ee);
						exit(-1);
						return false;
					}
					value = value.substring(2);				
					i = value.indexOf(".");
					if (i==0 || i==value.length()-1) {
						error("Invalid mount: %s", ee);
						exit(-1);
						return false;
					}
					String secret = value.substring(0, i);
					value = value.substring(i+1);
					mount.setSecret(secret);
					List<KeyPath> items = new ArrayList<>();
					if (!makeMountItems(value, mountPath, items)) {
						error("Invalid mount: %s", ee);
						exit(-1);
						return false;
					}
					mount.setItems(items);
				} else if (value.startsWith("^")) {
					mount.setType(MountType.CONFIGMAP);
					if (value.length()==1) {
						error("Invalid mount: %s", ee);
						exit(-1);
						return false;
					}
					value = value.substring(1);						
					i = value.indexOf(".");
					if (i==0 || i==value.length()-1) {
						error("Invalid mount: %s", ee);
						exit(-1);
						return false;
					}
					String configmap = value.substring(0, i);
					value = value.substring(i+1);
					mount.setConfigmap(configmap);
					List<KeyPath> items = new ArrayList<>();
					if (!makeMountItems(value, mountPath, items)) {
						error("Invalid mount: %s", ee);
						exit(-1);
						return false;
					}
					mount.setItems(items);
				} else {
					mount.setType(MountType.VOLUME);
					mount.setSize(value);
				}
				mounts.add(mount);
			}
		}
		return true;
	}
	
	
	/*
	 * config:/config=^configmap.key+key2
	 */
	private boolean makeMountItems(String value, String path, List<KeyPath> items) {
		if (value==null) {
			return false;
		}
		value = value.trim();
		if (value.isEmpty()) {
			return false;
		}
		String[] keys = value.split("+");
		for (String key: keys) {
			if (key==null || key.isEmpty()) {
				return false;
			}
			KeyPath item = new KeyPath();
			item.setKey(key);
			item.setPath(PathUtil.concat(path, key));
			items.add(item);
		}
		return items.size()>0 ? true : false;
	}

	public void updateDeployment(String[] cmds, String[] extra, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}

		String deployId = argIdx(op, cmds);
		String image = arg1(op, cmds, false);
		Deployment deployment = makeDeployment(null, image, extra, options);
		if (deployment==null) {
			return;
		}
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		debug("Updating Deployment: %s %s %s", deployId, deployment, options_);
		if (isDryrun()) {
			return;
		}
		devopsClient.updateDeployment(deployId, deployment, options_);
		if (isEcho()) {
			Deployment deployment2 = devopsClient.getDeployment(deployId, null);
			printObj(deployment2);			
		}
	}
	
	public void killDeployment(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		if (options.get("f")!=null || options.get("rm")!=null || options.get("del")!=null) {
			deleteDeployment(cmds, options);
		} else {
			stopDeployment(cmds, options);
		}
	}
	
	public void killDeployment(String deployId, Map<String, Object> options) {
		if (options.get("f")!=null || options.get("rm")!=null || options.get("del")!=null) {
			deleteDeployment(deployId, options);
		} else {
			stopDeployment(deployId, options);
		}
	}

	public void deleteDeployment(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argId(op, cmds);
		String[] ids = deployId.split(",");
		for (String id: ids) {
			if (id.isEmpty()) {
				continue;
			}
			deleteDeployment(makeIdx(id), options);			
		}
		if (isEcho()) {
			listDeployment(cmds, options);
		}
	}
	
	public void deleteDeployment(String deployId, Map<String, Object> options) {
		deployId = makeIdx(deployId);
		debug("Deleting Deployment: %s", deployId);	
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		if (options_.getDeployment()==null) {
			options_.setDeployment(true);
		}
		if (options_.getService()==null) {
			options_.setService(true);
		}
		if (options_.getIngress()==null) {
			options_.setIngress(true);
		}
		if (isDryrun()) {
			return;
		}
		devopsClient.deleteDeployment(deployId, options_);		
	}
	
	public void scaleDeployment(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		debug("Scaling Deployment: %s", deployId);		
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		String k = get(new String[] {"k", "replicas", "instances"}, options, String.class);
		if (k==null) {
			k = arg1(op, cmds, false);
		}
		if (k==null) {
			error("Missing replica count...");
			exit(-1);
		}
		Integer k_ = parseInt(k);
		if (k_==null || k_<0) {
			error("Invalid replica count: %s", k);
			exit(-1);
		}
		if (isDryrun()) {
			return;
		}
		devopsClient.scaleDeployment(deployId, k_, options_);
		if (isTrack()) {
			track(deployId, (id, n)-> {
				Deployment deployment = devopsClient.getDeployment(id, null);
				printObj(deployment, n==0);	
				return deployment.getAvailableReplicas()==k_;
			}, 0);
		}
		if (isEcho()) {
			podDeploymentList(deployId, options);
		}
	}
	
	public void resourcesDeployment(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		Resources resources = makeResources(options);
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		debug("Scaling Deployment resources: %s %s %s", deployId, resources, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.scaleDeployment(deployId, resources, options_);	
		if (isEcho()) {
			devopsClient.listPodsForDeployment(deployId, null, null);
		}
	}

	private Resources makeResources(Map<String, Object> options) {
		Resources resources = convert(options, Resources.class);
		String mem = get(new String[] {"m", "mem", "memory"}, options, String.class);
		if (mem!=null) {
			resources.setMemory(mem);
		}
		String disk = get(new String[] {"disk", "storage"}, options, String.class);
		if (disk!=null) {
			resources.setDisk(disk);
		}
		String cpu = get(new String[] {"cpu"}, options, String.class);
		if (cpu!=null) {
			resources.setCpu(cpu);
		}
		return resources;
	}
	public void startDeployment(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		debug("Starting Deployment: %s", deployId);		
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		if (isDryrun()) {
			return;
		}
		devopsClient.startDeployment(deployId, options_);		
		if (isEcho()) {
			getDeployment(deployId, options);
		}
	}

	public void stopDeployment(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argId(op, cmds);
		String[] ids = deployId.split(",");
		for (String id: ids) {
			if (id.isEmpty()) {
				continue;
			}
			stopDeployment(makeIdx(id), options);			
		}
	}

	public void stopDeployment(String deployId, Map<String, Object> options) {
		debug("Stopping Deployment: %s", deployId);		
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		if (isDryrun()) {
			return;
		}
		devopsClient.stopDeployment(deployId, options_);	
		if (isEcho()) {
			getDeployment(deployId, options);
		}
	}

	public void restartDeployment(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		debug("Restarting Deployment: %s %s", deployId, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.restartDeployment(deployId, options_);
		if (isEcho()) {
			getDeployment(deployId, options);
		}

	}
	
	public void syncDeployment(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		debug("Sync Deployment: %s %s", deployId, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.syncDeployment(deployId, options_);			
		if (isEcho()) {
			getDeployment(deployId, options);
		}
	}
	
	public void attachDeployment(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argId(op, cmds);
		String spaceId = argNS(options);
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		debug("Attach Deployment: %s %s %s", spaceId, deployId, options_);		
		if (isDryrun()) {
			return;
		}
		URI uri = devopsClient.attachDeployment(spaceId, deployId, options_);			
		if (isEcho()) {
			String id = extractId(uri);
			debug("Deployment URI: %s", uri);		
			getDeployment(id, options);
		}
	}

	public void execDeployment(String[] cmds, String[] extra, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		ExecOptions options_ = makeExecOptions(cmds, extra, options);
		debug("Exec Deployment: %s %s", deployId, options_);		
		if (isDryrun()) {
			return;
		}
		String out = devopsClient.execDeployment(deployId, options_);			
		System.out.println(out);
	}
	
	private ExecOptions makeExecOptions(String[] cmds, String[] extra, Map<String, Object> options) {
		ExecOptions options_ = convert(options, ExecOptions.class);
		String cmd = String.join(" ", extra);
		options_.setCmd(cmd);
		String pod = arg1(op, cmds, false);
		if (pod==null) {
			pod = (String)options.get("pod");			
		}
		if (pod!=null && !pod.isEmpty()) {
			options_.setPod(pod);			
		}
		return options_;
	}
	
	
	public void logDeployment(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		String pod = arg2(op, cmds, false);
		LogOptions options_ = makeLogOptions(options);
		if (pod!=null && !pod.isEmpty()) {
			options_.setPod(pod);
		}
		debug("Log Deployment: %s %s", deployId, options_);		
		if (isDryrun()) {
			return;
		}
		String out = devopsClient.logDeployment(deployId, options_);			
		System.out.println(out);
	}
	
	private LogOptions makeLogOptions(Map<String, Object> options) {
		LogOptions options_ = convert(options, LogOptions.class);
		String tail = (String)options.get("l");
		if (tail!=null) {
			Integer n = parseInt(tail);			
			if (n!=null) {
				options_.setTailLines(n);
			}
		}
		return options_;
	}

	public void buildDeployment(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		buildDeployment(deployId, options);
	}

	public void buildDeployment0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		buildDeployment(deployId, options);
	}

	public void buildDeployment(String deployId, Map<String, Object> options) {
		BuildOptions options_ = makeBuildOptions(options);
		debug("Build Deployment: %s %s", deployId, options_);		
		if (isDryrun()) {
			return;
		}
		String spaceId = argNS(options, true);
		if (spaceId==null) {
			return;
		}		
		URI uri = devopsClient.buildDeployment(deployId, options_);
		if (isEcho()) {
			debug("Build URI: %s", uri);
			String id = extractId(uri);
			Build build = devopsClient.getBuild(spaceId, id, options_);
			printObj(build);			
		}
	}

	//
	// Deployment Pod/Instances
	//
	

	public void podDeploymentList(String deployId, Map<String, Object> options) {
		debug("Instances of: %s", deployId);		
		PodFilter filter = convert(options, PodFilter.class);
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		List<Pod> instances = devopsClient.listPodsForDeployment(deployId, filter, pageable);			
		print(instances);
	}

	public void podDeploymentKill(String deployId, String pod, String[] cmds, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		debug("Delete Instance: %s %s %s", deployId, pod, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.deletePodForDeployment(deployId, pod, options_);
		if (isEcho()) {
			podDeploymentList(deployId, options);
		}
	}

	//
	//
	// Deployment ReplicaSet/ReplicaSets
	//
	
	public void replicasetDeployment(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		switch (op2) {
		case "help": case "":
			printUsage2();
			break;
		case "ls": case "list": {
			replicasetDeploymentList(cmds, options);
			break;
		}
		case "kill": case "remove": case "rm": case "delete": case "del": {
			replicasetDeploymentKill(cmds, options);
			break;
		}
		default:
			invalidOp();
			printUsage2();
			break;
		}
	}

	public void replicasetDeploymentList(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		replicasetDeploymentList(deployId, options);
	}


	public void replicasetDeploymentList(String deployId, Map<String, Object> options) {
		debug("ReplicaSets of: %s", deployId);		
		ReplicaSetFilter filter = convert(options, ReplicaSetFilter.class);
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		List<ReplicaSet> replicasets = devopsClient.listReplicaSetsForDeployment(deployId, filter, pageable);			
		print(replicasets);
	}

	public void replicasetDeploymentKill(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		RequestOptions options_ = convert(options, RequestOptions.class);
		String replicaset = arg2(op, cmds, true);
		debug("Delete ReplicaSet: %s %s %s", deployId, replicaset, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.deleteReplicaSetForDeployment(deployId, replicaset, options_);
		if (isEcho()) {
			replicasetDeploymentList(deployId, options);
		}
	}

	
	//
	// Deployment Events
	//
	
	public void eventDeployment(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		switch (op2) {
		case "help": case "":
			printUsage2();
			break;
		case "ls": case "list": {
			eventDeploymentList(cmds, options);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			eventDeploymentDelete(cmds, options);
			break;
		}
		case "schema": case "meta":
			if (isHelp3(op2)) {
				return;
			}
			schema(Event.class);
		default:
			invalidOp();
			printUsage2();
			break;
		}
	}



	public void eventDeploymentList(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		eventDeploymentList(deployId, options);
	}

	public void eventDeploymentList(String deployId, Map<String, Object> options) {
		EventFilter filter = convert(options, EventFilter.class);
		String c = (String)options.get("c");
		if (c!=null) {
			filter.setCluster(true);
		}
		PageOptions options_ = convert(options, PageOptions.class);
		debug("Deployment Events: %s %s %s", deployId,filter, options_);		
		Page<Event> events = devopsClient.listEvents(deployId, filter, options_.toPageRequest());			
		print(events);
	}


	public void eventDeploymentDelete(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		RequestOptions options_ = convert(options, RequestOptions.class);
		String eventId = arg2(op2, cmds);
		debug("Remove Event: %s %s %s", deployId, eventId, options_);		
		//devopsClient.removeEvent(deployId, eventId, options_);
		if (isEcho()) {
			eventDeploymentList(cmds, options);
		}
	}


	
	//
	// Deployment Route
	//
	
	public void routeDeployment(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		switch (op2) {
		case "help": case "":
			printUsage2();
			break;
		case "ls": case "list": {
			routeDeploymentList(cmds, options);
			break;
		}
		case "get": {
			routeDeploymentGet(cmds, options);
			break;
		}
		case "add": case "create": {
			routeDeploymentCreate(cmds, options);
			break;
		}
		case "update": {
			routeDeploymentUpdate(cmds, options);
			break;
		}
		case "remove": case "rm": case "delete": case "del": {
			routeDeploymentDelete(cmds, options);
			break;
		}
		case "go": {
			routeDeploymentGo(cmds, options);
			break;
		}
		case "schema": case "meta": {
			routeSchemaDeployment(cmds, options);
			break;
		}

		default:
			invalidOp();
			printUsage2();
			break;
		}
	}

	public void route(String[] cmds, Map<String, Object> options) {
		switch (op) {
		case "help": case "":
			printUsage1();
			break;
		case "get": 
			routeGet(cmds, options);
			break;
		case "ls": case "list":
			routeList(cmds, options);
			break;
		case "schema": case "meta":
			routeSchema(cmds, options);
			break;
		case "add": case "create": 
			routeCreate(cmds, options);
			break;
		case "update": 
			routeUpdate(cmds, options);
			break;
		case "delete": case "del": case "rm": case "remove":
			routeDelete(cmds, options);
			break;
		case "go":
			routeGo(cmds, options);
			break;
		default: 
			if (isHelp()) {
				printUsage();
				exit(0);
				return;
			}
			invalidOp(type, op);
			break;
		}
	}

	
	public void routeDeploymentList(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "ls";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		routeList(deployId, options);
	}

	public void routeList(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		routeList(deployId, options);
	}
	
	public void routeList(String deployId, Map<String, Object> options) {
		debug("Deployment Routes: %s", deployId);		
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		List<Route> routes = devopsClient.listRoutes(deployId, options_);			
		print(routes);
	}

	public void routeDeploymentGet(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "add";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		String routeId = arg2(op, cmds);
		routeGet(deployId, routeId, cmds, options);
	}

	public void routeGet(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		String routeId = arg1(op, cmds);
		routeGet(deployId, routeId, cmds, options);
	}
	
	public void routeGet(String deployId, String routeId, String[] cmds, Map<String, Object> options) {
		debug("Deployment Route: %s %s", deployId, routeId);		
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		Route route = devopsClient.getRoute(deployId, routeId, options_);			
		printObj(route);
	}

	public void routeDeploymentCreate(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		String routeId = arg2(op, cmds);
		routeCreate(deployId, routeId, cmds, options);
	}

	public void routeCreate(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		String routeId = arg1(op, cmds, false);
		if (routeId==null || routeId.isEmpty()) {
			error("Missing route hostname:");
			exit(-1);
			return;
		}
		routeCreate(deployId, routeId, cmds, options);
	}
	
	public void routeCreate(String deployId, String routeId, String[] cmds, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		Route route = makeRoute(routeId, options);
		if (route==null) {
			error("Missing route deails: %s", routeId);
			exit(-1);
			return;
		}
		debug("Add Route: %s %s %s", deployId, route, options_);		
		if (isDryrun()) {
			return;
		}
		URI uri = devopsClient.addRoute(deployId, route, options_);
		if (isEcho()) {
			String routeId2 = extractId(uri);
			routeGet(deployId, routeId2, cmds, options);
		}
	}

	public void routeDeploymentUpdate(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		String routeId = arg2(op, cmds);
		routeUpdate(deployId, routeId, cmds, options);
	}

	public void routeUpdate(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		String routeId = arg1(op, cmds);
		routeUpdate(deployId, routeId, cmds, options);
	}
	
	public void routeUpdate(String deployId, String routeId, String[] cmds, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		Route route = makeRoute(null, options);
		debug("Update Route: %s %s %s %s", deployId, routeId, route, options_);	
		if (isDryrun()) {
			return;
		}
		devopsClient.updateRoute(deployId, routeId, route, options_);
		if (isEcho()) {
			routeGet(deployId, routeId, cmds, options);
		}
	}

	public void routeDeploymentDelete(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		String routeId = arg2(op, cmds);
		routeDelete(deployId, routeId, cmds, options);
	}

	public void routeDelete(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		String routeId = arg1(op, cmds);
		routeDelete(deployId, routeId, cmds, options);
	}
	
	public void routeDelete(String deployId, String routeId, String[] cmds, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		debug("Remove Route: %s %s %s", deployId, routeId, options_);	
		if (isDryrun()) {
			return;
		}
		devopsClient.removeRoute(deployId, routeId, options_);
		if (isEcho()) {
			routeDeploymentList(cmds, options);
		}
	}

	public void routeDeploymentGo(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "go";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		String routeId = arg2(op, cmds);
		routeGo(deployId, routeId, options);
	}

	public void routeGo(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		String routeId = arg1(op, cmds, false);
		routeGo(deployId, routeId, options);
	}
	
	
	public void routeGo(String deployId, String routeId, Route route) {
		String url = route.getUrl();
		if (!StringUtil.hasText(url)) {
			error("missing url for route: %s %s", deployId, routeId);
			exit(-1);
			return;
		}
		debug("opening browser in url: %s %s", url);		
		openBrowser(url);
	}

	public void goDeployment(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		String routeId = arg1(op, cmds, false);
		routeGo(deployId, routeId, options);
	}
	
	public void routeGo(String deployId, String routeId, Map<String, Object> options) {
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		if (routeId==null) {
			Route route = getPrimaryRoute(deployId, options);
			if (route==null) {
				return;
			}
			routeGo(deployId, route.getUrl(), route);
		} else {
			Route route = devopsClient.getRoute(deployId, routeId, options_);
			routeGo(deployId, routeId, route);
		}
	}

	private Route getPrimaryRoute(String deployId, Map<String, Object> options) {
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		List<Route> routes = devopsClient.listRoutes(deployId, options_);
		Route route = Route.findPrimary(routes, true);
		if (route==null) {
			error("no Route found for: %s", deployId);
			exit(-1);
			return route;
		}
		return route;
	}
	public void routeSchema(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		schema(Route.class);
	}

	public void routeSchemaDeployment(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "schema";
		if (isHelp3(op2)) {
			return;
		}
		schema(Route.class);
	}


	//
	// Deployment Mount
	//
	
	public void mountDeployment(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		switch (op2) {
		case "help": case "":
			printUsage2();
			break;
		case "ls": case "list": {
			mountDeploymentList(cmds, options);
			break;
		}
		case "get": {
			mountDeploymentGet(cmds, options);
			break;
		}
		case "add": case "create": {
			mountDeploymentCreate(cmds, options);
			break;
		}
		case "update": {
			mountDeploymentUpdate(cmds, options);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			mountDeploymentDelete(cmds, options);
			break;
		}
		case "schema": case "meta":
			if (isHelp3(op2)) {
				return;
			}
			schema(Mount.class);
		default:
			invalidOp();
			printUsage2();
			break;
		}
	}
	
	public void mountDeployment0(String[] cmds, Map<String, Object> options) {
		switch (op) {
		case "help": case "":
			printUsage1();
			break;
		case "ls": case "list": {
			mountDeploymentList0(cmds, options);
			break;
		}
		case "get": {
			mountDeploymentGet0(cmds, options);
			break;
		}
		case "add": case "create": {
			mountDeploymentCreate0(cmds, options);
			break;
		}
		case "update": {
			mountDeploymentUpdate0(cmds, options);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			mountDeploymentDelete0(cmds, options);
			break;
		}
		case "schema": case "meta":
			if (isHelp2()) {
				return;
			}
			schema(Mount.class);
		default:
			invalidOp();
			printUsage1();
			break;
		}
	}


	public void mountDeploymentList(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		debug("Deployment Mounts: %s", deployId);		
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		List<Mount> mounts = devopsClient.listMounts(deployId, options_);			
		print(mounts);
	}

	public void mountDeploymentList0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		debug("Deployment Mounts: %s", deployId);		
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		List<Mount> mounts = devopsClient.listMounts(deployId, options_);			
		print(mounts);
	}

	public void mountDeploymentGet(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		String mountId = arg2(op, cmds);
		mountDeploymentGet(deployId, mountId, cmds, options);
	}

	public void mountDeploymentGet0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		String mountId = arg1(op, cmds);
		mountDeploymentGet(deployId, mountId, cmds, options);
	}

	public void mountDeploymentGet(String deployId, String mountId, String[] cmds, Map<String, Object> options) {
		debug("Deployment Mount: %s %s", deployId, mountId);		
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		Mount mount = devopsClient.getMount(deployId, mountId, options_);			
		printObj(mount);
	}
	
	public void mountDeploymentCreate(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		String name = arg2(op, cmds);
		mountDeploymentCreate(deployId, name, cmds, options);
	}

	public void mountDeploymentCreate0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		String name = arg1(op, cmds);
		mountDeploymentCreate(deployId, name, cmds, options);
	}

	public void mountDeploymentCreate(String deployId, String name, String[] cmds, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		Mount mount = makeMount(true, null, options);
		if (mount==null) {
			return;
		}
		mount.setName(name);
		debug("Add Deployment Mount: %s %s %s", deployId, mount, options_);	
		if (isDryrun()) {
			return;
		}
		/*URI uri =*/ devopsClient.addMount(deployId, mount, options_);
		if (isEcho()) {
			//String mountId = extractId(uri);
			mountDeploymentGet(deployId, name, cmds, options);
		}
	}

	private Mount makeMount(boolean create, Boolean stateful, Map<String, Object> options) {
		Mount mount = convert(options, Mount.class);
		
		String mountPath = mount.getMountPath();
		if (create) {
			if (mountPath==null || mountPath.isEmpty()) {
				error("missing mountPath");
				exit(-1);
				return null;
			}			
		}
		if ((String)options.get("secret")!=null) {
			mount.setType(MountType.SECRET);
		}
		if ((String)options.get("configmap")!=null) {
			mount.setType(MountType.CONFIGMAP);
		}
		if (mount.getType()==null) {
			if (Boolean.TRUE.equals(stateful)) {
				mount.setType(MountType.VOLUME_TEMPLATE);
			} else {
				mount.setType(MountType.VOLUME);				
			}
		}
		if (mount.getType()==MountType.SECRET || mount.getType()==MountType.CONFIGMAP) {
			String value = (String)options.get("items");
			if (value!=null) {
				mount.setSecret(value);
				List<KeyPath> items = new ArrayList<>();
				if (!makeMountItems(value, mountPath, items)) {
					error("Invalid mount items: %s", value);
					exit(-1);
					return null;
				}
			}			
		} else if (mount.getType()==MountType.VOLUME || mount.getType()==MountType.VOLUME_TEMPLATE) {
			if (mount.getSize()==null) {
				mount.setSize("1Gi");
			}
		}

		return mount;
	}
	
	public void mountDeploymentUpdate(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		String mountId = arg2(op2, cmds);
		mountDeploymentUpdate(deployId, mountId, cmds, options);
	}

	public void mountDeploymentUpdate0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		String mountId = arg1(op, cmds);
		mountDeploymentUpdate(deployId, mountId, cmds, options);
	}

	public void mountDeploymentUpdate(String deployId, String mountId, String[] cmds, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		Mount mount = makeMount(false, null, options);
		if (mount==null) {
			return;
		}
		debug("Update Deployment Mount: %s %s %s", deployId, mountId, mount);		
		if (isDryrun()) {
			return;
		}
		devopsClient.updateMount(deployId, mountId, mount, options_);
		if (isEcho()) {
			mountDeploymentGet(deployId, mountId, cmds, options);
		}
	}
	
	public void mountDeploymentDelete(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		String mountId = arg2(op2, cmds);
		mountDeploymentDelete(deployId, mountId, cmds, options);
	}
	
	public void mountDeploymentDelete0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		String mountId = arg1(op, cmds);
		mountDeploymentDelete(deployId, mountId, cmds, options);
	}
	
	public void mountDeploymentDelete(String deployId, String mountId, String[] cmds, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		debug("Remove Deployment Mount: %s %s", deployId, mountId);		
		if (isDryrun()) {
			return;
		}
		devopsClient.removeMount(deployId, mountId, options_);
		if (isEcho()) {
			mountDeploymentList(cmds, options);
		}
	}
	
	//
	// Deployment EnvVar
	//
	
	public void envDeployment(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		switch (op2) {
		case "help": case "":
			printUsage2();
			break;
		case "ls": case "list": {
			envDeploymentList(cmds, options);
			break;
		}
		case "get": {
			envDeploymentGet(cmds, options);
			break;
		}
		case "add": case "create": {
			envDeploymentCreate(cmds, options);
			break;
		}
		case "update": {
			envDeploymentUpdate(cmds, options);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			envDeploymentDelete(cmds, options);
			break;
		}
		case "schema": case "meta":
			if (isHelp3(op2)) {
				return;
			}
			schema(Variable.class);
		default:
			invalidOp();
			printUsage2();
			break;
		}
	}

	public void envDeployment0(String[] cmds, Map<String, Object> options) {
		switch (op) {
		case "help": case "":
			printUsage1();
			break;
		case "ls": case "list": {
			envDeploymentList0(cmds, options);
			break;
		}
		case "get": {
			envDeploymentGet0(cmds, options);
			break;
		}
		case "add": case "create": {
			envDeploymentCreate0(cmds, options);
			break;
		}
		case "update": {
			envDeploymentUpdate0(cmds, options);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			envDeploymentDelete0(cmds, options);
			break;
		}
		case "schema": case "meta":
			if (isHelp2()) {
				return;
			}
			schema(Variable.class);
		default:
			invalidOp();
			printUsage1();
			break;
		}
	}

	public void envDeploymentList(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		envDeploymentList(deployId, options);
	}

	public void envDeploymentList0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		envDeploymentList(deployId, options);
	}

	public void envDeploymentList(String deployId, Map<String, Object> options) {
		debug("Deployment Variables: %s", deployId);		
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		List<Variable> vars = devopsClient.listVariables(deployId, options_);			
		print(vars);
	}
	
	
	public void envDeploymentGet(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		String varId = arg2(op, cmds);
		envDeploymentGet(deployId, varId, cmds, options);
	}
	
	public void envDeploymentGet0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		String varId = arg1(op, cmds);
		envDeploymentGet(deployId, varId, cmds, options);
	}
	
	
	public void envDeploymentGet(String deployId, String varId, String[] cmds, Map<String, Object> options) {
		debug("Deployment Variable: %s %s", deployId, varId);		
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		Variable var = devopsClient.getVariable(deployId, varId, options_);			
		printObj(var);
	}
	
	public void envDeploymentCreate(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		String name = arg2(op, cmds);
		String value = arg3(op, cmds, false);
		envDeploymentCreate(deployId, name, value, options);
	}
	
	public void envDeploymentCreate0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		String name = arg1(op, cmds);
		String value = arg2(op, cmds, false);
		envDeploymentCreate(deployId, name, value, options);
	}
	
	public void envDeploymentCreate(String deployId, String name, String value, Map<String, Object> options) {
		Variable var = makeVariable(name, value, options);
		RequestOptions options_ = convert(options, RequestOptions.class);		
		debug("Add Var: %s %s %s", deployId, var, options_);		
		if (isDryrun()) {
			return;
		}
		URI uri = devopsClient.addVariable(deployId, var, options_);
		if (isEcho()) {
			String varId = extractId(uri);
			envDeploymentGet(deployId, varId, cmds, options);
		}
	}
	
	private Variable makeVariable(String name, String value, Map<String, Object> options) {
		Variable var = convert(options, Variable.class);
		if (value!=null) {
			if (var.getCategory()==VarCategory.CONFIGMAP || var.getCategory()==VarCategory.SECRET) {
				var.setPath(value);
			} else {
				var.setValue(value);				
			}
		}
		var.setName(name);
		return var;
	}

	public void envDeploymentUpdate(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		String value = arg3(op, cmds, false);
		String varId = arg2(op2, cmds);
		envDeploymentUpdate(deployId, varId, value, options);
	}
	
	public void envDeploymentUpdate0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		String value = arg1(op, cmds, false);
		String varId = arg2(op, cmds);
		envDeploymentUpdate(deployId, varId, value, options);
	}
	
	public void envDeploymentUpdate(String deployId, String varId, String value, Map<String, Object> options) {
		Variable var = makeVariable(null, value, options);
		RequestOptions options_ = convert(options, RequestOptions.class);
		debug("Update Var: %s %s %s %s", deployId, varId, var, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.updateVariable(deployId, varId, var, options_);
		if (isEcho()) {
			envDeploymentGet(deployId, varId, cmds, options);
		}
	}

	public void envDeploymentDelete(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		String varId = arg2(op2, cmds);
		envDeploymentDelete(deployId, varId, options);
	}
	
	public void envDeploymentDelete0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		String varId = arg1(op, cmds);
		envDeploymentDelete(deployId, varId, options);
	}
	
	public void envDeploymentDelete(String deployId, String varId, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		debug("Remove Var: %s %s %s", deployId, varId, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.removeVariable(deployId, varId, options_);
		if (isEcho()) {
			envDeploymentList(cmds, options);
		}
	}



	//
	// Deployment Binding
	//
	
	public void bindingDeployment(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		switch (op2) {
		case "help": case "":
			printUsage2();
			break;
		case "ls": case "list": {
			bindingDeploymentList(cmds, options);
			break;
		}
		case "get":{
			bindingDeploymentGet(cmds, options);
			break;
		}
		case "add": case "create": {
			bindingDeploymentAdd(cmds, options);
			break;
		}
		case "update": {
			bindingDeploymentUpdate(cmds, options);
			break;
		}
		case "refresh": {
			bindingDeploymentRefresh(cmds, options);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			bindingDeploymentDelete(cmds, options);
			break;
		}
		case "schema": case "meta":
			if (isHelp3(op2)) {
				return;
			}
			schema(Binding.class);
		default:
			invalidOp();
			printUsage2();
			break;
		}
	}

	public void bindingDeployment0(String[] cmds, Map<String, Object> options) {
		switch (op) {
		case "help": case "":
			printUsage1();
			break;
		case "ls": case "list": {
			bindingDeploymentList0(cmds, options);
			break;
		}
		case "get":{
			bindingDeploymentGet0(cmds, options);
			break;
		}
		case "add": case "create": {
			bindingDeploymentAdd0(cmds, options);
			break;
		}
		case "update": {
			bindingDeploymentUpdate0(cmds, options);
			break;
		}
		case "refresh": {
			bindingDeploymentRefresh0(cmds, options);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			bindingDeploymentDelete0(cmds, options);
			break;
		}
		case "schema": case "meta":
			if (isHelp2()) {
				return;
			}
			schema(Binding.class);
		default:
			invalidOp();
			printUsage1();
			break;
		}
	}

	public void bindingDeploymentList(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		bindingDeploymentList(deployId, options);
	}

	public void bindingDeploymentList0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		bindingDeploymentList(deployId, options);
	}

	public void bindingDeploymentList(String deployId, Map<String, Object> options) {
		debug("Deployment Bindings: %s", deployId);		
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		List<Binding> bindings = devopsClient.listBindings(deployId, options_);			
		print(bindings);
	}
	
	public void bindingDeploymentGet(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		String bindingId = arg2(op, cmds);
		bindingDeploymentGet(deployId, bindingId, cmds, options);
	}
	
	public void bindingDeploymentGet0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		String bindingId = arg1(op, cmds);
		bindingDeploymentGet(deployId, bindingId, cmds, options);
	}
	public void bindingDeploymentGet(String deployId, String bindingId, String[] cmds, Map<String, Object> options) {
		debug("Deployment Binding: %s %s", deployId, bindingId);		
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		Binding binding = devopsClient.getBinding(deployId, bindingId, options_);			
		printObj(binding);
	}

	public void bindingDeploymentAdd(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		String selector = arg2(op, cmds, false);
		bindingDeploymentAdd(deployId, selector, options);
	}
	
	public void bindingDeploymentAdd0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		String selector = arg1(op, cmds, false);
		bindingDeploymentAdd(deployId, selector, options);
	}

	public void bindingDeploymentAdd(String deployId, String selector, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		Binding binding = convert(options, Binding.class);
		if (StringUtil.hasText(selector)) {
			binding.setSelector(selector);			
		}
		debug("Add Binding: %s %s %s", deployId, binding, options_);		
		if (isDryrun()) {
			return;
		}
		URI uri = devopsClient.addBinding(deployId, binding, options_);
		if (isEcho()) {
			String bindingId = extractId(uri);
			bindingDeploymentGet(deployId, bindingId, cmds, options);
		}
	}

	public void bindingDeploymentUpdate(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		String bindingId = arg2(op2, cmds);
		bindingDeploymentUpdate(deployId, bindingId, options);
	}
	
	public void bindingDeploymentUpdate0(String[] cmds, Map<String, Object> options) {
		if (isHelp()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		String bindingId = arg1(op, cmds);
		bindingDeploymentUpdate(deployId, bindingId, options);
	}
	
	public void bindingDeploymentUpdate(String deployId, String bindingId, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		Binding binding = convert(options, Binding.class);
		debug("Update Binding: %s %s %s %s", deployId, bindingId, binding, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.updateBinding(deployId, bindingId, binding, options_);
		if (isEcho()) {
			bindingDeploymentGet(deployId, bindingId, cmds, options);
		}
		
	}

	public void bindingDeploymentRefresh(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		String bindingId = arg2(op2, cmds);
		bindingDeploymentRefresh(deployId, bindingId, options);
	}
	
	public void bindingDeploymentRefresh0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		String bindingId = arg1(op, cmds);
		bindingDeploymentRefresh(deployId, bindingId, options);
	}
	
	public void bindingDeploymentRefresh(String deployId, String bindingId, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		debug("Refresh Binding: %s %s", deployId, bindingId);		
		if (isDryrun()) {
			return;
		}
		devopsClient.refreshBinding(deployId, bindingId, options_);
		if (isEcho()) {
			bindingDeploymentGet(deployId, bindingId, cmds, options);
		}
	}

	public void bindingDeploymentDelete(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		String bindingId = arg2(op2, cmds);
		bindingDeploymentDelete(deployId, bindingId, options);
	}
	
	public void bindingDeploymentDelete0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		String bindingId = arg1(op, cmds);
		bindingDeploymentDelete(deployId, bindingId, options);
	}
	
	public void bindingDeploymentDelete(String deployId, String bindingId, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		debug("Remove Binding: %s %s %s", deployId, bindingId, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.removeBinding(deployId, bindingId, options_);
		if (isEcho()) {
			bindingDeploymentList(cmds, options);
		}
	}

	//
	// Deployment Connector
	//

	public void connector(String[] cmds, Map<String, Object> options) {
		switch (op) {
		case "help": case "":
			printUsage1();
			break;
		case "ls": case "list": {
			connectorDeploymentList0(cmds, options);
			break;
		}
		case "get": {
			connectorDeploymentGet0(cmds, options);
			break;
		}
		case "add": case "create": {
			connectorDeploymentAdd0(cmds, options);
			break;
		}
		case "update": {
			connectorDeploymentUpdate0(cmds, options);
			break;
		}
		case "refresh": {
			connectorDeploymentRefresh0(cmds, options);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			connectorDeploymentDelete0(cmds, options);
			break;
		}
		case "schema": case "meta":
			if (isHelp2()) {
				return;
			}
			schema(Connector.class);
		default:
			invalidOp();
			printUsage1();
			break;
		}
	}

	public void connectorDeployment(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		switch (op2) {
		case "help": case "":
			printUsage2();
			break;
		case "ls": case "list": {
			connectorDeploymentList(cmds, options);
			break;
		}
		case "get": {
			connectorDeploymentGet(cmds, options);
			break;
		}
		case "add": case "create": {
			connectorDeploymentAdd(cmds, options);
			break;
		}
		case "update": {
			connectorDeploymentUpdate(cmds, options);
			break;
		}
		case "refresh": {
			connectorDeploymentRefresh(cmds, options);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			connectorDeploymentDelete(cmds, options);
			break;
		}
		case "schema": case "meta":
			if (isHelp3(op2)) {
				return;
			}
			schema(Connector.class);
		default:
			invalidOp();
			printUsage2();
			break;
		}
	}
	
	public void connectorDeploymentList(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		connectorDeploymentList(deployId, options);
	}

	public void connectorDeploymentList0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		connectorDeploymentList(deployId, options);
	}

	public void connectorDeploymentList(String deployId, Map<String, Object> options) {
		debug("Deployment Connectors: %s", deployId);		
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		List<Connector> connectors = devopsClient.listConnectors(deployId, options_);			
		print(connectors);
	}
	
	public void connectorDeploymentGet(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		String connectorId = arg2(op, cmds);
		connectorDeploymentGet(deployId, connectorId, cmds, options);
	}
	
	public void connectorDeploymentGet0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		String connectorId = arg1(op, cmds);
		connectorDeploymentGet(deployId, connectorId, cmds, options);
	}
	
	public void connectorDeploymentGet(String deployId, String connectorId, String[] cmds, Map<String, Object> options) {
		debug("Deployment Connector: %s %s", deployId, connectorId);		
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		Connector connector = devopsClient.getConnector(deployId, connectorId, options_);			
		printObj(connector);
	}
	
	public void connectorDeploymentAdd(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		String name = arg2(op, cmds);
		connectorDeploymentAdd(deployId, name, options);
	}
	
	
	public void connectorDeploymentAdd0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		String name = arg1(op, cmds);
		connectorDeploymentAdd(deployId, name, options);
	}
	
	public void connectorDeploymentAdd(String deployId, String name, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		Connector connector = makeConnector(options);
		if (connector==null) {
			return;
		}
		connector.setName(name);
		debug("Add Connector: %s %s %s", deployId, connector, options_);		
		if (isDryrun()) {
			return;
		}
		URI uri = devopsClient.addConnector(deployId, connector, options_);
		if (isEcho()) {
			String connectorId = extractId(uri);
			connectorDeploymentGet(deployId, connectorId, cmds, options);
		}
	}

	@SuppressWarnings("unchecked")
	private Connector makeConnector(Map<String, Object> options) {
		String spec = (String)options.get("spec");
		Map<String, Object> specMap = null;
		if (StringUtil.hasText(spec)) {
			specMap = MappingUtils.fromJson(spec, Map.class);
			if (specMap==null) {
				error("spec value is not valid JSON");
				exit(-1);
				return null;
			}
		}
		options.remove("spec");
		Connector connector = convert(options, Connector.class);
		connector.setSpec(specMap);
		return connector;
	}
	
	public void connectorDeploymentUpdate(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		String connectorId = arg2(op2, cmds);
		connectorDeploymentUpdate(deployId, connectorId, options);
	}

	public void connectorDeploymentUpdate0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		String connectorId = arg1(op, cmds);
		connectorDeploymentUpdate(deployId, connectorId, options);
	}

	public void connectorDeploymentUpdate(String deployId, String connectorId, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		Connector connector = makeConnector(options);
		if (connector==null) {
			return;
		}
		debug("Update Connector: %s %s %s %s", deployId, connectorId, connector, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.updateConnector(deployId, connectorId, connector, options_);
		if (isEcho()) {
			connectorDeploymentGet(deployId, connectorId, cmds, options);
		}
	}

	public void connectorDeploymentRefresh(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		String connectorId = arg2(op2, cmds);
		connectorDeploymentRefresh(deployId, connectorId, options);
	}

	public void connectorDeploymentRefresh0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		String connectorId = arg1(op, cmds);
		connectorDeploymentRefresh(deployId, connectorId, options);
	}

	public void connectorDeploymentRefresh(String deployId, String connectorId, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		debug("Refresh Connector: %s %s %s", deployId, connectorId, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.refreshConnector(deployId, connectorId, options_);
		if (isEcho()) {
			connectorDeploymentGet(deployId, connectorId, cmds, options);
		}
	}

	public void connectorDeploymentDelete(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String deployId = argIdx1(op, cmds);
		String connectorId = arg2(op2, cmds);
		connectorDeploymentDelete(deployId, connectorId, options);
	}

	public void connectorDeploymentDelete0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String deployId = argIdx(op, cmds);
		String connectorId = arg1(op, cmds);
		connectorDeploymentDelete(deployId, connectorId, options);		
	}

	public void connectorDeploymentDelete(String deployId, String connectorId, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		debug("Remove Connector: %s %s %s", deployId, connectorId, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.removeConnector(deployId, connectorId, options_);
		if (isEcho()) {
			connectorDeploymentList(cmds, options);
		}
	}

	//
	// Jobs
	//
	
	public void job(String[] cmds, Map<String, Object> options) {
		switch (op) {
		case "help": case "":
			printUsage(type);
			break;
		case "ls": case "list": case "ps":
			listJob(cmds, options);
			break;
		case "get": 
			getJob(cmds, options);
			break;
		case "view": 
			viewJob(cmds, options);
			break;
		case "schema": case "meta":
			schemaJob(cmds, options);
			break;
		case "create": case "add": 
			createJob(cmds, extra, options);
			break;
		case "update": 
			updateJob(cmds, extra, options);
			break;
		case "delete": case "del": case "rm": case "remove":
			deleteJob(cmds, options);
			break;
		case "resources": case "resource": case "rscale":
			resourcesJob(cmds, options);
			break;
		case "start":
			startJob(cmds, options);
			break;
		case "stop":
			stopJob(cmds, options);
			break;
		case "restart":
			restartJob(cmds, options);
			break;
		case "sync":
			syncJob(cmds, options);
			break;
		case "attach":
			attachJob(cmds, options);
			break;
		case "exec":
			execJob(cmds, extra, options);
			break;
		case "logs": case "log":
			logJob(cmds, options);
			break;
		case "build":
			buildJob(cmds, options);
			break;
		case "events": case "event":
			eventJob(cmds, options);
			break;
		case "mount":
			mountJob(cmds, options);
			break;
		case "env": case "var": case "vars": 
			envJob(cmds, options);
			break;
		case "binding": case "bind":
			bindingJob(cmds, options);
			break;				
		default: 
			invalidOp(type, op);
			break;
		}		
	}
	
	public void listJob(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		JobFilter filter = convert(options, JobFilter.class);
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingArg("-n");
			return;
		}
		String q = argId(op, cmds, false);
		if (q!=null) {
			filter.setQ(q);
		}
		if (options.get("r")!=null) {
			filter.setStatus(JobStatus.RUNNING);
		}
		debug("Jobs: %s %s %s", spaceId, filter, pageable);
		Page<Job> jobs = devopsClient.listJobs(spaceId, filter, pageable);
		print(jobs);
	}


	public void getJob(String jobId, Map<String, Object> options) {
		JobOptions options_ = convert(options, JobOptions.class);
		Job job = devopsClient.getJob(jobId, options_);
		printObj(job);
	}
	
	public void getJob(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argIdx(op, cmds);
		JobOptions options_ = convert(options, JobOptions.class);
		debug("Get Job: %s", jobId);
		Job job = devopsClient.getJob(jobId, options_);
		printObj(job);
	}

	public void viewJob(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argIdx(op, cmds);
		JobOptions options_ = convert(options, JobOptions.class);
		debug("View Job: %s", jobId);
		Job job = devopsClient.getJob(jobId, options_);
		view("job", job.getUuid());
	}

	public void schemaJob(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		schema(Job.class, JobFilter.class, JobOptions.class, options);
	}
	
	public void createJob(String[] cmds, String[] extra, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String start = (String)options.get("--start");
		String name = argId(op, cmds);
		String image = arg1(op, cmds, false);
		createJob(name, image, start!=null, extra, options);
	}

	public void createJob(String name, String image, boolean start, String[] extra, Map<String, Object> options) {
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingArg("-n");
			return;
		}
		Job job = makeJob(name, image, extra, options);
		if (start) {
			job.setStart(true);
		}
		JobOptions options_ = convert(options, JobOptions.class);
		debug("Creating Job: %s %s", job, options_);
		if (isDryrun()) {
			return;
		}
		URI uri = devopsClient.createJob(spaceId, job, options_);
		if (isEcho()) {
			debug("Job URI: %s", uri);
			String jobId = extractId(uri);
			Job job2 = devopsClient.getJob(jobId, null);
			printObj(job2);			
		}
	}

	private Job makeJob(String name, String image, String[] extra, Map<String, Object> options) {
		setupDeployOptions(image, options);
		Job job = convert(options, Job.class);
		if (name!=null) {
			job.setName(name);
		}
		if (processDeployableOptions(job, extra, options)==null) {
			return null;
		}
		Integer completions = parseInt((String)options.get("k"));
		if (completions!=null) {
			job.setCompletions(completions);
		}
		Integer parallelism = parseInt((String)options.get("p"));
		if (parallelism!=null) {
			job.setParallelism(parallelism);
		}
		List<Variable> env = new ArrayList<>();
		if (!makeEnv(options, env)) {
			return null;
		}
		if (!env.isEmpty()) {
			job.setEnv(env);
		}
		List<Mount> mount = new ArrayList<>();
		if (!makeMounts(options, mount)) {
			return null;
		}
		if (!mount.isEmpty()) {
			job.setMounts(mount);
		}
		return job;
	}
	
	public void updateJob(String[] cmds, String[] extra, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argIdx(op, cmds);
		String image = arg1(op, cmds, false);
		Job job = makeJob(jobId, image, extra, options);
		JobOptions options_ = convert(options, JobOptions.class);
		debug("Updating Job: %s %s %s", jobId, job, options_);
		if (isDryrun()) {
			return;
		}
		devopsClient.updateJob(jobId, job, options_);
		if (isEcho()) {
			Job job2 = devopsClient.getJob(jobId, null);
			printObj(job2);
		}
	}
	
	public void killJob(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		if (options.get("f")!=null || options.get("rm")!=null || options.get("del")!=null) {
			deleteJob(cmds, options);
		} else {
			stopJob(cmds, options);
		}
	}

	public void killJob(String jobId, Map<String, Object> options) {
		if (options.get("f")!=null || options.get("rm")!=null || options.get("del")!=null) {
			deleteJob(jobId, options);
		} else {
			stopJob(jobId, options);
		}
	}

	public void deleteJob(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argIdx(op, cmds);
		String[] ids = jobId.split(",");
		for (String id: ids) {
			if (id.isEmpty()) {
				continue;
			}
			deleteJob(id, options);			
		}
		if (isEcho()) {
			listJob(cmds, options);
		}
	}
	
	public void deleteJob(String jobId, Map<String, Object> options) {
		jobId = makeIdx(jobId);
		debug("Deleting Job: %s", jobId);	
		JobOptions options_ = convert(options, JobOptions.class);
		if (isDryrun()) {
			return;
		}
		devopsClient.deleteJob(jobId, options_);	
	}

	public void resourcesJob(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argIdx(op, cmds);
		Resources resources = makeResources(options);
		debug("Scaling Job resources: %s %s", jobId, resources);		
		JobOptions options_ = convert(options, JobOptions.class);
		if (isDryrun()) {
			return;
		}
		devopsClient.scaleJob(jobId, resources, options_);
		if (isEcho()) {
			getJob(jobId, options);
		}
	}

	public void startJob(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argIdx(op, cmds);
		JobOptions options_ = convert(options, JobOptions.class);
		debug("Starting Job: %s %s", jobId, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.startJob(jobId, options_);		
		if (isEcho()) {
			getJob(jobId, options);
		}
	}

	public void stopJob(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argId(op, cmds);
		String[] ids = jobId.split(",");
		for (String id: ids) {
			if (id.isEmpty()) {
				continue;
			}
			stopJob(makeIdx(id), options);			
		}
	}
	
	public void stopJob(String jobId, Map<String, Object> options) {	
		JobOptions options_ = convert(options, JobOptions.class);
		debug("Stopping Job: %s %s", jobId, options_);
		if (isDryrun()) {
			return;
		}
		devopsClient.stopJob(jobId, options_);		
		if (isEcho()) {
			getJob(jobId, options);
		}
	}

	public void restartJob(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argIdx(op, cmds);
		JobOptions options_ = convert(options, JobOptions.class);
		debug("Restarting Job: %s %s", jobId, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.restartJob(jobId, options_);			
		if (isEcho()) {
			getJob(jobId, options);
		}
	}
	
	public void syncJob(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argIdx(op, cmds);
		JobOptions options_ = convert(options, JobOptions.class);
		debug("Sync Job: %s %s", jobId, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.syncJob(jobId, options_);	
		if (isEcho()) {
			getJob(jobId, options);
		}
	}

	public void attachJob(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argId(op, cmds);
		String spaceId = argNS(options);
		JobOptions options_ = convert(options, JobOptions.class);
		debug("Attach Job: %s %s %s", spaceId, jobId, options_);	
		if (isDryrun()) {
			return;
		}
		URI uri = devopsClient.attachJob(spaceId, jobId, options_);			
		if (isEcho()) {
			String id = extractId(uri);
			debug("Job URI: %s", uri);		
			getJob(id, options);
		}
	}
	
	public void execJob(String[] cmds, String[] extra, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argIdx(op, cmds);
		ExecOptions options_ = makeExecOptions(cmds, extra, options);
		debug("Exec Job: %s %s", jobId, options_);		
		if (isDryrun()) {
			return;
		}
		//String out = devopsClient.execJob(jobId, options_);			
		//System.out.println(out);
	}
	
	public void logJob(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argIdx(op, cmds);
		String pod = arg2(op, cmds, false);
		LogOptions options_ = makeLogOptions(options);
		if (pod!=null && !pod.isEmpty()) {
			options_.setPod(pod);
		}
		debug("Log Job: %s %s", jobId, options_);
		if (isDryrun()) {
			return;
		}
		String out = devopsClient.logJob(jobId, options_);			
		System.out.println(out);
	}
	
	public void buildJob(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argIdx(op, cmds);
		buildJob(jobId, options);
	}

	public void buildJob0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argIdx(op, cmds);
		buildJob(jobId, options);
	}

	public void buildJob(String jobId, Map<String, Object> options) {
		BuildOptions options_ = makeBuildOptions(options);
		debug("Build Job: %s %s", jobId, options_);		
		if (isDryrun()) {
			return;
		}
		String spaceId = argNS(options, true);
		if (spaceId==null) {
			return;
		}		
		URI uri = devopsClient.buildJob(jobId, options_);
		if (isEcho()) {
			debug("Build URI: %s", uri);
			String id = extractId(uri);
			Build build = devopsClient.getBuild(spaceId, id, options_);
			printObj(build);			
		}
	}

	//
	// Job Events
	//
	
	public void eventJob(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		switch (op2) {
		case "help": case "":
			printUsage2();
			break;
		case "ls": case "list": {
			eventJobList(cmds, options);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			eventJobDelete(cmds, options);
			break;
		}
		case "schema": case "meta":
			if (isHelp3(op2)) {
				return;
			}
			schema(Event.class);
		default:
			invalidOp();
			printUsage2();
			break;
		}
	}



	public void eventJobList(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String jobId = argIdx1(op, cmds);
		eventJobList(jobId, options);
	}

	public void eventJobList(String jobId, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		EventFilter filter = convert(options, EventFilter.class);
		String c = (String)options.get("c");
		if (c!=null) {
			filter.setCluster(true);
		}
		PageOptions options_ = convert(options, PageOptions.class);
		debug("Job Events: %s %s %s", jobId,filter, options_);		
		Page<Event> events = devopsClient.listEvents(jobId, filter, options_.toPageRequest());			
		print(events);
	}


	public void eventJobDelete(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String jobId = argIdx1(op, cmds);
		RequestOptions options_ = convert(options, RequestOptions.class);
		String eventId = arg2(op2, cmds);
		debug("Remove Job Event: %s %s %s", jobId, eventId, options_);		
		if (isDryrun()) {
			return;
		}
		//devopsClient.removeEvent(jobId, eventId, options_);
		if (isEcho()) {
			eventJobList(cmds, options);
		}
	}


	//
	// Job Instances/Replicas/Pods
	//

	
	public void podJobList(String jobId, Map<String, Object> options) {
		debug("Pods for Job: %s", jobId);		
		PodFilter filter = convert(options, PodFilter.class);
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		List<Pod> instances = devopsClient.listPodsForJob(jobId, filter, pageable);			
		print(instances);
	}
	

	public void podJobKill(String jobId, String pod, String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		RequestOptions options_ = convert(options, RequestOptions.class);
		debug("Delete Pod: %s %s %s", jobId, pod, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.deletePodForJob(jobId, pod, options_);
		if (isEcho()) {
			podJobList(jobId, options);
		}
	}


	//
	// Job Mount
	//
	
	public void mountJob(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		switch (op2) {
		case "help": case "":
			printUsage2();
			break;
		case "ls": case "list": {
			mountJobList(cmds, options);
			break;
		}
		case "get":{
			mountJobGet(cmds, options);
			break;
		}
		case "add": case "create": {
			mountJobCreate(cmds, options);
			break;
		}
		case "update": {
			mountJobUpdate(cmds, options);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			mountJobDelete(cmds, options);
			break;
		}
		case "schema": case "meta":
			if (isHelp3(op2)) {
				return;
			}
			schema(Mount.class);
		default:
			invalidOp();
			printUsage2();
			break;
		}
	}

	public void mountJob0(String[] cmds, Map<String, Object> options) {
		switch (op) {
		case "help": case "":
			printUsage1();
			break;
		case "ls": case "list": {
			mountJobList0(cmds, options);
			break;
		}
		case "get":{
			mountJobGet0(cmds, options);
			break;
		}
		case "add": case "create": {
			mountJobCreate0(cmds, options);
			break;
		}
		case "update": {
			mountJobUpdate0(cmds, options);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			mountJobDelete0(cmds, options);
			break;
		}
		case "schema": case "meta":
			if (isHelp2()) {
				return;
			}
			schema(Mount.class);
		default:
			invalidOp();
			printUsage1();
			break;
		}
	}

	public void mountJobList(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String jobId = argIdx1(op, cmds);
		mountJobList(jobId, options);
	}

	public void mountJobList0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argIdx(op, cmds);
		mountJobList(jobId, options);
	}

	public void mountJobList(String jobId, Map<String, Object> options) {
		JobOptions options_ = convert(options, JobOptions.class);
		debug("Job Mounts: %s %s", jobId, options_);		
		if (isDryrun()) {
			return;
		}
		List<Mount> mounts = devopsClient.listMountsJob(jobId, options_);			
		print(mounts);
	}
	
	public void mountJobGet(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String jobId = argIdx1(op, cmds);
		String mountId = arg2(op, cmds);
		mountJobGet(jobId, mountId, cmds, options);
	}
	
	public void mountJobGet0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argIdx(op, cmds);
		String mountId = arg1(op, cmds);
		mountJobGet(jobId, mountId, cmds, options);
	}
	
	public void mountJobGet(String jobId, String mountId, String[] cmds, Map<String, Object> options) {
		JobOptions options_ = convert(options, JobOptions.class);
		debug("Job Mount: %s %s %s", jobId, mountId, options_);		
		Mount mount = devopsClient.getMountJob(jobId, mountId, options_);			
		printObj(mount);
	}

	public void mountJobCreate(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String jobId = argIdx1(op, cmds);
		String name = arg2(op, cmds);
		mountJobCreate(jobId, name, cmds, options);
	}
	
	public void mountJobCreate0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argIdx(op, cmds);
		String name = arg1(op, cmds);
		mountJobCreate(jobId, name, cmds, options);
	}
	
	public void mountJobCreate(String jobId, String name, String[] cmds, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		Mount mount = makeMount(true, null, options);
		if (mount==null) {
			return;
		}
		mount.setName(name);
		debug("Add Job Mount: %s %s %s", jobId, mount, options_);		
		if (isDryrun()) {
			return;
		}
		/*URI uri =*/ devopsClient.addMount(jobId, mount, options_);
		if (isEcho()) {
			//String mountId = extractId(uri);
			mountJobGet(jobId, name, cmds, options);
		}
	}

	public void mountJobUpdate(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String jobId = argIdx1(op, cmds);
		String mountId = arg2(op2, cmds);
		mountJobUpdate(jobId, mountId, cmds, options);
	}

	public void mountJobUpdate0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argIdx(op, cmds);
		String mountId = arg1(op, cmds);
		mountJobUpdate(jobId, mountId, cmds, options);
	}

	public void mountJobUpdate(String jobId, String mountId, String[] cmds, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		Mount mount = makeMount(false, null, options);
		if (mount==null) {
			return;
		}
		debug("Update Job Mount: %s %s %s", jobId, mountId, mount, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.updateMount(jobId, mountId, mount, options_);
		if (isEcho()) {
			mountJobGet(jobId, mountId, cmds, options);
		}
	}
	
	public void mountJobDelete(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String jobId = argIdx1(op, cmds);
		String mountId = arg2(op2, cmds);
		mountJobDelete(jobId, mountId, cmds, options);
	}

	public void mountJobDelete0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argIdx(op, cmds);
		String mountId = arg1(op, cmds);
		mountJobDelete(jobId, mountId, cmds, options);
	}

	public void mountJobDelete(String jobId, String mountId, String[] cmds, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		debug("Remove Job Mount: %s %s %s", jobId, mountId, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.removeMount(jobId, mountId, options_);
		if (isEcho()) {
			mountJobList(cmds, options);
		}
	}
	
	//
	// Job Env Vars
	//
	
	public void envJob(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		switch (op2) {
		case "help": case "":
			printUsage2();
			break;
		case "ls": case "list": {
			envJobList(cmds, options);
			break;
		}
		case "get": {
			envJobGet(cmds, options);
			break;
		}
		case "add": case "create": {
			envJobCreate(cmds, options);
			break;
		}
		case "update": {
			envJobUpdate(cmds, options);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			envJobDelete(cmds, options);
			break;
		}
		case "schema": case "meta":
			if (isHelp3(op2)) {
				return;
			}
			schema(Variable.class);
		default:
			invalidOp();
			printUsage1();
			break;
		}
	}

	public void envJob0(String[] cmds, Map<String, Object> options) {
		switch (op) {
		case "help": case "":
			printUsage1();
			break;
		case "ls": case "list": {
			envJobList0(cmds, options);
			break;
		}
		case "get": {
			envJobGet0(cmds, options);
			break;
		}
		case "add": case "create": {
			envJobCreate0(cmds, options);
			break;
		}
		case "update": {
			envJobUpdate0(cmds, options);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			envJobDelete0(cmds, options);
			break;
		}
		case "schema": case "meta":
			if (isHelp2()) {
				return;
			}
			schema(Variable.class);
		default:
			invalidOp();
			printUsage2();
			break;
		}
	}

	public void envJobList(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String jobId = argIdx1(op, cmds);
		envJobList(jobId, options);
	}

	public void envJobList0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argIdx(op, cmds);
		envJobList(jobId, options);
	}

	public void envJobList(String jobId, Map<String, Object> options) {
		debug("Job Variables: %s", jobId);		
		JobOptions options_ = convert(options, JobOptions.class);
		List<Variable> vars = devopsClient.listVariablesJob(jobId, options_);			
		print(vars);
	}
	
	public void envJobGet(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String jobId = argIdx1(op, cmds);
		String varId = arg2(op, cmds);
		envJobGet(jobId, varId, cmds, options);
	}

	public void envJobGet0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argIdx(op, cmds);
		String varId = arg1(op, cmds);
		envJobGet(jobId, varId, cmds, options);
	}

	public void envJobGet(String jobId, String varId, String[] cmds, Map<String, Object> options) {
		debug("Job Var: %s %s", jobId, varId);		
		JobOptions options_ = convert(options, JobOptions.class);
		Variable var = devopsClient.getVariableJob(jobId, varId, options_);			
		printObj(var);
	}
	
	public void envJobCreate(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String jobId = argIdx1(op, cmds);
		String name = arg2(op, cmds);
		String value = arg3(op, cmds, false);
		envJobCreate(jobId, name, value, options);
	}

	public void envJobCreate0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argIdx(op, cmds);
		String name = arg1(op, cmds);
		String value = arg2(op, cmds, false);
		envJobCreate(jobId, name, value, options);
	}

	public void envJobCreate(String jobId, String name, String value, Map<String, Object> options) {
		Variable var = makeVariable(name, value, options);
		RequestOptions options_ = convert(options, RequestOptions.class);		
		debug("Add Var: %s %s %s", jobId, var, options_);		
		if (isDryrun()) {
			return;
		}
		URI uri = devopsClient.addVariable(jobId, var, options_);
		if (isEcho()) {
			String varId = extractId(uri);
			envJobGet(jobId, varId, cmds, options);
		}
	}
	
	public void envJobUpdate(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String jobId = argIdx1(op, cmds);
		String varId = arg2(op2, cmds);
		String value = arg3(op, cmds, false);
		envJobUpdate(jobId, varId, value);
	}
	
	public void envJobUpdate0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argIdx(op, cmds);
		String varId = arg1(op, cmds);
		String value = arg2(op, cmds, false);
		envJobUpdate(jobId, varId, value);
	}
	
	public void envJobUpdate(String jobId, String varId, String value) {
		Variable var = makeVariable(null, value, options);
		RequestOptions options_ = convert(options, RequestOptions.class);
		debug("Update Var: %s %s %s %s", jobId, varId, var, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.updateVariable(jobId, varId, var, options_);
		if (isEcho()) {
			envJobGet(jobId, varId, cmds, options);
		}
	}

	public void envJobDelete(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String jobId = argIdx1(op, cmds);
		String varId = arg2(op2, cmds);
		envJobDelete(jobId, varId, options);
	}
	
	public void envJobDelete0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argIdx(op, cmds);
		String varId = arg1(op, cmds);
		envJobDelete(jobId, varId, options);
	}
	
	public void envJobDelete(String jobId, String varId, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		debug("Remove Var: %s %s %s", jobId, varId, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.removeVariable(jobId, varId, options_);
		if (isEcho()) {
			envJobList(cmds, options);
		}
	}

	//
	// Job Binding
	//
	
	public void bindingJob(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		switch (op2) {
		case "help": case "":
			printUsage2();
			break;
		case "ls": case "list": {
			bindingJobList(cmds, options);
			break;
		}
		case "add": case "create": {
			bindingJobAdd(cmds, options);
			break;
		}
		case "update": {
			bindingJobUpdate(cmds, options);
			break;
		}
		case "refresh": {
			bindingJobRefresh(cmds, options);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			bindingJobDelete(cmds, options);
			break;
		}
		case "schema": case "meta":
			if (isHelp3(op2)) {
				return;
			}
			schema(Binding.class);
		default:
			invalidOp();
			printUsage2();
			break;
		}
	}

	public void bindingJob0(String[] cmds, Map<String, Object> options) {
		switch (op) {
		case "help": case "":
			printUsage1();
			break;
		case "ls": case "list": {
			bindingJobList0(cmds, options);
			break;
		}
		case "add": case "create": {
			bindingJobAdd0(cmds, options);
			break;
		}
		case "update": {
			bindingJobUpdate0(cmds, options);
			break;
		}
		case "refresh": {
			bindingJobRefresh0(cmds, options);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			bindingJobDelete0(cmds, options);
			break;
		}
		case "schema": case "meta":
			if (isHelp2()) {
				return;
			}
			schema(Binding.class);
		default:
			invalidOp();
			printUsage1();
			break;
		}
	}

	public void bindingJobList(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String jobId = argIdx1(op, cmds);
		bindingJobList(jobId, options);
	}

	public void bindingJobList0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argIdx(op, cmds);
		bindingJobList(jobId, options);
	}

	public void bindingJobList(String jobId, Map<String, Object> options) {
		debug("Job Bindings: %s", jobId);		
		JobOptions options_ = convert(options, JobOptions.class);
		List<Binding> bindings = devopsClient.listBindingsJob(jobId, options_);			
		print(bindings);
	}
	
	public void bindingJobGet(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String jobId = argIdx1(op, cmds);
		String bindingId = arg2(op, cmds);
		bindingJobGet(jobId, bindingId, cmds, options);
	}

	public void bindingJobGet0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argIdx(op, cmds);
		String bindingId = arg1(op, cmds);
		bindingJobGet(jobId, bindingId, cmds, options);
	}

	public void bindingJobGet(String jobId, String bindingId, String[] cmds, Map<String, Object> options) {
		debug("Job Binding: %s %s", jobId, bindingId);		
		JobOptions options_ = convert(options, JobOptions.class);
		Binding binding = devopsClient.getBindingJob(jobId, bindingId, options_);			
		printObj(binding);
	}

	public void bindingJobAdd(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String jobId = argIdx1(op, cmds);
		String selector = arg2(op, cmds, false);
		bindingJobAdd(jobId, selector, options);
	}

	public void bindingJobAdd0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argIdx(op, cmds);
		String selector = arg1(op, cmds, false);
		bindingJobAdd(jobId, selector, options);
	}

	public void bindingJobAdd(String jobId, String selector, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		Binding binding = convert(options, Binding.class);
		if (StringUtil.hasText(selector)) {
			binding.setSelector(selector);			
		}
		debug("Add Binding: %s %s %s", jobId, binding, options_);		
		if (isDryrun()) {
			return;
		}
		URI uri = devopsClient.addBinding(jobId, binding, options_);
		if (isEcho()) {
			String bindingId = extractId(uri);
			bindingJobGet(jobId, bindingId, cmds, options);
		}
	}

	public void bindingJobUpdate(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String jobId = argIdx1(op, cmds);
		String bindingId = arg2(op2, cmds);
		bindingJobUpdate(jobId, bindingId, options);
	}
	
	public void bindingJobUpdate0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argIdx(op, cmds);
		String bindingId = arg1(op, cmds);
		bindingJobUpdate(jobId, bindingId, options);
	}
	
	public void bindingJobUpdate(String jobId, String bindingId, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		Binding binding = convert(options, Binding.class);
		debug("Update Binding: %s %s %s", jobId, bindingId, binding);		
		if (isDryrun()) {
			return;
		}
		devopsClient.updateBinding(jobId, bindingId, binding, options_);
		if (isEcho()) {
			bindingJobGet(jobId, bindingId, cmds, options);
		}		
	}

	public void bindingJobRefresh(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String jobId = argIdx1(op, cmds);
		String bindingId = arg2(op2, cmds);
		bindingJobRefresh(jobId, bindingId, options);
	}
	
	public void bindingJobRefresh0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argIdx(op, cmds);
		String bindingId = arg1(op, cmds);
		bindingJobRefresh(jobId, bindingId, options);
	}
	
	public void bindingJobRefresh(String jobId, String bindingId, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		debug("Refresh Binding: %s %s %s", jobId, bindingId, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.refreshBinding(jobId, bindingId, options_);
		if (isEcho()) {
			bindingJobGet(jobId, bindingId, cmds, options);
		}
	}

	public void bindingJobDelete(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String jobId = argIdx1(op, cmds);
		String bindingId = arg2(op2, cmds);
		bindingJobDelete(jobId, bindingId, options);
	}
	
	public void bindingJobDelete0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String jobId = argIdx(op, cmds);
		String bindingId = arg1(op, cmds);
		bindingJobDelete(jobId, bindingId, options);
	}
	
	public void bindingJobDelete(String jobId, String bindingId, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		debug("Remove Binding: %s %s %s", jobId, bindingId, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.removeBinding(jobId, bindingId, options_);
		if (isEcho()) {
			bindingJobList(cmds, options);
		}
	}

	//
	// CronJob
	//
	
	public void cronjob(String[] cmds, Map<String, Object> options) {
		switch (op) {
		case "help": case "":
			printUsage(type);
			break;
		case "ls": case "list": case "ps":
			listCronJob(cmds, options);
			break;
		case "get": 
			getCronJob(cmds, options);
			break;
		case "view": 
			viewCronJob(cmds, options);
			break;
		case "schema": case "meta":
			schemaCronJob(cmds, options);
			break;
		case "create": case "add": 
			createCronJob(cmds, extra, options);
			break;
		case "update": 
			updateCronJob(cmds, extra, options);
			break;
		case "delete": case "del": case "rm": case "remove":
			deleteCronJob(cmds, options);
			break;
		case "jobs": case "job":
			jobsCronJob(cmds, options);
			break;
		case "resources": case "resource": case "rscale":
			resourcesCronJob(cmds, options);
			break;
		case "start":
			startCronJob(cmds, options);
			break;
		case "stop":
			stopCronJob(cmds, options);
			break;
		case "suspend":
			suspendCronJob(cmds, options);
			break;
		case "restart":
			restartCronJob(cmds, options);
			break;
		case "sync":
			syncCronJob(cmds, options);
			break;
		case "attach":
			attachCronJob(cmds, options);
			break;
		case "build":
			buildCronJob(cmds, options);
			break;
		case "events": case "event":
			eventCronJob(cmds, options);
			break;
		case "mount":
			mountCronJob(cmds, options);
			break;
		case "env": case "var": case "vars":
			envCronJob(cmds, options);
			break;
		case "binding": case "bind":
			bindingCronJob(cmds, options);
			break;
		default: 
			invalidOp(type, op);
			break;
		}
	}

	public void listCronJob(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		CronJobFilter filter = convert(options, CronJobFilter.class);
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingArg("-n");
			return;
		}
		String q = argId(op, cmds, false);
		if (q!=null) {
			filter.setQ(q);
		}
		if (options.get("r")!=null) {		
			filter.setAnyStatus(new CronJobStatus[]{CronJobStatus.ACTIVE, CronJobStatus.SCHEDULED});
		}
		debug("CronJobs: %s %s %s", spaceId, filter, pageable);
		Page<CronJob> cronjobs = devopsClient.listCronJobs(spaceId, filter, pageable);
		print(cronjobs, CronJob.class);
	}

	public void getCronJob(String cronjobId, Map<String, Object> options) {
		CronJobOptions options_ = convert(options, CronJobOptions.class);
		CronJob cronjob = devopsClient.getCronJob(cronjobId, options_);
		printObj(cronjob);
	}

	public void getCronJob(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		CronJobOptions options_ = convert(options, CronJobOptions.class);
		debug("CronJob: %s", cronjobId);
		CronJob cronjob = devopsClient.getCronJob(cronjobId, options_);
		printObj(cronjob);
	}

	public void viewCronJob(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		CronJobOptions options_ = convert(options, CronJobOptions.class);
		debug("View CronJob: %s", cronjobId);
		CronJob cronjob = devopsClient.getCronJob(cronjobId, options_);
		view("cronjob", cronjob.getUuid());
	}

	public void schemaCronJob(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		schema(CronJob.class, CronJobFilter.class, CronJobOptions.class, options);
	}
	
	public void createCronJob(String[] cmds, String[] extra, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String start = (String)options.get("--start");
		String cronjobId = argIdx(op, cmds);
		String image = arg1(op, cmds, false);
		createCronJob(cronjobId, image, start!=null, extra, options);
	}
		
	public void createCronJob(String name, String image, boolean start, String[] extra, Map<String, Object> options) {
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingArg("-n");
			return;
		}
		CronJob cronjob = makeCronJob(name, image, extra, options);
		if (start) {
			cronjob.setStart(true);
		}
		CronJobOptions options_ = convert(options, CronJobOptions.class);
		debug("Creating CronJob: %s %s", cronjob, options_);
		if (isDryrun()) {
			return;
		}
		URI uri = devopsClient.createCronJob(spaceId, cronjob, options_);
		if (isEcho()) {
			debug("CronJob URI: %s", uri);
			String cronjobId = extractId(uri);
			CronJob cronjob2 = devopsClient.getCronJob(cronjobId, null);
			printObj(cronjob2);			
		}
	}

	private CronJob makeCronJob(String name, String image, String[] extra, Map<String, Object> options) {
		setupDeployOptions(image, options);
		CronJob cronjob = convert(options, CronJob.class);
		if (name!=null) {
			cronjob.setName(name);
		}
		String schedule = (String)options.get("schedule");
		if (schedule==null || schedule.isEmpty()) {
			schedule = (String)options.get("c");
		}
		if (schedule==null || schedule.isEmpty()) {
			error("missing schedule");
			exit(-1);
			return null;
		}
		if (processDeployableOptions(cronjob, extra, options)==null) {
			return null;
		}
		List<Variable> env = new ArrayList<>();
		if (!makeEnv(options, env)) {
			return null;
		}
		if (!env.isEmpty()) {
			cronjob.setEnv(env);
		}
		List<Mount> mount = new ArrayList<>();
		if (!makeMounts(options, mount)) {
			return null;
		}
		if (!mount.isEmpty()) {
			cronjob.setMounts(mount);
		}
		return cronjob;
	}

	public void updateCronJob(String[] cmds, String[] extra, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		String image = arg1(op, cmds, false);
		CronJob cronjob = makeCronJob(null, image, extra, options);
		CronJobOptions options_ = convert(options, CronJobOptions.class);
		debug("Updating CronJob: %s %s %s", cronjobId, cronjob, options_);
		if (isDryrun()) {
			return;
		}
		devopsClient.updateCronJob(cronjobId, cronjob, options_);
		if (isEcho()) {
			CronJob cronjob2 = devopsClient.getCronJob(cronjobId, null);
			printObj(cronjob2);
		}
	}
	
	public void killCronJob(String cronjobId, Map<String, Object> options) {
		if (options.get("f")!=null || options.get("rm")!=null || options.get("del")!=null) {
			deleteCronJob(cronjobId, options);
		} else {
			stopCronJob(cronjobId, options);
		}
	}

	
	public void killCronJob(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		if (options.get("f")!=null || options.get("rm")!=null || options.get("del")!=null) {
			deleteCronJob(cmds, options);
		} else {
			stopCronJob(cmds, options);
		}
	}
	
	public void deleteCronJob(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		String[] ids = cronjobId.split(",");
		for (String id: ids) {
			if (id.isEmpty()) {
				continue;
			}
			deleteCronJob(id, options);
		}
		if (isEcho()) {
			listCronJob(cmds, options);
		}
	}
	
	public void deleteCronJob(String cronjobId, Map<String, Object> options) {
		cronjobId = makeIdx(cronjobId);
		CronJobOptions options_ = convert(options, CronJobOptions.class);
		debug("Deleting CronJob: %s %s", cronjobId, options_);
		if (isDryrun()) {
			return;
		}
		devopsClient.deleteCronJob(cronjobId, options_);	
	}

	public void resourcesCronJob(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		Resources resources = makeResources(options);
		CronJobOptions options_ = convert(options, CronJobOptions.class);
		debug("Scaling CronJob resources: %s %s", cronjobId, resources, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.scaleCronJob(cronjobId, resources, options_);			
	}

	public void startCronJob(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		CronJobOptions options_ = convert(options, CronJobOptions.class);
		debug("Starting CronJob: %s %s", cronjobId, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.startCronJob(cronjobId, options_);		
		if (isEcho()) {
			getCronJob(cronjobId, options);
		}
	}

	public void stopCronJob(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String cronjobId = argId(op, cmds);
		String[] ids = cronjobId.split(",");
		for (String id: ids) {
			if (id.isEmpty()) {
				continue;
			}
			stopCronJob(makeIdx(id), options);			
		}
	}
	
	public void stopCronJob(String cronjobId, Map<String, Object> options) {
		CronJobOptions options_ = convert(options, CronJobOptions.class);
		debug("Stopping CronJob: %s %s", cronjobId, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.stopCronJob(cronjobId, options_);			
		if (isEcho()) {
			getCronJob(cronjobId, options);
		}
	}

	public void suspendCronJob(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		CronJobOptions options_ = convert(options, CronJobOptions.class);
		debug("Suspending CronJob: %s", cronjobId, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.suspendCronJob(cronjobId, options_);	
		if (isEcho()) {
			getCronJob(cronjobId, options);
		}
	}

	public void restartCronJob(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		CronJobOptions options_ = convert(options, CronJobOptions.class);
		debug("Restarting CronJob: %s %s", cronjobId, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.restartCronJob(cronjobId, options_);		
		if (isEcho()) {
			getCronJob(cronjobId, options);
		}
	}
	
	public void syncCronJob(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		CronJobOptions options_ = convert(options, CronJobOptions.class);
		debug("Sync CronJob: %s %s", cronjobId, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.syncCronJob(cronjobId, options_);	
		if (isEcho()) {
			getCronJob(cronjobId, options);
		}
	}

	public void attachCronJob(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String cronjobId = argId(op, cmds);
		String spaceId = argNS(options);
		CronJobOptions options_ = convert(options, CronJobOptions.class);
		debug("Attach CronJob: %s %s %s", spaceId, cronjobId, options_);		
		if (isDryrun()) {
			return;
		}
		URI uri = devopsClient.attachCronJob(spaceId, cronjobId, options_);			
		if (isEcho()) {
			String id = extractId(uri);
			debug("CronJob URI: %s", uri);		
			getCronJob(id, options);
		}
	}
	
	public void buildCronJob(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		buildCronJob(cronjobId, options);
	}

	public void buildCronJob0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		buildCronJob(cronjobId, options);
	}

	public void buildCronJob(String cronjobId, Map<String, Object> options) {
		BuildOptions options_ = makeBuildOptions(options);
		debug("Build CronJob: %s %s", cronjobId, options_);		
		if (isDryrun()) {
			return;
		}
		String spaceId = argNS(options, true);
		if (spaceId==null) {
			return;
		}		
		URI uri = devopsClient.buildCronJob(cronjobId, options_);
		if (isEcho()) {
			debug("Build URI: %s", uri);
			String id = extractId(uri);
			Build build = devopsClient.getBuild(spaceId, id, options_);
			printObj(build);			
		}
	}

	
	//
	// CronJob Events
	//
	
	public void eventCronJob(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		switch (op2) {
		case "help": case "":
			printUsage2();
			break;
		case "ls": case "list": {
			eventCronJobList(cmds, options);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			eventCronJobDelete(cmds, options);
			break;
		}
		case "schema": case "meta":
			if (isHelp3(op2)) {
				return;
			}
			schema(Event.class);
		default:
			invalidOp();
			printUsage2();
			break;
		}
	}


	public void eventCronJobList(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String cronjobId = argIdx1(op, cmds);
		eventCronJobList(cronjobId, options);
	}

	public void eventCronJobList(String cronjobId, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		EventFilter filter = convert(options, EventFilter.class);
		String c = (String)options.get("c");
		if (c!=null) {
			filter.setCluster(true);
		}
		PageOptions options_ = convert(options, PageOptions.class);
		debug("CronJob Events: %s %s %s", cronjobId,filter, options_);		
		Page<Event> events = devopsClient.listEvents(cronjobId, filter, options_.toPageRequest());			
		print(events);
	}


	public void eventCronJobDelete(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String cronjobId = argIdx1(op, cmds);
		RequestOptions options_ = convert(options, RequestOptions.class);
		String eventId = arg2(op2, cmds);
		debug("Remove CronJob Event: %s %s %s", cronjobId, eventId, options_);		
		if (isDryrun()) {
			return;
		}
		//devopsClient.removeEvent(cronjobId, eventId, options_);
		if (isEcho()) {
			eventCronJobList(cmds, options);
		}
	}


	
	//
	// CronJob Job instances
	//
	
	public void jobsCronJob(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		String cronjobId = argIdx1(op, cmds);
		switch (op2) {
		case "": case "help":
			printUsage2();
			return;
		case "ls": case "list": {
			if (isHelp3(op2)) {
				return;
			}
			debug("Jobs for CronJob: %s", cronjobId);		
			CronJobOptions options_ = convert(options, CronJobOptions.class);
			List<Job> jobs = devopsClient.listJobsForCronJob(cronjobId, options_);			
			print(jobs);
			break;
		}
		}
	}

	//
	// CronJob Mount
	//
	
	public void mountCronJob(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		switch (op2) {
		case "help": case "":
			printUsage2();
			break;
		case "ls": case "list": {
			mountCronJobList(cmds, options);
			break;
		}
		case "get": {
			mountCronJobGet(cmds, options);
			break;
		}
		case "add": case "create": {
			mountCronJobCreate(cmds, options);
			break;
		}
		case "update": {
			mountCronJobUpdate(cmds, options);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			mountCronJobDelete(cmds, options);
			break;
		}
		case "schema": case "meta":
			if (isHelp3(op2)) {
				return;
			}
			schema(Mount.class);
		default:
			invalidOp();
			printUsage2();
			break;
		}
	}

	public void mountCronJob0(String[] cmds, Map<String, Object> options) {
		switch (op) {
		case "help": case "":
			printUsage1();
			break;
		case "ls": case "list": {
			mountCronJobList0(cmds, options);
			break;
		}
		case "get": {
			mountCronJobGet0(cmds, options);
			break;
		}
		case "add": case "create": {
			mountCronJobCreate0(cmds, options);
			break;
		}
		case "update": {
			mountCronJobUpdate0(cmds, options);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			mountCronJobDelete0(cmds, options);
			break;
		}
		case "schema": case "meta":
			if (isHelp2()) {
				return;
			}
			schema(Mount.class);
		default:
			invalidOp();
			printUsage1();
			break;
		}
	}
	
	public void mountCronJobList(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String cronjobId = argIdx1(op, cmds);
		mountCronJobList(cronjobId, options);
	}

	public void mountCronJobList0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		mountCronJobList(cronjobId, options);
	}

	public void mountCronJobList(String cronjobId, Map<String, Object> options) {
		CronJobOptions options_ = convert(options, CronJobOptions.class);
		debug("CronJob Mounts: %s %s", cronjobId, options_);		
		if (isDryrun()) {
			return;
		}
		List<Mount> mounts = devopsClient.listMountsCronJob(cronjobId, options_);			
		print(mounts);
	}
	
	public void mountCronJobGet(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String cronjobId = argIdx1(op, cmds);
		String mountId = arg2(op, cmds);
		mountCronJobGet(cronjobId, mountId, cmds, options);
	}

	public void mountCronJobGet0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		String mountId = arg1(op, cmds);
		mountCronJobGet(cronjobId, mountId, cmds, options);
	}

	public void mountCronJobGet(String cronjobId, String mountId, String[] cmds, Map<String, Object> options) {
		CronJobOptions options_ = convert(options, CronJobOptions.class);
		debug("CronJob Mount: %s %s %s", cronjobId, mountId, options_);		
		Mount mount = devopsClient.getMountCronJob(cronjobId, mountId, options_);			
		printObj(mount);
	}

	public void mountCronJobCreate(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String cronjobId = argIdx1(op, cmds);
		String name = arg2(op, cmds);
		mountCronJobCreate(cronjobId, name, cmds, options);
	}

	public void mountCronJobCreate0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		String name = arg1(op, cmds);
		mountCronJobCreate(cronjobId, name, cmds, options);
	}

	public void mountCronJobCreate(String cronjobId, String name, String[] cmds, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		Mount mount = makeMount(true, null, options);
		if (mount==null) {
			return;
		}
		mount.setName(name);
		debug("Add CronJob Mount: %s %s %s", cronjobId, mount, options_);		
		if (isDryrun()) {
			return;
		}
		/*URI uri =*/ devopsClient.addMount(cronjobId, mount, options_);
		if (isEcho()) {
			//String mountId = extractId(uri);
			mountCronJobGet(cronjobId, name, cmds, options);
		}
	}

	public void mountCronJobUpdate(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String cronjobId = argIdx1(op, cmds);
		String mountId = arg2(op2, cmds);
		mountCronJobUpdate(cronjobId, mountId, cmds, options);
	}
	
	public void mountCronJobUpdate0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		String mountId = arg1(op, cmds);
		mountCronJobUpdate(cronjobId, mountId, cmds, options);
	}
	
	public void mountCronJobUpdate(String cronjobId, String mountId, String[] cmds, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		Mount mount = makeMount(false, null, options);
		if (mount==null) {
			return;
		}
		debug("Update CronJob Mount: %s %s %s", cronjobId, mountId, mount, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.updateMount(cronjobId, mountId, mount, options_);
		if (isEcho()) {
			mountCronJobGet(cronjobId, mountId, cmds, options);
		}
	}
	
	public void mountCronJobDelete(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String cronjobId = argIdx1(op, cmds);
		String mountId = arg2(op2, cmds);
		mountCronJobDelete(cronjobId, mountId, cmds, options);
	}
	
	public void mountCronJobDelete0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		String mountId = arg1(op, cmds);
		mountCronJobDelete(cronjobId, mountId, cmds, options);
	}
	
	public void mountCronJobDelete(String cronjobId, String mountId, String[] cmds, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		debug("Remove CronJob Mount: %s %s %s", cronjobId, mountId, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.removeMount(cronjobId, mountId, options_);
		if (isEcho()) {
			mountCronJobList(cmds, options);
		}
	}
	
	//
	// CronJob Env Vars
	//
	
	public void envCronJob(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		switch (op2) {
		case "help": case "":
			printUsage2();
			break;
		case "ls": case "list": {
			envCronJobList(cmds, options);
			break;
		}
		case "get": {
			envCronJobGet(cmds, options);
			break;
		}
		case "add": case "create": {
			envCronJobCreate(cmds, options);
			break;
		}
		case "update": {
			envCronJobUpdate(cmds, options);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			envCronJobDelete(cmds, options);
			break;
		}
		case "schema": case "meta":
			if (isHelp3(op2)) {
				return;
			}
			schema(Variable.class);
		default:
			invalidOp();
			printUsage2();
			break;
		}
	}

	public void envCronJob0(String[] cmds, Map<String, Object> options) {
		switch (op) {
		case "help": case "":
			printUsage1();
			break;
		case "ls": case "list": {
			envCronJobList0(cmds, options);
			break;
		}
		case "get": {
			envCronJobGet0(cmds, options);
			break;
		}
		case "add": case "create": {
			envCronJobCreate0(cmds, options);
			break;
		}
		case "update": {
			envCronJobUpdate0(cmds, options);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			envCronJobDelete0(cmds, options);
			break;
		}
		case "schema": case "meta":
			if (isHelp2()) {
				return;
			}
			schema(Variable.class);
		default:
			invalidOp();
			printUsage1();
			break;
		}
	}

	public void envCronJobList(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String cronjobId = argIdx1(op, cmds);
		envCronJobList(cronjobId, options);
	}

	public void envCronJobList0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		envCronJobList(cronjobId, options);
	}

	public void envCronJobList(String cronjobId, Map<String, Object> options) {
		debug("CronJob Variables: %s", cronjobId);		
		CronJobOptions options_ = convert(options, CronJobOptions.class);
		List<Variable> vars = devopsClient.listVariablesCronJob(cronjobId, options_);			
		print(vars);
	}
	
	public void envCronJobGet(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String cronjobId = argIdx1(op, cmds);
		String varId = arg2(op, cmds);
		envCronJobGet(cronjobId, varId, cmds, options);
	}

	public void envCronJobGet0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		String varId = arg1(op, cmds);
		envCronJobGet(cronjobId, varId, cmds, options);
	}

	public void envCronJobGet(String cronjobId, String varId, String[] cmds, Map<String, Object> options) {
		debug("CronJob Var: %s %s", cronjobId, varId);		
		CronJobOptions options_ = convert(options, CronJobOptions.class);
		Variable var = devopsClient.getVariableCronJob(cronjobId, varId, options_);			
		printObj(var);
	}
	
	public void envCronJobCreate(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String cronjobId = argIdx1(op, cmds);
		String name = arg2(op, cmds);
		String value = arg3(op, cmds, false);
		envCronJobCreate(cronjobId, name, value, options);
	}

	public void envCronJobCreate0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		String name = arg1(op, cmds);
		String value = arg2(op, cmds, false);
		envCronJobCreate(cronjobId, name, value, options);
	}

	public void envCronJobCreate(String cronjobId, String name, String value, Map<String, Object> options) {
		Variable var = makeVariable(name, value, options);
		RequestOptions options_ = convert(options, RequestOptions.class);		
		debug("Add Var: %s %s %s", cronjobId, var, options_);		
		if (isDryrun()) {
			return;
		}
		URI uri = devopsClient.addVariable(cronjobId, var, options_);
		if (isEcho()) {
			String varId = extractId(uri);
			envCronJobGet(cronjobId, varId, cmds, options);
		}
	}
	
	public void envCronJobUpdate(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String cronjobId = argIdx1(op, cmds);
		String varId = arg2(op2, cmds);
		String value = arg3(op, cmds, false);
		envCronJobUpdate(cronjobId, varId, value);
	}

	public void envCronJobUpdate0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		String varId = arg1(op, cmds);
		String value = arg2(op, cmds, false);
		envCronJobUpdate(cronjobId, varId, value);
	}

	public void envCronJobUpdate(String cronjobId, String varId, String value) {
		Variable var = makeVariable(null, value, options);
		RequestOptions options_ = convert(options, RequestOptions.class);
		debug("Update Var: %s %s %s %s", cronjobId, varId, var, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.updateVariable(cronjobId, varId, var, options_);
		if (isEcho()) {
			envCronJobGet(cronjobId, varId, cmds, options);
		}
	}

	public void envCronJobDelete(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String cronjobId = argIdx1(op, cmds);
		String varId = arg2(op2, cmds);
		envCronJobDelete(cronjobId, varId, options);
	}

	public void envCronJobDelete0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		String varId = arg1(op, cmds);
		envCronJobDelete(cronjobId, varId, options);
	}

	public void envCronJobDelete(String cronjobId, String varId, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		debug("Remove Var: %s %s %s", cronjobId, varId, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.removeVariable(cronjobId, varId, options_);
		if (isEcho()) {
			envCronJobList(cmds, options);
		}
	}

	//
	// CronJob Binding
	//
	
	public void bindingCronJob(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		switch (op2) {
		case "help": case "":
			printUsage2();
			break;
		case "ls": case "list": {
			bindingCronJobList(cmds, options);
			break;
		}
		case "add": case "create": {
			bindingCronJobAdd(cmds, options);
			break;
		}
		case "update": {
			bindingCronJobUpdate(cmds, options);
			break;
		}
		case "refresh": {
			bindingCronJobRefresh(cmds, options);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			bindingCronJobDelete(cmds, options);
			break;
		}
		case "schema": case "meta":
			if (isHelp3(op2)) {
				return;
			}
			schema(Binding.class);
		default:
			invalidOp();
			printUsage2();
			break;
		}
	}

	public void bindingCronJob0(String[] cmds, Map<String, Object> options) {
		switch (op) {
		case "help": case "":
			printUsage1();
			break;
		case "ls": case "list": {
			bindingCronJobList0(cmds, options);
			break;
		}
		case "add": case "create": {
			bindingCronJobAdd0(cmds, options);
			break;
		}
		case "update": {
			bindingCronJobUpdate0(cmds, options);
			break;
		}
		case "refresh": {
			bindingCronJobRefresh0(cmds, options);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			bindingCronJobDelete0(cmds, options);
			break;
		}
		case "schema": case "meta":
			if (isHelp2()) {
				return;
			}
			schema(Binding.class);
		default:
			invalidOp();
			printUsage1();
			break;
		}
	}

	public void bindingCronJobList(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String cronjobId = argIdx1(op, cmds);
		bindingCronJobList(cronjobId, options);
	}

	public void bindingCronJobList0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		bindingCronJobList(cronjobId, options);
	}

	public void bindingCronJobList(String cronjobId, Map<String, Object> options) {
		debug("CronJob Bindings: %s", cronjobId);		
		CronJobOptions options_ = convert(options, CronJobOptions.class);
		List<Binding> bindings = devopsClient.listBindingsCronJob(cronjobId, options_);			
		print(bindings);
	}
	
	public void bindingCronJobGet(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String cronjobId = argIdx1(op, cmds);
		String bindingId = arg2(op, cmds);
		bindingCronJobGet(cronjobId, bindingId, cmds, options);
	}
	
	public void bindingCronJobGet0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		String bindingId = arg1(op, cmds);
		bindingCronJobGet(cronjobId, bindingId, cmds, options);
	}
	
	public void bindingCronJobGet(String cronjobId, String bindingId, String[] cmds, Map<String, Object> options) {
		debug("CronJob Binding: %s %s", cronjobId, bindingId);		
		CronJobOptions options_ = convert(options, CronJobOptions.class);
		Binding binding = devopsClient.getBindingCronJob(cronjobId, bindingId, options_);			
		printObj(binding);
	}

	public void bindingCronJobAdd(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String cronjobId = argIdx1(op, cmds);
		String selector = arg2(op, cmds, false);
		bindingCronJobAdd(cronjobId, selector, options);
	}
	
	public void bindingCronJobAdd0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		String selector = arg1(op, cmds, false);
		bindingCronJobAdd(cronjobId, selector, options);
	}
	
	public void bindingCronJobAdd(String cronjobId, String selector, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		Binding binding = convert(options, Binding.class);
		if (StringUtil.hasText(selector)) {
			binding.setSelector(selector);			
		}
		debug("Add Binding: %s %s %s", cronjobId, binding, options_);		
		if (isDryrun()) {
			return;
		}
		URI uri = devopsClient.addBinding(cronjobId, binding, options_);
		if (isEcho()) {
			String bindingId = extractId(uri);
			bindingCronJobGet(cronjobId, bindingId, cmds, options);
		}
	}

	public void bindingCronJobUpdate(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String cronjobId = argIdx1(op, cmds);
		String bindingId = arg2(op, cmds);
		bindingCronJobUpdate(cronjobId, bindingId, options);
	}
	
	public void bindingCronJobUpdate0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		String bindingId = arg1(op, cmds);
		bindingCronJobUpdate(cronjobId, bindingId, options);
	}
	
	public void bindingCronJobUpdate(String cronjobId, String bindingId, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		Binding binding = convert(options, Binding.class);
		debug("Update Binding: %s %s %s", cronjobId, bindingId, binding);		
		if (isDryrun()) {
			return;
		}
		devopsClient.updateBinding(cronjobId, bindingId, binding, options_);
		if (isEcho()) {
			bindingCronJobGet(cronjobId, bindingId, cmds, options);
		}		
	}

	public void bindingCronJobRefresh(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String cronjobId = argIdx1(op, cmds);
		String bindingId = arg2(op2, cmds);
		bindingCronJobRefresh(cronjobId, bindingId, options);
	}
	
	public void bindingCronJobRefresh0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		String bindingId = arg1(op, cmds);
		bindingCronJobRefresh(cronjobId, bindingId, options);
	}
	
	public void bindingCronJobRefresh(String cronjobId, String bindingId, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		debug("Refresh Binding: %s %s %s", cronjobId, bindingId, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.refreshBinding(cronjobId, bindingId, options_);
		if (isEcho()) {
			bindingCronJobGet(cronjobId, bindingId, cmds, options);
		}
	}

	public void bindingCronJobDelete(String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		if (isHelp3(op2)) {
			return;
		}
		String cronjobId = argIdx1(op, cmds);
		String bindingId = arg2(op2, cmds);
		bindingCronJobDelete(cronjobId, bindingId, options);
	}
	
	public void bindingCronJobDelete0(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		String bindingId = arg1(op, cmds);
		bindingCronJobDelete(cronjobId, bindingId, options);
	}
	
	public void bindingCronJobDelete(String cronjobId, String bindingId, Map<String, Object> options) {
		RequestOptions options_ = convert(options, RequestOptions.class);
		debug("Remove Binding: %s %s %s", cronjobId, bindingId, options_);		
		if (isDryrun()) {
			return;
		}
		devopsClient.removeBinding(cronjobId, bindingId, options_);
		if (isEcho()) {
			bindingCronJobList(cmds, options);
		}
	}

	//
	// Domain
	//

	public void domain(String[] cmds, Map<String, Object> options) {
		switch (op) {
		case "help": case "":
			printUsage(type);
			break;
		case "ls": case "list":
			listDomain(cmds, options);
			break;
		case "get": 
			getDomain(cmds, options);
			break;
		case "view": 
			viewDomain(cmds, options);
			break;
		case "schema": case "meta":
			schemaDomain(cmds, options);
			break;
		case "create": case "add": 
			createDomain(cmds, options);
			break;
		case "update": 
			updateDomain(cmds, options);
			break;
		case "delete": case "del": case "rm":
			deleteDomain(cmds, options);
			break;
		case "set":
			setDomain(cmds, options);
			break;
		case "unset":
			unsetDomain(cmds, options);
			break;				
		default: 
			invalidOp(type, op);
			break;
		}
	}
	
	public void listDomain(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String q = argId(op, cmds, false);
		listDomain(q, options);
	}
	
	public void listDomain(String q, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		DomainFilter filter = convert(options, DomainFilter.class);
		if (StringUtil.hasText(q)) {
			filter.setQ(q);
		}
		debug("Domains: %s %s", filter, pageable);
		Page<Domain> domains = devopsClient.listDomains(filter, pageable);
		print(domains, Domain.class);
	}

	public void getDomain(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String domainId = argId(op, cmds, this.domain);
		if (domainId==null) {
			return;
		}
		DomainOptions options_ = convert(options, DomainOptions.class);
		debug("Domain: %s", domainId);
		Domain domain = devopsClient.getDomain(domainId, options_);
		printObj(domain);
	}

	public void viewDomain(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String domainId = argId(op, cmds, this.domain);
		if (domainId==null) {
			return;
		}
		DomainOptions options_ = convert(options, DomainOptions.class);
		debug("View Domain: %s", domainId);
		Domain domain = devopsClient.getDomain(domainId, options_);
		view("domain", domain.getUuid());
	}

	public void setDomain(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String domainId = argId(op, cmds);
		setDomain(domainId, options);
	}

	public void setDomain(String domainId, Map<String, Object> options) {
		debug("Set Domain: %s", domainId);
		DomainOptions options_ = convert(options, DomainOptions.class);
		Domain domain = devopsClient.getDomain(domainId, options_);
		this.domain = domainId;
		if (isEcho()) {
			printObj(domain);
		}
		writeConfig();
	}
	
	public void unsetDomain(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String domainId = argId(op, cmds);
		debug("Unset Domain: %s", domainId);
		this.domain = null;
		writeConfig();
	}

	public void schemaDomain(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		schema(Domain.class, DomainFilter.class, DomainOptions.class, options);
	}

	public void createDomain(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		Domain domain = makeDomain(options);
		if (domain==null) {
			return;
		}
		domain.setName(argId(op, cmds));
		DomainOptions options_ = convert(options, DomainOptions.class);
		debug("Domain: %s %s", domain, options_);
		if (isDryrun()) {
			return;
		}
		URI uri = devopsClient.createDomain(domain, options_);
		if (isEcho()) {
			debug("Domain URI: %s", uri);
			String id = extractId(uri);
			Domain domain2 = devopsClient.getDomain(id, null);
			printObj(domain2);			
		}
	}
	
	private Domain makeDomain(Map<String, Object> options) {
		Domain domain = convert(options, Domain.class);
		String ca = (String)options.get("ca");
		Certificate cert = new Certificate();
		if (ca!=null && !ca.isEmpty()) {
			String ca_ = readFile(ca, true);
			if (ca_==null) {
				return null;
			}
			cert.setCa(ca_); 
		}
		String crt = (String)options.get("crt");
		if (crt!=null && !crt.isEmpty()) {
			String crt_ = readFile(crt, true);
			if (crt_==null) {
				return null;
			}
			cert.setCa(crt_); 
		}
		String key = (String)options.get("key");
		if (key!=null && !key.isEmpty()) {
			String key_ = readFile(key, true);
			if (key_==null) {
				return null;
			}
			cert.setCa(key_); 
		}
		if (cert.getCa()!=null || cert.getCrt()!=null || cert.getKey()!=null) {
			domain.setCertificate(cert);
			domain.setCert(true);
			domain.setTls(true);
		}
		return domain;
	}

	public void updateDomain(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String domainId = (String)get("domain", options);
		Domain domain = makeDomain(options);
		if (domain==null) {
			return;
		}
		DomainOptions options_ = convert(options, DomainOptions.class);
		debug("Updating Domain: %s %s %s", domainId, domain, options_);
		if (isDryrun()) {
			return;
		}
		devopsClient.updateDomain(domainId, domain, options_);
		if (isEcho()) {
			Domain domain2 = devopsClient.getDomain(domainId, null);
			printObj(domain2);
		}
	}

	public void deleteDomain(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String domainId = (String)get(new String[] {"id", "uuid"}, options);
		String[] ids = domainId.split(",");
		for (String id: ids) {
			if (id.isEmpty()) {
				continue;
			}
			debug("Deleting Domain: %s", domainId);
			if (!isDryrun()) {
				devopsClient.deleteDomain(domainId, null);	
			}
		}
		if (isDryrun()) {
			return;
		}
		if (isEcho()) {
			listDomain(cmds, options);
		}
	}


	//
	// Registry
	//

	public void registry(String[] cmds, Map<String, Object> options) {
		switch (op) {
		case "help": case "":
			printUsage(type);
			break;
		case "ls": case "list":
			listRegistry(cmds, options);
			break;
		case "get": 
			getRegistry(cmds, options);
			break;
		case "view": 
			viewRegistry(cmds, options);
			break;
		case "schema": case "meta":
			schemaRegistry(cmds, options);
			break;
		case "create": case "add": 
			createRegistry(cmds, options);
			break;
		case "update": 
			updateRegistry(cmds, options);
			break;
		case "delete": case "del": case "rm":
			deleteRegistry(cmds, options);
			break;
		case "set":
			setRegistry(cmds, options);
			break;
		case "unset":
			unsetRegistry(cmds, options);
			break;
		default: 
			invalidOp(type, op);
			break;
		}
	}

	
	public void listRegistry(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String q = argId(op, cmds, false);
		listRegistry(q, options);
	}
	
	public void listRegistry(String q, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		RegistryFilter filter = convert(options, RegistryFilter.class);
		if (StringUtil.hasText(q)) {
			filter.setQ(q);
		}
		debug("Registries: %s %s", filter, pageable);
		Page<Registry> registrys = devopsClient.listRegistries(filter, pageable);
		print(registrys, Registry.class);
	}
	
	public void getRegistry(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String registryId = argId(op, cmds, this.registry);
		if (registryId==null) {
			return;
		}
		RegistryOptions options_ = convert(options, RegistryOptions.class);
		debug("Registry: %s %s", registryId, options_);
		Registry registry = devopsClient.getRegistry(registryId, options_);
		printObj(registry);
	}
	
	public void viewRegistry(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String registryId = argId(op, cmds, this.registry);
		if (registryId==null) {
			return;
		}
		RegistryOptions options_ = convert(options, RegistryOptions.class);
		debug("View Registry: %s %s", registryId, options_);
		Registry registry = devopsClient.getRegistry(registryId, options_);
		view("registry", registry.getUuid());
	}
	
	public void setRegistry(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String registryId = argId(op, cmds);
		setRegistry(registryId, options);
	}

	public void setRegistry(String registryId, Map<String, Object> options) {
		debug("Set Registry: %s", registryId);
		RegistryOptions options_ = convert(options, RegistryOptions.class);
		Registry registry = devopsClient.getRegistry(registryId, options_);
		this.registry = registryId;
		if (isEcho()) {
			printObj(registry);
		}
		writeConfig();
	}
	
	public void unsetRegistry(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String registryId = argId(op, cmds);
		debug("Unset Registry: %s", registryId);
		this.registry = null;
		writeConfig();
	}

	public void schemaRegistry(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		schema(Registry.class, RegistryFilter.class, RegistryOptions.class, options);
	}

	
	public void createRegistry(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String name = argId(op, cmds);
		Registry registry = makeRegistry(name, options);
		RegistryOptions options_ = convert(options, RegistryOptions.class);
		debug("Creating Registry: %s %s", registry, options_);
		if (isDryrun()) {
			return;
		}
		URI uri = devopsClient.createRegistry(registry, options_);
		if (isEcho()) {
			debug("Registry URI: %s", uri);
			String registryId = extractId(uri);
			Registry registry2 = devopsClient.getRegistry(registryId, null);
			printObj(registry2);			
		}
	}

	private Registry makeRegistry(String name, Map<String, Object> options) {
		Registry registry = convert(options, Registry.class);
		if (!StringUtil.hasText(registry.getUsername())) {
			String username = (String)options.get("u");
			if (username!=null) {
				registry.setUsername(username);
			}
		}
		if (!StringUtil.hasText(registry.getPassword())) {
			String password = (String)options.get("u");
			if (password!=null) {
				registry.setPassword(password);
			}
		}
		if (!StringUtil.hasText(registry.getEmail())) {
			String email = (String)options.get("m");
			if (email!=null) {
				registry.setEmail(email);
			}
		}
		if (!StringUtil.hasText(registry.getServer())) {
			String server = (String)options.get("url");
			if (server!=null) {
				registry.setServer(server);
			}
		}
		if (StringUtil.hasText(name)) {
			registry.setName(name);
		}
		if (registry.getCredentialsType()==null) {
			String auth = (String)options.get("auth");
			if (auth!=null) {
				CredentialsType ctype = CredentialsType.parse(auth);
				registry.setCredentialsType(ctype);
			}
		}
		if (!StringUtil.hasText(registry.getDisplayName())) {
			String dname = (String)options.get("dname");
			if (dname!=null) {
				registry.setDisplayName(dname);
			}
		}
		return registry;
	}


	public void updateRegistry(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String registryId = argId(op, cmds);
		Registry registry = makeRegistry(null, options);
		RegistryOptions options_ = convert(options, RegistryOptions.class);
		debug("Updating Registry: %s %s %s", registryId, registry, options_);
		if (isDryrun()) {
			return;
		}
		devopsClient.updateRegistry(registryId, registry, options_);
		if (isEcho()) {
			Registry registry2 = devopsClient.getRegistry(registryId, null);
			printObj(registry2);			
		}
	}
	
	public void deleteRegistry(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String registryId = argId(op, cmds);
		String[] ids = registryId.split(",");
		for (String id: ids) {
			if (id.isEmpty()) {
				continue;
			}
			debug("Deleting Registry: %s", registryId);		
			if (!isDryrun()) {
				devopsClient.deleteRegistry(registryId, null);	
			}
		}
		if (isDryrun()) {
			return;
		}
		if (isEcho()) {
			listRegistry(cmds, options);
		}
	}


	//
	// Vcs
	//	

	public void vcs(String[] cmds, Map<String, Object> options) {
		switch (op) {
		case "help": case "":
			printUsage(type);
			break;
		case "ls": case "list":
			listVcs(cmds, options);
			break;
		case "get": 
			getVcs(cmds, options);
			break;
		case "view": 
			viewVcs(cmds, options);
			break;
		case "schema": case "meta":
			schemaVcs(cmds, options);
			break;
		case "create": case "add": 
			createVcs(cmds, options);
			break;
		case "update": 
			updateVcs(cmds, options);
			break;
		case "delete": case "del": case "rm":
			deleteVcs(cmds, options);
			break;
		case "set":
			setVcs(cmds, options);
			break;
		case "unset":
			unsetVcs(cmds, options);
			break;
		default: 
			invalidOp(type, op);
			break;
		}
	}


	
	public void listVcs(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String q = argId(op, cmds, false);
		listVcs(q, options);
	}
	
	public void listVcs(String q, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		VcsFilter filter = convert(options, VcsFilter.class);
		if (StringUtil.hasText(q)) {
			filter.setQ(q);
		}
		debug("Vcs: %s %s", filter, pageable);
		Page<Vcs> vcss = devopsClient.listVcss(filter, pageable);
		print(vcss, Vcs.class);
	}

	public void getVcs(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String vcsId = argId(op, cmds, this.vcs);
		if (vcsId==null) {
			return;
		}
		VcsOptions options_ = convert(options, VcsOptions.class);
		debug("Vcs: %s %s", vcsId, options_);
		Vcs vcs = devopsClient.getVcs(vcsId, options_);
		printObj(vcs);
	}

	public void viewVcs(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String vcsId = argId(op, cmds, this.vcs);
		if (vcsId==null) {
			return;
		}
		VcsOptions options_ = convert(options, VcsOptions.class);
		debug("View Vcs: %s %s", vcsId, options_);
		Vcs vcs = devopsClient.getVcs(vcsId, options_);
		view("vcs", vcs.getUuid());
	}

	public void setVcs(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String vcsId = argId(op, cmds);
		setVcs(vcsId, options);
	}

	public void setVcs(String vcsId, Map<String, Object> options) {
		debug("Set Vcs: %s", vcsId);
		VcsOptions options_ = convert(options, VcsOptions.class);
		Vcs vcs = devopsClient.getVcs(vcsId, options_);
		this.vcs = vcsId;
		if (isEcho()) {
			printObj(vcs);
		}
		writeConfig();
	}
	
	public void unsetVcs(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String vcsId = argId(op, cmds);
		debug("Unset Vcs: %s", vcsId);
		this.vcs = null;
		writeConfig();
	}

	public void schemaVcs(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		schema(Vcs.class, VcsFilter.class, VcsOptions.class, options);
	}

	public void createVcs(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String name = argId(op, cmds); 
		Vcs vcs = makeVcs(name, options);
		VcsOptions options_ = convert(options, VcsOptions.class);
		debug("Creating Vcs: %s %s", vcs, options_);
		if (isDryrun()) {
			return;
		}
		URI uri = devopsClient.createVcs(vcs, options_);
		if (isEcho()) {
			debug("Vcs URI: %s", uri);
			String id = extractId(uri);
			Vcs vcs2 = devopsClient.getVcs(id, null);
			printObj(vcs2);			
		}
	}

	private Vcs makeVcs(String name, Map<String, Object> options) {
		Vcs vcs = convert(options, Vcs.class);
		if (!StringUtil.hasText(vcs.getUsername())) {
			String username = (String)options.get("u");
			if (username!=null) {
				vcs.setUsername(username);
			}
		}
		if (!StringUtil.hasText(vcs.getPassword())) {
			String password = (String)options.get("u");
			if (password!=null) {
				vcs.setPassword(password);
			}
		}
		if (!StringUtil.hasText(vcs.getUrl())) {
			String server = (String)options.get("server");
			if (server!=null) {
				vcs.setUrl(server);
			}
		}
		if (vcs.getCredentialsType()==null) {
			String auth = (String)options.get("auth");
			if (auth!=null) {
				CredentialsType ctype = CredentialsType.parse(auth);
				vcs.setCredentialsType(ctype);
			}
		}
		if (StringUtil.hasText(name)) {
			vcs.setName(name);
		}
		if (!StringUtil.hasText(vcs.getDisplayName())) {
			String dname = (String)options.get("dname");
			if (dname!=null) {
				vcs.setDisplayName(dname);
			}
		}
		return vcs;
	}

	public void updateVcs(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String vcsId = argId(op, cmds);
		Vcs vcs = makeVcs(null, options);
		VcsOptions options_ = convert(options, VcsOptions.class);
		debug("Updating Vcs: %s %s %s", vcsId, vcs, options_);
		if (isDryrun()) {
			return;
		}
		devopsClient.updateVcs(vcsId, vcs, options_);
		if (isEcho()) {
			Vcs vcs2 = devopsClient.getVcs(vcsId, null);
			printObj(vcs2);			
		}
	}

	public void deleteVcs(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String vcsId = argId(op, cmds);
		VcsOptions options_ = convert(options, VcsOptions.class);
		String[] ids = vcsId.split(",");
		for (String id: ids) {
			if (id.isEmpty()) {
				continue;
			}
			debug("Deleting Vcs: %s", id);
			if (!isDryrun()) {
				devopsClient.deleteVcs(vcsId, options_);	
			}
		}
		if (isDryrun()) {
			return;
		}
		if (isEcho()) {
			listVcs(cmds, options);
		}
	}

	//
	// Catalog
	//

	public void catalog(String[] cmds, Map<String, Object> options) {
		switch (op) {
		case "help": case "":
			printUsage(type);
			break;
		case "ls": case "list":
			listCatalog(cmds, options);
			break;
		case "get": 
			getCatalog(cmds, options);
			break;
		case "view": 
			viewCatalog(cmds, options);
			break;
		case "schema": case "meta":
			schemaCatalog(cmds, options);
			break;
		case "create": case "add": 
			createCatalog(cmds, options);
			break;
		case "update": 
			updateCatalog(cmds, options);
			break;
		case "delete": case "del": case "rm":
			deleteCatalog(cmds, options);
			break;
		case "solution": case "solutions":
			listSolutionsFor(cmds, options);
			break;
		case "install":
			installFromCatalog(cmds, options);
			break;
		case "set":
			setCatalog(cmds, options);
			break;
		case "unset":
			unsetCatalog(cmds, options);
			break;
		default: 
			invalidOp(type, op);
			break;
		}
	}

	
	public void listCatalog(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		CatalogFilter filter = convert(options, CatalogFilter.class);
		String q = argId(op, cmds, false);
		if (q!=null) {
			filter.setQ(q);
		}
		debug("Catalogs: %s %s", filter, pageable);
		Page<Catalog> catalogs = devopsClient.listCatalogs(filter, pageable);
		print(catalogs, Catalog.class);
	}

	public void getCatalog(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String catalogId = argId(op, cmds, this.catalog);
		if (catalogId==null) {
			return;
		}
		CatalogOptions options_ = convert(options, CatalogOptions.class);
		debug("Catalog: %s", catalogId);
		Catalog catalog = devopsClient.getCatalog(catalogId, options_);
		printObj(catalog);
	}

	public void viewCatalog(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String catalogId = argId(op, cmds, this.catalog);
		if (catalogId==null) {
			return;
		}
		CatalogOptions options_ = convert(options, CatalogOptions.class);
		debug("View Catalog: %s", catalogId);
		Catalog catalog = devopsClient.getCatalog(catalogId, options_);
		view("catalog", catalog.getUuid());
	}

	public void setCatalog(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String catalogId = argId(op, cmds);
		setCatalog(catalogId, options);
	}

	public void setCatalog(String catalogId, Map<String, Object> options) {
		debug("Set Catalog: %s", catalogId);
		CatalogOptions options_ = convert(options, CatalogOptions.class);
		Catalog catalog = devopsClient.getCatalog(catalogId, options_);
		this.catalog = catalogId;
		if (isEcho()) {
			printObj(catalog);
		}
		writeConfig();
	}
	
	public void unsetCatalog(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String catalogId = argId(op, cmds);
		debug("Unset Catalog: %s", catalogId);
		this.catalog = null;
		writeConfig();
	}

	public void schemaCatalog(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		schema(Catalog.class, CatalogFilter.class, CatalogOptions.class, options);
	}

	public void createCatalog(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		Catalog catalog = convert(options, Catalog.class);
		catalog.setName(argId(op, cmds));
		CatalogOptions options_ = convert(options, CatalogOptions.class);
		debug("Creating Catalog: %s %s", catalog, options_);
		if (isDryrun()) {
			return;
		}
		URI uri = devopsClient.createCatalog(catalog, new CatalogOptions());
		if (isEcho()) {
			debug("Catalog URI: %s", uri);
			String id = extractId(uri);
			Catalog catalog2 = devopsClient.getCatalog(id, options_);
			printObj(catalog2);			
		}
	}
	
	
	public void updateCatalog(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String catalogId = argId(op, cmds);
		Catalog catalog = convert(options, Catalog.class);
		CatalogOptions options_ = convert(options, CatalogOptions.class);
		debug("Updating Catalog: %s %s %s", catalogId, catalog, options_);
		if (isDryrun()) {
			return;
		}
		devopsClient.updateCatalog(catalogId, catalog, options_);
		if (isEcho()) {
			Catalog catalog2 = devopsClient.getCatalog(catalogId, options_);
			printObj(catalog2);			
		}
	}

	public void deleteCatalog(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String catalogId = argId(op, cmds);
		CatalogOptions options_ = convert(options, CatalogOptions.class);
		String[] ids = catalogId.split(",");
		for (String id: ids) {
			if (id.isEmpty()) {
				continue;
			}
			debug("Deleting Catalog: %s", id);		
			if (!isDryrun()) {
				devopsClient.deleteCatalog(id, options_);	
			}
		}
		if (isDryrun()) {
			return;
		}
		if (isEcho()) {
			listCatalog(cmds, options);
		}
	}

	public void installFromCatalog(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String catalogId = argId(op, cmds);
		String solutionId = arg1(op, cmds);
		installFromCatalog(catalogId, solutionId, options);
	}

	public void installFromCatalog(String catalogId, String solutionId, Map<String, Object> options) {
		InstallOptions install = makeInstallOptions(options);
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingArg("install", "", "-n");
			return;
		}
		install.setSpace(spaceId);
		debug("Install Solution from Catalog: %s %s", solutionId, install);
		if (isDryrun()) {
			return;
		}
		URI uri = devopsClient.install(catalogId, solutionId, install);
		if (isEcho()) {
			Object deploy = getAny(uri, options);
			printObj(deploy);
		}
	}
	
	public void listSolutionsFor(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String catalogId = argId(op, cmds);
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		SolutionFilter filter = convert(options, SolutionFilter.class);
		debug("Catalog Solutions: %s %s %s", catalogId, filter, pageable);
		Page<Solution> solutions = devopsClient.listSolutionsFor(catalogId, filter, pageable);
		Page<CatalogSolution> solutions2 = CatalogSolution.convert(solutions);
		print(solutions2, CatalogSolution.class);
	}
	
	public static class CatalogSolution extends Solution {
		CatalogSolution(Solution solution) {
			MappingUtils.updateObjectFrom(this, solution);
		}

		static Page<CatalogSolution> convert(Page<Solution> page) {
			if (page==null) {
				return null;
			}
			return new PageImpl<CatalogSolution>(convert(page.getContent()), new PageRequest(page.getNumber(), page.getSize()), page.getTotalElements());
		}

		static List<CatalogSolution> convert(List<Solution> solutions) {
			if (solutions==null) {
				return null;
			}
			List<CatalogSolution> solutions2 = new ArrayList<>();
			for (Solution solution: solutions) {
				solutions2.add(new CatalogSolution(solution));
			}
			return solutions2;
		}
	}
	
	//
	// Solution
	//

	public void solution(String[] cmds, Map<String, Object> options) {
		switch (op) {
		case "help": case "":
			printUsage(type);
			break;
		case "ls": case "list":
			listSolution(cmds, options);
			break;
		case "get": 
			getSolution(cmds, options);
			break;
		case "view": 
			viewSolution(cmds, options);
			break;
		case "schema": case "meta":
			schemaSolution(cmds, options);
			break;
		case "create": case "add": 
			createSolution(cmds, options);
			break;
		case "update": 
			updateSolution(cmds, options);
			break;
		case "delete": case "del": case "rm":
			deleteSolution(cmds, options);
			break;
		case "install":
			installSolution(cmds, options);
			break;
		default: 
			invalidOp(type, op);
			break;
		}
	}


	
	public void listSolution(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		SolutionFilter filter = convert(options, SolutionFilter.class);
		String q = argId(op, cmds, false);
		if (q!=null) {
			filter.setQ(q);
		}
		debug("Solution: %s %s", filter, pageable);
		Page<Solution> solutions = devopsClient.listSolutions(filter, pageable);
		print(solutions, Solution.class);
	}

	public void getSolution(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String solutionId = argId(op, cmds);
		SolutionOptions options_ = convert(options, SolutionOptions.class);
		debug("Solution: %s", solutionId);
		Solution solution = devopsClient.getSolution(solutionId, options_);
		printObj(solution);
	}

	public void viewSolution(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String solutionId = argId(op, cmds);
		SolutionOptions options_ = convert(options, SolutionOptions.class);
		debug("View Solution: %s", solutionId);
		Solution solution = devopsClient.getSolution(solutionId, options_);
		view("solution", solution.getUuid());
	}

	public void schemaSolution(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		schema(Solution.class, SolutionFilter.class, SolutionOptions.class, options);
	}

	public void createSolution(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		Solution solution = convert(options, Solution.class);
		solution.setName(argId(op, cmds));
		debug("Creating Solution: %s", solution);
		URI uri = devopsClient.createSolution(solution, new SolutionOptions());
		if (isEcho()) {
			debug("Solution URI: %s", uri);
			String id = extractId(uri);
			Solution solution2 = devopsClient.getSolution(id, null);
			printObj(solution2);
		}
	}
	
	
	public void updateSolution(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String solutionId = argId(op, cmds);
		Solution solution = convert(options, Solution.class);
		debug("Updating Solution: %s %s", solutionId, solution);
		devopsClient.updateSolution(solutionId, solution, null);
		if (isEcho()) {
			Solution solution2 = devopsClient.getSolution(solutionId, null);
			printObj(solution2);			
		}
	}

	public void deleteSolution(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String solutionId = argId(op, cmds);
		SolutionOptions options_ = convert(options, SolutionOptions.class);
		String[] ids = solutionId.split(",");
		for (String id: ids) {
			if (id.isEmpty()) {
				continue;
			}
			debug("Deleting Solution: %s", id);		
			devopsClient.deleteSolution(id, options_);	
		}
		if (isEcho()) {
			listSolution(cmds, options);
		}
	}

	public void installSolution(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String solutionId = argId(op, cmds);
		installSolution(solutionId, options);
	}

	public void installSolution(String solutionId, Map<String, Object> options) {
		InstallOptions install = makeInstallOptions(options);
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingArg("install", "", "-n");
			return;
		}
		install.setSpace(spaceId);
		debug("Install Solution: %s %s", solutionId, install);
		if (isDryrun()) {
			return;
		}
		URI uri = devopsClient.install(solutionId, install);
		if (isEcho()) {
			Object deploy = getAny(uri, options);
			printObj(deploy);
		}			
	}
	
	private InstallOptions makeInstallOptions(Map<String, Object> options) {
		InstallOptions install = convert(options, InstallOptions.class);
		Integer k = parseInt((String)options.get("k"));
		if (k!=null) {
			install.setInstances(k);
		}
		Resources resources = makeResources(options);
		if (resources!=null) {
			install.setResources(resources);
		}
		if (install.getStart()==null) {
			install.setStart(true);			
		}
		String host = (String)options.get("host");
		String r = (String)options.get("r");
		if (r!=null) {
			if (r.equalsIgnoreCase("false")) {
				install.setRoute(false);
				host = null;
			} else {
				if (host==null) {
					host = r;			
				}
			}
		}
		if (host!=null && !host.isEmpty()) {
			install.setHost(host);
			install.setRoute(true);
			Domain domain = makeRouteDomain(options);
			if (domain!=null) {
				install.setDomain(domain.getUuid());
			}
		}

		List<Variable> env = new ArrayList<>();
		if (!makeEnv(options, env)) {
			return null;
		}
		if (!env.isEmpty()) {
			//install.setEnv(env);
		}
		List<Mount> mount = new ArrayList<>();
		if (!makeMounts(options, mount)) {
			return null;
		}
		if (!mount.isEmpty()) {
			install.setMounts(mount);
		}
		return install;
	}
	
	private Object getAny(URI uri, Map<String, Object> options) {
		String path = uri.getPath();
		String id = extractId(uri);
		if (path.indexOf("/deploy/")>=0) {
			DeploymentOptions options_ = convert(options, DeploymentOptions.class);
			return devopsClient.getDeployment(id, options_);
		}
		if (path.indexOf("/job/")>=0) {
			JobOptions options_ = convert(options, JobOptions.class);
			return devopsClient.getJob(id, options_);
		}
		if (path.indexOf("/deploy/")>=0) {
			CronJobOptions options_ = convert(options, CronJobOptions.class);
			return devopsClient.getCronJob(id, options_);
		}
		return null;
	}

	//
	// Markeplace
	//

	public void listMarketplace(String[] cmds, Map<String, Object> options) {
		if (isHelp1()) {
			return;
		}
		printMarketplace(op, options);
	}
	
	private void printMarketplace(String q, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		CatalogFilter filter = convert(options, CatalogFilter.class);
		filter.setQ(null);
		SolutionFilter filter2 = convert(options, SolutionFilter.class);
		if (StringUtil.hasText(q)) {
			filter2.setQ(q);
		}
		debug("Marketplace: %s %s %s", filter, filter2, pageable);
		int i = 0;
		Page<Solution> solutions0 = devopsClient.listSolutions(filter2, pageable);
		if (size(solutions0)>0) {
			print(solutions0, Solution.class, true);
			i++;	
		}		
		Page<Catalog> catalogs = devopsClient.listCatalogs(filter, null);
		if (catalogs.getContent()!=null) {
			for (Catalog catalog: catalogs.getContent()) {
				Page<Solution> solutions = devopsClient.listSolutionsFor(catalog.getUuid(), filter2, pageable);				
				if (size(solutions)>0) {
					if (i>0) {
						System.out.println();
					}
					Page<CatalogSolution> solutions2 = CatalogSolution.convert(solutions);
					System.out.println(String.format("%s %s %s", catalog.getId(), catalog.getName(), catalog.getDisplayName()!=null ? " - " + catalog.getDisplayName() : ""));
					System.out.println();
					print(solutions2, CatalogSolution.class, true);
					i++;					
				}
			}			
		}
	}

	static class Solution2 extends Solution {
		Catalog catalog;
		public Solution2(Catalog catalog, Solution solution) {
			MappingUtils.updateObjectFrom(this, solution);
			this.catalog = catalog;
		}
		
		public static List<Solution2> make(Catalog catalog, List<Solution> solutions) {
			List<Solution2> solutions2 = new ArrayList<Solution2>(solutions.size()); 
			if (solutions!=null) {
				for (Solution solution: solutions) {
					solutions2.add(new Solution2(catalog, solution));
				}
			}
			return solutions2;
		}
	}
	
	private void searchMarketplace(String q, Map<String, Object> options, List<Solution2> strictMatches, List<Solution2> matches) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		CatalogFilter filter = convert(options, CatalogFilter.class);
		filter.setQ(null);
		SolutionFilter filter2 = convert(options, SolutionFilter.class);
		if (StringUtil.hasText(q)) {
			filter2.setQ(q);
		}
		Page<Solution> solutions0 = devopsClient.listSolutions(filter2, pageable);
		if (size(solutions0)>0) {
			if (matches!=null) {
				matches.addAll(Solution2.make(null, solutions0.getContent()));
			}
			if (strictMatches!=null) {
				List<Solution> solution2 = findAll(q, true, solutions0.getContent());						
				strictMatches.addAll(Solution2.make(null, solution2));
			}
		}		
		Page<Catalog> catalogs = devopsClient.listCatalogs(filter, null);
		if (catalogs.getContent()!=null) {
			for (Catalog catalog: catalogs.getContent()) {
				Page<Solution> solutions = devopsClient.listSolutionsFor(catalog.getUuid(), filter2, pageable);				
				if (size(solutions)>0) {
					if (matches!=null) {
						matches.addAll(Solution2.make(catalog, solutions0.getContent()));
					}
					if (strictMatches!=null) {
						List<Solution> solutions2 = findAll(q, true, solutions.getContent());						
						strictMatches.addAll(Solution2.make(catalog, solutions2));
					}
				}
			}			
		}
	}

	private List<Solution> findAll(String q, boolean strict, List<Solution> solutions) {
		if (solutions==null) {
			return null;
		}
		List<Solution> solutions2 = new ArrayList<>();
		for (Solution solution: solutions) {
			if ((strict && q.equalsIgnoreCase(solution.getName())) ||
				(!strict && solution.getName()!=null && solution.getName().contains(q))) {
				solutions2.add(solution);
			}
		}
		return solutions2;
	}
	
	public void install(String[] cmds, Map<String, Object> options) {
		if (isHelp1()) {
			return;
		}
		if (op==null || op.isEmpty()) {
			error(String.format("missing solution id"));
			exit(-1);
			return;
		}
		int i = op.indexOf("/");
		String id = null;
		String catalogId = null;
		if (i<0) {
			id = op;
		} else if (i==op.length()-1) {
			error(String.format("missing solution id"));
			exit(-1);
			return;
		} else if (i==0) {
			id = op.substring(1);
			catalogId = "";
		} else {
			id = op.substring(i+1);
			catalogId = op.substring(0, i);
		}	
		if (id==null || id.trim().isEmpty()) {
			error(String.format("missing solution id"));
			exit(-1);
			return;
		}
		if (catalogId!=null) {
			if (catalogId.isEmpty()) {
				installSolution(id, options);				
			} else {
				installFromCatalog(catalogId, id, options);			
			}
		} else {
			List<Solution2> strictMatches = new ArrayList<>();
			List<Solution2> matches = new ArrayList<>();
			searchMarketplace(id, options, strictMatches, matches);
			if (strictMatches.size()==1) {
				Solution2 solution = strictMatches.get(0);
				if (solution.catalog==null) {
					installSolution(id, options);				
				} else {
					installFromCatalog(solution.catalog.getUuid(), id, options);	
				}
			} else if (strictMatches.size()>0) {
				println("Found %s Solutions with name: %s", strictMatches.size(), id);
				printMarketplace(strictMatches);
			} else if (matches.size()>0) {
				println("Found %s partial Solution matches. Did you mean:", matches.size());
				printMarketplace(matches);
			} else {
				error("No Solution matching: %s", id);
			}
		}
	}
	
	private void printMarketplace(List<Solution2> solutions) {
		for (Solution2 solution: solutions) {
			println("  %s%s/%s", solution.catalog!=null ? (solution.catalog.getId() + " "): "", solution.catalog!=null ? solution.catalog.getName() : "", solution.getName());
		}
	}

	//
	// Columns/Formatting
	//
	
	@Override
	protected String getDefaultFormat(Class<? extends Object> type) {
		if (Cluster.class.equals(type)) {
			return CLUSTER_DEFAULT_FORMAT;
		}
		if (Space.class.equals(type)) {
			return SPACE_DEFAULT_FORMAT;
		}
		if (Authority.class.equals(type)) {
			return AUTHORITY_DEFAULT_FORMAT;
		}
		if (Deployment.class.equals(type)) {
			return DEPLOYMENT_DEFAULT_FORMAT;
		}
		if (Job.class.equals(type)) {
			return JOB_DEFAULT_FORMAT;
		}
		if (CronJob.class.equals(type)) {
			return CRONJOB_DEFAULT_FORMAT;
		}
		if (Domain.class.equals(type)) {
			return DOMAIN_DEFAULT_FORMAT;
		}
		if (Registry.class.equals(type)) {
			return REGISTRY_DEFAULT_FORMAT;
		}
		if (Vcs.class.equals(type)) {
			return VCS_DEFAULT_FORMAT;
		}
		if (Catalog.class.equals(type)) {
			return CATALOG_DEFAULT_FORMAT;
		}
		if (Catalog.class.equals(type)) {
			return CATALOG_DEFAULT_FORMAT;
		}
		if (Solution.class.equals(type)) {
			return SOLUTION_DEFAULT_FORMAT;
		}
		if (CatalogSolution.class.equals(type)) {
			return CATALOG_SOLUTION_DEFAULT_FORMAT;
		}
		if (Binding.class.equals(type)) {
			return BINDING_DEFAULT_FORMAT;
		}
		if (Connector.class.equals(type)) {
			return CONNECTOR_DEFAULT_FORMAT;
		}
		if (Route.class.equals(type)) {
			return ROUTE_DEFAULT_FORMAT;
		}
		if (Mount.class.equals(type)) {
			return MOUNT_DEFAULT_FORMAT;
		}
		if (Variable.class.equals(type)) {
			return VAR_DEFAULT_FORMAT;
		}
		if (Pod.class.equals(type) || Instance.class.equals(type)) {
			return POD_DEFAULT_FORMAT;
		}
		if (ReplicaSet.class.equals(type)) {
			return REPLICASET_DEFAULT_FORMAT;
		}		
		if (VolumeClaim.class.equals(type)) {
			return VOLUMECLAIM_DEFAULT_FORMAT;
		}
		if (Event.class.equals(type)) {
			return EVENT_DEFAULT_FORMAT;
		}
		if (Build.class.equals(type)) {
			return BUILD_DEFAULT_FORMAT;
		}
		return super.getDefaultFormat(type);
	}

	@Override
	protected String getWideFormat(Class<? extends Object> type) {
		if (Cluster.class.equals(type)) {
			return CLUSTER_WIDE_FORMAT;
		}
		if (Space.class.equals(type)) {
			return SPACE_WIDE_FORMAT;
		}
		if (Authority.class.equals(type)) {
			return AUTHORITY_WIDE_FORMAT;
		}
		if (Deployment.class.equals(type)) {
			return DEPLOYMENT_WIDE_FORMAT;
		}
		if (Job.class.equals(type)) {
			return JOB_WIDE_FORMAT;
		}
		if (CronJob.class.equals(type)) {
			return CRONJOB_WIDE_FORMAT;
		}
		if (Domain.class.equals(type)) {
			return DOMAIN_WIDE_FORMAT;
		}
		if (Registry.class.equals(type)) {
			return REGISTRY_WIDE_FORMAT;
		}
		if (Vcs.class.equals(type)) {
			return VCS_WIDE_FORMAT;
		}
		if (Catalog.class.equals(type)) {
			return CATALOG_WIDE_FORMAT;
		}
		if (Catalog.class.equals(type)) {
			return CATALOG_WIDE_FORMAT;
		}
		if (Solution.class.equals(type)) {
			return SOLUTION_WIDE_FORMAT;
		}
		if (CatalogSolution.class.equals(type)) {
			return CATALOG_SOLUTION_WIDE_FORMAT;
		}
		if (Binding.class.equals(type)) {
			return BINDING_WIDE_FORMAT;
		}
		if (Connector.class.equals(type)) {
			return CONNECTOR_WIDE_FORMAT;
		}
		if (Route.class.equals(type)) {
			return ROUTE_WIDE_FORMAT;
		}
		if (Mount.class.equals(type)) {
			return MOUNT_WIDE_FORMAT;
		}
		if (Variable.class.equals(type)) {
			return VAR_WIDE_FORMAT;
		}
		if (Pod.class.equals(type) || Instance.class.equals(type)) {
			return POD_WIDE_FORMAT;
		}
		if (ReplicaSet.class.equals(type)) {
			return REPLICASET_WIDE_FORMAT;
		}		
		if (VolumeClaim.class.equals(type)) {
			return VOLUMECLAIM_WIDE_FORMAT;
		}
		if (Event.class.equals(type)) {
			return EVENT_WIDE_FORMAT;
		}
		if (Build.class.equals(type)) {
			return BUILD_WIDE_FORMAT;
		}

		return super.getWideFormat(type);
	}
	
	@Override
	protected String getCols(String fmt, String cols, Class<? extends Object> type) {
		if ("wide".equals(fmt)) {
			return getWideFormat(type);
		}

		if (Cluster.class.equals(type)) {
		} else if (Space.class.equals(type)) {
		} else if (Authority.class.equals(type)) {
		} else if (Deployment.class.equals(type)) {
			switch (cols) {
			case "cicd": case "build":
				return DEPLOYMENT_CICD_FORMAT;
			case "resources":
				return DEPLOYMENT_RESOURCES_FORMAT;
			}
		} else if (Job.class.equals(type)) {
			switch (cols) {
			case "cicd": case "build":
				return JOB_CICD_FORMAT;
			case "resources":
				return JOB_RESOURCES_FORMAT;				
			}
		} else if (CronJob.class.equals(type)) {
			switch (cols) {
			case "cicd": case "build":
				return CRONJOB_CICD_FORMAT;
			case "resources":
				return CRONJOB_RESOURCES_FORMAT;
			}
		} else if (Domain.class.equals(type)) {
		} else if (Registry.class.equals(type)) {
		} else if (Vcs.class.equals(type)) {
		} else if (Catalog.class.equals(type)) {
		} else if (Catalog.class.equals(type)) {
		} else if (Solution.class.equals(type)) {
		} else if (CatalogSolution.class.equals(type)) {
		} else if (Binding.class.equals(type)) {
		} else if (Connector.class.equals(type)) {
		} else if (Route.class.equals(type)) {
		} else if (Mount.class.equals(type)) {
		} else if (ReplicaSet.class.equals(type)) {
		} else if (VolumeClaim.class.equals(type)) {
		} else if (Build.class.equals(type)) {
		}
		if (cols!=null && !cols.isEmpty()) {
			return cols;
		}
		return getDefaultFormat(type);
	}
	
	protected String[] getFormats(Class<? extends Object> type) {
		if (Cluster.class.equals(type)) {
			return new String[] {};
		}
		if (Space.class.equals(type)) {
			return new String[] {};
		}
		if (Authority.class.equals(type)) {
			return new String[] {};
		}
		if (Deployment.class.equals(type)) {
			return c("cicd","build", "resources");
		}
		if (Job.class.equals(type)) {
			return new String[] {};
		}
		if (CronJob.class.equals(type)) {
			return new String[] {};
		}
		if (Domain.class.equals(type)) {
			return new String[] {};
		}
		if (Registry.class.equals(type)) {
			return new String[] {};
		}
		if (Vcs.class.equals(type)) {
			return new String[] {};
		}
		if (Catalog.class.equals(type)) {
			return new String[] {};
		}
		if (Solution.class.equals(type)) {
			return new String[] {};
		}
		if (CatalogSolution.class.equals(type)) {
			return new String[] {};
		}
		if (Binding.class.equals(type)) {
			return new String[] {};
		}
		if (Connector.class.equals(type)) {
			return new String[] {};
		}
		if (Route.class.equals(type)) {
			return new String[] {};
		}
		if (Mount.class.equals(type)) {
			return new String[] {};
		}
		if (Variable.class.equals(type)) {
			return new String[] {};
		}
		if (Pod.class.equals(type) || Instance.class.equals(type)) {
			return new String[] {};
		}
		if (ReplicaSet.class.equals(type)) {
			return new String[] {};
		}
		if (VolumeClaim.class.equals(type)) {
			return new String[] {};
		}
		if (Build.class.equals(type)) {
			return new String[] {};
		}
		return null;
	}

	@Override
	protected String argPID(Map<String, Object> options) {
		return argNS(options);
	}

	protected String argNS(Map<String, Object> options) {
		return argNS(options, false);
	}

	protected String argNS(Map<String, Object> options, boolean required) {
		String spaceId = (String)options.get("n");
		if (spaceId==null) {
			spaceId = this.space;
		}
		if (required && spaceId==null) {
			missingSpaceId();
			exit(-1);
		}
		return spaceId;
	}


	protected String argCluster(Map<String, Object> options) {
		String clusterId = (String)options.get("c");
		if (clusterId!=null) {
			return clusterId;
		}
		return this.cluster;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Instance extends Pod {
	}
	
	protected void missingSpaceId() {
		error(String.format("missing space id"));
		exit(-1);
	}
	
	protected String extractSpaceId(URI uri) {
		String s = uri.toString();
		int i = s.indexOf("/space/");
		if (i<0) {
			return null;
		}
		s = s.substring(i + "/space".length());
		i = s.indexOf("/");
		if (i>0) {
			s = s.substring(0, i);
		}
		return s;
	}

}