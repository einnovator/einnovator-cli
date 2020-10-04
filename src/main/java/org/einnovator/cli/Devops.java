package org.einnovator.cli;

import static  org.einnovator.util.MappingUtils.convert;
import static  org.einnovator.util.MappingUtils.updateObjectFrom;

import java.net.URI;
import java.util.Map;

import org.einnovator.devops.client.DevopsClient;
import org.einnovator.devops.client.config.DevopsClientConfiguration;
import org.einnovator.devops.client.model.Cluster;
import org.einnovator.devops.client.model.Domain;
import org.einnovator.devops.client.model.Registry;
import org.einnovator.devops.client.model.Space;
import org.einnovator.devops.client.model.Vcs;
import org.einnovator.devops.client.modelx.ClusterFilter;
import org.einnovator.devops.client.modelx.DomainFilter;
import org.einnovator.devops.client.modelx.DomainOptions;
import org.einnovator.devops.client.modelx.RegistryFilter;
import org.einnovator.devops.client.modelx.SpaceFilter;
import org.einnovator.devops.client.modelx.VcsFilter;
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

	private DevopsClient devopsClient;

	private DevopsClientConfiguration config = new DevopsClientConfiguration();

	public void init(Map<String, Object> args, OAuth2RestTemplate template) {
		super.init(args, template);
		updateObjectFrom(config, convert(args, DevopsClientConfiguration.class));
		config.setServer("http://localhost:2501");
		devopsClient = new DevopsClient(template, config);
	}
	
	@Override
	public String getPrefix() {
		return DEVOPS_PREFIX;
	}
	
	public void run(String type, String op, Map<String, Object> argsMap, String[] args) {
		System.out.println("!1!");
		
		switch (type) {
		case "cluster": case "clusters": case "c":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getCluster(argsMap);
				break;
			case "list": case "l": case "":
				System.out.println("!3!");
				listClusters(argsMap);
				break;
			case "create": case "c":
				createCluster(argsMap);
				break;
			case "update": case "u":
				updateCluster(argsMap);
				break;
			case "delete": case "del": case "d":
				deleteCluster(argsMap);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "space": case "spaces": case "g":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getSpace(argsMap);
				break;
			case "list": case "l": case "":
				listSpaces(argsMap);
				break;
			case "create": case "c":
				createSpace(argsMap);
				break;
			case "update": case "u":
				updateSpace(argsMap);
				break;
			case "delete": case "del": case "d":
				deleteSpace(argsMap);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "domain": case "domains": case "d":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getDomain(argsMap);
				break;
			case "list": case "l": case "":
				listDomains(argsMap);
				break;
			case "create": case "c":
				createDomain(argsMap);
				break;
			case "update": case "u":
				updateDomain(argsMap);
				break;
			case "delete": case "del": case "d":
				deleteDomain(argsMap);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "registry": case "registries": case "r":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getRegistry(argsMap);
				break;
			case "list": case "l": case "":
				listRegistrys(argsMap);
				break;
			case "create": case "c":
				createRegistry(argsMap);
				break;
			case "update": case "u":
				updateRegistry(argsMap);
				break;
			case "delete": case "del": case "d":
				deleteRegistry(argsMap);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "vcs": case "vcss": case "v": case "git":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getVcs(argsMap);
				break;
			case "list": case "l": case "":
				listVcss(argsMap);
				break;
			case "create": case "c":
				createVcs(argsMap);
				break;
			case "update": case "u":
				updateVcs(argsMap);
				break;
			case "delete": case "del": case "d":
				deleteVcs(argsMap);
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
	
	public void listClusters(Map<String, Object> args) {
		Pageable pageable = convert(args, PageOptions.class).toPageRequest();
		ClusterFilter filter = convert(args, ClusterFilter.class);
		Page<Cluster> clusters = devopsClient.listClusters(filter, pageable);
		printLine("Listing Clusters...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("Clusters:");
		print(clusters);
	}

	public void getCluster(Map<String, Object> args) {
		String clusterId = (String)get(new String[] {"id", "uuid", "clustername", "email"}, args);
		Cluster cluster = devopsClient.getCluster(clusterId, null);
		printLine("Get Cluster...");
		printLine("ID:", clusterId);
		printLine("Cluster:");
		print(cluster);
	}

	public void createCluster(Map<String, Object> args) {
		Cluster cluster = convert(args, Cluster.class);
		printLine("Creating Cluster...");
		print(cluster);
		URI uri = devopsClient.createCluster(cluster, null);
		printLine("URI:", uri);
		print("Created Cluster:");
		String id = UriUtils.extractId(uri);
		Cluster cluster2 = devopsClient.getCluster(id, null);
		print(cluster2);

	}

	
	public void updateCluster(Map<String, Object> args) {
		String clusterId = (String)get("cluster", args);
		Cluster cluster = convert(args, Cluster.class);
		printLine("Updating Cluster...");
		print(cluster);
		devopsClient.updateCluster(cluster, null);
		print("Updated Cluster:");
		Cluster cluster2 = devopsClient.getCluster(clusterId, null);
		print(cluster2);
	}

	public void deleteCluster(Map<String, Object> args) {
		String clusterId = (String)get(new String[] {"id", "clustername"}, args);
		printLine("Deleting Cluster...");
		printLine("ID:", clusterId);		
		devopsClient.deleteCluster(clusterId, null);	
	}

	//
	// Spaces
	//
	
	public void listSpaces(Map<String, Object> args) {
		Pageable pageable = convert(args, PageOptions.class).toPageRequest();
		SpaceFilter filter = convert(args, SpaceFilter.class);
		Page<Space> spaces = devopsClient.listSpaces(filter, pageable);
		printLine("Listing Spaces...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("Spaces:");
		print(spaces);
	}
	
	public void getSpace(Map<String, Object> args) {
		String spaceId = (String)get(new String[] {"id", "uuid"}, args);
		Space space = devopsClient.getSpace(spaceId, null);
		printLine("Get Space...");
		printLine("ID:", spaceId);
		printLine("Space:");
		print(space);
	}
	
	public void createSpace(Map<String, Object> args) {
		Space space = convert(args, Space.class);
		printLine("Creating Space...");
		print(space);
		URI uri = devopsClient.createSpace(space, null);
		printLine("URI:", uri);
		String spaceId = UriUtils.extractId(uri);
		Space space2 = devopsClient.getSpace(spaceId, null);
		print("Created Space:");
		print(space2);
	}

	public void updateSpace(Map<String, Object> args) {
		String spaceId = (String)get(new String[] {"id", "uuid"}, args);
		Space space = convert(args, Space.class);
		printLine("Updating Space...");
		print(space);
		devopsClient.updateSpace(space, null);
		Space space2 = devopsClient.getSpace(spaceId, null);
		print("Updated Space:");
		print(space2);

	}
	
	public void deleteSpace(Map<String, Object> args) {
		String spaceId = (String)get(new String[] {"id", "uuid"}, args);
		printLine("Deleting Space...");
		printLine("ID:", spaceId);		
		devopsClient.deleteSpace(spaceId, null);		
	}




	
	//
	// Domain
	//
	

	public void createDomain(Map<String, Object> args) {
		Domain domain = convert(args, Domain.class);
		Boolean sendMail = null;
		printLine(Boolean.TRUE.equals(sendMail) ? "Sending Domain..." : "Creating Domain...");
		printLine("Domain", domain);
		URI uri = devopsClient.createDomain(domain, new DomainOptions());
		printLine("URI:", uri);
		String id = UriUtils.extractId(uri);
		Domain domain2 = devopsClient.getDomain(id, null);
		print("Created Domain:");
		print(domain2);
	}
	
	
	public void listDomains(Map<String, Object> args) {
		Pageable pageable = convert(args, PageOptions.class).toPageRequest();
		DomainFilter filter = convert(args, DomainFilter.class);
		Page<Domain> domains = devopsClient.listDomains(filter, pageable);
		printLine("Listing Domains...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("Domains:");
		print(domains);
	}

	public void getDomain(Map<String, Object> args) {
		String domainId = (String)get(new String[] {"id", "uuid"}, args);
		Domain domain = devopsClient.getDomain(domainId, null);
		printLine("Get Domain...");
		printLine("ID:", domainId);
		printLine("Domain:");
		print(domain);
	}


	
	public void updateDomain(Map<String, Object> args) {
		String domainId = (String)get("domain", args);
		Domain domain = convert(args, Domain.class);
		printLine("Updating Domain...");
		print(domain);
		devopsClient.updateDomain(domain, null);
		print("Updated Domain:");
		Domain domain2 = devopsClient.getDomain(domainId, null);
		print(domain2);
	}

	public void deleteDomain(Map<String, Object> args) {
		String domainId = (String)get(new String[] {"id", "uuid"}, args);
		printLine("Deleting Domain...");
		printLine("ID:", domainId);		
		devopsClient.deleteDomain(domainId, null);	
	}


	//
	// Registrys
	//
	
	public void listRegistrys(Map<String, Object> args) {
		Pageable pageable = convert(args, PageOptions.class).toPageRequest();
		RegistryFilter filter = convert(args, RegistryFilter.class);
		Page<Registry> registrys = devopsClient.listRegistrys(filter, pageable);
		printLine("Listing Registrys...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("Registrys:");
		print(registrys);
	}
	
	public void getRegistry(Map<String, Object> args) {
		String registryId = (String)get(new String[] {"id", "uuid"}, args);
		Registry registry = devopsClient.getRegistry(registryId, null);
		printLine("Get Registry...");
		printLine("ID:", registryId);
		printLine("Registry:");
		print(registry);
	}
	
	public void createRegistry(Map<String, Object> args) {
		Registry registry = convert(args, Registry.class);
		printLine("Creating Registry...");
		print(registry);
		URI uri = devopsClient.createRegistry(registry, null);
		printLine("URI:", uri);
		String registryId = UriUtils.extractId(uri);
		Registry registry2 = devopsClient.getRegistry(registryId, null);
		print("Created Registry:");
		print(registry2);
	}

	public void updateRegistry(Map<String, Object> args) {
		String registryId = (String)get(new String[] {"id", "uuid"}, args);
		Registry registry = convert(args, Registry.class);
		printLine("Updating Registry...");
		print(registry);
		devopsClient.updateRegistry(registry, null);
		Registry registry2 = devopsClient.getRegistry(registryId, null);
		print("Updated Registry:");
		print(registry2);

	}
	
	public void deleteRegistry(Map<String, Object> args) {
		String registryId = (String)get(new String[] {"id", "uuid"}, args);
		printLine("Deleting Registry...");
		printLine("ID:", registryId);		
		devopsClient.deleteRegistry(registryId, null);		
	}



	//
	// Vcss
	//
	

	public void listVcss(Map<String, Object> args) {
		Pageable pageable = convert(args, PageOptions.class).toPageRequest();
		VcsFilter filter = convert(args, VcsFilter.class);
		Page<Vcs> vcss = devopsClient.listVcss(filter, pageable);
		printLine("Listing Vcss...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("Vcss:");
		print(vcss);
	}

	public void getVcs(Map<String, Object> args) {
		String vcsId = (String)get(new String[] {"id", "uuid"}, args);
		Vcs vcs = devopsClient.getVcs(vcsId, null);
		printLine("Get Vcs...");
		printLine("ID:", vcsId);
		printLine("Vcs:");
		print(vcs);
	}

	public void createVcs(Map<String, Object> args) {
		Vcs vcs = convert(args, Vcs.class);
		printLine("Creating Vcs...");
		print(vcs);
		URI uri = devopsClient.createVcs(vcs, null);
		printLine("URI:", uri);
		print("Created Vcs:");
		String id = UriUtils.extractId(uri);
		Vcs vcs2 = devopsClient.getVcs(id, null);
		print(vcs2);

	}

	
	public void updateVcs(Map<String, Object> args) {
		String vcsId = (String)get("vcs", args);
		Vcs vcs = convert(args, Vcs.class);
		printLine("Updating Vcs...");
		print(vcs);
		devopsClient.updateVcs(vcs, null);
		print("Updated Vcs:");
		Vcs vcs2 = devopsClient.getVcs(vcsId, null);
		print(vcs2);
	}

	public void deleteVcs(Map<String, Object> args) {
		String vcsId = (String)get(new String[] {"id", "uuid"}, args);
		printLine("Deleting Vcs...");
		printLine("ID:", vcsId);		
		devopsClient.deleteVcs(vcsId, null);	
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

	@SuppressWarnings("rawtypes")
	void print(Object obj, int n) {
		if (obj instanceof Iterable) {
			for (Object o: (Iterable)obj) {
				print(o, n+1);
			}
			return;
		}
		System.out.println(String.format("%" + (n+1) + "s%s", "", format(obj)));
	}
	
	void printLine(Object... objs) {
		boolean first = true;
		for (Object obj: objs) {
			if (!first) {
				System.out.print(" ");						
			}
			System.out.print(obj);		
			first = false;
		}
		System.out.println();		
	}
	
	String format(Object obj) {
		String o = (String)argsMap.get("o");
		if (o!=null && !o.isEmpty()) {
			String[] a = o.split(",");
			StringBuilder sb = new StringBuilder();
			Map<String, Object> map = MappingUtils.toMap(obj);
			for (String s: a) {
				if (!s.isEmpty()) {
					sb.append(" ");						
				}
				sb.append(map.get(s));
			}
			return sb.toString();
		} else {
			return obj.toString();			
		}
	}

	
	
}