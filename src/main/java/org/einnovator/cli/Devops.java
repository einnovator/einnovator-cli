package org.einnovator.cli;

import static  org.einnovator.util.MappingUtils.updateObjectFrom;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.einnovator.devops.client.DevopsClient;
import org.einnovator.devops.client.config.DevopsClientConfiguration;
import org.einnovator.devops.client.model.Binding;
import org.einnovator.devops.client.model.Catalog;
import org.einnovator.devops.client.model.Cluster;
import org.einnovator.devops.client.model.Connector;
import org.einnovator.devops.client.model.CronJob;
import org.einnovator.devops.client.model.Deployment;
import org.einnovator.devops.client.model.Domain;
import org.einnovator.devops.client.model.Instance;
import org.einnovator.devops.client.model.Job;
import org.einnovator.devops.client.model.Mount;
import org.einnovator.devops.client.model.Pod;
import org.einnovator.devops.client.model.Registry;
import org.einnovator.devops.client.model.Resources;
import org.einnovator.devops.client.model.Route;
import org.einnovator.devops.client.model.Solution;
import org.einnovator.devops.client.model.Space;
import org.einnovator.devops.client.model.Variable;
import org.einnovator.devops.client.model.Vcs;
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
import org.einnovator.devops.client.modelx.ExecOptions;
import org.einnovator.devops.client.modelx.InstallOptions;
import org.einnovator.devops.client.modelx.JobFilter;
import org.einnovator.devops.client.modelx.JobOptions;
import org.einnovator.devops.client.modelx.LogOptions;
import org.einnovator.devops.client.modelx.RegistryFilter;
import org.einnovator.devops.client.modelx.RegistryOptions;
import org.einnovator.devops.client.modelx.SolutionFilter;
import org.einnovator.devops.client.modelx.SolutionOptions;
import org.einnovator.devops.client.modelx.SpaceFilter;
import org.einnovator.devops.client.modelx.SpaceOptions;
import org.einnovator.devops.client.modelx.VcsFilter;
import org.einnovator.devops.client.modelx.VcsOptions;
import org.einnovator.util.PageOptions;
import org.einnovator.util.UriUtils;
import org.einnovator.util.web.RequestOptions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;


@Component
public class Devops extends CommandRunnerBase {
	public static final String DEVOPS_NAME = "devops";

	public static final String DEVOPS_DEFAULT_SERVER = "http://localhost:2500";
	public static final String DEVOPS_MONITOR_SERVER = "http://localhost:2501";

	private static final String CLUSTER_DEFAULT_FORMAT = "id,name,displayName,provider,region";
	private static final String CLUSTER_WIDE_FORMAT = "id,name,displayName,provider,region";

	private static final String SPACE_DEFAULT_FORMAT = "id,name,displayName,cluster.name:cluster,cluster.provider:provider,cluster.region:region";
	private static final String SPACE_WIDE_FORMAT = "id,name,displayName,cluster.name:cluster,cluster.provider:provider,cluster.region:region";

	private static final String DEPLOYMENT_DEFAULT_FORMAT = "id,name,displayName,kind,status,availableReplicas:available,desiredReplicas:desired,readyReplicas:ready";
	private static final String DEPLOYMENT_WIDE_FORMAT = "id,name,displayName,kind,type,category,status,availableReplicas:available,desiredReplicas:desired,readyReplicas:ready,image.name:image";
	private static final String DEPLOYMENT_CICD_FORMAT = "id,name,displayName,repositories.url:git,buildImage.name:image,buildImage.registry.name:registry,builder,builderKind,workspace:workspace,webhook:webhook";

	private static final String JOB_DEFAULT_FORMAT = "id,name,displayName,status";
	private static final String JOB_WIDE_FORMAT = "id,name,displayName,status,completions,parallelism,backoffLimit,manualSelector,ttlSecondsAfterFinished";

	private static final String CRONJOB_DEFAULT_FORMAT = "id,name,displayName,status,suspend";
	private static final String CRONJOB_WIDE_FORMAT = "id,name,displayName,status,suspend,schedule,lastScheduleTime,backoffLimit";

	private static final String DOMAIN_DEFAULT_FORMAT ="id,name,tls";
	private static final String DOMAIN_WIDE_FORMAT ="id,name,tls,enabled";

	private static final String REGISTRY_DEFAULT_FORMAT = "id,name,server,username";
	private static final String REGISTRY_WIDE_FORMAT = "id,name,server,username";

	private static final String VCS_DEFAULT_FORMAT = "id,name,url,username";
	private static final String VCS_WIDE_FORMAT = "id,name,url,username";

	private static final String CATALOG_DEFAULT_FORMAT = "id,name,type,enabled";
	private static final String CATALOG_WIDE_FORMAT = "id,name,type,enabled";

	private static final String SOLUTION_DEFAULT_FORMAT = "id,name,type,kind,category,keywords";
	private static final String SOLUTION_WIDE_FORMAT = "id,name,type,kind,category,keywords,url";

	private static final String BINDING_DEFAULT_FORMAT = "selector";
	private static final String BINDING_WIDE_FORMAT = "selector";

	private static final String CONNECTOR_DEFAULT_FORMAT = "id,name";
	private static final String CONNECTOR_WIDE_FORMAT = "id,name";

	private static final String ROUTE_DEFAULT_FORMAT = "id,host,dns,domain.dns:domain,tls";
	private static final String ROUTE_WIDE_FORMAT = "id,host,dns,domain.dns:domain,tls";

	private static final String MOUNT_DEFAULT_FORMAT = "id,name,type,mountPath";
	private static final String MOUNT_WIDE_FORMAT = "id,name,type,mountPath";

	private static final String VAR_DEFAULT_FORMAT = "id,name,type,value,configMap,secret";
	private static final String VAR_WIDE_FORMAT = "id,name,type,value,configMap,secret";

	private static final String POD_DEFAULT_FORMAT = "name,status,restarts,creationDateFormatted:age";
	private static final String POD_WIDE_FORMAT = "name,status,restarts,creationDateFormatted:age,ip,node";

	private DevopsClient devopsClient;

	private String server = DEVOPS_DEFAULT_SERVER;
	
	private String cluster;

	private String space;

	private DevopsClientConfiguration config = new DevopsClientConfiguration();

	@Override
	public void init(String[] cmds, Map<String, Object> options, OAuth2RestTemplate template, boolean interactive, ResourceBundle bundle) {
		if (!init) {
			super.init(cmds, options, template, interactive, bundle);
			updateObjectFrom(config, convert(options, DevopsClientConfiguration.class));
			config.setServer(server);
			devopsClient = new DevopsClient(template, config);
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
	
	@Override
	public String getName() {
		return DEVOPS_NAME;
	}
	
	@Override
	public Map<String, Object> getSettings() {
		Map<String, Object> settings = new LinkedHashMap<>();
		settings.put("cluster", cluster);
		settings.put("space", space);
		return settings;
	}

	@Override
	public void loadSettings(Map<String, Object> settings) {
		cluster = get("cluster", settings, cluster);
		space = get("space", settings, space);
	}

	static String[][] DEVOPS_COMMANDS = c(
		c("cluster", "clusters"),
		c("space", "spaces", "namespace", "namespaces", "ns"),
		c("deployment", "deploy", "deployments", "deploys"),
		c("job", "jobs"),
		c("cronjob", "cronjobs"),
		c("domain", "domains"),
		c("registry", "registries"),
		c("vcs", "vcss", "git"),
		c("catalog", "catalogs"),
		c("solution", "solutions"),
		c("marketplace", "market"),
		c("ps"),
		c("kill"),
		c("run"),
		c("install")
	);
	
	@Override
	protected String[][] getCommands() {
		return DEVOPS_COMMANDS;
	}

	static Map<String, String[][]> subcommands;
	
	static {
		Map<String, String[][]> map = new LinkedHashMap<>();
		subcommands = map;
		map.put("cluster", c(c("ls", "list", ""), c("get"), c("schema", "meta"), 
			c("create", "import"), c("update"), c("delete", "del", "rm"),
			c("set"), c("unset"),
			c("help")));
		map.put("space", c(c("ls", "list", ""), c("get"), c("schema", "meta"), 
			c("create"), c("update"), c("delete"), c("del"), c("rm"), 
			c("attach"),
			c("set"), c("unset"),
			c("help")));
		map.put("deployment", c(c("ls", "list", "ps", ""), c("get"), c("schema", "meta"), 
			c("create"), c("update"), c("delete", "del", "rm"), 
			c("scale"), c("resources", "rscale"), c("start"), c("stop"), c("restart"), c("sync"), c("attach"), c("exec"), c("logs", "log"),
			c("pod", "pods", "instances", "instance", "replica", "replicas"),
			c("route"), c("mount"), c("env", "var"), c("binding"), c("connector"),
			c("help")));
		map.put("job", c(c("ls", "list", "ps", ""), c("get"), c("schema", "meta"), 
			c("create"), c("update"), c("delete", "del", "rm"), 
			c("resources", "rscale"), c("start"), c("stop"), c("restart"), c("sync"), c("exec"), c("logs", "log"),
			c("pod", "pods", "instances", "instance", "replica", "replicas"),
			c("mount"), c("env", "var"), c("binding"),
			c("help")));
		map.put("cronjob", c(c("ls", "list", "ps", ""), c("get"), c("schema", "meta"), 
			c("create"), c("update"), c("delete", "del", "rm"),
			c("job", "jobs"),
			c("resources", "rscale"), c("start"), c("stop"), c("suspend"), c("restart"), c("sync"),
			c("pod", "pods", "instances", "instance", "replica", "replicas"),
			c("mount"), c("env", "var"), c("binding"),
			c("help")));			
		map.put("domain", c(c("ls", "list", ""), c("get"), c("schema", "meta"), 
			c("create"), c("update"), c("delete", "del", "rm"), c("help")));
		map.put("registry", c(c("ls", "list", ""), c("get"), c("schema", "meta"), 
			c("create"), c("update"), c("delete", "del", "rm"), c("help")));
		map.put("vcs", c(c("ls", "list", ""), c("get"), c("schema", "meta"), 
			c("create"), c("update"), c("delete", "del", "rm"), c("help")));
		map.put("catalog", c(c("ls", "list", ""), c("get"), c("schema", "meta"), 
				c("create"), c("update"), c("delete", "del", "rm"), 
				c("solution", "solutions"),
				c("help")));
		map.put("solution", c(c("ls", "list", ""), c("get"), c("schema", "meta"), 
			c("create"), c("update"), c("delete", "del", "rm"), c("help")));
		map.put("marketplace", c(c("", "ls", "list"), c("help")));

	}
	
	@Override
	protected Map<String, String[][]> getSubCommands() {
		return subcommands;
	}
	
	private static Map<String, Map<String, String[][]>> subsubcommands;
	
	static {
		Map<String, Map<String, String[][]>> map = new LinkedHashMap<>();
		subsubcommands = map;
		Map<String, String[][]> deploy = m("deployment", map);
		deploy.put("route", c(c("ls", "list", ""), c("get"), c("schema", "meta"), 
			c("add", "create"), c("update"), c("delete", "del", "rm"), c("help")));
		deploy.put("env", c(c("ls", "list", ""), c("get"), c("schema", "meta"), 
			c("add", "create"), c("update"), c("delete", "del", "rm"), c("help")));
		deploy.put("mount", c(c("ls", "list", ""), c("get"), c("schema", "meta"), 
			c("add", "create"), c("update"), c("delete", "del", "rm"), c("help")));
		deploy.put("binding", c(c("ls", "list", ""), c("get"), c("schema", "meta"), 
			c("add", "create"), c("update"), c("delete", "del", "rm"), c("help")));
		deploy.put("connector", c(c("ls", "list", ""), c("get"), c("schema", "meta"), 
			c("add", "create"), c("update"), c("delete", "del", "rm"), c("help")));
		deploy.put("pod", c(c("ls", "list", ""), c("kill", "delete", "del", "rm"), c("help")));

		Map<String, String[][]> job = m("job", map);
		job.put("env", c(c("ls", "list", ""), c("get"), c("schema", "meta"), 
				c("add", "create"), c("update"), c("delete", "del", "rm"), c("help")));
		job.put("mount", c(c("ls", "list", ""), c("get"), c("schema", "meta"), 
				c("add", "create"), c("update"), c("delete", "del", "rm"), c("help")));
		job.put("binding", c(c("ls", "list", ""), c("get"), c("schema", "meta"), 
				c("add", "create"), c("update"), c("delete", "del", "rm"), c("help")));
		job.put("pod", c(c("ls", "list", ""), c("kill", "delete", "del", "rm"), c("help")));

		Map<String, String[][]> cronjob = m("cronjob", map);
		cronjob.put("env", c(c("ls", "list", ""), c("get"), c("schema", "meta"), 
				c("add", "create"), c("update"), c("delete", "del", "rm"), c("help")));
		cronjob.put("mount", c(c("ls", "list", ""), c("get"), c("schema", "meta"), 
				c("add", "create"), c("update"), c("delete", "del", "rm"), c("help")));
		cronjob.put("binding", c(c("ls", "list", ""), c("get"), c("schema", "meta"), 
				c("add", "create"), c("update"), c("delete", "del", "rm"), c("help")));
		cronjob.put("job", c(c("ls", "list", ""), c("help")));

	}
	
	@Override
	protected Map<String, Map<String, String[][]>> getSubSubCommands() {
		return subsubcommands;
	}

	public void run(String type, String op, String[] cmds, Map<String, Object> options) {		
		switch (type) {
		case "help": case "":
			printUsage();
			break;
		case "ps": 
			ps(type, op, cmds, options);
		case "kill": 
			kill(type, op, cmds, options);
		case "run": 
			runop(type, op, cmds, options);
		case "install": 
			install(type, op, cmds, options);
		case "cluster": case "clusters": 
			switch (op) {
			case "help": case "":
				printUsage("cluster");
				break;
			case "get": 
				getCluster(type, op, cmds, options);
				break;
			case "ls": case "list":
				listCluster(type, op, cmds, options);
				break;
			case "schema": case "meta":
				schemaCluster(type, op, cmds, options);
				break;
			case "set":
				setCluster(type, op, cmds, options);
				break;
			case "unset":
				unsetCluster(type, op, cmds, options);
				break;				
			case "create": case "import":
				createCluster(type, op, cmds, options);
				break;
			case "update": 
				updateCluster(type, op, cmds, options);
				break;
			case "delete": case "del": case "rm":
				deleteCluster(type, op, cmds, options);
				break;
			default: 
				invalidOp(type, op);
			}
			break;
		case "space": case "spaces": case "g":
			switch (op) {
			case "help": case "":
				printUsage("space");
				break;
			case "get": 
				getSpace(type, op, cmds, options);
				break;
			case "ls": case "list":
				listSpace(type, op, cmds, options);
				break;
			case "set":
				setSpace(type, op, cmds, options);
				break;
			case "unset":
				unsetSpace(type, op, cmds, options);
				break;
			case "schema": case "meta":
				schemaSpace(type, op, cmds, options);
				break;
			case "create": 
				createSpace(type, op, cmds, options);
				break;
			case "update": 
				updateSpace(type, op, cmds, options);
				break;
			case "delete": case "del": case "rm":
				deleteSpace(type, op, cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "deployment": case "deploy": case "deploys": case "deployments":
			switch (op) {
			case "help": case "":
				printUsage("deployment");
				break;
			case "get": 
				getDeployment(type, op, cmds, options);
				break;
			case "ls": case "list": case "ps":
				listDeployment(type, op, cmds, options);
				break;
			case "schema": case "meta":
				schemaDeployment(type, op, cmds, options);
				break;
			case "create": 
				createDeployment(type, op, cmds, options);
				break;
			case "update": 
				updateDeployment(type, op, cmds, options);
				break;
			case "delete": case "del": case "rm": case "kill":
				deleteDeployment(type, op, cmds, options);
				break;
			case "scale":
				scaleDeployment(type, op, cmds, options);
				break;
			case "resouces": case "rscale":
				resourcesDeployment(type, op, cmds, options);
				break;
			case "start":
				startDeployment(type, op, cmds, options);
				break;
			case "stop":
				stopDeployment(type, op, cmds, options);
				break;
			case "restart":
				restartDeployment(type, op, cmds, options);
				break;
			case "sync":
				syncDeployment(type, op, cmds, options);
				break;
			case "exec":
				execDeployment(type, op, cmds, options);
				break;
			case "logs": case "log":
				logDeployment(type, op, cmds, options);
				break;
			case "pod": case "pods": case "instances": case "instance": case "replica": case "replicas": 
				instancesDeployment(type, op, cmds, options);
				break;
			case "route":
				routeDeployment(type, op, cmds, options);
				break;
			case "mount":
				mountDeployment(type, op, cmds, options);
				break;
			case "var": case "env":
				varDeployment(type, op, cmds, options);
				break;
			case "binding": case "bind":
				bindingDeployment(type, op, cmds, options);
				break;
			case "connector": case "conn":
				connectorDeployment(type, op, cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;			
		case "job":
			switch (op) {
			case "help": case "":
				printUsage("job");
				break;
			case "get": 
				getJob(type, op, cmds, options);
				break;
			case "ls": case "list": case "ps":
				listJob(type, op, cmds, options);
				break;
			case "schema": case "meta":
				schemaJob(type, op, cmds, options);
				break;
			case "create": 
				createJob(type, op, cmds, options);
				break;
			case "update": 
				updateJob(type, op, cmds, options);
				break;
			case "delete": case "del": case "rm": case "kill":
				deleteJob(type, op, cmds, options);
				break;
			case "pod": case "pods": case "instances": case "instance": case "replica": case "replicas": 
				instancesJob(type, op, cmds, options);
				break;
			case "resouces": case "rscale":
				resourcesJob(type, op, cmds, options);
				break;
			case "start":
				startJob(type, op, cmds, options);
				break;
			case "stop":
				stopJob(type, op, cmds, options);
				break;
			case "restart":
				restartJob(type, op, cmds, options);
				break;
			case "sync":
				syncJob(type, op, cmds, options);
				break;
			case "exec":
				execJob(type, op, cmds, options);
				break;
			case "logs": case "log":
				logJob(type, op, cmds, options);
				break;
			case "mount":
				mountJob(type, op, cmds, options);
				break;
			case "var": case "env":
				varJob(type, op, cmds, options);
				break;
			case "binding": case "bind":
				bindingJob(type, op, cmds, options);
				break;				
			default: 
				invalidOp(type, op);
				break;
			}
			break;			
		case "cronjob":
			switch (op) {
			case "help": case "":
				printUsage("cronjob");
				break;
			case "get": 
				getCronJob(type, op, cmds, options);
				break;
			case "ls": case "list": case "ps":
				listCronJob(type, op, cmds, options);
				break;
			case "schema": case "meta":
				schemaCronJob(type, op, cmds, options);
				break;
			case "create": 
				createCronJob(type, op, cmds, options);
				break;
			case "update": 
				updateCronJob(type, op, cmds, options);
				break;
			case "delete": case "del": case "rm": case "kill":
				deleteCronJob(type, op, cmds, options);
				break;
			case "jobs": case "job":
				jobsCronJob(type, op, cmds, options);
				break;
			case "resouces": case "rscale":
				resourcesCronJob(type, op, cmds, options);
				break;
			case "start":
				startCronJob(type, op, cmds, options);
				break;
			case "stop":
				stopCronJob(type, op, cmds, options);
				break;
			case "suspend":
				suspendCronJob(type, op, cmds, options);
				break;
			case "restart":
				restartCronJob(type, op, cmds, options);
				break;
			case "sync":
				syncCronJob(type, op, cmds, options);
				break;
			case "mount":
				mountCronJob(type, op, cmds, options);
				break;
			case "var": case "env":
				varCronJob(type, op, cmds, options);
				break;
			case "binding": case "bind":
				bindingCronJob(type, op, cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;			
		case "domain": case "domains":
			switch (op) {
			case "help": case "":
				printUsage("domain");
				break;
			case "get": 
				getDomain(type, op, cmds, options);
				break;
			case "ls": case "list":
				listDomain(type, op, cmds, options);
				break;
			case "schema": case "meta":
				schemaDomain(type, op, cmds, options);
				break;
			case "create": 
				createDomain(type, op, cmds, options);
				break;
			case "update": 
				updateDomain(type, op, cmds, options);
				break;
			case "delete": case "del": case "rm":
				deleteDomain(type, op, cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "registry": case "registries": case "r":
			switch (op) {
			case "help": case "":
				printUsage("registry");
				break;
			case "get": 
				getRegistry(type, op, cmds, options);
				break;
			case "ls": case "list":
				listRegistry(type, op, cmds, options);
				break;
			case "schema": case "meta":
				schemaRegistry(type, op, cmds, options);
				break;
			case "create": 
				createRegistry(type, op, cmds, options);
				break;
			case "update": 
				updateRegistry(type, op, cmds, options);
				break;
			case "delete": case "del": case "rm":
				deleteRegistry(type, op, cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "vcs": case "vcss": case "git":
			switch (op) {
			case "help": case "":
				printUsage("vcs");
				break;
			case "get": 
				getVcs(type, op, cmds, options);
				break;
			case "ls": case "list":
				listVcs(type, op, cmds, options);
				break;
			case "schema": case "meta":
				schemaVcs(type, op, cmds, options);
				break;
			case "create": 
				createVcs(type, op, cmds, options);
				break;
			case "update": 
				updateVcs(type, op, cmds, options);
				break;
			case "delete": case "del": case "rm":
				deleteVcs(type, op, cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "catalog": case "catalogs":
			switch (op) {
			case "help": case "":
				printUsage("catalog");
				break;
			case "get": 
				getCatalog(type, op, cmds, options);
				break;
			case "ls": case "list":
				listCatalog(type, op, cmds, options);
				break;
			case "schema": case "meta":
				schemaCatalog(type, op, cmds, options);
				break;
			case "create": 
				createCatalog(type, op, cmds, options);
				break;
			case "update": 
				updateCatalog(type, op, cmds, options);
				break;
			case "delete": case "del": case "rm":
				deleteCatalog(type, op, cmds, options);
				break;
			case "solution": case "solutions":
				listSolutionsFor(type, op, cmds, options);
				break;
			case "install":
				installFromCatalog(type, op, cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "solution": case "solutions":
			switch (op) {
			case "help": case "":
				printUsage("solution");
				break;
			case "get": 
				getSolution(type, op, cmds, options);
				break;
			case "ls": case "list":
				listSolution(type, op, cmds, options);
				break;
			case "schema": case "meta":
				schemaSolution(type, op, cmds, options);
				break;
			case "create": 
				createSolution(type, op, cmds, options);
				break;
			case "update": 
				updateSolution(type, op, cmds, options);
				break;
			case "delete": case "del": case "rm":
				deleteSolution(type, op, cmds, options);
				break;
			case "install":
				installSolution(type, op, cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "marketplace": case "market":
			switch (op) {
			case "help": case "":
				printUsage("marketplace");
				break;
			case "ls": case "list":
				listMarketplace(type, op, cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
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
	
	public void listCluster(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("cluster", "ls")) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		ClusterFilter filter = convert(options, ClusterFilter.class);
		debug("Clusters: %s %s", filter, pageable);
		Page<Cluster> clusters = devopsClient.listClusters(filter, pageable);
		print(clusters, Cluster.class);
	}


	public void getCluster(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("cluster", "get")) {
			return;
		}
		String clusterId = argId(op, cmds);
		ClusterOptions options_ = convert(options, ClusterOptions.class);
		debug("Cluster: %s", clusterId);
		Cluster cluster = devopsClient.getCluster(clusterId, options_);
		printObj(cluster);
	}

	public void schemaCluster(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("cluster", "schema")) {
			return;
		}
		printLine(schemaToString(Cluster.class));
	}
	
	public void setCluster(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("cluster", "set")) {
			return;
		}
		String clusterId = argId(op, cmds);
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
	
	public void unsetCluster(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("cluster", "unset")) {
			return;
		}
		String clusterId = argId(op, cmds);
		debug("Unset Cluster: %s", clusterId);
		this.cluster = null;
		this.space = null;
		writeConfig();
	}

	public void createCluster(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("cluster", "create")) {
			return;
		}
		Cluster cluster = convert(options, Cluster.class);
		cluster.setName(argName(op, cmds));
		debug("Creating Cluster: %s", cluster);
		URI uri = devopsClient.createCluster(cluster, null);
		if (isEcho()) {
			printLine("Cluster URI:", uri);
			String id = UriUtils.extractId(uri);
			Cluster cluster2 = devopsClient.getCluster(id, null);
			printObj(cluster2);
		}
	}

	
	public void updateCluster(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("cluster", "update")) {
			return;
		}
		String clusterId = argId(op, cmds);
		Cluster cluster = convert(options, Cluster.class);
		setId(cluster, clusterId);
		debug("Updating Cluster: %s %s", clusterId, cluster);
		devopsClient.updateCluster(cluster, null);
		if (isEcho()) {
			Cluster cluster2 = devopsClient.getCluster(clusterId, null);
			printObj(cluster2);			
		}
	}

	public void deleteCluster(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("cluster", "delete")) {
			return;
		}
		String clusterId = argId(op, cmds);
		debug("Deleting Cluster: %s", clusterId);		
		devopsClient.deleteCluster(clusterId, null);	
	}

	//
	// Spaces
	//
	
	public void listSpace(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("space", "ls")) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		SpaceFilter filter = convert(options, SpaceFilter.class);
		debug("Spaces: %s %s", filter, pageable);
		Page<Space> spaces = devopsClient.listSpaces(filter, pageable);
		print(spaces, Space.class);
	}
	
	public void getSpace(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("space", "get")) {
			return;
		}
		String spaceId = argId(op, cmds);
		SpaceOptions options_ = convert(options, SpaceOptions.class);
		debug("Space: %s", spaceId);
		Space space = devopsClient.getSpace(spaceId, options_);
		printObj(space);
	}
	
	public void setSpace(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("space", "set")) {
			return;
		}
		String spaceId = argId(op, cmds);
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
	
	public void unsetSpace(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("space", "unset")) {
			return;
		}
		String spaceId = argId(op, cmds);
		debug("Unset Space: %s", spaceId);
		this.space = null;
		writeConfig();
	}
	
	public void schemaSpace(String type, String op, String[] cmds, Map<String, Object> options) {
		printLine(schemaToString(Space.class));
	}
	
	public void createSpace(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("space", "create")) {
			return;
		}
		Space space = convert(options, Space.class);
		space.setName(argName(op, cmds));
		debug("Creating Space: %s", space);
		printObj(space);
		URI uri = devopsClient.createSpace(space, null);
		if (isEcho()) {
			printLine("Space URI:", uri);
			String spaceId = UriUtils.extractId(uri);
			Space space2 = devopsClient.getSpace(spaceId, null);
			printObj(space2);			
		}
	}

	public void updateSpace(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("space", "update")) {
			return;
		}
		String spaceId = argIdx(op, cmds);
		Space space = convert(options, Space.class);
		debug("Updating Space: %s %s", spaceId, space);
		setId(space, spaceId);
		devopsClient.updateSpace(space, null);
		if (isEcho()) {
			Space space2 = devopsClient.getSpace(spaceId, null);
			debug("Updated Space: %s", spaceId);
			printObj(space2);
		}
	}
	
	public void deleteSpace(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("space", "delete")) {
			return;
		}
		String spaceId = argIdx(op, cmds);
		debug("Deleting Space: %s", spaceId);		
		devopsClient.deleteSpace(spaceId, null);		
	}
		
	
	//
	// Generic
	//

	public void ps(String type, String op, String[] cmds, Map<String, Object> options) {
		String[] ops = op.split(",");
		for (String op1: ops) {
			switch (op1) {
			case "": case "deployment": case "deploy": case "deployments": case "deploys":
				if (isHelp("ps", "deployment")) {
					return;
				}
				listDeployment(type, "deployment", cmds, options);
				break;
			case "job": case "jobs":
				if (isHelp("ps", "job")) {
					return;
				}
				listJob(type, op1, cmds, options);
				break;
			case "cronjob": case "cronjobs":
				if (isHelp("ps", "cronjob")) {
					return;
				}
				listCronJob(type, op, cmds, options);
				break;
			default:
				invalidOp(type, op);
				break;
			}			
		}
	}

	public void kill(String type, String op, String[] cmds, Map<String, Object> options) {
		if (op==null || op.isEmpty()) {
			error(String.format("missing resource id"));
			exit(-1);
			return;
		}
		String[] ops = op.split(",");
		for (String op1: ops) {
			int i = op1.indexOf(":");
			String id = null;
			if (i<0) {
				id = op1;
				op1 = "deploy";
			} else if (i==op.length()-1) {
				error(String.format("missing resource id"));
				exit(-1);
				return;
			} else if (i==0) {
				id = op1.substring(1);
				op1 = "deploy";
			} else {
				id = op1.substring(i+1);
				op1 = op1.substring(0, i);
			}		
			switch (op1) {
			case "": case "deployment": case "deploy": case "deployments": case "deploys":
				if (isHelp("kill", "deployment")) {
					return;
				}
				deleteDeployment(id, options);
			case "job": case "jobs":
				if (isHelp("kill", "job")) {
					return;
				}
				deleteJob(id, options);
			case "cronjob": case "cronjobs":
				if (isHelp("kill", "cronjob")) {
					return;
				}
				deleteCronJob(id, options);
			default:
				error(String.format("missing resource type"));
				exit(-1);
				break;
			}
		}
	}

	public void runop(String type, String op, String[] cmds, Map<String, Object> options) {
		if (op==null || op.isEmpty()) {
			error(String.format("missing resource id"));
			exit(-1);
			return;
		}
		String[] ops = op.split(",");
		for (String op1: ops) {
			int i = op1.indexOf(":");
			String id = null;
			if (i<0) {
				id = op1;
				op1 = "deploy";
			} else if (i==op.length()-1) {
				error(String.format("missing resource id"));
				exit(-1);
				return;
			} else if (i==0) {
				id = op1.substring(1);
				op1 = "deploy";
			} else {
				id = op1.substring(i+1);
				op1 = op1.substring(0, i);
			}		
			if (op1.isEmpty()) {
				op1 = "deployment";
			}
			if (id==null || id.trim().isEmpty()) {
				error(String.format("missing resource id"));
				exit(-1);
				return;
			}
			switch (op1) {
			case "deployment": case "deploy": case "deployments": case "deploys":
				if (isHelp("run", "deployment")) {
					return;
				}
				createDeployment(true, "run", op1, c(id), options);
			case "job": case "jobs":
				if (isHelp("run", "job")) {
					return;
				}
				createJob(true,  "run", op1, c(id), options);
			case "cronjob": case "cronjobs":
				if (isHelp("run", "cronjob")) {
					return;
				}
				createCronJob(true,  "run", op1, c(id), options);
			default:
				error(String.format("missing resource type"));
				exit(-1);
				break;
			}
		}
	}
	
	public void install(String type, String op, String[] cmds, Map<String, Object> options) {
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
		} else {
			id = op.substring(i+1);
			catalogId = op.substring(0, i);
		}	
		if (id==null || id.trim().isEmpty()) {
			error(String.format("missing solution id"));
			exit(-1);
			return;
		}
		if (isHelp("install", "")) {
			return;
		}
		if (catalogId!=null) {
			installFromCatalog(type, op, cmds, options);;
		} else {
			installSolution(type, op, cmds, options);
		}
	}
	
	//
	// Deployments
	//

	public void listDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("deployment", "ls")) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		DeploymentFilter filter = convert(options, DeploymentFilter.class);
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingArg(type, op, "-n");
			return;
		}
		debug("Deployments: %s %s %s", spaceId, filter, pageable);
		Page<Deployment> deployments = devopsClient.listDeployments(spaceId, filter, pageable);
		print(deployments, Deployment.class);
	}
	
	
	public void getDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("deployment", "get")) {
			return;
		}
		String deployId = argIdx(op, cmds);
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		debug("Get Deployment: %s", deployId);
		Deployment deployment = devopsClient.getDeployment(deployId, options_);
		printObj(deployment);
	}
	

	public void schemaDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("deployment", "schema")) {
			return;
		}
		printLine(schemaToString(Deployment.class));
	}

	public void createDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("deployment", "create")) {
			return;
		}
		String start = (String)options.get("--start");
		createDeployment(start!=null, type, op, cmds, options);
	}

	public void createDeployment(boolean start, String type, String op, String[] cmds, Map<String, Object> options) {
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingArg(type, op, "-n");
			return;
		}
		Deployment deployment = convert(options, Deployment.class);
		deployment.setName(argName(op, cmds));
		if (start) {
			deployment.setStart(true);
		}
		debug("Creating Deployment: %s", deployment);
		URI uri = devopsClient.createDeployment(spaceId, deployment, null);
		if (isEcho()) {
			printLine("Deployment URI:", uri);
			String deployId = UriUtils.extractId(uri);
			Deployment deployment2 = devopsClient.getDeployment(deployId, null);
			printObj(deployment2);
		}
	}

	public void updateDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("deployment", "update")) {
			return;
		}
		String deployId = argIdx(op, cmds);
		Deployment deployment = convert(options, Deployment.class);
		setId(deployment, deployId);
		debug("Updating Deployment: %s %s", deployId, deployment);
		RequestOptions options_ = convert(options, RequestOptions.class);
		devopsClient.updateDeployment(deployment, options_);
		Deployment deployment2 = devopsClient.getDeployment(deployId, null);
		printObj(deployment2);
	}
	
	public void deleteDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("deployment", "delete")) {
			return;
		}
		String deployId = argIdx(op, cmds);
		deleteDeployment(deployId, options);
	}
	
	public void deleteDeployment(String deployId, Map<String, Object> options) {
		debug("Deleting Deployment: %s", deployId);	
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		devopsClient.deleteDeployment(deployId, options_);		
	}
	
	public void scaleDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("deployment", "scale")) {
			return;
		}
		String deployId = argIdx(op, cmds);
		debug("Scaling Deployment: %s", deployId);		
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		String n = get(new String[] {"n", "replicas", "instances"}, options, String.class);
		if (n==null) {
			n = cmds.length>0 ? cmds[0] : null;
		}
		if (n==null) {
			error("Missing replica count...");
			exit(-1);
		}
		Integer n_ = parseInt(n);
		if (n_==null || n_<0) {
			error("Invalid replica count...");
			exit(-1);
		}
		if (n!=null) {
			devopsClient.scaleDeployment(deployId, n_, options_);			
		}
	}
	
	public void resourcesDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("deployment", "resources")) {
			return;
		}
		String deployId = argIdx(op, cmds);
		debug("Scaling Deployment: %s", deployId);		
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		Resources resources = convert(options, Resources.class);
		devopsClient.scaleDeployment(deployId, resources, options_);			
	}

	public void startDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("deployment", "start")) {
			return;
		}
		String deployId = argIdx(op, cmds);
		debug("Starting Deployment: %s", deployId);		
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		devopsClient.startDeployment(deployId, options_);			
	}

	public void stopDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("deployment", "stop")) {
			return;
		}
		String deployId = argIdx(op, cmds);
		debug("Stopping Deployment: %s", deployId);		
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		devopsClient.startDeployment(deployId, options_);			
	}

	public void restartDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("deployment", "restart")) {
			return;
		}
		String deployId = argIdx(op, cmds);
		debug("Restarting Deployment: %s", deployId);		
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		devopsClient.restartDeployment(deployId, options_);			
	}
	
	public void syncDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("deployment", "sync")) {
			return;
		}
		String deployId = argIdx(op, cmds);
		debug("Sync Deployment: %s", deployId);		
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		devopsClient.startDeployment(deployId, options_);			
	}

	public void execDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("deployment", "exec")) {
			return;
		}
		String deployId = argIdx(op, cmds);
		debug("Exec Deployment: %s", deployId);		
		ExecOptions options_ = convert(options, ExecOptions.class);
		String cmd = ""; //TODO
		options_.setCmd(cmd);
		devopsClient.execDeployment(deployId, options_);			
	}
	
	public void logDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("deployment", "logs")) {
			return;
		}
		String deployId = argIdx(op, cmds);
		debug("Log Deployment: %s", deployId);		
		LogOptions options_ = convert(options, LogOptions.class);
		String out = devopsClient.logDeployment(deployId, options_);			
		System.out.println(out);
	}
	
	public void instancesDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		switch (op2) {
		case "help": case "":
			printUsage("deployment", "pod");
			break;
		case "ls": case "list": {
			if (isHelp("deployment", "pod", "ls")) {
				return;
			}
			String deployId = argIdx1(op, cmds);
			debug("Instances of: %s", deployId);		
			DeploymentOptions options_ = convert(options, DeploymentOptions.class);
			List<Instance> instances = devopsClient.listInstances(deployId, options_);			
			print(instances);
			break;
		}
		case "kill": case "remove": case "rm": case "delete": case "del": {
			if (isHelp("deployment", "pod", "kill")) {
				return;
			}
			String deployId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			String pod = argId2(op, cmds, true);
			debug("Delete Instance: %s %s", deployId, pod);		
			devopsClient.deleteInstance(deployId, pod, options_);
			break;
		}
		}
	}
	
	public void routeDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		switch (op2) {
		case "help": case "":
			printUsage("deployment", "route");
			break;
		case "ls": case "list": {
			if (isHelp("deployment", "route", "ls")) {
				return;
			}
			String deployId = argIdx1(op, cmds);
			debug("Routes: %s", deployId);		
			DeploymentOptions options_ = convert(options, DeploymentOptions.class);
			List<Route> routes = devopsClient.listRoutes(deployId, options_);			
			print(routes);
			break;
		}
		case "add": case "create": {
			if (isHelp("deployment", "route", "add")) {
				return;
			}
			String deployId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			Route route = convert(options, Route.class);
			debug("Add Route: %s %s", deployId, route);		
			devopsClient.addRoute(deployId, route, options_);
			break;
		}
		case "remove": case "rm": case "delete": case "del": {
			if (isHelp("deployment", "route", "remove")) {
				return;
			}
			String deployId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			String routeId = argId1(op2, cmds);
			debug("Remove Route: %s %s", deployId, routeId);		
			devopsClient.removeRoute(deployId, routeId, options_);
			break;
		}
		case "update": {
			if (isHelp("deployment", "route", "update")) {
				return;
			}
			String deployId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			Route route = convert(options, Route.class);
			String routeId = argId1(op2, cmds);
			debug("Update Route: %s %s %s", deployId, routeId, route);		
			devopsClient.updateRoute(deployId, routeId, route, options_);
			break;
		}
		}
	}


	public void mountDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		switch (op2) {
		case "help": case "":
			printUsage("deployment", "mount");
			break;
		case "ls": case "list": {
			if (isHelp("deployment", "mount", "ls")) {
				return;
			}
			String deployId = argIdx1(op, cmds);
			debug("Mounts: %s", deployId);		
			DeploymentOptions options_ = convert(options, DeploymentOptions.class);
			List<Mount> mounts = devopsClient.listMounts(deployId, options_);			
			print(mounts);
			break;
		}
		case "add": case "create": {
			if (isHelp("deployment", "mount", "add")) {
				return;
			}
			String deployId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			Mount mount = convert(options, Mount.class);
			debug("Add Mount: %s %s", deployId, mount);		
			devopsClient.addMount(deployId, mount, options_);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			if (isHelp("deployment", "mount", "remove")) {
				return;
			}
			String deployId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			String mountId = argId1(op2, cmds);
			debug("Remove Mount: %s %s", deployId, mountId);		
			devopsClient.removeMount(deployId, mountId, options_);
			break;
		}
		case "update": {
			if (isHelp("deployment", "mount", "update")) {
				return;
			}
			String deployId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			Mount mount = convert(options, Mount.class);
			String mountId = argId1(op2, cmds);
			debug("Update Mount: %s %s %s", deployId, mountId, mount);		
			devopsClient.updateMount(deployId, mountId, mount, options_);
			break;
		}
		}
	}

	public void varDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		switch (op2) {
		case "help": case "":
			printUsage("deployment", "env");
			break;
		case "ls": case "list": {
			if (isHelp("deployment", "env", "ls")) {
				return;
			}
			String deployId = argIdx1(op, cmds);
			debug("EnvVars: %s", deployId);		
			DeploymentOptions options_ = convert(options, DeploymentOptions.class);
			List<Variable> vars = devopsClient.listVariables(deployId, options_);			
			print(vars);
			break;
		}
		case "add": case "create": {
			if (isHelp("deployment", "env", "add")) {
				return;
			}
			String deployId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			Variable var = convert(options, Variable.class);
			debug("Add Var: %s %s", deployId, var);		
			devopsClient.addVariable(deployId, var, options_);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			if (isHelp("deployment", "env", "remove")) {
				return;
			}
			String deployId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			String varId = argId1(op2, cmds);
			debug("Remove Var: %s %s", deployId, varId);		
			devopsClient.removeVariable(deployId, varId, options_);
			break;
		}
		case "update": {
			if (isHelp("deployment", "env", "update")) {
				return;
			}
			String deployId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			Variable var = convert(options, Variable.class);
			String varId = argId1(op2, cmds);
			debug("Update Var: %s %s %s", deployId, varId, var);		
			devopsClient.updateVariable(deployId, varId, var, options_);
			break;
		}
		}
	}

	public void bindingDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		switch (op2) {
		case "help": case "":
			printUsage("deployment", "binding");
			break;
		case "ls": case "list": {
			if (isHelp("deployment", "binding", "ls")) {
				return;
			}
			String deployId = argIdx1(op, cmds);
			debug("Bindings: %s", deployId);		
			DeploymentOptions options_ = convert(options, DeploymentOptions.class);
			List<Binding> bindings = devopsClient.listBindings(deployId, options_);			
			print(bindings);
			break;
		}
		case "add": case "create": {
			if (isHelp("deployment", "binding", "add")) {
				return;
			}
			String deployId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			Binding binding = convert(options, Binding.class);
			debug("Add Binding: %s %s", deployId, binding);		
			devopsClient.addBinding(deployId, binding, options_);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			if (isHelp("deployment", "binding", "remove")) {
				return;
			}
			String deployId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			String bindingId = argId1(op2, cmds);
			debug("Remove Binding: %s %s", deployId, bindingId);		
			devopsClient.removeBinding(deployId, bindingId, options_);
			break;
		}
		case "update": {
			if (isHelp("deployment", "binding", "update")) {
				return;
			}
			String deployId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			Binding binding = convert(options, Binding.class);
			String bindingId = argId1(op2, cmds);
			debug("Update Binding: %s %s %s", deployId, bindingId, binding);		
			devopsClient.updateBinding(deployId, bindingId, binding, options_);
			break;
		}
		}
	}

	public void connectorDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		switch (op2) {
		case "help": case "":
			printUsage("deployment", "connector");
			break;
		case "ls": case "list": {
			if (isHelp("deployment", "connector", "ls")) {
				return;
			}
			String deployId = argIdx1(op, cmds);
			debug("Connectors: %s", deployId);		
			DeploymentOptions options_ = convert(options, DeploymentOptions.class);
			List<Connector> connectors = devopsClient.listConnectors(deployId, options_);		
			printObj(connectors);
			break;
		}
		case "add": case "create": {
			if (isHelp("deployment", "connector", "add")) {
				return;
			}
			String deployId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			Connector connector = convert(options, Connector.class);
			debug("Add Connector: %s %s", deployId, connector);		
			devopsClient.addConnector(deployId, connector, options_);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			if (isHelp("deployment", "connector", "remove")) {
				return;
			}
			String deployId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			String connectorId = argId1(op2, cmds);
			debug("Remove Connector: %s %s", deployId, connectorId);		
			devopsClient.removeConnector(deployId, connectorId, options_);
			break;
		}
		case "update": {
			if (isHelp("deployment", "connector", "update")) {
				return;
			}
			String deployId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			Connector connector = convert(options, Connector.class);
			String connectorId = argId1(op2, cmds);
			debug("Update Connector: %s %s %s", deployId, connectorId, connector);		
			devopsClient.updateConnector(deployId, connectorId, connector, options_);
			break;
		}
		}
	}
	//
	// Jobs
	//
	
	public void listJob(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("job", "ls")) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		JobFilter filter = convert(options, JobFilter.class);
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingArg(type, op, "-n");
			return;
		}
		debug("Jobs: %s %s %s", spaceId, filter, pageable);
		Page<Job> jobs = devopsClient.listJobs(spaceId, filter, pageable);
		print(jobs);
	}

	public void getJob(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("job", "get")) {
			return;
		}
		String jobId = argIdx(op, cmds);
		JobOptions options_ = convert(options, JobOptions.class);
		debug("Get Job: %s", jobId);
		Job job = devopsClient.getJob(jobId, options_);
		printObj(job);
	}
	

	public void schemaJob(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("job", "schema")) {
			return;
		}
		printLine(schemaToString(Job.class));
	}
	
	public void createJob(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("job", "create")) {
			return;
		}
		String start = (String)options.get("--start");
		createJob(start!=null, type, op, cmds, options);
	}

	public void createJob(boolean start, String type, String op, String[] cmds, Map<String, Object> options) {
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingArg(type, op, "-n");
			return;
		}
		Job job = convert(options, Job.class);
		job.setName(argName(op, cmds));
		if (start) {
			job.setStart(true);
		}
		debug("Creating Job: %s", job);
		URI uri = devopsClient.createJob(spaceId, job, null);
		if (isEcho()) {
			printLine("Job URI:", uri);
			String jobId = UriUtils.extractId(uri);
			Job job2 = devopsClient.getJob(jobId, null);
			printObj(job2);			
		}
	}

	public void updateJob(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("job", "update")) {
			return;
		}
		String jobId = argIdx(op, cmds);
		Job job = convert(options, Job.class);
		debug("Updating Job: %s %s", jobId, job);
		setId(job, jobId);
		devopsClient.updateJob(job, null);
		if (isEcho()) {
			Job job2 = devopsClient.getJob(jobId, null);
			printObj(job2);
		}
	}
	
	public void deleteJob(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("job", "delete")) {
			return;
		}
		String jobId = argIdx(op, cmds);
		deleteJob(jobId, options);
	}
	
	public void deleteJob(String jobId, Map<String, Object> options) {
		debug("Deleting Job: %s", jobId);	
		JobOptions options_ = convert(options, JobOptions.class);
		devopsClient.deleteJob(jobId, options_);	
	}

	public void resourcesJob(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("job", "resources")) {
			return;
		}
		String jobId = argIdx(op, cmds);
		debug("Scaling Job: %s", jobId);		
		JobOptions options_ = convert(options, JobOptions.class);
		Resources resources = convert(options, Resources.class);
		devopsClient.scaleJob(jobId, resources, options_);			
	}

	public void startJob(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("job", "start")) {
			return;
		}
		String jobId = argIdx(op, cmds);
		debug("Starting Job: %s", jobId);		
		JobOptions options_ = convert(options, JobOptions.class);
		devopsClient.startJob(jobId, options_);			
	}

	public void stopJob(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("job", "stop")) {
			return;
		}
		String jobId = argIdx(op, cmds);
		debug("Stopping Job: %s", jobId);		
		JobOptions options_ = convert(options, JobOptions.class);
		devopsClient.startJob(jobId, options_);			
	}

	public void restartJob(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("job", "restart")) {
			return;
		}
		String jobId = argIdx(op, cmds);
		debug("Restarting Job: %s", jobId);		
		JobOptions options_ = convert(options, JobOptions.class);
		devopsClient.restartJob(jobId, options_);			
	}
	
	public void syncJob(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("job", "sync")) {
			return;
		}
		String jobId = argIdx(op, cmds);
		debug("Sync Job: %s", jobId);		
		JobOptions options_ = convert(options, JobOptions.class);
		devopsClient.startJob(jobId, options_);			
	}

	public void execJob(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("job", "exec")) {
			return;
		}
		String jobId = argIdx(op, cmds);
		debug("Exec Job: %s", jobId);		
		ExecOptions options_ = convert(options, ExecOptions.class);
		String cmd = ""; //TODO
		options_.setCmd(cmd);
		//devopsClient.execJob(jobId, options_);			
	}
	
	public void logJob(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("job", "logs")) {
			return;
		}
		String jobId = argIdx(op, cmds);
		debug("Log Job: %s", jobId);		
		LogOptions options_ = convert(options, LogOptions.class);
		String out = devopsClient.logJob(jobId, options_);			
		System.out.println(out);
	}

	public void instancesJob(String type, String op, String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		switch (op2) {
		case "help": case "":
			printUsage("job", "pod");
			break;
		case "ls": case "list": {
			if (isHelp("job", "pod", "ls")) {
				return;
			}
			String jobId = argIdx1(op, cmds);
			debug("Instances for Job: %s", jobId);		
			JobOptions options_ = convert(options, JobOptions.class);
			List<Instance> instances = devopsClient.listInstancesForJob(jobId, options_);			
			print(instances);
			break;
		}
		case "kill": case "remove": 	case "rm": case "delete": case "del": {
			if (isHelp("job", "pod", "kill")) {
				return;
			}
			String jobId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			String pod = argId2(op, cmds, true);
			debug("Delete Instance of Job: %s %s", jobId, pod);		
			devopsClient.deleteInstance(jobId, pod, options_);
			break;
		}
		}
	}

	public void mountJob(String type, String op, String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		switch (op2) {
		case "help": case "":
			printUsage("job", "mount");
			break;
		case "ls": case "list": {
			if (isHelp("job", "mount", "ls")) {
				return;
			}
			String jobId = argIdx1(op, cmds);
			debug("Job Mounts: %s", jobId);		
			JobOptions options_ = convert(options, JobOptions.class);
			List<Mount> mounts = devopsClient.listMountsJob(jobId, options_);			
			print(mounts);
			break;
		}
		case "add": case "create": {
			if (isHelp("job", "mount", "add")) {
				return;
			}
			String jobId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			Mount mount = convert(options, Mount.class);
			debug("Add Job Mount: %s %s", jobId, mount);		
			devopsClient.addMountJob(jobId, mount, options_);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			if (isHelp("job", "mount", "remove")) {
				return;
			}
			String jobId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			String mountId = argId1(op2, cmds);
			debug("Remove Job Mount: %s %s", jobId, mountId);		
			devopsClient.removeMountJob(jobId, mountId, options_);
			break;
		}
		case "update": {
			if (isHelp("job", "mount", "update")) {
				return;
			}
			String jobId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			Mount mount = convert(options, Mount.class);
			String mountId = argId1(op2, cmds);
			debug("Update Job Mount: %s %s %s", jobId, mountId, mount);		
			devopsClient.updateMountJob(jobId, mountId, mount, options_);
			break;
		}
		}
	}

	public void varJob(String type, String op, String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		switch (op2) {
		case "help": case "":
			printUsage("job", "env");
			break;
		case "ls": case "list": {
			if (isHelp("job", "env", "ls")) {
				return;
			}
			String jobId = argIdx1(op, cmds);
			debug("Job EnvVars: %s", jobId);		
			JobOptions options_ = convert(options, JobOptions.class);
			List<Variable> vars = devopsClient.listVariablesJob(jobId, options_);			
			print(vars);
			break;
		}
		case "get": {
			if (isHelp("job", "env", "get")) {
				return;
			}
			String jobId = argIdx1(op, cmds);
			String name = argId2(op, cmds);
			debug("Job EnvVar: %s %s", jobId, name);		
			JobOptions options_ = convert(options, JobOptions.class);
			Variable var = devopsClient.getVariableJob(jobId, name, options_);			
			printObj(var);
			break;
		}
		case "add": case "create": {
			if (isHelp("job", "env", "add")) {
				return;
			}
			String jobId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			Variable var = convert(options, Variable.class);
			debug("Add Job EnvVar: %s %s", jobId, var);		
			devopsClient.addVariableJob(jobId, var, options_);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			if (isHelp("job", "env", "remove")) {
				return;
			}
			String jobId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			String varId = argId1(op2, cmds);
			debug("Remove EnvVar: %s %s", jobId, varId);		
			devopsClient.removeVariableJob(jobId, varId, options_);
			break;
		}
		case "update": {
			if (isHelp("job", "env", "update")) {
				return;
			}
			String jobId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			Variable var = convert(options, Variable.class);
			String varId = argId1(op2, cmds);
			debug("Update Job EnvVar: %s %s %s", jobId, varId, var);		
			devopsClient.updateVariableJob(jobId, varId, var, options_);
			break;
		}
		}
	}

	public void bindingJob(String type, String op, String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		switch (op2) {
		case "help": case "":
			printUsage("job", "binding");
			break;
		case "ls": case "list": {
			if (isHelp("job", "binding", "ls")) {
				return;
			}
			String jobId = argIdx1(op, cmds);
			debug("Job Bindings: %s", jobId);		
			JobOptions options_ = convert(options, JobOptions.class);
			List<Binding> bindings = devopsClient.listBindingsJob(jobId, options_);			
			print(bindings);
			break;
		}
		case "add": case "create": {
			if (isHelp("job", "binding", "add")) {
				return;
			}
			String jobId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			Binding binding = convert(options, Binding.class);
			debug("Add Job Binding: %s %s", jobId, binding);		
			devopsClient.addBindingJob(jobId, binding, options_);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			if (isHelp("job", "binding", "remove")) {
				return;
			}
			String jobId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			String bindingId = argId1(op2, cmds);
			debug("Remove Job Binding: %s %s", jobId, bindingId);		
			devopsClient.removeBindingJob(jobId, bindingId, options_);
			break;
		}
		case "update": {
			if (isHelp("job", "binding", "update")) {
				return;
			}
			String jobId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			Binding binding = convert(options, Binding.class);
			String bindingId = argId1(op2, cmds);
			debug("Update Binding: %s %s %s", jobId, bindingId, binding);		
			devopsClient.updateBindingJob(jobId, bindingId, binding, options_);
			break;
		}
		}
	}

	//
	// CronJobs
	//
	
	public void listCronJob(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("cronjob", "ls")) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		CronJobFilter filter = convert(options, CronJobFilter.class);
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingArg(type, op, "-n");
			return;
		}
		debug("CronJobs: %s %s %s", spaceId, filter, pageable);
		Page<CronJob> cronjobs = devopsClient.listCronJobs(spaceId, filter, pageable);
		print(cronjobs, CronJob.class);
	}

	public void getCronJob(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("cronjob", "get")) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		CronJobOptions options_ = convert(options, CronJobOptions.class);
		debug("CronJob: %s", cronjobId);
		CronJob cronjob = devopsClient.getCronJob(cronjobId, options_);
		printObj(cronjob);
	}
	

	public void schemaCronJob(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("cronjob", "schema")) {
			return;
		}
		printLine(schemaToString(CronJob.class));
	}
	
	public void createCronJob(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("cronjob", "create")) {
			return;
		}
		String start = (String)options.get("--start");
		createCronJob(start!=null, type, op, cmds, options);
	}
		
	public void createCronJob(boolean start, String type, String op, String[] cmds, Map<String, Object> options) {
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingArg(type, op, "-n");
			return;
		}
		CronJob cronjob = convert(options, CronJob.class);
		cronjob.setName(argName(op, cmds));
		if (start) {
			cronjob.setStart(true);
		}
		debug("Creating CronJob: %s", cronjob);
		URI uri = devopsClient.createCronJob(spaceId, cronjob, null);
		if (isEcho()) {
			printLine("CronJob URI:", uri);
			String cronjobId = UriUtils.extractId(uri);
			CronJob cronjob2 = devopsClient.getCronJob(cronjobId, null);
			printObj(cronjob2);			
		}
	}

	public void updateCronJob(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("cronjob", "update")) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		CronJob cronjob = convert(options, CronJob.class);
		setId(cronjob, cronjobId);
		debug("Updating CronJob: %s %s", cronjobId, cronjob);
		devopsClient.updateCronJob(cronjob, null);
		if (isEcho()) {
			CronJob cronjob2 = devopsClient.getCronJob(cronjobId, null);
			printObj(cronjob2);
		}
	}
	
	public void deleteCronJob(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("cronjob", "delete")) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		deleteCronJob(cronjobId, options);
	}
	
	public void deleteCronJob(String cronjobId, Map<String, Object> options) {
		debug("Deleting CronJob: %s", cronjobId);
		devopsClient.deleteCronJob(cronjobId, null);	
	}

	
	public void jobsCronJob(String type, String op, String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		String cronjobId = argIdx1(op, cmds);
		switch (op2) {
		case "": case "help":
			printUsage("cronjob", "job");
			return;
		case "ls": case "list": {
			if (isHelp("cronjob", "job", "ls")) {
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

	public void resourcesCronJob(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("cronjob", "resources")) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		debug("Scaling CronJob: %s", cronjobId);		
		CronJobOptions options_ = convert(options, CronJobOptions.class);
		Resources resources = convert(options, Resources.class);
		devopsClient.scaleCronJob(cronjobId, resources, options_);			
	}

	public void startCronJob(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("cronjob", "start")) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		debug("Starting CronJob: %s", cronjobId);		
		CronJobOptions options_ = convert(options, CronJobOptions.class);
		devopsClient.startCronJob(cronjobId, options_);			
	}

	public void stopCronJob(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("cronjob", "stop")) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		debug("Stopping CronJob: %s", cronjobId);		
		CronJobOptions options_ = convert(options, CronJobOptions.class);
		devopsClient.startCronJob(cronjobId, options_);			
	}

	public void suspendCronJob(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("cronjob", "suspend")) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		debug("Suspending CronJob: %s", cronjobId);		
		CronJobOptions options_ = convert(options, CronJobOptions.class);
		devopsClient.suspendCronJob(cronjobId, options_);			
	}

	public void restartCronJob(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("cronjob", "restart")) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		debug("Restarting CronJob: %s", cronjobId);		
		CronJobOptions options_ = convert(options, CronJobOptions.class);
		devopsClient.restartCronJob(cronjobId, options_);			
	}
	
	public void syncCronJob(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("cronjob", "sync")) {
			return;
		}
		String cronjobId = argIdx(op, cmds);
		debug("Sync CronJob: %s", cronjobId);		
		CronJobOptions options_ = convert(options, CronJobOptions.class);
		devopsClient.startCronJob(cronjobId, options_);			
	}
	

	public void mountCronJob(String type, String op, String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		String cronjobId = argIdx1(op, cmds);
		switch (op2) {
		case "help": case "":
			printUsage("cronjob", "mount");
			break;
		case "ls": case "list": {
			if (isHelp("cronjob", "mount", "ls")) {
				return;
			}
			debug("CronJob Mounts: %s", cronjobId);		
			CronJobOptions options_ = convert(options, CronJobOptions.class);
			List<Mount> mounts = devopsClient.listMountsCronJob(cronjobId, options_);			
			print(mounts);
			break;
		}
		case "add": case "create": {
			if (isHelp("cronjob", "mount", "add")) {
				return;
			}
			RequestOptions options_ = convert(options, RequestOptions.class);
			Mount mount = convert(options, Mount.class);
			debug("Add CronJob Mount: %s %s", cronjobId, mount);		
			devopsClient.addMountCronJob(cronjobId, mount, options_);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			if (isHelp("cronjob", "mount", "remove")) {
				return;
			}
			RequestOptions options_ = convert(options, RequestOptions.class);
			String mountId = argId1(op2, cmds);
			debug("Remove CronJob Mount: %s %s", cronjobId, mountId);		
			devopsClient.removeMountCronJob(cronjobId, mountId, options_);
			break;
		}
		case "update": {
			if (isHelp("cronjob", "mount", "update")) {
				return;
			}
			RequestOptions options_ = convert(options, RequestOptions.class);
			Mount mount = convert(options, Mount.class);
			String mountId = argId1(op2, cmds);
			debug("Update CronJob Mount: %s %s %s", cronjobId, mountId, mount);		
			devopsClient.updateMountCronJob(cronjobId, mountId, mount, options_);
			break;
		}
		}
	}

	public void varCronJob(String type, String op, String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		switch (op2) {
		case "help": case "":
			printUsage("cronjob", "env");
			break;
		case "ls": case "list": {
			if (isHelp("cronjob", "env", "ls")) {
				return;
			}
			String cronjobId = argIdx1(op, cmds);
			debug("CronJob EnvVars: %s", cronjobId);		
			CronJobOptions options_ = convert(options, CronJobOptions.class);
			List<Variable> vars = devopsClient.listVariablesCronJob(cronjobId, options_);			
			print(vars);
			break;
		}
		case "add": case "create": {
			if (isHelp("cronjob", "env", "add")) {
				return;
			}
			String cronjobId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			Variable var = convert(options, Variable.class);
			debug("Add CronJob EnvVar: %s %s", cronjobId, var);		
			devopsClient.addVariableCronJob(cronjobId, var, options_);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			if (isHelp("cronjob", "env", "remove")) {
				return;
			}
			String cronjobId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			String varId = argId1(op2, cmds);
			debug("Remove EnvVar: %s %s", cronjobId, varId);		
			devopsClient.removeVariableCronJob(cronjobId, varId, options_);
			break;
		}
		case "update": {
			if (isHelp("cronjob", "env", "update")) {
				return;
			}
			String cronjobId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			Variable var = convert(options, Variable.class);
			String varId = argId1(op2, cmds);
			debug("Update CronJob EnvVar: %s %s %s", cronjobId, varId, var);		
			devopsClient.updateVariableCronJob(cronjobId, varId, var, options_);
			break;
		}
		}
	}

	public void bindingCronJob(String type, String op, String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		switch (op2) {
		case "help": case "":
			printUsage("cronjob", "binding");
			break;
		case "ls": case "list": {
			if (isHelp("cronjob", "binding", "ls")) {
				return;
			}
			String cronjobId = argIdx1(op, cmds);
			debug("CronJob Bindings: %s", cronjobId);		
			CronJobOptions options_ = convert(options, CronJobOptions.class);
			List<Binding> bindings = devopsClient.listBindingsCronJob(cronjobId, options_);			
			print(bindings);
			break;
		}
		case "add": case "create": {
			if (isHelp("cronjob", "binding", "add")) {
				return;
			}
			String cronjobId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			Binding binding = convert(options, Binding.class);
			debug("Add CronJob Binding: %s %s", cronjobId, binding);		
			devopsClient.addBindingCronJob(cronjobId, binding, options_);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			if (isHelp("cronjob", "binding", "remove")) {
				return;
			}
			String cronjobId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			String bindingId = argId1(op2, cmds);
			debug("Remove CronJob Binding: %s %s", cronjobId, bindingId);		
			devopsClient.removeBindingCronJob(cronjobId, bindingId, options_);
			break;
		}
		case "update": {
			if (isHelp("cronjob", "binding", "update")) {
				return;
			}
			String cronjobId = argIdx1(op, cmds);
			RequestOptions options_ = convert(options, RequestOptions.class);
			Binding binding = convert(options, Binding.class);
			String bindingId = argId1(op2, cmds);
			debug("Update Binding: %s %s %s", cronjobId, bindingId, binding);		
			devopsClient.updateBindingCronJob(cronjobId, bindingId, binding, options_);
			break;
		}
		}
	}


	//
	// Domain
	//

	public void listDomain(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("domain", "list")) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		DomainFilter filter = convert(options, DomainFilter.class);
		debug("Domains: %s %s", filter, pageable);
		Page<Domain> domains = devopsClient.listDomains(filter, pageable);
		print(domains, Domain.class);
	}

	public void getDomain(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("domain", "get")) {
			return;
		}
		String domainId = argId(op, cmds);
		DomainOptions options_ = convert(options, DomainOptions.class);
		debug("Domain: %s", domainId);
		Domain domain = devopsClient.getDomain(domainId, options_);
		printObj(domain);
	}

	public void schemaDomain(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("domain", "schema")) {
			return;
		}
		printLine(schemaToString(Domain.class));
	}

	public void createDomain(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("domain", "create")) {
			return;
		}
		Domain domain = convert(options, Domain.class);
		domain.setName(argName(op, cmds));
		debug("Domain: %s", domain);
		URI uri = devopsClient.createDomain(domain, new DomainOptions());
		if (isEcho()) {
			printLine("Domain URI:", uri);
			String id = UriUtils.extractId(uri);
			Domain domain2 = devopsClient.getDomain(id, null);
			printObj(domain2);			
		}
	}
	
	
	public void updateDomain(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("domain", "update")) {
			return;
		}
		String domainId = (String)get("domain", options);
		Domain domain = convert(options, Domain.class);
		debug("Updating Domain: %s %s", domainId, domain);
		devopsClient.updateDomain(domain, null);
		if (isEcho()) {
			Domain domain2 = devopsClient.getDomain(domainId, null);
			printObj(domain2);
		}
	}

	public void deleteDomain(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("domain", "delete")) {
			return;
		}
		String domainId = (String)get(new String[] {"id", "uuid"}, options);
		debug("Deleting Domain: %s", domainId);
		devopsClient.deleteDomain(domainId, null);	
	}


	//
	// Registry
	//
	
	public void listRegistry(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("registry", "list")) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		RegistryFilter filter = convert(options, RegistryFilter.class);
		debug("Registries: %s %s", filter, pageable);
		Page<Registry> registrys = devopsClient.listRegistries(filter, pageable);
		print(registrys, Registry.class);
	}
	
	public void getRegistry(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("registry", "get")) {
			return;
		}
		String registryId = argId(op, cmds);
		debug("Registry: %s", registryId);
		Registry registry = devopsClient.getRegistry(registryId, null);
		printObj(registry);
	}
	

	public void schemaRegistry(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("registry", "schema")) {
			return;
		}
		printLine(schemaToString(Registry.class));
	}

	
	public void createRegistry(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("registry", "create")) {
			return;
		}
		Registry registry = convert(options, Registry.class);
		registry.setName(argName(op, cmds));
		debug("Creating Registry: %s", registry);
		RegistryOptions options_ = convert(options, RegistryOptions.class);
		URI uri = devopsClient.createRegistry(registry, options_);
		if (isEcho()) {
			printLine("Registry URI:", uri);
			String registryId = UriUtils.extractId(uri);
			Registry registry2 = devopsClient.getRegistry(registryId, null);
			printObj(registry2);			
		}
	}

	public void updateRegistry(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("registry", "update")) {
			return;
		}
		String registryId = argId(op, cmds);
		Registry registry = convert(options, Registry.class);
		debug("Updating Registry: %s %s", registryId, registry);
		devopsClient.updateRegistry(registry, null);
		if (isEcho()) {
			Registry registry2 = devopsClient.getRegistry(registryId, null);
			printObj(registry2);			
		}
	}
	
	public void deleteRegistry(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("registry", "delete")) {
			return;
		}
		String registryId = argId(op, cmds);
		debug("Deleting Registry: %s", registryId);		
		devopsClient.deleteRegistry(registryId, null);		
	}



	//
	// Vcs
	//
	

	public void listVcs(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("vcs", "ls")) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		VcsFilter filter = convert(options, VcsFilter.class);
		debug("Vcs: %s %s", filter, pageable);
		Page<Vcs> vcss = devopsClient.listVcss(filter, pageable);
		print(vcss, Vcs.class);
	}

	public void getVcs(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("vcs", "get")) {
			return;
		}
		String vcsId = argId(op, cmds);
		VcsOptions options_ = convert(options, VcsOptions.class);
		debug("Vcs: %s", vcsId);
		Vcs vcs = devopsClient.getVcs(vcsId, options_);
		printObj(vcs);
	}


	public void schemaVcs(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("vcs", "schema")) {
			return;
		}
		printLine(schemaToString(Vcs.class));
	}

	public void createVcs(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("vcs", "create")) {
			return;
		}
		Vcs vcs = convert(options, Vcs.class);
		vcs.setName(argName(op, cmds));
		debug("Creating Vcs: %s", vcs);
		URI uri = devopsClient.createVcs(vcs, null);
		if (isEcho()) {
			printLine("Vcs URI:", uri);
			String id = UriUtils.extractId(uri);
			Vcs vcs2 = devopsClient.getVcs(id, null);
			printObj(vcs2);			
		}

	}

	
	public void updateVcs(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("vcs", "update")) {
			return;
		}
		String vcsId = argId(op, cmds);
		Vcs vcs = convert(options, Vcs.class);
		debug("Updating Vcs: %s %s", vcsId, vcs);
		devopsClient.updateVcs(vcs, null);
		if (isEcho()) {
			Vcs vcs2 = devopsClient.getVcs(vcsId, null);
			printObj(vcs2);			
		}
	}

	public void deleteVcs(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("vcs", "delete")) {
			return;
		}
		String vcsId = argId(op, cmds);
		debug("Deleting Vcs: %s", vcsId);
		devopsClient.deleteVcs(vcsId, null);	
	}

	//
	// Catalog
	//

	public void listCatalog(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("catalog", "ls")) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		CatalogFilter filter = convert(options, CatalogFilter.class);
		debug("Catalogs: %s %s", filter, pageable);
		Page<Catalog> catalogs = devopsClient.listCatalogs(filter, pageable);
		print(catalogs, Catalog.class);
	}

	public void getCatalog(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("catalog", "get")) {
			return;
		}
		String catalogId = argId(op, cmds);
		CatalogOptions options_ = convert(options, CatalogOptions.class);
		debug("Catalog: %s", catalogId);
		Catalog catalog = devopsClient.getCatalog(catalogId, options_);
		printObj(catalog);
	}

	public void schemaCatalog(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("catalog", "schema")) {
			return;
		}
		printLine(schemaToString(Catalog.class));
	}

	public void createCatalog(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("catalog", "create")) {
			return;
		}
		Catalog catalog = convert(options, Catalog.class);
		catalog.setName(argName(op, cmds));
		debug("Creating Catalog: %s", catalog);
		URI uri = devopsClient.createCatalog(catalog, new CatalogOptions());
		if (isEcho()) {
			printLine("Catalog URI:", uri);
			String id = UriUtils.extractId(uri);
			Catalog catalog2 = devopsClient.getCatalog(id, null);
			printObj(catalog2);			
		}
	}
	
	
	public void updateCatalog(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("catalog", "update")) {
			return;
		}
		String catalogId = argId(op, cmds);
		Catalog catalog = convert(options, Catalog.class);
		debug("Updating Catalog: %s %s", catalogId, catalog);
		devopsClient.updateCatalog(catalog, null);
		if (isEcho()) {
			Catalog catalog2 = devopsClient.getCatalog(catalogId, null);
			printObj(catalog2);			
		}
	}

	public void deleteCatalog(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("catalog", "delete")) {
			return;
		}
		String catalogId = argId(op, cmds);
		debug("Deleting Catalog: %s", catalogId);		
		devopsClient.deleteCatalog(catalogId, null);	
	}

	public void installFromCatalog(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("catalog", "install")) {
			return;
		}
		String catalogId = argId(op, cmds);
		String solutionId = argId1(op, cmds);
		installFromCatalog(catalogId, solutionId, options);
	}

	public void installFromCatalog(String catalogId, String solutionId, Map<String, Object> options) {
		InstallOptions options_ = convert(options, InstallOptions.class);
		debug("Install Solution from Catalog: %s %s", solutionId, options_);
		URI uri = devopsClient.install(catalogId, solutionId, options_);
		if (isEcho()) {
			Object deploy = getAny(uri, options);
			printObj(deploy);
		}
	}
	
	public void listSolutionsFor(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("catalog", "solution")) {
			return;
		}
		String catalogId = argId(op, cmds);
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		SolutionFilter filter = convert(options, SolutionFilter.class);
		debug("Catalog Solutions: %s %s %s", catalogId, filter, pageable);
		Page<Solution> solutions = devopsClient.listSolutionsFor(catalogId, filter, pageable);
		print(solutions, Catalog.class);
	}
	//
	// Solution
	//

	public void listSolution(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("solution", "ls")) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		SolutionFilter filter = convert(options, SolutionFilter.class);
		debug("Solution: %s %s", filter, pageable);
		Page<Solution> solutions = devopsClient.listSolutions(filter, pageable);
		print(solutions, Solution.class);
	}

	public void getSolution(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("solution", "get")) {
			return;
		}
		String solutionId = argId(op, cmds);
		SolutionOptions options_ = convert(options, SolutionOptions.class);
		debug("Solution: %s", solutionId);
		Solution solution = devopsClient.getSolution(solutionId, options_);
		printObj(solution);
	}

	public void schemaSolution(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("solution", "schema")) {
			return;
		}
		printLine(schemaToString(Solution.class));
	}

	public void createSolution(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("solution", "create")) {
			return;
		}
		Solution solution = convert(options, Solution.class);
		solution.setName(argName(op, cmds));
		debug("Creating Solution: %s", solution);
		URI uri = devopsClient.createSolution(solution, new SolutionOptions());
		if (isEcho()) {
			printLine("Solution URI:", uri);
			String id = UriUtils.extractId(uri);
			Solution solution2 = devopsClient.getSolution(id, null);
			printObj(solution2);
		}
	}
	
	
	public void updateSolution(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("solution", "update")) {
			return;
		}
		String solutionId = argId(op, cmds);
		Solution solution = convert(options, Solution.class);
		debug("Updating Solution: %s %s", solutionId, solution);
		devopsClient.updateSolution(solution, null);
		if (isEcho()) {
			Solution solution2 = devopsClient.getSolution(solutionId, null);
			printObj(solution2);			
		}
	}

	public void deleteSolution(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("solution", "delete")) {
			return;
		}
		String solutionId = argId(op, cmds);
		debug("Deleting Solution: %s", solutionId);
		devopsClient.deleteSolution(solutionId, null);	
	}

	public void installSolution(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("solution", "install")) {
			return;
		}
		String solutionId = argId(op, cmds);
		installSolution(solutionId, options);
		InstallOptions options_ = convert(options, InstallOptions.class);
		debug("Install Solution: %s %s", solutionId, options_);
		URI uri = devopsClient.install(solutionId, options_);
		if (isEcho()) {
			Object deploy = getAny(uri, options);
			printObj(deploy);
		}
	}

	public void installSolution(String solutionId, Map<String, Object> options) {
		InstallOptions options_ = convert(options, InstallOptions.class);
		debug("Install Solution: %s %s", solutionId, options_);
		URI uri = devopsClient.install(solutionId, options_);
		if (isEcho()) {
			Object deploy = getAny(uri, options);
			printObj(deploy);
		}
	}
	
	private Object getAny(URI uri, Map<String, Object> options) {
		String path = uri.getPath();
		String id = UriUtils.extractId(uri);
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

	public void listMarketplace(String type, String op, String[] cmds, Map<String, Object> options) {
		if (isHelp("marketplace", "ls")) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		CatalogFilter filter = convert(options, CatalogFilter.class);
		debug("Marketplace: %s %s", filter, pageable);
		Page<Catalog> catalogs = devopsClient.listCatalogs(filter, pageable);
		if (catalogs.getContent()!=null) {
			int i = 0;
			for (Catalog catalog: catalogs.getContent()) {
				if (i>0) {
					System.out.println();
				}
				System.out.println(String.format("%s %s", catalog.getId(), catalog.getName()));
				System.out.println();
				SolutionFilter filter2 = convert(options, SolutionFilter.class);
				Page<Solution> solutions = devopsClient.listSolutionsFor(catalog.getUuid(), filter2, pageable);				
				print(solutions, Solution.class);
				i++;
			}			
		}
	}
	
	@Override
	protected String getDefaultFormat(Class<? extends Object> type) {
		if (Cluster.class.equals(type)) {
			return CLUSTER_DEFAULT_FORMAT;
		}
		if (Space.class.equals(type)) {
			return SPACE_DEFAULT_FORMAT;
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

		return null;
	}

	@Override
	protected String getWideFormat(Class<? extends Object> type) {
		if (Cluster.class.equals(type)) {
			return CLUSTER_WIDE_FORMAT;
		}
		if (Space.class.equals(type)) {
			return SPACE_WIDE_FORMAT;
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

		return null;
	}
	
	@Override
	protected String getFormat(String fmt, Class<? extends Object> type) {
		if (fmt==null || fmt.isEmpty()) {
			return getDefaultFormat(type);
		}
		if ("wide".equals(fmt)) {
			return getWideFormat(type);
		}
		if (Cluster.class.equals(type)) {
			return null;
		}
		if (Space.class.equals(type)) {
			return null;
		}
		if (Deployment.class.equals(type)) {
			switch (fmt) {
			case "cicd": case "build":
				return DEPLOYMENT_CICD_FORMAT;
			}
			return null;
		}
		if (Job.class.equals(type)) {
			switch (fmt) {
			case "cicd": case "build":
				return DEPLOYMENT_CICD_FORMAT;
			}
			return null;
		}
		if (CronJob.class.equals(type)) {
			switch (fmt) {
			case "cicd": case "build":
				return DEPLOYMENT_CICD_FORMAT;
			}
			return null;
		}
		if (Domain.class.equals(type)) {
			return null;
		}
		if (Registry.class.equals(type)) {
			return null;
		}
		if (Vcs.class.equals(type)) {
			return null;
		}
		if (Catalog.class.equals(type)) {
			return null;
		}
		if (Catalog.class.equals(type)) {
			return null;
		}
		if (Solution.class.equals(type)) {
			return null;
		}
		if (Binding.class.equals(type)) {
			return null;
		}
		if (Connector.class.equals(type)) {
			return null;
		}
		if (Route.class.equals(type)) {
			return null;
		}
		if (Mount.class.equals(type)) {
			return null;
		}
		return null;
	}
	
	protected String[] getFormats(Class<? extends Object> type) {
		if (Cluster.class.equals(type)) {
			return new String[] {};
		}
		if (Space.class.equals(type)) {
			return new String[] {};
		}
		if (Deployment.class.equals(type)) {
			return new String[] {"cicd","build"};
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
		if (Catalog.class.equals(type)) {
			return new String[] {};
		}
		if (Solution.class.equals(type)) {
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
		return null;
	}

	@Override
	protected String argPID(Map<String, Object> options) {
		return argNS(options);
	}

	protected String argNS(Map<String, Object> options) {
		String spaceId = (String)options.get("n");
		if (spaceId!=null) {
			return spaceId;
		}
		return this.space;
	}

	protected String argCluster(Map<String, Object> options) {
		String clusterId = (String)options.get("c");
		if (clusterId!=null) {
			return clusterId;
		}
		return this.cluster;
	}

}