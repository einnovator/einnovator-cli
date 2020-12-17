package org.einnovator.cli;

import static  org.einnovator.util.MappingUtils.convert;
import static  org.einnovator.util.MappingUtils.updateObjectFrom;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bouncycastle.util.Arrays;
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
import org.einnovator.util.MapUtil;
import org.einnovator.util.MappingUtils;
import org.einnovator.util.PageOptions;
import org.einnovator.util.UriUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;


@Component
public class Devops extends CommandRunnerBase {
	public static final String DEVOPS_PREFIX = "devops";

	public static final String DEVOPS_DEFAULT_SERVER = "http://localhost:2500";
	public static final String DEVOPS_MONITOR_SERVER = "http://localhost:2501";

	private static final String CLUSTER_DEFAULT_FORMAT = "name,displayName,provider,region";
	private static final String CLUSTER_WIDE_FORMAT = "name,displayName,provider,region";

	private static final String SPACE_DEFAULT_FORMAT = "name,displayName,cluster.name,cluster.provider,cluster.region";
	private static final String SPACE_WIDE_FORMAT = "name,displayName,cluster.name,cluster.provider,cluster.region";

	private static final String DEPLOYMENT_DEFAULT_FORMAT = "name,displayName,kind,status,availableReplicas:available,desiredReplicas:desired,readyReplicas:ready";
	private static final String DEPLOYMENT_WIDE_FORMAT = "name,displayName,kind,type,category,status,availableReplicas:available,desiredReplicas:desired,readyReplicas:ready,image.name:image,image.registry.name:registry";

	private static final String JOB_DEFAULT_FORMAT = "name,displayName,status";
	private static final String JOB_WIDE_FORMAT = "name,displayName,status";

	private static final String CRONJOB_DEFAULT_FORMAT = "name,displayName,status";
	private static final String CRONJOB_WIDE_FORMAT = "name,displayName,status";

	private static final String DOMAIN_DEFAULT_FORMAT ="name,tls";
	private static final String DOMAIN_WIDE_FORMAT ="name,tls,enabled";

	private static final String REGISTRY_DEFAULT_FORMAT = "name,server,username";
	private static final String REGISTRY_WIDE_FORMAT = "name,server,username";

	private static final String VCS_DEFAULT_FORMAT = "name,url,username";
	private static final String VCS_WIDE_FORMAT = "name,url,username";

	private static final String CATALOG_DEFAULT_FORMAT = "name,type,enabled";
	private static final String CATALOG_WIDE_FORMAT = "name,type,enabled";

	private static final String SOLUTION_DEFAULT_FORMAT = "name,type,url";
	private static final String SOLUTION_WIDE_FORMAT = "name,type,url";

	private static final String BINDING_DEFAULT_FORMAT = "selector";
	private static final String BINDING_WIDE_FORMAT = "selector";

	private static final String CONNECTOR_DEFAULT_FORMAT = "name";
	private static final String CONNECTOR_WIDE_FORMAT = "name";

	private static final String ROUTE_DEFAULT_FORMAT = "name,dns,tls";
	private static final String ROUTE_WIDE_FORMAT = "name,dns,tls";

	private static final String MOUNT_DEFAULT_FORMAT = "name,type,mountPath";
	private static final String MOUNT_WIDE_FORMAT = "name,type,mountPath";

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
			case "delete": case "del": case "d":
				deleteCluster(type, op, cmds, options);
				break;
			default: 
				invalidOp(type, op);
				break;
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
			case "delete": case "del": case "d":
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
			case "delete": case "del": case "d":
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
			case "delete": case "del": case "d":
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
			case "delete": case "del": case "d":
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
			case "delete": case "del": case "d":
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
			case "delete": case "del": case "d":
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
			case "delete": case "del": case "d":
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
			case "delete": case "del": case "d":
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
			case "delete": case "del": case "d":
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
		Page<Cluster> clusters = devopsClient.listClusters(filter, pageable);
		debug("Listing Clusters...");
		debug("Filter:", filter);
		debug("Pageable:", pageable);
		debug("Clusters:");
		if (clusters==null) {
			operationFailed(type, op, options);
			System.exit(-1);
			return;
		}
		if (clusters.getContent()==null || clusters.getContent().isEmpty()) {
			noresources(type, op, options);
			System.exit(0);
			return;
		}
		print(clusters);
	}


	public void getCluster(String type, String op, String[] cmds, Map<String, Object> options) {
		String clusterId = (String)get(new String[] {"id", "uuid", "clustername", "email"}, options);
		ClusterOptions options_ = convert(options, ClusterOptions.class);
		Cluster cluster = devopsClient.getCluster(clusterId, options_);
		debug("Get Cluster...");
		debug("ID:", clusterId);
		debug("Cluster:");
		printObj(cluster);
	}

	public void schemaCluster(String type, String op, String[] cmds, Map<String, Object> options) {
		printLine(schemaToString(Cluster.class));
	}
	
	public void createCluster(String type, String op, String[] cmds, Map<String, Object> options) {
		Cluster cluster = convert(options, Cluster.class);
		debug("Creating Cluster...");
		printObj(cluster);
		URI uri = devopsClient.createCluster(cluster, null);
		printLine("URI:", uri);
		debug("Created Cluster:");
		String id = UriUtils.extractId(uri);
		Cluster cluster2 = devopsClient.getCluster(id, null);
		printObj(cluster2);

	}

	
	public void updateCluster(String type, String op, String[] cmds, Map<String, Object> options) {
		String clusterId = (String)get("cluster", options);
		Cluster cluster = convert(options, Cluster.class);
		debug("Updating Cluster...");
		printObj(cluster);
		devopsClient.updateCluster(cluster, null);
		debug("Updated Cluster:");
		Cluster cluster2 = devopsClient.getCluster(clusterId, null);
		printObj(cluster2);
	}

	public void deleteCluster(String type, String op, String[] cmds, Map<String, Object> options) {
		String clusterId = (String)get(new String[] {"id", "clustername"}, options);
		debug("Deleting Cluster...");
		debug("ID:", clusterId);		
		devopsClient.deleteCluster(clusterId, null);	
	}

	//
	// Spaces
	//
	
	public void listSpace(String type, String op, String[] cmds, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		SpaceFilter filter = convert(options, SpaceFilter.class);
		Page<Space> spaces = devopsClient.listSpaces(filter, pageable);
		debug("Listing Spaces...");
		debug("Filter:", filter);
		debug("Pageable:", pageable);
		debug("Spaces:");
		if (spaces==null) {
			operationFailed(type, op, options);
			System.exit(-1);
			return;
		}
		if (spaces.getContent()==null || spaces.getContent().isEmpty()) {
			noresources(type, op, options);
			System.exit(0);
			return;
		}
		print(spaces);
	}
	
	public void getSpace(String type, String op, String[] cmds, Map<String, Object> options) {
		String spaceId = (String)get(new String[] {"id", "uuid"}, options);
		SpaceOptions options_ = convert(options, SpaceOptions.class);
		Space space = devopsClient.getSpace(spaceId, options_);
		debug("Get Space...");
		debug("ID:", spaceId);
		debug("Space:");
		printObj(space);
	}
	

	public void schemaSpace(String type, String op, String[] cmds, Map<String, Object> options) {
		printLine(schemaToString(Space.class));
	}
	
	public void createSpace(String type, String op, String[] cmds, Map<String, Object> options) {
		Space space = convert(options, Space.class);
		debug("Creating Space...");
		printObj(space);
		URI uri = devopsClient.createSpace(space, null);
		printLine("URI:", uri);
		String spaceId = UriUtils.extractId(uri);
		Space space2 = devopsClient.getSpace(spaceId, null);
		debug("Created Space:");
		printObj(space2);
	}

	public void updateSpace(String type, String op, String[] cmds, Map<String, Object> options) {
		String spaceId = (String)get(new String[] {"id", "uuid"}, options);
		Space space = convert(options, Space.class);
		debug("Updating Space...");
		printObj(space);
		devopsClient.updateSpace(space, null);
		Space space2 = devopsClient.getSpace(spaceId, null);
		debug("Updated Space:");
		printObj(space2);

	}
	
	public void deleteSpace(String type, String op, String[] cmds, Map<String, Object> options) {
		String spaceId = (String)get(new String[] {"id", "uuid"}, options);
		debug("Deleting Space...");
		debug("ID:", spaceId);		
		devopsClient.deleteSpace(spaceId, null);		
	}

	//
	// Deployments
	//
	
	public void listDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		DeploymentFilter filter = convert(options, DeploymentFilter.class);
		String spaceId = argSpaceId(options);
		if (spaceId==null) {
			missingArg(type, op, "-n");
			return;
		}
		Page<Deployment> deployments = devopsClient.listDeployments(spaceId, filter, pageable);
		debug("Listing Deployments...");
		debug("Filter:", filter);
		debug("Pageable:", pageable);
		debug("Deployments:");
		if (deployments==null) {
			operationFailed(type, op, options);
			System.exit(-1);
			return;
		}
		if (deployments.getContent()==null || deployments.getContent().isEmpty()) {
			noresources(type, op, options);
			System.exit(0);
			return;
		}
		print(deployments);
	}
	
	
	private String argSpaceId(Map<String, Object> options) {
		String spaceId = (String)options.get("n");
		if (spaceId!=null) {
			return spaceId;
		}
		return null;
	}
	
	public void getDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		String deploymentId = cmds.length > 0 ? cmds[0] : null;			
		if (deploymentId==null) {
			deploymentId = (String)get(new String[] {"id", "uuid"}, options);
		}
		if (deploymentId==null) {
			error(String.format("missing deployment id"));
			System.exit(-1);
			return;
		}
		String spaceId = argSpaceId(options);
		if (spaceId!=null) {
			if (deploymentId.indexOf("/")<0) {
				deploymentId = spaceId + "/" + deploymentId;
			}
		}
		DeploymentOptions options_ = convert(options, DeploymentOptions.class);
		Deployment deployment = devopsClient.getDeployment(deploymentId, options_);
		debug("Get Deployment...");
		debug("ID:", deploymentId);
		debug("Deployment:");
		printObj(deployment);
	}
	

	public void schemaDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		printLine(schemaToString(Deployment.class));
	}
	
	public void createDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		String spaceId = argSpaceId(options);
		if (spaceId==null) {
			missingArg(type, op, "-n");
			return;
		}
		Deployment deployment = convert(options, Deployment.class);
		debug("Creating Deployment...");
		printObj(deployment);
		URI uri = devopsClient.createDeployment(spaceId, deployment, null);
		printLine("URI:", uri);
		String deploymentId = UriUtils.extractId(uri);
		Deployment deployment2 = devopsClient.getDeployment(deploymentId, null);
		debug("Created Deployment:");
		printObj(deployment2);
	}

	public void updateDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		String spaceId = argSpaceId(options);
		if (spaceId==null) {
			//missingArg(type, op, "-n");
			//return;
		}
		String deploymentId = (String)get(new String[] {"id", "uuid"}, options);
		Deployment deployment = convert(options, Deployment.class);
		debug("Updating Deployment...");
		printObj(deployment);
		devopsClient.updateDeployment(deployment, null);
		Deployment deployment2 = devopsClient.getDeployment(deploymentId, null);
		debug("Updated Deployment:");
		printObj(deployment2);

	}
	
	public void deleteDeployment(String type, String op, String[] cmds, Map<String, Object> options) {
		String spaceId = argSpaceId(options);
		if (spaceId==null) {
			//missingArg(type, op, "-n");
			//return;
		}
		String deploymentId = (String)get(new String[] {"id", "uuid"}, options);
		debug("Deleting Deployment...");
		debug("ID:", deploymentId);		
		devopsClient.deleteDeployment(deploymentId, null);		
	}

	//
	// Jobs
	//
	
	public void listJob(String type, String op, String[] cmds, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		JobFilter filter = convert(options, JobFilter.class);
		String spaceId = argSpaceId(options);
		if (spaceId==null) {
			missingArg(type, op, "-n");
			return;
		}
		Page<Job> jobs = devopsClient.listJobs(spaceId, filter, pageable);
		debug("Listing Jobs...");
		debug("Filter:", filter);
		debug("Pageable:", pageable);
		debug("Jobs:");
		if (jobs==null) {
			operationFailed(type, op, options);
			System.exit(-1);
			return;
		}
		if (jobs.getContent()==null || jobs.getContent().isEmpty()) {
			noresources(type, op, options);
			System.exit(0);
			return;
		}
		print(jobs);
	}

	public void getJob(String type, String op, String[] cmds, Map<String, Object> options) {
		String jobId = (String)get(new String[] {"id", "uuid"}, options);
		String spaceId = argSpaceId(options);
		if (spaceId==null) {
			missingArg(type, op, "-n");
			return;
		}
		JobOptions options_ = convert(options, JobOptions.class);
		Job job = devopsClient.getJob(jobId, options_);
		debug("Get Job...");
		debug("ID:", jobId);
		debug("Job:");
		printObj(job);
	}
	

	public void schemaJob(String type, String op, String[] cmds, Map<String, Object> options) {
		printLine(schemaToString(Job.class));
	}
	
	public void createJob(String type, String op, String[] cmds, Map<String, Object> options) {
		String spaceId = argSpaceId(options);
		if (spaceId==null) {
			missingArg(type, op, "-n");
			return;
		}
		Job job = convert(options, Job.class);
		debug("Creating Job...");
		printObj(job);
		URI uri = devopsClient.createJob(spaceId, job, null);
		printLine("URI:", uri);
		String jobId = UriUtils.extractId(uri);
		Job job2 = devopsClient.getJob(jobId, null);
		debug("Created Job:");
		printObj(job2);
	}

	public void updateJob(String type, String op, String[] cmds, Map<String, Object> options) {
		String spaceId = argSpaceId(options);
		if (spaceId==null) {
			//missingArg(type, op, "-n");
			//return;
		}
		String jobId = (String)get(new String[] {"id", "uuid"}, options);
		Job job = convert(options, Job.class);
		debug("Updating Job...");
		printObj(job);
		devopsClient.updateJob(job, null);
		Job job2 = devopsClient.getJob(jobId, null);
		debug("Updated Job:");
		printObj(job2);

	}
	
	public void deleteJob(String type, String op, String[] cmds, Map<String, Object> options) {
		String spaceId = argSpaceId(options);
		if (spaceId==null) {
			//missingArg(type, op, "-n");
			//return;
		}
		String jobId = (String)get(new String[] {"id", "uuid"}, options);
		debug("Deleting Job...");
		debug("ID:", jobId);		
		devopsClient.deleteJob(jobId, null);		
	}

	//
	// CronJobs
	//
	
	public void listCronJob(String type, String op, String[] cmds, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		CronJobFilter filter = convert(options, CronJobFilter.class);
		String spaceId = argSpaceId(options);
		if (spaceId==null) {
			missingArg(type, op, "-n");
			return;
		}
		Page<CronJob> cronjobs = devopsClient.listCronJobs(spaceId, filter, pageable);
		debug("Listing CronJobs...");
		debug("Filter:", filter);
		debug("Pageable:", pageable);
		debug("CronJobs:");
		if (cronjobs==null) {
			operationFailed(type, op, options);
			System.exit(-1);
			return;
		}
		if (cronjobs.getContent()==null || cronjobs.getContent().isEmpty()) {
			noresources(type, op, options);
			System.exit(0);
			return;
		}
		print(cronjobs);
	}

	public void getCronJob(String type, String op, String[] cmds, Map<String, Object> options) {
		String cronjobId = (String)get(new String[] {"id", "uuid"}, options);
		String spaceId = argSpaceId(options);
		if (spaceId==null) {
			missingArg(type, op, "-n");
			return;
		}
		CronJobOptions options_ = convert(options, CronJobOptions.class);
		CronJob cronjob = devopsClient.getCronJob(cronjobId, options_);
		debug("Get CronJob...");
		debug("ID:", cronjobId);
		debug("CronJob:");
		printObj(cronjob);
	}
	

	public void schemaCronJob(String type, String op, String[] cmds, Map<String, Object> options) {
		printLine(schemaToString(CronJob.class));
	}
	
	public void createCronJob(String type, String op, String[] cmds, Map<String, Object> options) {
		String spaceId = argSpaceId(options);
		if (spaceId==null) {
			missingArg(type, op, "-n");
			return;
		}
		CronJob cronjob = convert(options, CronJob.class);
		debug("Creating CronJob...");
		printObj(cronjob);
		URI uri = devopsClient.createCronJob(spaceId, cronjob, null);
		printLine("URI:", uri);
		String cronjobId = UriUtils.extractId(uri);
		CronJob cronjob2 = devopsClient.getCronJob(cronjobId, null);
		debug("Created CronJob:");
		printObj(cronjob2);
	}

	public void updateCronJob(String type, String op, String[] cmds, Map<String, Object> options) {
		String spaceId = argSpaceId(options);
		if (spaceId==null) {
			//missingArg(type, op, "-n");
			//return;
		}
		String cronjobId = (String)get(new String[] {"id", "uuid"}, options);
		CronJob cronjob = convert(options, CronJob.class);
		debug("Updating CronJob...");
		printObj(cronjob);
		devopsClient.updateCronJob(cronjob, null);
		CronJob cronjob2 = devopsClient.getCronJob(cronjobId, null);
		debug("Updated CronJob:");
		printObj(cronjob2);

	}
	
	public void deleteCronJob(String type, String op, String[] cmds, Map<String, Object> options) {
		String spaceId = argSpaceId(options);
		if (spaceId==null) {
			//missingArg(type, op, "-n");
			//return;
		}
		String cronjobId = (String)get(new String[] {"id", "uuid"}, options);
		debug("Deleting CronJob...");
		debug("ID:", cronjobId);		
		devopsClient.deleteCronJob(cronjobId, null);		
	}

	//
	// Domain
	//

	public void listDomain(String type, String op, String[] cmds, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		DomainFilter filter = convert(options, DomainFilter.class);
		Page<Domain> domains = devopsClient.listDomains(filter, pageable);
		debug("Listing Domains...");
		debug("Filter:", filter);
		debug("Pageable:", pageable);
		debug("Domains:");
		if (domains==null) {
			operationFailed(type, op, options);
			System.exit(-1);
			return;
		}
		if (domains.getContent()==null || domains.getContent().isEmpty()) {
			noresources(type, op, options);
			System.exit(0);
			return;
		}
		print(domains);
	}

	public void getDomain(String type, String op, String[] cmds, Map<String, Object> options) {
		String domainId = (String)get(new String[] {"id", "uuid"}, options);
		DomainOptions options_ = convert(options, DomainOptions.class);
		Domain domain = devopsClient.getDomain(domainId, options_);
		debug("Get Domain...");
		debug("ID:", domainId);
		debug("Domain:");
		printObj(domain);
	}

	public void schemaDomain(String type, String op, String[] cmds, Map<String, Object> options) {
		printLine(schemaToString(Domain.class));
	}

	public void createDomain(String type, String op, String[] cmds, Map<String, Object> options) {
		Domain domain = convert(options, Domain.class);
		Boolean sendMail = null;
		debug(Boolean.TRUE.equals(sendMail) ? "Sending Domain..." : "Creating Domain...");
		debug("Domain", domain);
		URI uri = devopsClient.createDomain(domain, new DomainOptions());
		printLine("URI:", uri);
		String id = UriUtils.extractId(uri);
		Domain domain2 = devopsClient.getDomain(id, null);
		debug("Created Domain:");
		printObj(domain2);
	}
	
	
	public void updateDomain(String type, String op, String[] cmds, Map<String, Object> options) {
		String domainId = (String)get("domain", options);
		Domain domain = convert(options, Domain.class);
		debug("Updating Domain...");
		printObj(domain);
		devopsClient.updateDomain(domain, null);
		debug("Updated Domain:");
		Domain domain2 = devopsClient.getDomain(domainId, null);
		printObj(domain2);
	}

	public void deleteDomain(String type, String op, String[] cmds, Map<String, Object> options) {
		String domainId = (String)get(new String[] {"id", "uuid"}, options);
		debug("Deleting Domain...");
		debug("ID:", domainId);		
		devopsClient.deleteDomain(domainId, null);	
	}


	//
	// Registrys
	//
	
	public void listRegistry(String type, String op, String[] cmds, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		RegistryFilter filter = convert(options, RegistryFilter.class);
		Page<Registry> registrys = devopsClient.listRegistries(filter, pageable);
		debug("Listing Registrys...");
		debug("Filter:", filter);
		debug("Pageable:", pageable);
		debug("Registrys:");
		if (registrys==null) {
			operationFailed(type, op, options);
			System.exit(-1);
			return;
		}
		if (registrys.getContent()==null || registrys.getContent().isEmpty()) {
			noresources(type, op, options);
			System.exit(0);
			return;
		}
		print(registrys);
	}
	
	public void getRegistry(String type, String op, String[] cmds, Map<String, Object> options) {
		String registryId = (String)get(new String[] {"id", "uuid"}, options);
		Registry registry = devopsClient.getRegistry(registryId, null);
		debug("Get Registry...");
		debug("ID:", registryId);
		debug("Registry:");
		printObj(registry);
	}
	

	public void schemaRegistry(String type, String op, String[] cmds, Map<String, Object> options) {
		printLine(schemaToString(Registry.class));
	}

	
	public void createRegistry(String type, String op, String[] cmds, Map<String, Object> options) {
		Registry registry = convert(options, Registry.class);
		debug("Creating Registry...");
		printObj(registry);
		RegistryOptions options_ = convert(options, RegistryOptions.class);
		URI uri = devopsClient.createRegistry(registry, options_);
		printLine("URI:", uri);
		String registryId = UriUtils.extractId(uri);
		Registry registry2 = devopsClient.getRegistry(registryId, null);
		debug("Created Registry:");
		printObj(registry2);
	}

	public void updateRegistry(String type, String op, String[] cmds, Map<String, Object> options) {
		String registryId = (String)get(new String[] {"id", "uuid"}, options);
		Registry registry = convert(options, Registry.class);
		debug("Updating Registry...");
		printObj(registry);
		devopsClient.updateRegistry(registry, null);
		Registry registry2 = devopsClient.getRegistry(registryId, null);
		debug("Updated Registry:");
		printObj(registry2);

	}
	
	public void deleteRegistry(String type, String op, String[] cmds, Map<String, Object> options) {
		String registryId = (String)get(new String[] {"id", "uuid"}, options);
		debug("Deleting Registry...");
		debug("ID:", registryId);		
		devopsClient.deleteRegistry(registryId, null);		
	}



	//
	// Vcss
	//
	

	public void listVcs(String type, String op, String[] cmds, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		VcsFilter filter = convert(options, VcsFilter.class);
		Page<Vcs> vcss = devopsClient.listVcss(filter, pageable);
		debug("Listing Vcss...");
		debug("Filter:", filter);
		debug("Pageable:", pageable);
		debug("Vcss:");
		if (vcss==null) {
			operationFailed(type, op, options);
			System.exit(-1);
			return;
		}
		if (vcss.getContent()==null || vcss.getContent().isEmpty()) {
			noresources(type, op, options);
			System.exit(0);
			return;
		}
		print(vcss);
	}

	public void getVcs(String type, String op, String[] cmds, Map<String, Object> options) {
		String vcsId = (String)get(new String[] {"id", "uuid"}, options);
		VcsOptions options_ = convert(options, VcsOptions.class);
		Vcs vcs = devopsClient.getVcs(vcsId, options_);
		debug("Get Vcs...");
		debug("ID:", vcsId);
		debug("Vcs:");
		printObj(vcs);
	}


	public void schemaVcs(String type, String op, String[] cmds, Map<String, Object> options) {
		printLine(schemaToString(Vcs.class));
	}

	public void createVcs(String type, String op, String[] cmds, Map<String, Object> options) {
		Vcs vcs = convert(options, Vcs.class);
		debug("Creating Vcs...");
		printObj(vcs);
		URI uri = devopsClient.createVcs(vcs, null);
		printLine("URI:", uri);
		debug("Created Vcs:");
		String id = UriUtils.extractId(uri);
		Vcs vcs2 = devopsClient.getVcs(id, null);
		printObj(vcs2);

	}

	
	public void updateVcs(String type, String op, String[] cmds, Map<String, Object> options) {
		String vcsId = (String)get("vcs", options);
		Vcs vcs = convert(options, Vcs.class);
		debug("Updating Vcs...");
		printObj(vcs);
		devopsClient.updateVcs(vcs, null);
		debug("Updated Vcs:");
		Vcs vcs2 = devopsClient.getVcs(vcsId, null);
		printObj(vcs2);
	}

	public void deleteVcs(String type, String op, String[] cmds, Map<String, Object> options) {
		String vcsId = (String)get(new String[] {"id", "uuid"}, options);
		debug("Deleting Vcs...");
		debug("ID:", vcsId);		
		devopsClient.deleteVcs(vcsId, null);	
	}

	//
	// Catalog
	//

	public void listCatalog(String type, String op, String[] cmds, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		CatalogFilter filter = convert(options, CatalogFilter.class);
		Page<Catalog> catalogs = devopsClient.listCatalogs(filter, pageable);
		debug("Listing Catalogs...");
		debug("Filter:", filter);
		debug("Pageable:", pageable);
		debug("Catalogs:");
		if (catalogs==null) {
			operationFailed(type, op, options);
			System.exit(-1);
			return;
		}
		if (catalogs.getContent()==null || catalogs.getContent().isEmpty()) {
			noresources(type, op, options);
			System.exit(0);
			return;
		}
		print(catalogs);
	}

	public void getCatalog(String type, String op, String[] cmds, Map<String, Object> options) {
		String catalogId = (String)get(new String[] {"id", "uuid"}, options);
		CatalogOptions options_ = convert(options, CatalogOptions.class);
		Catalog catalog = devopsClient.getCatalog(catalogId, options_);
		debug("Get Catalog...");
		debug("ID:", catalogId);
		debug("Catalog:");
		printObj(catalog);
	}

	public void schemaCatalog(String type, String op, String[] cmds, Map<String, Object> options) {
		printLine(schemaToString(Catalog.class));
	}

	public void createCatalog(String type, String op, String[] cmds, Map<String, Object> options) {
		Catalog catalog = convert(options, Catalog.class);
		Boolean sendMail = null;
		debug(Boolean.TRUE.equals(sendMail) ? "Sending Catalog..." : "Creating Catalog...");
		debug("Catalog", catalog);
		URI uri = devopsClient.createCatalog(catalog, new CatalogOptions());
		printLine("URI:", uri);
		String id = UriUtils.extractId(uri);
		Catalog catalog2 = devopsClient.getCatalog(id, null);
		debug("Created Catalog:");
		printObj(catalog2);
	}
	
	
	public void updateCatalog(String type, String op, String[] cmds, Map<String, Object> options) {
		String catalogId = (String)get("catalog", options);
		Catalog catalog = convert(options, Catalog.class);
		debug("Updating Catalog...");
		printObj(catalog);
		devopsClient.updateCatalog(catalog, null);
		debug("Updated Catalog:");
		Catalog catalog2 = devopsClient.getCatalog(catalogId, null);
		printObj(catalog2);
	}

	public void deleteCatalog(String type, String op, String[] cmds, Map<String, Object> options) {
		String catalogId = (String)get(new String[] {"id", "uuid"}, options);
		debug("Deleting Catalog...");
		debug("ID:", catalogId);		
		devopsClient.deleteCatalog(catalogId, null);	
	}


	//
	// Solution
	//

	public void listSolution(String type, String op, String[] cmds, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		SolutionFilter filter = convert(options, SolutionFilter.class);
		Page<Solution> solutions = devopsClient.listSolutions(filter, pageable);
		debug("Listing Solutions...");
		debug("Filter:", filter);
		debug("Pageable:", pageable);
		debug("Solutions:");
		if (solutions==null) {
			operationFailed(type, op, options);
			System.exit(-1);
			return;
		}
		if (solutions.getContent()==null || solutions.getContent().isEmpty()) {
			noresources(type, op, options);
			System.exit(0);
			return;
		}
		print(solutions);
	}

	public void getSolution(String type, String op, String[] cmds, Map<String, Object> options) {
		String solutionId = (String)get(new String[] {"id", "uuid"}, options);
		SolutionOptions options_ = convert(options, SolutionOptions.class);
		Solution solution = devopsClient.getSolution(solutionId, options_);
		debug("Get Solution...");
		debug("ID:", solutionId);
		debug("Solution:");
		printObj(solution);
	}

	public void schemaSolution(String type, String op, String[] cmds, Map<String, Object> options) {
		printLine(schemaToString(Solution.class));
	}

	public void createSolution(String type, String op, String[] cmds, Map<String, Object> options) {
		Solution solution = convert(options, Solution.class);
		Boolean sendMail = null;
		debug(Boolean.TRUE.equals(sendMail) ? "Sending Solution..." : "Creating Solution...");
		debug("Solution", solution);
		URI uri = devopsClient.createSolution(solution, new SolutionOptions());
		printLine("URI:", uri);
		String id = UriUtils.extractId(uri);
		Solution solution2 = devopsClient.getSolution(id, null);
		debug("Created Solution:");
		printObj(solution2);
	}
	
	
	public void updateSolution(String type, String op, String[] cmds, Map<String, Object> options) {
		String solutionId = (String)get("solution", options);
		Solution solution = convert(options, Solution.class);
		debug("Updating Solution...");
		printObj(solution);
		devopsClient.updateSolution(solution, null);
		debug("Updated Solution:");
		Solution solution2 = devopsClient.getSolution(solutionId, null);
		printObj(solution2);
	}

	public void deleteSolution(String type, String op, String[] cmds, Map<String, Object> options) {
		String solutionId = (String)get(new String[] {"id", "uuid"}, options);
		debug("Deleting Solution...");
		debug("ID:", solutionId);		
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
	
}