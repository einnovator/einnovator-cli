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

	private static final String ROUTE_DEFAULT_FORMAT = "id,host,dns,domain,tls";
	private static final String ROUTE_WIDE_FORMAT = "id,host,dns,domain,tls";

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

	String[][] DEVOPS_COMMANDS = new String[][] { 
		new String[] {"cluster", "clusters", "c"},
		new String[] {"space", "spaces", "namespace", "namespaces", "ns"},
		new String[] {"deploy", "deployment", "deploys", "deployments"},
		new String[] {"job", "jobs"},
		new String[] {"cronjob", "cronjobs"},
		new String[] {"domain", "domains", "dom"},
		new String[] {"registry", "registries", "reg", "docker"},
		new String[] {"vcs", "vcss", "v", "git"},
		new String[] {"catalog", "catalogs", "cat"},
		new String[] {"solution", "solutions", "sol"},
	};

	@Override
	protected String[][] getCommands() {
		return DEVOPS_COMMANDS;
	}


	public void run(String type, String op, String[] cmds, Map<String, Object> options) {		
		switch (type) {
		case "help":
			printUsage();
			break;
		case "cluster": case "clusters": 
			switch (op) {
			case "help":
				printUsage("cluster");
				break;
			case "get": case "show": case "view":
				getCluster(type, op, cmds, options);
				break;
			case "list": case "ls": case "":
				listCluster(type, op, cmds, options);
				break;
			case "schema": case "meta":
				schemaCluster(type, op, cmds, options);
				break;
			case "create": 
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
			case "help":
				printUsage("space");
				break;
			case "get": case "show": case "view":
				getSpace(type, op, cmds, options);
				break;
			case "list": case "ls": case "":
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
			case "help":
				printUsage("deployment");
				break;
			case "get": case "show": case "view":
				getDeployment(type, op, cmds, options);
				break;
			case "list": case "ls": case "ps": case "":
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
			case "rscale":
				rscaleDeployment(type, op, cmds, options);
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
			case "log": case "logs":
				logDeployment(type, op, cmds, options);
				break;
			case "instances": case "instance": case "replica": case "replicas": case "pods": case "pod":
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
			case "help":
				printUsage("job");
				break;
			case "get": case "show": case "view":
				getJob(type, op, cmds, options);
				break;
			case "list": case "ls": case "ps": case "":
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
			case "instances": case "instance": case "replica": case "replicas": case "pods": case "pod":
				instancesJob(type, op, cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;			
		case "cronjob":
			switch (op) {
			case "help":
				printUsage("cronjob");
				break;
			case "get": case "show": case "view":
				getCronJob(type, op, cmds, options);
				break;
			case "list": case "ls": case "ps": case "":
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
			default: 
				invalidOp(type, op);
				break;
			}
			break;			
		case "domain": case "domains":
			switch (op) {
			case "help":
				printUsage("domain");
				break;
			case "get": case "show": case "view":
				getDomain(type, op, cmds, options);
				break;
			case "list": case "ls": case "":
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
			case "help":
				printUsage("registry");
				break;
			case "get": case "show": case "view":
				getRegistry(type, op, cmds, options);
				break;
			case "list": case "ls": case "":
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
			case "help":
				printUsage("vcs");
				break;
			case "get": case "show": case "view":
				getVcs(type, op, cmds, options);
				break;
			case "list": case "ls": case "":
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
			case "help":
				printUsage("catalog");
				break;
			case "get": case "show": case "view":
				getCatalog(type, op, cmds, options);
				break;
			case "list": case "ls": case "":
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
			case "help":
				printUsage("solution");
				break;
			case "get": case "show": case "view":
				getSolution(type, op, cmds, options);
				break;
			case "list": case "ls": case "":
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
			case "help":
				printUsage("marketplace");
				break;
			case "list": case "ls": case "":
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
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		ClusterFilter filter = convert(options, ClusterFilter.class);
		debug("Clusters: %s %s", filter, pageable);
		Page<Cluster> clusters = devopsClient.listClusters(filter, pageable);
		print(clusters, Cluster.class);
	}


	public void getCluster(String type, String op, String[] cmds, Map<String, Object> options) {
		String clusterId = argId(op, cmds);
		ClusterOptions options_ = convert(options, ClusterOptions.class);
		debug("Cluster: %s", clusterId);
		Cluster cluster = devopsClient.getCluster(clusterId, options_);
		printObj(cluster);
	}

	public void schemaCluster(String type, String op, String[] cmds, Map<String, Object> options) {
		printLine(schemaToString(Cluster.class));
	}
	
	public void createCluster(String type, String op, String[] cmds, Map<String, Object> options) {
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
		String clusterId = argId(op, cmds);
		debug("Deleting Cluster: %s", clusterId);		
		devopsClient.deleteCluster(clusterId, null);	
	}

	//
	// Spaces
	//
	
	public void listSpace(String type, String op, String[] cmds, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		SpaceFilter filter = convert(options, SpaceFilter.class);
		debug("Spaces: %s %s", filter, pageable);
		Page<Space> spaces = devopsClient.listSpaces(filter, pageable);
		print(spaces, Space.class);
	}
	
	public void getSpace(String type, String op, String[] cmds, Map<String, Object> options) {
		String spaceId = argId(op, cmds);
		SpaceOptions options_ = convert(options, SpaceOptions.class);
		debug("Space: %s", spaceId);
		Space space = devopsClient.getSpace(spaceId, options_);
		printObj(space);
	}
	
	public void setSpace(String type, String op, String[] cmds, Map<String, Object> options) {
		String spaceId = argId(op, cmds);
		debug("Set Space: %s", spaceId);
		SpaceOptions options_ = convert(options, SpaceOptions.class);
		Space space = devopsClient.getSpace(spaceId, options_);
		this.space = spaceId;
		if (isEcho()) {
			printObj(space);
		}
		writeConfig();
	}
	public void unsetSpace(String type, String op, String[] cmds, Map<String, Object> options) {
		String spaceId = argId(op, cmds);
		debug("Unset Space: %s", spaceId);
		this.space = null;
		writeConfig();
	}
	
	public void schemaSpace(String type, String op, String[] cmds, Map<String, Object> options) {
		printLine(schemaToString(Space.class));
	}
	
	public void createSpace(String type, String op, String[] cmds, Map<String, Object> options) {
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
		String spaceId = argIdx(op, cmds);
		debug("Deleting Space: %s", spaceId);		
		devopsClient.deleteSpace(spaceId, null);		
	}
	
	// Util
	
	
	//
	// Deployments
	//
	
	public void listDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
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
		String deployId = argIdx(op, cmds);
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		debug("Get Deployment: %s", deployId);
		Deployment deployment = devopsClient.getDeployment(deployId, options_);
		printObj(deployment);
	}
	

	public void schemaDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		printLine(schemaToString(Deployment.class));
	}
	
	public void createDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingArg(type, op, "-n");
			return;
		}
		Deployment deployment = convert(options, Deployment.class);
		deployment.setName(argName(op, cmds));
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
		String deployId = argIdx(op, cmds);
		debug("Deleting Deployment: %s", deployId);	
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		devopsClient.deleteDeployment(deployId, options_);		
	}
	
	public void scaleDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
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
	
	public void rscaleDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		String deployId = argIdx(op, cmds);
		debug("Scaling Deployment: %s", deployId);		
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		Resources resources = convert(options, Resources.class);
		devopsClient.scaleDeployment(deployId, resources, options_);			
	}

	public void startDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		String deployId = argIdx(op, cmds);
		debug("Starting Deployment: %s", deployId);		
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		devopsClient.startDeployment(deployId, options_);			
	}

	public void stopDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		String deployId = argIdx(op, cmds);
		debug("Stopping Deployment: %s", deployId);		
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		devopsClient.startDeployment(deployId, options_);			
	}

	public void restartDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		String deployId = argIdx(op, cmds);
		debug("Restarting Deployment: %s", deployId);		
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		devopsClient.restartDeployment(deployId, options_);			
	}
	
	public void syncDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		String deployId = argIdx(op, cmds);
		debug("Sync Deployment: %s", deployId);		
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		devopsClient.startDeployment(deployId, options_);			
	}

	public void execDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		String deployId = argIdx(op, cmds);
		debug("Exec Deployment: %s", deployId);		
		ExecOptions options_ = convert(options, ExecOptions.class);
		String cmd = ""; //TODO
		options_.setCmd(cmd);
		devopsClient.execDeployment(deployId, options_);			
	}
	
	public void logDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		String deployId = argIdx(op, cmds);
		debug("Log Deployment: %s", deployId);		
		LogOptions options_ = convert(options, LogOptions.class);
		String out = devopsClient.logDeployment(deployId, options_);			
		System.out.println(out);
	}
	
	public void instancesDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		String deployId = argIdx1(op, cmds);
		switch (op2) {
		case "list": case "ls": case "": {
			debug("Instances of: %s", deployId);		
			DeploymentOptions options_ = convert(options, DeploymentOptions.class);
			List<Instance> instances = devopsClient.listInstances(deployId, options_);			
			print(instances);
			break;
		}
		case "kill": case "remove": case "rm": case "delete": case "del": {
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
		String deployId = argIdx1(op, cmds);
		switch (op2) {
		case "list": case "ls": case "": {
			debug("Routes: %s", deployId);		
			DeploymentOptions options_ = convert(options, DeploymentOptions.class);
			List<Route> routes = devopsClient.listRoutes(deployId, options_);			
			print(routes);
			break;
		}
		case "add": case "create": {
			RequestOptions options_ = convert(options, RequestOptions.class);
			Route route = convert(options, Route.class);
			debug("Add Route: %s %s", deployId, route);		
			devopsClient.addRoute(deployId, route, options_);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			RequestOptions options_ = convert(options, RequestOptions.class);
			String routeId = argId1(op2, cmds);
			debug("Remove Route: %s %s", deployId, routeId);		
			devopsClient.removeRoute(deployId, routeId, options_);
			break;
		}
		case "update": {
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
		String deployId = argIdx1(op, cmds);
		switch (op2) {
		case "list": case "ls": case "": {
			debug("Mounts: %s", deployId);		
			DeploymentOptions options_ = convert(options, DeploymentOptions.class);
			List<Mount> mounts = devopsClient.listMounts(deployId, options_);			
			print(mounts);
			break;
		}
		case "add": case "create": {
			RequestOptions options_ = convert(options, RequestOptions.class);
			Mount mount = convert(options, Mount.class);
			debug("Add Mount: %s %s", deployId, mount);		
			devopsClient.addMount(deployId, mount, options_);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			RequestOptions options_ = convert(options, RequestOptions.class);
			String mountId = argId1(op2, cmds);
			debug("Remove Mount: %s %s", deployId, mountId);		
			devopsClient.removeMount(deployId, mountId, options_);
			break;
		}
		case "update": {
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
		String deployId = argIdx1(op, cmds);
		switch (op2) {
		case "list": case "ls": case "": {
			debug("EnvVars: %s", deployId);		
			DeploymentOptions options_ = convert(options, DeploymentOptions.class);
			List<Variable> vars = devopsClient.listVariables(deployId, options_);			
			print(vars);
			break;
		}
		case "add": case "create": {
			RequestOptions options_ = convert(options, RequestOptions.class);
			Variable var = convert(options, Variable.class);
			debug("Add Var: %s %s", deployId, var);		
			devopsClient.addVariable(deployId, var, options_);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			RequestOptions options_ = convert(options, RequestOptions.class);
			String varId = argId1(op2, cmds);
			debug("Remove Var: %s %s", deployId, varId);		
			devopsClient.removeVariable(deployId, varId, options_);
			break;
		}
		case "update": {
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
		String deployId = argIdx1(op, cmds);
		switch (op2) {
		case "list": case "ls": case "": {
			debug("Bindings: %s", deployId);		
			DeploymentOptions options_ = convert(options, DeploymentOptions.class);
			List<Binding> bindings = devopsClient.listBindings(deployId, options_);			
			print(bindings);
			break;
		}
		case "add": case "create": {
			RequestOptions options_ = convert(options, RequestOptions.class);
			Binding binding = convert(options, Binding.class);
			debug("Add Binding: %s %s", deployId, binding);		
			devopsClient.addBinding(deployId, binding, options_);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			RequestOptions options_ = convert(options, RequestOptions.class);
			String bindingId = argId1(op2, cmds);
			debug("Remove Binding: %s %s", deployId, bindingId);		
			devopsClient.removeBinding(deployId, bindingId, options_);
			break;
		}
		case "update": {
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
		String deployId = argIdx1(op, cmds);
		switch (op2) {
		case "list": case "ls": case "": {
			debug("Connectors: %s", deployId);		
			DeploymentOptions options_ = convert(options, DeploymentOptions.class);
			List<Connector> connectors = devopsClient.listConnectors(deployId, options_);		
			printObj(connectors);
			break;
		}
		case "add": case "create": {
			RequestOptions options_ = convert(options, RequestOptions.class);
			Connector connector = convert(options, Connector.class);
			debug("Add Connector: %s %s", deployId, connector);		
			devopsClient.addConnector(deployId, connector, options_);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			RequestOptions options_ = convert(options, RequestOptions.class);
			String connectorId = argId1(op2, cmds);
			debug("Remove Connector: %s %s", deployId, connectorId);		
			devopsClient.removeConnector(deployId, connectorId, options_);
			break;
		}
		case "update": {
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
		String jobId = argIdx(op, cmds);
		JobOptions options_ = convert(options, JobOptions.class);
		debug("Get Job: %s", jobId);
		Job job = devopsClient.getJob(jobId, options_);
		printObj(job);
	}
	

	public void schemaJob(String type, String op, String[] cmds, Map<String, Object> options) {
		printLine(schemaToString(Job.class));
	}
	
	public void createJob(String type, String op, String[] cmds, Map<String, Object> options) {
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingArg(type, op, "-n");
			return;
		}
		Job job = convert(options, Job.class);
		job.setName(argName(op, cmds));
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
		String jobId = argIdx(op, cmds);
		debug("Deleting Job: %s", jobId);		
		devopsClient.deleteJob(jobId, null);		
	}

	public void instancesJob(String type, String op, String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		String jobId = argIdx1(op, cmds);
		switch (op2) {
		case "list": case "ls": case "": {
			debug("Instances for Job: %s", jobId);		
			JobOptions options_ = convert(options, JobOptions.class);
			List<Instance> instances = devopsClient.listInstancesForJob(jobId, options_);			
			print(instances);
			break;
		}
		case "kill": case "remove": 	case "rm": case "delete": case "del": {
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
		String jobId = argIdx1(op, cmds);
		switch (op2) {
		case "list": case "ls": case "": {
			debug("Job Mounts: %s", jobId);		
			JobOptions options_ = convert(options, JobOptions.class);
			List<Mount> mounts = devopsClient.listMountsJob(jobId, options_);			
			print(mounts);
			break;
		}
		case "add": case "create": {
			RequestOptions options_ = convert(options, RequestOptions.class);
			Mount mount = convert(options, Mount.class);
			debug("Add Job Mount: %s %s", jobId, mount);		
			devopsClient.addMountJob(jobId, mount, options_);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			RequestOptions options_ = convert(options, RequestOptions.class);
			String mountId = argId1(op2, cmds);
			debug("Remove Job Mount: %s %s", jobId, mountId);		
			devopsClient.removeMountJob(jobId, mountId, options_);
			break;
		}
		case "update": {
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
		String jobId = argIdx1(op, cmds);
		switch (op2) {
		case "list": case "ls": case "": {
			debug("Job EnvVars: %s", jobId);		
			JobOptions options_ = convert(options, JobOptions.class);
			List<Variable> vars = devopsClient.listVariablesJob(jobId, options_);			
			print(vars);
			break;
		}
		case "add": case "create": {
			RequestOptions options_ = convert(options, RequestOptions.class);
			Variable var = convert(options, Variable.class);
			debug("Add Job EnvVar: %s %s", jobId, var);		
			devopsClient.addVariableJob(jobId, var, options_);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			RequestOptions options_ = convert(options, RequestOptions.class);
			String varId = argId1(op2, cmds);
			debug("Remove EnvVar: %s %s", jobId, varId);		
			devopsClient.removeVariableJob(jobId, varId, options_);
			break;
		}
		case "update": {
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
		String jobId = argIdx1(op, cmds);
		switch (op2) {
		case "list": case "ls": case "": {
			debug("Job Bindings: %s", jobId);		
			JobOptions options_ = convert(options, JobOptions.class);
			List<Binding> bindings = devopsClient.listBindingsJob(jobId, options_);			
			print(bindings);
			break;
		}
		case "add": case "create": {
			RequestOptions options_ = convert(options, RequestOptions.class);
			Binding binding = convert(options, Binding.class);
			debug("Add Job Binding: %s %s", jobId, binding);		
			devopsClient.addBindingJob(jobId, binding, options_);
			break;
		}
		case "remove": 	case "rm": case "delete": case "del": {
			RequestOptions options_ = convert(options, RequestOptions.class);
			String bindingId = argId1(op2, cmds);
			debug("Remove Job Binding: %s %s", jobId, bindingId);		
			devopsClient.removeBindingJob(jobId, bindingId, options_);
			break;
		}
		case "update": {
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
		String cronjobId = argIdx(op, cmds);
		CronJobOptions options_ = convert(options, CronJobOptions.class);
		debug("CronJob: %s", cronjobId);
		CronJob cronjob = devopsClient.getCronJob(cronjobId, options_);
		printObj(cronjob);
	}
	

	public void schemaCronJob(String type, String op, String[] cmds, Map<String, Object> options) {
		printLine(schemaToString(CronJob.class));
	}
	
	public void createCronJob(String type, String op, String[] cmds, Map<String, Object> options) {
		String spaceId = argNS(options);
		if (spaceId==null) {
			missingArg(type, op, "-n");
			return;
		}
		CronJob cronjob = convert(options, CronJob.class);
		cronjob.setName(argName(op, cmds));
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
		String cronjobId = argIdx(op, cmds);
		debug("Deleting CronJob: %s", cronjobId);
		devopsClient.deleteCronJob(cronjobId, null);		
	}
	
	public void jobsCronJob(String type, String op, String[] cmds, Map<String, Object> options) {
		String op2 = cmds.length>0 ? cmds[0] : "";
		String cronjobId = argIdx1(op, cmds);
		switch (op2) {
		case "list": case "ls": case "": {
			debug("Jobs for CronJob: %s", cronjobId);		
			CronJobOptions options_ = convert(options, CronJobOptions.class);
			List<Job> jobs = devopsClient.listJobsForCronJob(cronjobId, options_);			
			print(jobs);
			break;
		}
		}
	}

	//
	// Domain
	//

	public void listDomain(String type, String op, String[] cmds, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		DomainFilter filter = convert(options, DomainFilter.class);
		debug("Domains: %s %s", filter, pageable);
		Page<Domain> domains = devopsClient.listDomains(filter, pageable);
		print(domains, Domain.class);
	}

	public void getDomain(String type, String op, String[] cmds, Map<String, Object> options) {
		String domainId = argId(op, cmds);
		DomainOptions options_ = convert(options, DomainOptions.class);
		debug("Domain: %s", domainId);
		Domain domain = devopsClient.getDomain(domainId, options_);
		printObj(domain);
	}

	public void schemaDomain(String type, String op, String[] cmds, Map<String, Object> options) {
		printLine(schemaToString(Domain.class));
	}

	public void createDomain(String type, String op, String[] cmds, Map<String, Object> options) {
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
		String domainId = (String)get(new String[] {"id", "uuid"}, options);
		debug("Deleting Domain: %s", domainId);
		devopsClient.deleteDomain(domainId, null);	
	}


	//
	// Registry
	//
	
	public void listRegistry(String type, String op, String[] cmds, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		RegistryFilter filter = convert(options, RegistryFilter.class);
		debug("Registries: %s %s", filter, pageable);
		Page<Registry> registrys = devopsClient.listRegistries(filter, pageable);
		print(registrys, Registry.class);
	}
	
	public void getRegistry(String type, String op, String[] cmds, Map<String, Object> options) {
		String registryId = argId(op, cmds);
		debug("Registry: %s", registryId);
		Registry registry = devopsClient.getRegistry(registryId, null);
		printObj(registry);
	}
	

	public void schemaRegistry(String type, String op, String[] cmds, Map<String, Object> options) {
		printLine(schemaToString(Registry.class));
	}

	
	public void createRegistry(String type, String op, String[] cmds, Map<String, Object> options) {
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
		String registryId = argId(op, cmds);
		debug("Deleting Registry: %s", registryId);		
		devopsClient.deleteRegistry(registryId, null);		
	}



	//
	// Vcss
	//
	

	public void listVcs(String type, String op, String[] cmds, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		VcsFilter filter = convert(options, VcsFilter.class);
		debug("Vcs: %s %s", filter, pageable);
		Page<Vcs> vcss = devopsClient.listVcss(filter, pageable);
		print(vcss, Vcs.class);
	}

	public void getVcs(String type, String op, String[] cmds, Map<String, Object> options) {
		String vcsId = argId(op, cmds);
		VcsOptions options_ = convert(options, VcsOptions.class);
		debug("Vcs: %s", vcsId);
		Vcs vcs = devopsClient.getVcs(vcsId, options_);
		printObj(vcs);
	}


	public void schemaVcs(String type, String op, String[] cmds, Map<String, Object> options) {
		printLine(schemaToString(Vcs.class));
	}

	public void createVcs(String type, String op, String[] cmds, Map<String, Object> options) {
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
		String vcsId = argId(op, cmds);
		debug("Deleting Vcs: %s", vcsId);
		devopsClient.deleteVcs(vcsId, null);	
	}

	//
	// Catalog
	//

	public void listCatalog(String type, String op, String[] cmds, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		CatalogFilter filter = convert(options, CatalogFilter.class);
		debug("Vcs: %s %s", filter, pageable);
		Page<Catalog> catalogs = devopsClient.listCatalogs(filter, pageable);
		print(catalogs, Catalog.class);
	}

	public void getCatalog(String type, String op, String[] cmds, Map<String, Object> options) {
		String catalogId = argId(op, cmds);
		CatalogOptions options_ = convert(options, CatalogOptions.class);
		debug("Catalog: %s", catalogId);
		Catalog catalog = devopsClient.getCatalog(catalogId, options_);
		printObj(catalog);
	}

	public void schemaCatalog(String type, String op, String[] cmds, Map<String, Object> options) {
		printLine(schemaToString(Catalog.class));
	}

	public void createCatalog(String type, String op, String[] cmds, Map<String, Object> options) {
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
		String catalogId = argId(op, cmds);
		debug("Deleting Catalog: %s", catalogId);		
		devopsClient.deleteCatalog(catalogId, null);	
	}


	public void installFromCatalog(String type, String op, String[] cmds, Map<String, Object> options) {
		String catalogId = argId(op, cmds);
		String solutionId = argId1(op, cmds);
		InstallOptions options_ = convert(options, InstallOptions.class);
		debug("Install Solution from Catalog: %s %s", solutionId, options_);
		URI uri = devopsClient.install(catalogId, solutionId, options_);
		if (isEcho()) {
			Object deploy = getAny(uri, options);
			printObj(deploy);
		}
	}
	
	//
	// Solution
	//

	public void listSolution(String type, String op, String[] cmds, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		SolutionFilter filter = convert(options, SolutionFilter.class);
		debug("Solution: %s %s", filter, pageable);
		Page<Solution> solutions = devopsClient.listSolutions(filter, pageable);
		print(solutions, Solution.class);
	}

	public void getSolution(String type, String op, String[] cmds, Map<String, Object> options) {
		String solutionId = argId(op, cmds);
		SolutionOptions options_ = convert(options, SolutionOptions.class);
		debug("Solution: %s", solutionId);
		Solution solution = devopsClient.getSolution(solutionId, options_);
		printObj(solution);
	}

	public void schemaSolution(String type, String op, String[] cmds, Map<String, Object> options) {
		printLine(schemaToString(Solution.class));
	}

	public void createSolution(String type, String op, String[] cmds, Map<String, Object> options) {
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
		String solutionId = argId(op, cmds);
		debug("Deleting Solution: %s", solutionId);
		devopsClient.deleteSolution(solutionId, null);	
	}

	public void installSolution(String type, String op, String[] cmds, Map<String, Object> options) {
		String solutionId = argId(op, cmds);
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
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		CatalogFilter filter = convert(options, CatalogFilter.class);
		debug("Vcs: %s %s", filter, pageable);
		Page<Catalog> catalogs = devopsClient.listCatalogs(filter, pageable);
		print(catalogs, Catalog.class);
		//TODO
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
		return space;
	}

}