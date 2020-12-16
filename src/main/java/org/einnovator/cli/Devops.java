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
import org.einnovator.devops.client.model.DeploymentKind;
import org.einnovator.devops.client.model.DeploymentStatus;
import org.einnovator.devops.client.model.Domain;
import org.einnovator.devops.client.model.Job;
import org.einnovator.devops.client.model.Mount;
import org.einnovator.devops.client.model.Registry;
import org.einnovator.devops.client.model.Route;
import org.einnovator.devops.client.model.Solution;
import org.einnovator.devops.client.model.SolutionCategory;
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
import org.einnovator.util.StringUtil;
import org.einnovator.util.UriUtils;
import org.einnovator.util.meta.MetaUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;


@Component
public class Devops extends CommandRunnerBase {
	public static final String DEVOPS_PREFIX = "devops";

	private static final String CLUSTER_DEFAULT_FORMAT = "name,displayName,provider,region";
	private static final String CLUSTER_WIDE_FORMAT = "name,displayName,provider,region";

	private static final String SPACE_DEFAULT_FORMAT = "name,displayName,cluster.name,cluster.provider,cluster.region";
	private static final String SPACE_WIDE_FORMAT = "name,displayName,cluster.name,cluster.provider,cluster.region";

	private static final String DEPLOYMENT_DEFAULT_FORMAT = "name,displayName,kind,status,availableReplicas,desiredReplicas,readyReplicas";
	private static final String DEPLOYMENT_WIDE_FORMAT = "name,displayName,kind,type,category,status,availableReplicas,desiredReplicas,readyReplicas,image.name";

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

	private DevopsClientConfiguration config = new DevopsClientConfiguration();

	public void init(String[] cmds, Map<String, Object> args, OAuth2RestTemplate template) {
		super.init(cmds, args, template);
		updateObjectFrom(config, convert(args, DevopsClientConfiguration.class));
		config.setServer("http://localhost:2501");
		devopsClient = new DevopsClient(template, config);
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

	protected String[] getCommands() {
		return DEVOPS_COMMANDS;
	}

	
	public void run(String type, String op, Map<String, Object> argsMap, String[] args) {		
		switch (type) {
		case "cluster": case "clusters": case "c":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getCluster(type, op, argsMap);
				break;
			case "list": case "l": case "":
				listCluster(type, op, argsMap);
				break;
			case "schema": case "meta":
				schemaCluster(type, op, argsMap);
				break;
			case "create": case "c":
				createCluster(type, op, argsMap);
				break;
			case "update": case "u":
				updateCluster(type, op, argsMap);
				break;
			case "delete": case "del": case "d":
				deleteCluster(type, op, argsMap);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "space": case "spaces": case "g":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getSpace(type, op, argsMap);
				break;
			case "list": case "l": case "":
				listSpace(type, op, argsMap);
				break;
			case "schema": case "meta":
				schemaSpace(type, op, argsMap);
				break;
			case "create": case "c":
				createSpace(type, op, argsMap);
				break;
			case "update": case "u":
				updateSpace(type, op, argsMap);
				break;
			case "delete": case "del": case "d":
				deleteSpace(type, op, argsMap);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "deploy": case "deployment": case "deploys": case "deployments":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getDeployment(type, op, argsMap);
				break;
			case "list": case "l": case "":
				listDeployment(type, op, argsMap);
				break;
			case "schema": case "meta":
				schemaDeployment(type, op, argsMap);
				break;
			case "create": case "c":
				createDeployment(type, op, argsMap);
				break;
			case "update": case "u":
				updateDeployment(type, op, argsMap);
				break;
			case "delete": case "del": case "d":
				deleteDeployment(type, op, argsMap);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;			
		case "job":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getJob(type, op, argsMap);
				break;
			case "list": case "l": case "":
				listJob(type, op, argsMap);
				break;
			case "schema": case "meta":
				schemaJob(type, op, argsMap);
				break;
			case "create": case "c":
				createJob(type, op, argsMap);
				break;
			case "update": case "u":
				updateJob(type, op, argsMap);
				break;
			case "delete": case "del": case "d":
				deleteJob(type, op, argsMap);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;			
		case "cronjob":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getCronJob(type, op, argsMap);
				break;
			case "list": case "l": case "":
				listCronJob(type, op, argsMap);
				break;
			case "schema": case "meta":
				schemaCronJob(type, op, argsMap);
				break;
			case "create": case "c":
				createCronJob(type, op, argsMap);
				break;
			case "update": case "u":
				updateCronJob(type, op, argsMap);
				break;
			case "delete": case "del": case "d":
				deleteCronJob(type, op, argsMap);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;			
		case "domain": case "domains":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getDomain(type, op, argsMap);
				break;
			case "list": case "l": case "":
				listDomain(type, op, argsMap);
				break;
			case "schema": case "meta":
				schemaDomain(type, op, argsMap);
				break;
			case "create": case "c":
				createDomain(type, op, argsMap);
				break;
			case "update": case "u":
				updateDomain(type, op, argsMap);
				break;
			case "delete": case "del": case "d":
				deleteDomain(type, op, argsMap);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "registry": case "registries": case "r":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getRegistry(type, op, argsMap);
				break;
			case "list": case "l": case "":
				listRegistry(type, op, argsMap);
				break;
			case "schema": case "meta":
				schemaRegistry(type, op, argsMap);
				break;
			case "create": case "c":
				createRegistry(type, op, argsMap);
				break;
			case "update": case "u":
				updateRegistry(type, op, argsMap);
				break;
			case "delete": case "del": case "d":
				deleteRegistry(type, op, argsMap);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "vcs": case "vcss": case "v": case "git":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getVcs(type, op, argsMap);
				break;
			case "list": case "l": case "":
				listVcs(type, op, argsMap);
				break;
			case "schema": case "meta":
				schemaVcs(type, op, argsMap);
				break;
			case "create": case "c":
				createVcs(type, op, argsMap);
				break;
			case "update": case "u":
				updateVcs(type, op, argsMap);
				break;
			case "delete": case "del": case "d":
				deleteVcs(type, op, argsMap);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "catalog": case "catalogs":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getCatalog(type, op, argsMap);
				break;
			case "list": case "l": case "":
				listCatalog(type, op, argsMap);
				break;
			case "schema": case "meta":
				schemaCatalog(type, op, argsMap);
				break;
			case "create": case "c":
				createCatalog(type, op, argsMap);
				break;
			case "update": case "u":
				updateCatalog(type, op, argsMap);
				break;
			case "delete": case "del": case "d":
				deleteCatalog(type, op, argsMap);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "solution": case "solutions":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getSolution(type, op, argsMap);
				break;
			case "list": case "l": case "":
				listSolution(type, op, argsMap);
				break;
			case "schema": case "meta":
				schemaSolution(type, op, argsMap);
				break;
			case "create": case "c":
				createSolution(type, op, argsMap);
				break;
			case "update": case "u":
				updateSolution(type, op, argsMap);
				break;
			case "delete": case "del": case "d":
				deleteSolution(type, op, argsMap);
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
	
	public void listCluster(String type, String op, Map<String, Object> args) {
		Pageable pageable = convert(args, PageOptions.class).toPageRequest();
		ClusterFilter filter = convert(args, ClusterFilter.class);
		Page<Cluster> clusters = devopsClient.listClusters(filter, pageable);
		debug("Listing Clusters...");
		debug("Filter:", filter);
		debug("Pageable:", pageable);
		debug("Clusters:");
		if (clusters==null) {
			operationFailed(type, op, args);
			System.exit(-1);
			return;
		}
		if (clusters.getContent()==null || clusters.getContent().isEmpty()) {
			noresources(type, op, args);
			System.exit(0);
			return;
		}
		printObj(clusters);
	}


	public void getCluster(String type, String op, Map<String, Object> args) {
		String clusterId = (String)get(new String[] {"id", "uuid", "clustername", "email"}, args);
		ClusterOptions options = convert(args, ClusterOptions.class);
		Cluster cluster = devopsClient.getCluster(clusterId, options);
		debug("Get Cluster...");
		debug("ID:", clusterId);
		debug("Cluster:");
		printObj(cluster);
	}

	public void schemaCluster(String type, String op, Map<String, Object> args) {
		printLine(schemaToString(Cluster.class));
	}
	
	public void createCluster(String type, String op, Map<String, Object> args) {
		Cluster cluster = convert(args, Cluster.class);
		debug("Creating Cluster...");
		printObj(cluster);
		URI uri = devopsClient.createCluster(cluster, null);
		printLine("URI:", uri);
		printObj("Created Cluster:");
		String id = UriUtils.extractId(uri);
		Cluster cluster2 = devopsClient.getCluster(id, null);
		printObj(cluster2);

	}

	
	public void updateCluster(String type, String op, Map<String, Object> args) {
		String clusterId = (String)get("cluster", args);
		Cluster cluster = convert(args, Cluster.class);
		debug("Updating Cluster...");
		printObj(cluster);
		devopsClient.updateCluster(cluster, null);
		printObj("Updated Cluster:");
		Cluster cluster2 = devopsClient.getCluster(clusterId, null);
		printObj(cluster2);
	}

	public void deleteCluster(String type, String op, Map<String, Object> args) {
		String clusterId = (String)get(new String[] {"id", "clustername"}, args);
		debug("Deleting Cluster...");
		debug("ID:", clusterId);		
		devopsClient.deleteCluster(clusterId, null);	
	}

	//
	// Spaces
	//
	
	public void listSpace(String type, String op, Map<String, Object> args) {
		Pageable pageable = convert(args, PageOptions.class).toPageRequest();
		SpaceFilter filter = convert(args, SpaceFilter.class);
		Page<Space> spaces = devopsClient.listSpaces(filter, pageable);
		debug("Listing Spaces...");
		debug("Filter:", filter);
		debug("Pageable:", pageable);
		debug("Spaces:");
		if (spaces==null) {
			operationFailed(type, op, args);
			System.exit(-1);
			return;
		}
		if (spaces.getContent()==null || spaces.getContent().isEmpty()) {
			noresources(type, op, args);
			System.exit(0);
			return;
		}
		printObj(spaces);
	}
	
	public void getSpace(String type, String op, Map<String, Object> args) {
		String spaceId = (String)get(new String[] {"id", "uuid"}, args);
		SpaceOptions options = convert(args, SpaceOptions.class);
		Space space = devopsClient.getSpace(spaceId, options);
		debug("Get Space...");
		debug("ID:", spaceId);
		debug("Space:");
		printObj(space);
	}
	

	public void schemaSpace(String type, String op, Map<String, Object> args) {
		printLine(schemaToString(Space.class));
	}
	
	public void createSpace(String type, String op, Map<String, Object> args) {
		Space space = convert(args, Space.class);
		debug("Creating Space...");
		printObj(space);
		URI uri = devopsClient.createSpace(space, null);
		printLine("URI:", uri);
		String spaceId = UriUtils.extractId(uri);
		Space space2 = devopsClient.getSpace(spaceId, null);
		printObj("Created Space:");
		printObj(space2);
	}

	public void updateSpace(String type, String op, Map<String, Object> args) {
		String spaceId = (String)get(new String[] {"id", "uuid"}, args);
		Space space = convert(args, Space.class);
		debug("Updating Space...");
		printObj(space);
		devopsClient.updateSpace(space, null);
		Space space2 = devopsClient.getSpace(spaceId, null);
		printObj("Updated Space:");
		printObj(space2);

	}
	
	public void deleteSpace(String type, String op, Map<String, Object> args) {
		String spaceId = (String)get(new String[] {"id", "uuid"}, args);
		debug("Deleting Space...");
		debug("ID:", spaceId);		
		devopsClient.deleteSpace(spaceId, null);		
	}

	//
	// Deployments
	//
	
	public void listDeployment(String type, String op, Map<String, Object> args) {
		Pageable pageable = convert(args, PageOptions.class).toPageRequest();
		DeploymentFilter filter = convert(args, DeploymentFilter.class);
		String spaceId = argSpaceId(args);
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
			operationFailed(type, op, args);
			System.exit(-1);
			return;
		}
		if (deployments.getContent()==null || deployments.getContent().isEmpty()) {
			noresources(type, op, args);
			System.exit(0);
			return;
		}
		printObj(deployments);
	}
	
	
	private String argSpaceId(Map<String, Object> args) {
		String spaceId = (String)args.get("n");
		if (spaceId!=null) {
			return spaceId;
		}
		return null;
	}
	
	public void getDeployment(String type, String op, Map<String, Object> args) {
		String deploymentId = cmds.length > 0 ? cmds[0] : null;			
		if (deploymentId==null) {
			deploymentId = (String)get(new String[] {"id", "uuid"}, args);
		}
		if (deploymentId==null) {
			error(String.format("missing deployment id"));
			System.exit(-1);
			return;
		}
		String spaceId = argSpaceId(args);
		if (spaceId!=null) {
			if (deploymentId.indexOf("/")<0) {
				deploymentId = spaceId + "/" + deploymentId;
			}
		}
		DeploymentOptions options = convert(args, DeploymentOptions.class);
		Deployment deployment = devopsClient.getDeployment(deploymentId, options);
		debug("Get Deployment...");
		debug("ID:", deploymentId);
		debug("Deployment:");
		printObj(deployment);
	}
	

	public void schemaDeployment(String type, String op, Map<String, Object> args) {
		printLine(schemaToString(Deployment.class));
	}
	
	public void createDeployment(String type, String op, Map<String, Object> args) {
		String spaceId = argSpaceId(args);
		if (spaceId==null) {
			missingArg(type, op, "-n");
			return;
		}
		Deployment deployment = convert(args, Deployment.class);
		debug("Creating Deployment...");
		printObj(deployment);
		URI uri = devopsClient.createDeployment(spaceId, deployment, null);
		printLine("URI:", uri);
		String deploymentId = UriUtils.extractId(uri);
		Deployment deployment2 = devopsClient.getDeployment(deploymentId, null);
		printObj("Created Deployment:");
		printObj(deployment2);
	}

	public void updateDeployment(String type, String op, Map<String, Object> args) {
		String spaceId = argSpaceId(args);
		if (spaceId==null) {
			//missingArg(type, op, "-n");
			//return;
		}
		String deploymentId = (String)get(new String[] {"id", "uuid"}, args);
		Deployment deployment = convert(args, Deployment.class);
		debug("Updating Deployment...");
		printObj(deployment);
		devopsClient.updateDeployment(deployment, null);
		Deployment deployment2 = devopsClient.getDeployment(deploymentId, null);
		printObj("Updated Deployment:");
		printObj(deployment2);

	}
	
	public void deleteDeployment(String type, String op, Map<String, Object> args) {
		String spaceId = argSpaceId(args);
		if (spaceId==null) {
			//missingArg(type, op, "-n");
			//return;
		}
		String deploymentId = (String)get(new String[] {"id", "uuid"}, args);
		debug("Deleting Deployment...");
		debug("ID:", deploymentId);		
		devopsClient.deleteDeployment(deploymentId, null);		
	}

	//
	// Jobs
	//
	
	public void listJob(String type, String op, Map<String, Object> args) {
		Pageable pageable = convert(args, PageOptions.class).toPageRequest();
		JobFilter filter = convert(args, JobFilter.class);
		String spaceId = argSpaceId(args);
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
			operationFailed(type, op, args);
			System.exit(-1);
			return;
		}
		if (jobs.getContent()==null || jobs.getContent().isEmpty()) {
			noresources(type, op, args);
			System.exit(0);
			return;
		}
		printObj(jobs);
	}

	public void getJob(String type, String op, Map<String, Object> args) {
		String jobId = (String)get(new String[] {"id", "uuid"}, args);
		String spaceId = argSpaceId(args);
		if (spaceId==null) {
			missingArg(type, op, "-n");
			return;
		}
		JobOptions options = convert(args, JobOptions.class);
		Job job = devopsClient.getJob(jobId, options);
		debug("Get Job...");
		debug("ID:", jobId);
		debug("Job:");
		printObj(job);
	}
	

	public void schemaJob(String type, String op, Map<String, Object> args) {
		printLine(schemaToString(Job.class));
	}
	
	public void createJob(String type, String op, Map<String, Object> args) {
		String spaceId = argSpaceId(args);
		if (spaceId==null) {
			missingArg(type, op, "-n");
			return;
		}
		Job job = convert(args, Job.class);
		debug("Creating Job...");
		printObj(job);
		URI uri = devopsClient.createJob(spaceId, job, null);
		printLine("URI:", uri);
		String jobId = UriUtils.extractId(uri);
		Job job2 = devopsClient.getJob(jobId, null);
		printObj("Created Job:");
		printObj(job2);
	}

	public void updateJob(String type, String op, Map<String, Object> args) {
		String spaceId = argSpaceId(args);
		if (spaceId==null) {
			//missingArg(type, op, "-n");
			//return;
		}
		String jobId = (String)get(new String[] {"id", "uuid"}, args);
		Job job = convert(args, Job.class);
		debug("Updating Job...");
		printObj(job);
		devopsClient.updateJob(job, null);
		Job job2 = devopsClient.getJob(jobId, null);
		printObj("Updated Job:");
		printObj(job2);

	}
	
	public void deleteJob(String type, String op, Map<String, Object> args) {
		String spaceId = argSpaceId(args);
		if (spaceId==null) {
			//missingArg(type, op, "-n");
			//return;
		}
		String jobId = (String)get(new String[] {"id", "uuid"}, args);
		debug("Deleting Job...");
		debug("ID:", jobId);		
		devopsClient.deleteJob(jobId, null);		
	}

	//
	// CronJobs
	//
	
	public void listCronJob(String type, String op, Map<String, Object> args) {
		Pageable pageable = convert(args, PageOptions.class).toPageRequest();
		CronJobFilter filter = convert(args, CronJobFilter.class);
		String spaceId = argSpaceId(args);
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
			operationFailed(type, op, args);
			System.exit(-1);
			return;
		}
		if (cronjobs.getContent()==null || cronjobs.getContent().isEmpty()) {
			noresources(type, op, args);
			System.exit(0);
			return;
		}
		printObj(cronjobs);
	}

	public void getCronJob(String type, String op, Map<String, Object> args) {
		String cronjobId = (String)get(new String[] {"id", "uuid"}, args);
		String spaceId = argSpaceId(args);
		if (spaceId==null) {
			missingArg(type, op, "-n");
			return;
		}
		CronJobOptions options = convert(args, CronJobOptions.class);
		CronJob cronjob = devopsClient.getCronJob(cronjobId, options);
		debug("Get CronJob...");
		debug("ID:", cronjobId);
		debug("CronJob:");
		printObj(cronjob);
	}
	

	public void schemaCronJob(String type, String op, Map<String, Object> args) {
		printLine(schemaToString(CronJob.class));
	}
	
	public void createCronJob(String type, String op, Map<String, Object> args) {
		String spaceId = argSpaceId(args);
		if (spaceId==null) {
			missingArg(type, op, "-n");
			return;
		}
		CronJob cronjob = convert(args, CronJob.class);
		debug("Creating CronJob...");
		printObj(cronjob);
		URI uri = devopsClient.createCronJob(spaceId, cronjob, null);
		printLine("URI:", uri);
		String cronjobId = UriUtils.extractId(uri);
		CronJob cronjob2 = devopsClient.getCronJob(cronjobId, null);
		printObj("Created CronJob:");
		printObj(cronjob2);
	}

	public void updateCronJob(String type, String op, Map<String, Object> args) {
		String spaceId = argSpaceId(args);
		if (spaceId==null) {
			//missingArg(type, op, "-n");
			//return;
		}
		String cronjobId = (String)get(new String[] {"id", "uuid"}, args);
		CronJob cronjob = convert(args, CronJob.class);
		debug("Updating CronJob...");
		printObj(cronjob);
		devopsClient.updateCronJob(cronjob, null);
		CronJob cronjob2 = devopsClient.getCronJob(cronjobId, null);
		printObj("Updated CronJob:");
		printObj(cronjob2);

	}
	
	public void deleteCronJob(String type, String op, Map<String, Object> args) {
		String spaceId = argSpaceId(args);
		if (spaceId==null) {
			//missingArg(type, op, "-n");
			//return;
		}
		String cronjobId = (String)get(new String[] {"id", "uuid"}, args);
		debug("Deleting CronJob...");
		debug("ID:", cronjobId);		
		devopsClient.deleteCronJob(cronjobId, null);		
	}

	//
	// Domain
	//

	public void listDomain(String type, String op, Map<String, Object> args) {
		Pageable pageable = convert(args, PageOptions.class).toPageRequest();
		DomainFilter filter = convert(args, DomainFilter.class);
		Page<Domain> domains = devopsClient.listDomains(filter, pageable);
		debug("Listing Domains...");
		debug("Filter:", filter);
		debug("Pageable:", pageable);
		debug("Domains:");
		if (domains==null) {
			operationFailed(type, op, args);
			System.exit(-1);
			return;
		}
		if (domains.getContent()==null || domains.getContent().isEmpty()) {
			noresources(type, op, args);
			System.exit(0);
			return;
		}
		printObj(domains);
	}

	public void getDomain(String type, String op, Map<String, Object> args) {
		String domainId = (String)get(new String[] {"id", "uuid"}, args);
		DomainOptions options = convert(args, DomainOptions.class);
		Domain domain = devopsClient.getDomain(domainId, options);
		debug("Get Domain...");
		debug("ID:", domainId);
		debug("Domain:");
		printObj(domain);
	}

	public void schemaDomain(String type, String op, Map<String, Object> args) {
		printLine(schemaToString(Domain.class));
	}

	public void createDomain(String type, String op, Map<String, Object> args) {
		Domain domain = convert(args, Domain.class);
		Boolean sendMail = null;
		debug(Boolean.TRUE.equals(sendMail) ? "Sending Domain..." : "Creating Domain...");
		debug("Domain", domain);
		URI uri = devopsClient.createDomain(domain, new DomainOptions());
		printLine("URI:", uri);
		String id = UriUtils.extractId(uri);
		Domain domain2 = devopsClient.getDomain(id, null);
		printObj("Created Domain:");
		printObj(domain2);
	}
	
	
	public void updateDomain(String type, String op, Map<String, Object> args) {
		String domainId = (String)get("domain", args);
		Domain domain = convert(args, Domain.class);
		debug("Updating Domain...");
		printObj(domain);
		devopsClient.updateDomain(domain, null);
		printObj("Updated Domain:");
		Domain domain2 = devopsClient.getDomain(domainId, null);
		printObj(domain2);
	}

	public void deleteDomain(String type, String op, Map<String, Object> args) {
		String domainId = (String)get(new String[] {"id", "uuid"}, args);
		debug("Deleting Domain...");
		debug("ID:", domainId);		
		devopsClient.deleteDomain(domainId, null);	
	}


	//
	// Registrys
	//
	
	public void listRegistry(String type, String op, Map<String, Object> args) {
		Pageable pageable = convert(args, PageOptions.class).toPageRequest();
		RegistryFilter filter = convert(args, RegistryFilter.class);
		Page<Registry> registrys = devopsClient.listRegistries(filter, pageable);
		debug("Listing Registrys...");
		debug("Filter:", filter);
		debug("Pageable:", pageable);
		debug("Registrys:");
		if (registrys==null) {
			operationFailed(type, op, args);
			System.exit(-1);
			return;
		}
		if (registrys.getContent()==null || registrys.getContent().isEmpty()) {
			noresources(type, op, args);
			System.exit(0);
			return;
		}
		printObj(registrys);
	}
	
	public void getRegistry(String type, String op, Map<String, Object> args) {
		String registryId = (String)get(new String[] {"id", "uuid"}, args);
		Registry registry = devopsClient.getRegistry(registryId, null);
		debug("Get Registry...");
		debug("ID:", registryId);
		debug("Registry:");
		printObj(registry);
	}
	

	public void schemaRegistry(String type, String op, Map<String, Object> args) {
		printLine(schemaToString(Registry.class));
	}

	
	public void createRegistry(String type, String op, Map<String, Object> args) {
		Registry registry = convert(args, Registry.class);
		debug("Creating Registry...");
		printObj(registry);
		RegistryOptions options = convert(args, RegistryOptions.class);
		URI uri = devopsClient.createRegistry(registry, options);
		printLine("URI:", uri);
		String registryId = UriUtils.extractId(uri);
		Registry registry2 = devopsClient.getRegistry(registryId, null);
		printObj("Created Registry:");
		printObj(registry2);
	}

	public void updateRegistry(String type, String op, Map<String, Object> args) {
		String registryId = (String)get(new String[] {"id", "uuid"}, args);
		Registry registry = convert(args, Registry.class);
		debug("Updating Registry...");
		printObj(registry);
		devopsClient.updateRegistry(registry, null);
		Registry registry2 = devopsClient.getRegistry(registryId, null);
		printObj("Updated Registry:");
		printObj(registry2);

	}
	
	public void deleteRegistry(String type, String op, Map<String, Object> args) {
		String registryId = (String)get(new String[] {"id", "uuid"}, args);
		debug("Deleting Registry...");
		debug("ID:", registryId);		
		devopsClient.deleteRegistry(registryId, null);		
	}



	//
	// Vcss
	//
	

	public void listVcs(String type, String op, Map<String, Object> args) {
		Pageable pageable = convert(args, PageOptions.class).toPageRequest();
		VcsFilter filter = convert(args, VcsFilter.class);
		Page<Vcs> vcss = devopsClient.listVcss(filter, pageable);
		debug("Listing Vcss...");
		debug("Filter:", filter);
		debug("Pageable:", pageable);
		debug("Vcss:");
		if (vcss==null) {
			operationFailed(type, op, args);
			System.exit(-1);
			return;
		}
		if (vcss.getContent()==null || vcss.getContent().isEmpty()) {
			noresources(type, op, args);
			System.exit(0);
			return;
		}
		printObj(vcss);
	}

	public void getVcs(String type, String op, Map<String, Object> args) {
		String vcsId = (String)get(new String[] {"id", "uuid"}, args);
		VcsOptions options = convert(args, VcsOptions.class);
		Vcs vcs = devopsClient.getVcs(vcsId, options);
		debug("Get Vcs...");
		debug("ID:", vcsId);
		debug("Vcs:");
		printObj(vcs);
	}


	public void schemaVcs(String type, String op, Map<String, Object> args) {
		printLine(schemaToString(Vcs.class));
	}

	public void createVcs(String type, String op, Map<String, Object> args) {
		Vcs vcs = convert(args, Vcs.class);
		debug("Creating Vcs...");
		printObj(vcs);
		URI uri = devopsClient.createVcs(vcs, null);
		printLine("URI:", uri);
		printObj("Created Vcs:");
		String id = UriUtils.extractId(uri);
		Vcs vcs2 = devopsClient.getVcs(id, null);
		printObj(vcs2);

	}

	
	public void updateVcs(String type, String op, Map<String, Object> args) {
		String vcsId = (String)get("vcs", args);
		Vcs vcs = convert(args, Vcs.class);
		debug("Updating Vcs...");
		printObj(vcs);
		devopsClient.updateVcs(vcs, null);
		printObj("Updated Vcs:");
		Vcs vcs2 = devopsClient.getVcs(vcsId, null);
		printObj(vcs2);
	}

	public void deleteVcs(String type, String op, Map<String, Object> args) {
		String vcsId = (String)get(new String[] {"id", "uuid"}, args);
		debug("Deleting Vcs...");
		debug("ID:", vcsId);		
		devopsClient.deleteVcs(vcsId, null);	
	}

	//
	// Catalog
	//

	public void listCatalog(String type, String op, Map<String, Object> args) {
		Pageable pageable = convert(args, PageOptions.class).toPageRequest();
		CatalogFilter filter = convert(args, CatalogFilter.class);
		Page<Catalog> catalogs = devopsClient.listCatalogs(filter, pageable);
		debug("Listing Catalogs...");
		debug("Filter:", filter);
		debug("Pageable:", pageable);
		debug("Catalogs:");
		if (catalogs==null) {
			operationFailed(type, op, args);
			System.exit(-1);
			return;
		}
		if (catalogs.getContent()==null || catalogs.getContent().isEmpty()) {
			noresources(type, op, args);
			System.exit(0);
			return;
		}
		printObj(catalogs);
	}

	public void getCatalog(String type, String op, Map<String, Object> args) {
		String catalogId = (String)get(new String[] {"id", "uuid"}, args);
		CatalogOptions options = convert(args, CatalogOptions.class);
		Catalog catalog = devopsClient.getCatalog(catalogId, options);
		debug("Get Catalog...");
		debug("ID:", catalogId);
		debug("Catalog:");
		printObj(catalog);
	}

	public void schemaCatalog(String type, String op, Map<String, Object> args) {
		printLine(schemaToString(Catalog.class));
	}

	public void createCatalog(String type, String op, Map<String, Object> args) {
		Catalog catalog = convert(args, Catalog.class);
		Boolean sendMail = null;
		debug(Boolean.TRUE.equals(sendMail) ? "Sending Catalog..." : "Creating Catalog...");
		debug("Catalog", catalog);
		URI uri = devopsClient.createCatalog(catalog, new CatalogOptions());
		printLine("URI:", uri);
		String id = UriUtils.extractId(uri);
		Catalog catalog2 = devopsClient.getCatalog(id, null);
		printObj("Created Catalog:");
		printObj(catalog2);
	}
	
	
	public void updateCatalog(String type, String op, Map<String, Object> args) {
		String catalogId = (String)get("catalog", args);
		Catalog catalog = convert(args, Catalog.class);
		debug("Updating Catalog...");
		printObj(catalog);
		devopsClient.updateCatalog(catalog, null);
		printObj("Updated Catalog:");
		Catalog catalog2 = devopsClient.getCatalog(catalogId, null);
		printObj(catalog2);
	}

	public void deleteCatalog(String type, String op, Map<String, Object> args) {
		String catalogId = (String)get(new String[] {"id", "uuid"}, args);
		debug("Deleting Catalog...");
		debug("ID:", catalogId);		
		devopsClient.deleteCatalog(catalogId, null);	
	}


	//
	// Solution
	//

	public void listSolution(String type, String op, Map<String, Object> args) {
		Pageable pageable = convert(args, PageOptions.class).toPageRequest();
		SolutionFilter filter = convert(args, SolutionFilter.class);
		Page<Solution> solutions = devopsClient.listSolutions(filter, pageable);
		debug("Listing Solutions...");
		debug("Filter:", filter);
		debug("Pageable:", pageable);
		debug("Solutions:");
		if (solutions==null) {
			operationFailed(type, op, args);
			System.exit(-1);
			return;
		}
		if (solutions.getContent()==null || solutions.getContent().isEmpty()) {
			noresources(type, op, args);
			System.exit(0);
			return;
		}
		printObj(solutions);
	}

	public void getSolution(String type, String op, Map<String, Object> args) {
		String solutionId = (String)get(new String[] {"id", "uuid"}, args);
		SolutionOptions options = convert(args, SolutionOptions.class);
		Solution solution = devopsClient.getSolution(solutionId, options);
		debug("Get Solution...");
		debug("ID:", solutionId);
		debug("Solution:");
		printObj(solution);
	}

	public void schemaSolution(String type, String op, Map<String, Object> args) {
		printLine(schemaToString(Solution.class));
	}

	public void createSolution(String type, String op, Map<String, Object> args) {
		Solution solution = convert(args, Solution.class);
		Boolean sendMail = null;
		debug(Boolean.TRUE.equals(sendMail) ? "Sending Solution..." : "Creating Solution...");
		debug("Solution", solution);
		URI uri = devopsClient.createSolution(solution, new SolutionOptions());
		printLine("URI:", uri);
		String id = UriUtils.extractId(uri);
		Solution solution2 = devopsClient.getSolution(id, null);
		printObj("Created Solution:");
		printObj(solution2);
	}
	
	
	public void updateSolution(String type, String op, Map<String, Object> args) {
		String solutionId = (String)get("solution", args);
		Solution solution = convert(args, Solution.class);
		debug("Updating Solution...");
		printObj(solution);
		devopsClient.updateSolution(solution, null);
		printObj("Updated Solution:");
		Solution solution2 = devopsClient.getSolution(solutionId, null);
		printObj(solution2);
	}

	public void deleteSolution(String type, String op, Map<String, Object> args) {
		String solutionId = (String)get(new String[] {"id", "uuid"}, args);
		debug("Deleting Solution...");
		debug("ID:", solutionId);		
		devopsClient.deleteSolution(solutionId, null);	
	}


	//
	// Util
	//
	
	public <T> T get(String name, Map<String, Object> map, T defaultValue) {
		@SuppressWarnings("unchecked")
		T value = (T)map.get(name);
		if (value==null) {
			value = defaultValue;
		}
		return value;
	}
	
	public Object get(String[] names, Map<String, Object> map) {
		for (String name: names) {
			Object value = map.get(name);
			if (value!=null) {
				return value;
			}
		}
		return null;
	}
	

	//
	// Print
	//
	
	void print(Object obj) {
		print(obj, 0);
	}

	void println(Object obj) {
		print(obj, 0);
		System.out.println();
	}

	@SuppressWarnings("rawtypes")
	void print(Object obj, int n) {
		if (obj instanceof Iterable) {
			for (Object o: (Iterable)obj) {
				print(o, n+1);
			}
			return;
		}
		System.out.print(String.format((n>0 ? "%" + (n+1) + "s" : "") + "%s", "", format(obj)));
	}

	void printW(Object obj, int n) {
		System.out.print(String.format("%" + (n>1 ? "-" + n : "") + "s", formatSimple(obj)));		
	}

	void print(Page<?> page) {
		print(page.getContent());
	}
	
	void print(Iterable<?> it) {
		boolean ln = false;
		boolean hrule = false;
		if (isTabular()) {
			String fmt = null;
			List<List<String>> table = new ArrayList<>();
			for (Object o: it) {
				if (fmt==null) {
					fmt = getFormat(o);
				}
				List<String> row = getFields(o, fmt);
				table.add(row);
			}
			int[] widths = getColsWidth(table);
			String[] cols = getFormatCols(fmt);
			if (cols.length>widths.length) {
				widths = Arrays.copyOf(widths, cols.length);
			}
			for (int j = 0; j<cols.length; j++) {
				if (cols[j].length()>widths[j]) {
					widths[j] = cols[j].length();
				}
			}
			for (int j = 0; j<cols.length; j++) {
				String col = formatColName(cols[j]);
				printW(col, widths[j]+3);
			}
			System.out.println();
			if (hrule) {
				for (int j = 0; j<cols.length; j++) {
					System.out.print(new String(new char[widths[j]+3]).replace('\0', '-'));
				}
				System.out.println();				
			}
			int i = 0;
			for (List<String> row: table) {
				if (ln) {
					print("[%-" + digits(table.size()) + "s] ", i);
				}
				int j = 0;
				for (String value: row) {
					printW(value, widths[j]+3);
					j++;
				}
				System.out.println();
				i++;
			}
		} else {
			String fmt = null;
			for (Object o: it) {
				if (fmt==null) {
					fmt = getFormat(o);
				}
				print(o, 0);
			}			
		}

	}

	void printObj(Object obj) {
		String fmt = getFormat(obj);
		if (isTabular()) {
			List<String> values = getFields(obj, fmt);
			String[] cols = getFormatCols(fmt);
			int[] widths = new int[Math.max(values.size(), cols.length)];
			for (int i=0; i<widths.length; i++) {
				widths[i] = i<cols.length && i<values.size() ? Math.max(values.get(i).length(), cols[i].length()) :
					i<cols.length ? cols[i].length() : values.get(i).length();
			}
			for (int j = 0; j<cols.length; j++) {
				String col = formatColName(cols[j]);
				printW(col, widths[j]+3);
			}
			System.out.println();
			int j = 0;
			for (String value: values) {
				printW(value, widths[j]+3);
				j++;
			}
			System.out.println();
		} else {
			print(obj, 0);
		}		
	}
	
	private String formatColName(String col) {
		return col.toUpperCase();
	}

	private int digits(int n) {
		return (int)(Math.log10(n)+1);
	}

	private int[] getColsWidth(List<List<String>> table) {
		int[] ww = new int[getWidth(table)];
		for (List<String> row: table) {
			int i = 0;
			for (String value: row) {
				int l = value!=null ? value.length() : 0;
				if (l>ww[i]) {
					ww[i] = l;
				}
				i++;
			}				
		}
		return ww;
	}

	private int getWidth(List<List<String>> table) {
		int w = 0;
		for (List<String> row: table) {
			if (row.size()>w) {
				w = row.size();
			}
		}
		return w;
	}

	void printLine(Object... objs) {
		for (Object obj: objs) {
			System.out.print(obj);		
		}
		System.out.println();		
	}
	
	protected boolean isTabular() {
		String o = getFormat();
		if (o==null) {
			return true;
		}
		switch (o) {
		case "wide":
		case "raw":
		case "json":
		case "yaml":
			return false;
		default: 
			return true;
		}
	}
	
	protected String getFormat() {
		String o = (String)argsMap.get("o");
		if (o==null || o.isEmpty()) {
			return null;
		}
		o = o.toLowerCase();
		return o;
	}

	protected String getFormat(Object obj) {
		String o = getFormat();
		if (o==null || o.isEmpty()) {
			o = getDefaultFormat(obj.getClass());
		}
		return o;
	}
	
	String[] getFormatCols(String fmt) {
		String[] cols = fmt.split(",");
		return cols;
	}
	
	String format(Object obj) {
		if (obj==null) {
			return "";
		}
		String o = getFormat();
		if (o==null || o.isEmpty()) {
			o = getDefaultFormat(obj.getClass());
		}
		if (o==null || o.isEmpty()) {
			o = getDefaultFormat(obj.getClass());
		} else if ("wide".equals(o)) {
			o = getWideFormat(obj.getClass());
		}  else if ("raw".equals(o)) {
			return obj.toString();
		} else if ("json".equals(o)) {
			String s = MappingUtils.toJson(obj);
			if (s==null) {
				s = "";
			}
			return s;
		} else if ("yaml".equals(o)) {
			String s = MappingUtils.toJson(obj);
			if (s==null) {
				s = "";
			}
			return s;
		}
		if (o!=null && !o.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			Map<String, Object> map = MappingUtils.toMap(obj);
			if (map==null) {
				sb.append("???");
			} else if (o=="+") {
				for (Map.Entry<String, Object> e: map.entrySet()) {
					if (sb.length()>0) {
						sb.append(" ");						
					}
					sb.append(e.getValue());
				}
			} else {
				String[] a = o.split(",");
				for (String s: a) {
					if (!s.isEmpty()) {
						sb.append(" ");						
					}
					Object value = MapUtil.resolve(s, map);
					if (value==null) {
						value = "";
					}
					sb.append(value);						
				}
			}
			return sb.toString();
		} else {
			return obj.toString();			
		}
	}

	String formatCols(Object obj, String fmt) {
		StringBuilder sb = new StringBuilder();
		Map<String, Object> map = MappingUtils.toMap(obj);
		if (map==null) {
			sb.append("???");
		} else if (fmt=="+") {
			for (Map.Entry<String, Object> e: map.entrySet()) {
				if (sb.length()>0) {
					sb.append(" ");						
				}
				sb.append(e.getValue());
			}
		} else {
			String[] a = fmt.split(",");
			for (String s: a) {
				if (!s.isEmpty()) {
					sb.append(" ");						
				}
				Object value = MapUtil.resolve(s, map);
				if (value==null) {
					value = "";
				}
				sb.append(value);						
			}
		}
		return sb.toString();
	}
	
	List<String> getFields(Object obj, String fmt) {
		List<String> values = new ArrayList<>();
		Map<String, Object> map = MappingUtils.toMap(obj);
		if (map==null) {
		} else if (fmt=="+") {
			for (Map.Entry<String, Object> e: map.entrySet()) {
				values.add(formatSimple(e.getValue()));						
			}
		} else {
			String[] a = fmt.split(",");
			for (String s: a) {
				Object value = MapUtil.resolve(s, map);
				values.add(formatSimple(value));						
			}
		}
		return values;
	}

	private String formatSimple(Object value) {
		if (value==null) {
			return "";
		}
		return value.toString();
	}

	protected void debug(Object... s) {
		if (isDebug()) {
			System.out.println(s);
		}
	}
	
	protected boolean isDebug() {
		String s = (String)argsMap.get("debug");
		return s!=null;
	}
	
	private String getDefaultFormat(Class<? extends Object> type) {
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

	private String getWideFormat(Class<? extends Object> type) {
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