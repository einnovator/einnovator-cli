package org.einnovator.cli;

import static  org.einnovator.util.MappingUtils.updateObjectFromNonNull;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.einnovator.documents.client.DocumentsClient;
import org.einnovator.documents.client.config.DocumentsClientConfiguration;
import org.einnovator.documents.client.model.Document;
import org.einnovator.documents.client.model.Mount;
import org.einnovator.documents.client.modelx.DocumentFilter;
import org.einnovator.documents.client.modelx.DocumentOptions;
import org.einnovator.documents.client.modelx.MountFilter;
import org.einnovator.util.PageOptions;
import org.einnovator.util.UriUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Component;


@Component
public class Documents extends CommandRunnerBase {

	public static final String DOCUMENTS_DEFAULT_SERVER = "http://localhost:2020";
	public static final String DOCUMENTS_MONITOR_SERVER = "http://localhost:2021";

	public static final String DOCUMENTS_NAME = "documents";


	private static final String DOCUMENT_DEFAULT_FORMAT = "name,type,contentLength:size,owner,lastModified";
	private static final String DOCUMENT_WIDE_FORMAT = "name,type,contentLength:size,contentType,owner,lastModified";

	private static final String MOUNT_DEFAULT_FORMAT = "name,type,path,scope,enabled,readonly";
	private static final String MOUNT_WIDE_FORMAT = "name,type,path,scope,enabled,readonly,versioned,root";
	
	@Autowired
	Sso sso;

	OAuth2AccessToken token;
	
	private DocumentsClient documentsClient;

	private String server = DOCUMENTS_DEFAULT_SERVER;

	private DocumentsClientConfiguration config = new DocumentsClientConfiguration();

	
	@Override
	public String getName() {
		return DOCUMENTS_NAME;
	}

	String[][] DOCUMENTS_COMMANDS = new String[][] { 
		new String[] {"documents", "document", "docs", "doc", "d"},
		new String[] {"mounts", "mount", "m"},
		};

	@Override
	protected String[][] getCommands() {
		return DOCUMENTS_COMMANDS;
	}


	@Override
	public void init(String[] cmds, Map<String, Object> options, OAuth2RestTemplate template, boolean interactive, ResourceBundle bundle) {
		if (!init) {
			super.init(cmds, options, template, interactive, bundle);
			config.setServer(server);
			updateObjectFromNonNull(config, convert(options, DocumentsClientConfiguration.class));

			template = makeOAuth2RestTemplate(sso.getRequiredResourceDetails(), config.getConnection());
			super.init(cmds, options, template, interactive, bundle);

			documentsClient = new DocumentsClient(template, config);
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
		if (isHelp()) {
			printUsage();
		}
		String path = op;
		switch (type) {
		case "help": case "":
			printUsage();
			break;
		case "documents": case "document": case "docs": case "doc": case "d":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				read(path, options);
				break;
			case "list": case "l": case "ls": case "dir": case "":
				list(path, options);
				break;
			case "create": case "c":
				write(path, options);
				break;
			case "mkdir": case "mkd":
				mkdir(path, options);
				break;				
			case "delete": case "del": case "rm": case "d":
				delete(path, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "mount": case "mounts": case "m":
			switch (op) {
			case "get": case "g": case "show": case "s": case "view": case "v":
				getMount(type, op, cmds, options);
				break;
			case "list": case "l": case "":
				listMounts(type, op, cmds, options);
				break;
			case "create": case "c":
				createMount(type, op, cmds, options);
				break;
			case "update": case "u":
				updateMount(type, op, cmds, options);
				break;
			case "delete": case "del": case "rm": case "d":
				deleteMount(type, op, cmds, options);
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
	
	public void list(String path, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		DocumentFilter filter = convert(options, DocumentFilter.class);
		debug("Documents: %s %s", filter, pageable);
		List<Document> documents = documentsClient.list(path, filter, pageable);
		print(documents);
	}

	public void read(String path, Map<String, Object> options) {
		String documentId = argId(null, cmds);
		debug("Document: %s", path);
		Document document = documentsClient.read(documentId, null);
		printObj(document);
	}

	public void write(String path, Map<String, Object> options) {		
		Document document = convert(options, Document.class);
		debug("Write Document: %s", path);
		URI uri = documentsClient.write(document, null);
		if (isEcho()) {
			printLine("Document URI:", uri);
			String id = UriUtils.extractId(uri);
			Mount mount2 = null; documentsClient.read(id, DocumentOptions.META_ONLY);
			printObj(mount2);			
		}
	}

	public void mkdir(String path, Map<String, Object> options) {
		debug("mkdir " + path);
		URI uri = documentsClient.mkdir(path, null);
		debug("URI:", uri);
	}
	
	public void delete(String path, Map<String, Object> options) {
		String documentId = argId(path, cmds);
		debug("Deleting Document: %s", documentId);
		documentsClient.delete(path, null);	
	}


	//
	// Mounts
	//
	
	public void listMounts(String type, String op, String[] cmds, Map<String, Object> options) {
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		MountFilter filter = convert(options, MountFilter.class);
		debug("Mounts: %s %s", filter, pageable);
		Page<Mount> mounts = null; // documentsClient.listMounts(filter, pageable);
		print(mounts);
	}
	
	public void getMount(String type, String op, String[] cmds, Map<String, Object> options) {
		String mountId = argId(op, cmds);
		debug("Mount: %s", mountId);
		Mount mount = null; //documentsClient.getMount(mountId, null);
		printObj(mount);
	}
	
	public void createMount(String type, String op, String[] cmds, Map<String, Object> options) {
		Mount mount = convert(options, Mount.class);
		debug("Creating Mount: %s", mount);
		URI uri = null; //documentsClient.createMount(mount, new RequestOptions());
		if (isEcho()) {
			printLine("Mount URI:", uri);
			String id = UriUtils.extractId(uri);
			Mount mount2 = null; //notificationsClient.getMount(id, null);
			printObj(mount2);			
		}
		
	}

	public void updateMount(String type, String op, String[] cmds, Map<String, Object> options) {
		String mountId = argId(op, cmds);
		Mount mount = convert(options, Mount.class);
		debug("Updating Mount: %s %s", mountId, mount);
		//documentsClient.updateMount(mount, null);
		if (isEcho()) {
			Mount mount2 = null; //documentsClient.getMount(mountId, null);
			printObj(mount2);			
		}
	}
	
	public void deleteMount(String type, String op, String[] cmds, Map<String, Object> options) {
		String mountId = argId(op, cmds);
		debug("Deleting Mount: %s", mountId);
		//documentsClient.deleteMount(mountId, null);		
	}
	
	@Override
	protected String getDefaultFormat(Class<? extends Object> type) {
		if (Document.class.equals(type)) {
			return DOCUMENT_DEFAULT_FORMAT;
		}
		if (Mount.class.equals(type)) {
			return MOUNT_DEFAULT_FORMAT;
		}
		return null;
	}

	@Override
	protected String getWideFormat(Class<? extends Object> type) {
		if (Document.class.equals(type)) {
			return DOCUMENT_WIDE_FORMAT;
		}
		if (Mount.class.equals(type)) {
			return MOUNT_WIDE_FORMAT;
		}
		return null;
	}
	
}