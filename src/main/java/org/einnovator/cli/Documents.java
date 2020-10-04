package org.einnovator.cli;

import static  org.einnovator.util.MappingUtils.convert;
import static  org.einnovator.util.MappingUtils.updateObjectFromNonNull;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.einnovator.documents.client.DocumentsClient;
import org.einnovator.documents.client.config.DocumentsClientConfiguration;
import org.einnovator.documents.client.model.Document;
import org.einnovator.documents.client.model.Mount;
import org.einnovator.documents.client.modelx.DocumentFilter;
import org.einnovator.documents.client.modelx.MountFilter;
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
public class Documents extends CommandRunnerBase {

	private static final String DOCUMENTS_DEFAULT_SERVER = "http://localhost:2021";
	public static final String DOCUMENTS_PREFIX = "documents";

	@Autowired
	Sso sso;

	OAuth2AccessToken token;
	
	private DocumentsClient documentsClient;

	private DocumentsClientConfiguration config = new DocumentsClientConfiguration();

	
	
	@Override
	public String getPrefix() {
		return DOCUMENTS_PREFIX;
	}

	String[] DOCUMENTS_COMMANDS = new String[] { 
		"documents", "document", "docs", "doc", "d",
		"mounts", "mount", "m",
		};

	protected String[] getCommands() {
		return DOCUMENTS_COMMANDS;
	}



	public void init(Map<String, Object> args) {
		super.init(args, template);
		config.setServer(DOCUMENTS_DEFAULT_SERVER);
		updateObjectFromNonNull(config, convert(args, DocumentsClientConfiguration.class));

		ResourceOwnerPasswordResourceDetails resource = sso.getRequiredResourceDetails();
		DefaultOAuth2ClientContext context = new DefaultOAuth2ClientContext();
		OAuth2RestTemplate template = new OAuth2RestTemplate(resource, context);
		template.setRequestFactory(config.getConnection().makeClientHttpRequestFactory());
		
		documentsClient = new DocumentsClient(template, config);
	}

	public void run(String type, String op, Map<String, Object> argsMap, String[] args) {

		String path = op;
		switch (type) {
		case "documents": case "document": case "docs": case "doc": case "d":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				read(path, argsMap);
				break;
			case "list": case "l": case "ls": case "dir": case "":
				list(path, argsMap);
				break;
			case "create": case "c":
				write(path, argsMap);
				break;
			case "mkdir": case "mkd":
				mkdir(path, argsMap);
				break;				
			case "delete": case "del": case "d":
				delete(path, argsMap);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "mount": case "mounts": case "m":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getMount(argsMap);
				break;
			case "list": case "l": case "":
				listMounts(argsMap);
				break;
			case "create": case "c":
				createMount(argsMap);
				break;
			case "update": case "u":
				updateMount(argsMap);
				break;
			case "delete": case "del": case "d":
				deleteMount(argsMap);
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
	// Document
	//
	
	public void list(String path, Map<String, Object> args) {
		Pageable pageable = convert(args, PageOptions.class).toPageRequest();
		DocumentFilter filter = convert(args, DocumentFilter.class);
		List<Document> documents = documentsClient.list(path, filter, pageable);
		printLine("Listing Documents...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("Documents:");
		print(documents);
	}

	public void read(String path, Map<String, Object> args) {
		String documentId = (String)get(new String[] {"id", "uuid", "documentname", "email"}, args);
		Document document = documentsClient.read(documentId, null);
		printLine("Get Document...");
		printLine("ID:", documentId);
		printLine("Document:");
		print(document);
	}

	public void write(String path, Map<String, Object> args) {
		Document document = convert(args, Document.class);
		printLine("Creating Document...");
		print(document);
		URI uri = documentsClient.write(document, null);
		printLine("URI:", uri);
	}

	public void mkdir(String path, Map<String, Object> args) {
		printLine("mkdir " + path);
		URI uri = documentsClient.mkdir(path, null);
		printLine("URI:", uri);
	}
	
	public void delete(String path, Map<String, Object> args) {
		String documentId = (String)get(new String[] {"id", "documentname"}, args);
		printLine("Deleting Document...");
		printLine("ID:", documentId);		
		documentsClient.delete(path, null);	
	}


	//
	// Mounts
	//
	
	public void listMounts(Map<String, Object> args) {
		Pageable pageable = convert(args, PageOptions.class).toPageRequest();
		MountFilter filter = convert(args, MountFilter.class);
		Page<Mount> mounts = null; // documentsClient.listMounts(filter, pageable);
		printLine("Listing Mounts...");
		printLine("Filter:", filter);
		printLine("Pageable:", pageable);
		printLine("Mounts:");
		print(mounts);
	}
	
	public void getMount(Map<String, Object> args) {
		String mountId = (String)get(new String[] {"id", "uuid"}, args);
		Mount mount = null; //documentsClient.getMount(mountId, null);
		printLine("Get Mount...");
		printLine("ID:", mountId);
		printLine("Mount:");
		print(mount);
	}
	
	public void createMount(Map<String, Object> args) {
		Mount mount = convert(args, Mount.class);
		printLine("Creating Mount...");
		print(mount);
		URI uri = null; //documentsClient.createMount(mount, null);
		printLine("URI:", uri);
		String mountId = UriUtils.extractId(uri);
		Mount mount2 = null; //documentsClient.getMount(mountId, null);
		print("Created Mount:");
		print(mount2);
	}

	public void updateMount(Map<String, Object> args) {
		String mountId = (String)get(new String[] {"id", "uuid"}, args);
		Mount mount = convert(args, Mount.class);
		printLine("Updating Mount...");
		print(mount);
		//documentsClient.updateMount(mount, null);
		Mount mount2 = null; //documentsClient.getMount(mountId, null);
		print("Updated Mount:");
		print(mount2);

	}
	
	public void deleteMount(Map<String, Object> args) {
		String mountId = (String)get(new String[] {"id", "uuid"}, args);
		printLine("Deleting Mount...");
		printLine("ID:", mountId);		
		//documentsClient.deleteMount(mountId, null);		
	}
	
}