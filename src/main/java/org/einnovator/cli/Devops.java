package org.einnovator.cli;

import static  org.einnovator.util.MappingUtils.updateObjectFrom;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import org.einnovator.devops.client.DevopsClient;
import org.einnovator.devops.client.config.DevopsClientConfiguration;
import org.einnovator.devops.client.model.Binding;
import org.einnovator.devops.client.model.Catalog;
import org.einnovator.devops.client.model.Cluster;
import org.einnovator.devops.client.model.Connector;
import org.einnovator.devops.client.model.CronJob;
import org.einnovator.devops.client.model.Deployment;
import org.einnovator.devops.client.model.Domain;
import org.einnovator.devops.client.model.Job;
import org.einnovator.devops.client.model.Mount;
import org.einnovator.devops.client.model.Registry;
import org.einnovator.devops.client.model.Route;
import org.einnovator.devops.client.model.Solution;
import org.einnovator.devops.client.model.Space;
import org.einnovator.devops.client.model.Vcs;
import org.einnovator.devops.client.model.Webhook;
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
import org.einnovator.devops.client.modelx.JobFilter;
import org.einnovator.devops.client.modelx.JobOptions;
import org.einnovator.devops.client.modelx.RegistryFilter;
import org.einnovator.devops.client.modelx.RegistryOptions;
import org.einnovator.devops.client.modelx.SolutionFilter;
import org.einnovator.devops.client.modelx.SolutionOptions;
import org.einnovator.devops.client.modelx.SpaceFilter;
import org.einnovator.devops.client.modelx.SpaceOptions;
import org.einnovator.devops.client.modelx.VcsFilter;
import org.einnovator.devops.client.modelx.VcsOptions;
import org.einnovator.util.PageOptions;
import org.einnovator.util.PageUtil;
import org.einnovator.util.UriUtils;
import org.einnovator.util.model.EntityOptions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;


@Component
public class Devops extends CommandRunnerBase {
	public static final String DEVOPS_PREFIX = "devops";

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
	private static final String JOB_WIDE_FORMAT = "id,name,displayName,status";

	private static final String CRONJOB_DEFAULT_FORMAT = "id,name,displayName,status";
	private static final String CRONJOB_WIDE_FORMAT = "id,name,displayName,status";

	private static final String DOMAIN_DEFAULT_FORMAT ="id,name,tls";
	private static final String DOMAIN_WIDE_FORMAT ="id,name,tls,enabled";

	private static final String REGISTRY_DEFAULT_FORMAT = "id,name,server,username";
	private static final String REGISTRY_WIDE_FORMAT = "id,name,server,username";

	private static final String VCS_DEFAULT_FORMAT = "id,name,url,username";
	private static final String VCS_WIDE_FORMAT = "id,name,url,username";

	private static final String CATALOG_DEFAULT_FORMAT = "id,name,type,enabled";
	private static final String CATALOG_WIDE_FORMAT = "id,name,type,enabled";

	private static final String SOLUTION_DEFAULT_FORMAT = "id,name,type,url";
	private static final String SOLUTION_WIDE_FORMAT = "id,name,type,url";

	private static final String BINDING_DEFAULT_FORMAT = "selector";
	private static final String BINDING_WIDE_FORMAT = "selector";

	private static final String CONNECTOR_DEFAULT_FORMAT = "id,name";
	private static final String CONNECTOR_WIDE_FORMAT = "id,name";

	private static final String ROUTE_DEFAULT_FORMAT = "id,name,dns,tls";
	private static final String ROUTE_WIDE_FORMAT = "id,name,dns,tls";

	private static final String MOUNT_DEFAULT_FORMAT = "id,name,type,mountPath";
	private static final String MOUNT_WIDE_FORMAT = "id,name,type,mountPath";

	private DevopsClient devopsClient;

	private String server = DEVOPS_DEFAULT_SERVER;
	
	private DevopsClientConfiguration config = new DevopsClientConfiguration();

	@Override
	public void init(String[] cmds, Map<String, Object> options, OAuth2RestTemplate template) {
		super.init(cmds, options, template);
		updateObjectFrom(config, convert(options, DevopsClientConfiguration.class));
		config.setServer(server);
		devopsClient = new DevopsClient(template, config);
	}
	
	@Override
	public void setEndpoints(Map<String, Object> endpoints) {
		String server = (String)endpoints.get("server");
		if (server!=null) {
			this.server = server;
		}
	}
	
	@Override
	public String getPrefix() {
		return DEVOPS_PREFIX;
	}
	

	String[] DEVOPS_COMMANDS = new String[] { 
		"cluster", "clusters", "c",
		"space", "spaces", "namespace", "namespaces", "ns",
		"deploy", "deployment", "deploys", "deployments",
		"job", "jobs",
		"cronjob", "cronjobs",
		"domain", "domains",
		"registry", "registries", "docker",
		"vcs", "vcss", "v", "git",
		"catalog", "catalogs",
		"solution", "solutions",
		};

	@Override
	protected String[] getCommands() {
		return DEVOPS_COMMANDS;
	}

	
	public void run(String type, String op, String[] cmds, Map<String, Object> options) {		
		switch (type) {
		case "cluster": case "clusters": case "c":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getCluster(type, op, cmds, options);
				break;
			case "list": case "l": case "":
				listCluster(type, op, cmds, options);
				break;
			case "schema": case "meta":
				schemaCluster(type, op, cmds, options);
				break;
			case "create": case "c":
				createCluster(type, op, cmds, options);
				break;
			case "update": case "u":
				updateCluster(type, op, cmds, options);
				break;
			case "delete": case "del": case "rm": case "d":
				deleteCluster(type, op, cmds, options);
				break;
			default: 
				invalidOp(type, op);
			}
			break;
		case "space": case "spaces": case "g":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getSpace(type, op, cmds, options);
				break;
			case "list": case "l": case "":
				listSpace(type, op, cmds, options);
				break;
			case "schema": case "meta":
				schemaSpace(type, op, cmds, options);
				break;
			case "create": case "c":
				createSpace(type, op, cmds, options);
				break;
			case "update": case "u":
				updateSpace(type, op, cmds, options);
				break;
			case "delete": case "del": case "rm": case "d":
				deleteSpace(type, op, cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "deploy": case "deployment": case "deploys": case "deployments":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getDeployment(type, op, cmds, options);
				break;
			case "list": case "l": case "":
				listDeployment(type, op, cmds, options);
				break;
			case "schema": case "meta":
				schemaDeployment(type, op, cmds, options);
				break;
			case "create": case "c":
				createDeployment(type, op, cmds, options);
				break;
			case "update": case "u":
				updateDeployment(type, op, cmds, options);
				break;
			case "delete": case "del": case "rm": case "d":
				deleteDeployment(type, op, cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;			
		case "job":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getJob(type, op, cmds, options);
				break;
			case "list": case "l": case "":
				listJob(type, op, cmds, options);
				break;
			case "schema": case "meta":
				schemaJob(type, op, cmds, options);
				break;
			case "create": case "c":
				createJob(type, op, cmds, options);
				break;
			case "update": case "u":
				updateJob(type, op, cmds, options);
				break;
			case "delete": case "del": case "rm": case "d":
				deleteJob(type, op, cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;			
		case "cronjob":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getCronJob(type, op, cmds, options);
				break;
			case "list": case "l": case "":
				listCronJob(type, op, cmds, options);
				break;
			case "schema": case "meta":
				schemaCronJob(type, op, cmds, options);
				break;
			case "create": case "c":
				createCronJob(type, op, cmds, options);
				break;
			case "update": case "u":
				updateCronJob(type, op, cmds, options);
				break;
			case "delete": case "del": case "rm": case "d":
				deleteCronJob(type, op, cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;			
		case "domain": case "domains":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getDomain(type, op, cmds, options);
				break;
			case "list": case "l": case "":
				listDomain(type, op, cmds, options);
				break;
			case "schema": case "meta":
				schemaDomain(type, op, cmds, options);
				break;
			case "create": case "c":
				createDomain(type, op, cmds, options);
				break;
			case "update": case "u":
				updateDomain(type, op, cmds, options);
				break;
			case "delete": case "del": case "rm": case "d":
				deleteDomain(type, op, cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "registry": case "registries": case "r":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getRegistry(type, op, cmds, options);
				break;
			case "list": case "l": case "":
				listRegistry(type, op, cmds, options);
				break;
			case "schema": case "meta":
				schemaRegistry(type, op, cmds, options);
				break;
			case "create": case "c":
				createRegistry(type, op, cmds, options);
				break;
			case "update": case "u":
				updateRegistry(type, op, cmds, options);
				break;
			case "delete": case "del": case "rm": case "d":
				deleteRegistry(type, op, cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "vcs": case "vcss": case "v": case "git":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getVcs(type, op, cmds, options);
				break;
			case "list": case "l": case "":
				listVcs(type, op, cmds, options);
				break;
			case "schema": case "meta":
				schemaVcs(type, op, cmds, options);
				break;
			case "create": case "c":
				createVcs(type, op, cmds, options);
				break;
			case "update": case "u":
				updateVcs(type, op, cmds, options);
				break;
			case "delete": case "del": case "rm": case "d":
				deleteVcs(type, op, cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "catalog": case "catalogs":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getCatalog(type, op, cmds, options);
				break;
			case "list": case "l": case "":
				listCatalog(type, op, cmds, options);
				break;
			case "schema": case "meta":
				schemaCatalog(type, op, cmds, options);
				break;
			case "create": case "c":
				createCatalog(type, op, cmds, options);
				break;
			case "update": case "u":
				updateCatalog(type, op, cmds, options);
				break;
			case "delete": case "del": case "rm": case "d":
				deleteCatalog(type, op, cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "solution": case "solutions":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getSolution(type, op, cmds, options);
				break;
			case "list": case "l": case "":
				listSolution(type, op, cmds, options);
				break;
			case "schema": case "meta":
				schemaSolution(type, op, cmds, options);
				break;
			case "create": case "c":
				createSolution(type, op, cmds, options);
				break;
			case "update": case "u":
				updateSolution(type, op, cmds, options);
				break;
			case "delete": case "del": case "rm": case "d":
				deleteSolution(type, op, cmds, options);
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
		printLine("Cluster URI:", uri);
		String id = UriUtils.extractId(uri);
		Cluster cluster2 = devopsClient.getCluster(id, null);
		printObj(cluster2);

	}

	
	public void updateCluster(String type, String op, String[] cmds, Map<String, Object> options) {
		String clusterId = argId(op, cmds);
		Cluster cluster = convert(options, Cluster.class);
		debug("Updating Cluster: %s %s", clusterId, cluster);
		devopsClient.updateCluster(cluster, null);
		Cluster cluster2 = devopsClient.getCluster(clusterId, null);
		printObj(cluster2);
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
		Page<Space> spaces = devopsClient.listSpaces(filter, pageable);
		debug("Spaces: %s %s", filter, pageable);
		print(spaces, Space.class);
	}
	
	public void getSpace(String type, String op, String[] cmds, Map<String, Object> options) {
		String spaceId = argId(op, cmds);
		SpaceOptions options_ = convert(options, SpaceOptions.class);
		debug("Space: %s", spaceId);
		Space space = devopsClient.getSpace(spaceId, options_);
		printObj(space);
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
		printLine("Space URI:", uri);
		String spaceId = UriUtils.extractId(uri);
		Space space2 = devopsClient.getSpace(spaceId, null);
		printObj(space2);
	}

	public void updateSpace(String type, String op, String[] cmds, Map<String, Object> options) {
		String spaceId = argIdx(op, cmds);
		Space space = convert(options, Space.class);
		debug("Updating Space: %s %s", spaceId, space);
		printObj(space);
		devopsClient.updateSpace(space, null);
		Space space2 = devopsClient.getSpace(spaceId, null);
		debug("Updated Space: %s", spaceId);
		printObj(space2);
	}
	
	public void deleteSpace(String type, String op, String[] cmds, Map<String, Object> options) {
		String spaceId = argIdx(op, cmds);
		debug("Deleting Space: %s", spaceId);		
		devopsClient.deleteSpace(spaceId, null);		
	}
	
	// Util
	
	private String argId(String op, String[] cmds, boolean required) {
		String id = cmds.length > 0 ? cmds[0] : null;
		if (required && id==null) {
			error(String.format("missing resource id"));
			System.exit(-1);
			return null;
		}
		return id;
	}

	private String argName(String op, String[] cmds, boolean required) {
		String id = cmds.length > 0 ? cmds[0] : null;
		if (required && id==null) {
			error(String.format("missing resource name"));
			System.exit(-1);
			return null;
		}
		return id;
	}

	private String argName(String op, String[] cmds) {
		return argName(op, cmds, true);
	}

	private String argId(String op, String[] cmds) {
		return argId(op, cmds, true);
	}
	
	private String argNS(Map<String, Object> options) {
		String spaceId = (String)options.get("n");
		if (spaceId!=null) {
			return spaceId;
		}
		return null;
	}
	
	private String argIdx(String op, String[] cmds) {
		String id = cmds.length > 0 ? cmds[0] : null;
		if (id==null) {
			error(String.format("missing resource id"));
			System.exit(-1);
			return null;
		}
		try {
			Long.parseLong(id);			
			return id;
		} catch (IllegalArgumentException e) {			
		}
		try {
			UUID.fromString(id);
			return id;
		} catch (IllegalArgumentException e) {			
		}
		String spaceId = argNS(options);
		if (spaceId!=null) {
			if (id.indexOf("/")<0) {
				id = spaceId + "/" + id;
			}
		}
		return id;
	}
	

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
		printLine("Deployment URI:", uri);
		String deployId = UriUtils.extractId(uri);
		Deployment deployment2 = devopsClient.getDeployment(deployId, null);
		printObj(deployment2);
	}

	public void updateDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		String deployId = argIdx(op, cmds);
		Deployment deployment = convert(options, Deployment.class);
		debug("Updating Deployment: %s %s", deployId, deployment);
		devopsClient.updateDeployment(deployment, null);
		Deployment deployment2 = devopsClient.getDeployment(deployId, null);
		printObj(deployment2);
	}
	
	public void deleteDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		String deployId = argIdx(op, cmds);
		debug("Deleting Deployment: %s", deployId);		
		devopsClient.deleteDeployment(deployId, null);		
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
		printLine("Job URI:", uri);
		String jobId = UriUtils.extractId(uri);
		Job job2 = devopsClient.getJob(jobId, null);
		printObj(job2);
	}

	public void updateJob(String type, String op, String[] cmds, Map<String, Object> options) {
		String jobId = argIdx(op, cmds);
		Job job = convert(options, Job.class);
		debug("Updating Job: %s %s", jobId, job);
		devopsClient.updateJob(job, null);
		Job job2 = devopsClient.getJob(jobId, null);
		printObj(job2);

	}
	
	public void deleteJob(String type, String op, String[] cmds, Map<String, Object> options) {
		String jobId = argIdx(op, cmds);
		debug("Deleting Job: %s", jobId);		
		devopsClient.deleteJob(jobId, null);		
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
		printLine("CronJob URI:", uri);
		String cronjobId = UriUtils.extractId(uri);
		CronJob cronjob2 = devopsClient.getCronJob(cronjobId, null);
		printObj(cronjob2);
	}

	public void updateCronJob(String type, String op, String[] cmds, Map<String, Object> options) {
		String cronjobId = argIdx(op, cmds);
		CronJob cronjob = convert(options, CronJob.class);
		debug("Updating CronJob: %s %s", cronjobId, cronjob);
		devopsClient.updateCronJob(cronjob, null);
		CronJob cronjob2 = devopsClient.getCronJob(cronjobId, null);
		printObj(cronjob2);

	}
	
	public void deleteCronJob(String type, String op, String[] cmds, Map<String, Object> options) {
		String cronjobId = argIdx(op, cmds);
		debug("Deleting CronJob: %s", cronjobId);
		devopsClient.deleteCronJob(cronjobId, null);		
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
		printLine("Domain URI:", uri);
		String id = UriUtils.extractId(uri);
		Domain domain2 = devopsClient.getDomain(id, null);
		printObj(domain2);
	}
	
	
	public void updateDomain(String type, String op, String[] cmds, Map<String, Object> options) {
		String domainId = (String)get("domain", options);
		Domain domain = convert(options, Domain.class);
		debug("Updating Domain: %s %s", domainId, domain);
		devopsClient.updateDomain(domain, null);
		Domain domain2 = devopsClient.getDomain(domainId, null);
		printObj(domain2);
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
		printLine("Registry URI:", uri);
		String registryId = UriUtils.extractId(uri);
		Registry registry2 = devopsClient.getRegistry(registryId, null);
		printObj(registry2);
	}

	public void updateRegistry(String type, String op, String[] cmds, Map<String, Object> options) {
		String registryId = argId(op, cmds);
		Registry registry = convert(options, Registry.class);
		debug("Updating Registry: %s %s", registryId, registry);
		printObj(registry);
		devopsClient.updateRegistry(registry, null);
		Registry registry2 = devopsClient.getRegistry(registryId, null);
		printObj(registry2);

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
		printLine("Vcs URI:", uri);
		String id = UriUtils.extractId(uri);
		Vcs vcs2 = devopsClient.getVcs(id, null);
		printObj(vcs2);

	}

	
	public void updateVcs(String type, String op, String[] cmds, Map<String, Object> options) {
		String vcsId = argId(op, cmds);
		Vcs vcs = convert(options, Vcs.class);
		debug("Updating Vcs: %s %s", vcsId, vcs);
		devopsClient.updateVcs(vcs, null);
		Vcs vcs2 = devopsClient.getVcs(vcsId, null);
		printObj(vcs2);
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
		printLine("Catalog URI:", uri);
		String id = UriUtils.extractId(uri);
		Catalog catalog2 = devopsClient.getCatalog(id, null);
		printObj(catalog2);
	}
	
	
	public void updateCatalog(String type, String op, String[] cmds, Map<String, Object> options) {
		String catalogId = argId(op, cmds);
		Catalog catalog = convert(options, Catalog.class);
		debug("Updating Catalog: %s %s", catalogId, catalog);
		devopsClient.updateCatalog(catalog, null);
		Catalog catalog2 = devopsClient.getCatalog(catalogId, null);
		printObj(catalog2);
	}

	public void deleteCatalog(String type, String op, String[] cmds, Map<String, Object> options) {
		String catalogId = argId(op, cmds);
		debug("Deleting Catalog: %s", catalogId);		
		devopsClient.deleteCatalog(catalogId, null);	
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
		printLine("Solution URI:", uri);
		String id = UriUtils.extractId(uri);
		Solution solution2 = devopsClient.getSolution(id, null);
		printObj(solution2);
	}
	
	
	public void updateSolution(String type, String op, String[] cmds, Map<String, Object> options) {
		String solutionId = argId(op, cmds);
		Solution solution = convert(options, Solution.class);
		debug("Updating Solution: %s %s", solutionId, solution);
		devopsClient.updateSolution(solution, null);
		Solution solution2 = devopsClient.getSolution(solutionId, null);
		printObj(solution2);
	}

	public void deleteSolution(String type, String op, String[] cmds, Map<String, Object> options) {
		String solutionId = argId(op, cmds);
		debug("Deleting Solution: %s", solutionId);
		devopsClient.deleteSolution(solutionId, null);	
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
		return null;
	}

	
}