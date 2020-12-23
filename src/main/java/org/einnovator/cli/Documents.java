package org.einnovator.cli;

import static  org.einnovator.util.MappingUtils.updateObjectFromNonNull;

import java.net.URI;
import java.util.LinkedHashMap;
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
import org.einnovator.documents.client.modelx.MountOptions;
import org.einnovator.util.PageOptions;
import org.einnovator.util.UriUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


@Component
public class Documents extends CommandRunnerBase {

	public static final String DOCUMENTS_DEFAULT_SERVER = "http://localhost:2020";
	public static final String DOCUMENTS_MONITOR_SERVER = "http://localhost:2021";

	public static final String DOCUMENTS_NAME = "documents";


	private static final String DOCUMENT_DEFAULT_FORMAT = "name,type,contentLength:size,owner,lastModified";
	private static final String DOCUMENT_WIDE_FORMAT = "name,type,contentLength:size,contentType,owner,lastModified";

	private static final String MOUNT_DEFAULT_FORMAT = "name,type,path,scope,enabled,readonly";
	private static final String MOUNT_WIDE_FORMAT = "name,type,path,scope,enabled,readonly,versioned,root";

	OAuth2AccessToken token;
	
	private DocumentsClient documentsClient;

	private String server = DOCUMENTS_DEFAULT_SERVER;

	private DocumentsClientConfiguration config = new DocumentsClientConfiguration();

	
	@Override
	public String getName() {
		return DOCUMENTS_NAME;
	}

	String[][] DOCUMENTS_COMMANDS = c(
		c("document", "documents", "docs", "doc"),
		c("fmount", "fmounts", "mount", "mounts")
	);

	
	@Override
	protected String[][] getCommands() {
		return DOCUMENTS_COMMANDS;
	}

	static Map<String, String[][]> subcommands;

	static {
		Map<String, String[][]> map = new LinkedHashMap<>();
		subcommands = map;
		map.put("document", c(c("ls", "list"), c("get"), c("schema", "meta"), 
			c("submit", "create", "add"), c("update"), c("delete", "del", "remove", "rm"), 
			c("help")));
		map.put("fmount", c(c("ls", "list"), c("get"), c("schema", "meta"), 
			c("create", "add"), c("update"), c("delete", "del", "remove", "rm"),
			c("help")));
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
			updateObjectFromNonNull(config, convert(options, DocumentsClientConfiguration.class));

			if (template instanceof OAuth2RestTemplate) {
				documentsClient = new DocumentsClient((OAuth2RestTemplate)template, config);
			} else {
				documentsClient = new DocumentsClient(template, config);				
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
		String path = op;
		switch (type) {
		case "help": case "":
			printUsage();
			break;
		}
		setupToken();
		switch (type) {
		case "documents": case "document": case "doc": case "docs":
			switch (op) {
			case "help": case "":
				printUsage1();
				break;
			case "get": 
				read(path, options);
				break;
			case "ls": case "list": case "dir":
				list(path, options);
				break;
			case "create": case "add":
				write(path, options);
				break;
			case "mkdir": case "mkd":
				mkdir(path, options);
				break;				
			case "delete": case "del": case "rm": case "remove":
				delete(path, options);
				break;
			default: 
				invalidOp(type, op);
				break;
			}
			break;
		case "fmount": case "fmounts": case "mount": case "mounts":
			switch (op) {
			case "help": case "":
				printUsage1();
				break;
			case "get": 
				getMount(cmds, options);
				break;
			case "ls": case "list":
				listMounts(cmds, options);
				break;
			case "create": case "add":
				createMount(cmds, options);
				break;
			case "update":
				updateMount(cmds, options);
				break;
			case "delete": case "del": case "rm": case "remove":
				deleteMount(cmds, options);
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
		if (isHelp2()) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		DocumentFilter filter = convert(options, DocumentFilter.class);
		debug("Documents: %s %s", filter, pageable);
		List<Document> documents = documentsClient.list(path, filter, pageable);
		print(documents);
	}

	public void read(String path, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String documentId = argId(null, cmds);
		DocumentOptions options_ = convert(options, DocumentOptions.class);
		debug("Document: %s %s", path, options_);
		Document document = documentsClient.read(documentId, options_);
		printObj(document);
	}

	public void write(String path, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		Document document = convert(options, Document.class);
		DocumentOptions options_ = convert(options, DocumentOptions.class);
		debug("Write Document: %s %s", path, options_);
		URI uri = documentsClient.write(document, options_);
		if (isEcho()) {
			printLine("Document URI:", uri);
			String id = UriUtils.extractId(uri);
			Mount mount2 = null; documentsClient.read(id, DocumentOptions.META_ONLY);
			printObj(mount2);			
		}
	}

	public void mkdir(String path, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		debug("mkdir " + path);
		URI uri = documentsClient.mkdir(path, null);
		debug("URI:", uri);
	}
	
	public void delete(String path, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String documentId = argId(path, cmds);
		debug("Deleting Document: %s", documentId);
		documentsClient.delete(path, null);	
		if (isEcho()) {
			list(path, options);
		}
	}


	//
	// Mounts
	//
	
	public void listMounts(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		Pageable pageable = convert(options, PageOptions.class).toPageRequest();
		MountFilter filter = convert(options, MountFilter.class);
		debug("Mounts: %s %s", filter, pageable);
		Page<Mount> mounts = null; // documentsClient.listMounts(filter, pageable);
		print(mounts);
	}
	
	public void getMount(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String mountId = argId(op, cmds);
		debug("Mount: %s", mountId);
		Mount mount = null; //documentsClient.getMount(mountId, null);
		printObj(mount);
	}
	
	public void createMount(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		Mount mount = convert(options, Mount.class);
		MountOptions options_ = convert(options, MountOptions.class);
		debug("Creating Mount: %s %s", mount, options_);
		URI uri = null; //documentsClient.createMount(mount, options_);
		if (isEcho()) {
			printLine("Mount URI:", uri);
			//String id = UriUtils.extractId(uri);
			Mount mount2 = null; //notificationsClient.getMount(id, null);
			printObj(mount2);			
		}
		
	}

	public void updateMount(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String mountId = argId(op, cmds);
		Mount mount = convert(options, Mount.class);
		MountOptions options_ = convert(options, MountOptions.class);
		debug("Updating Mount: %s %s %s", mountId, mount, options_);
		//documentsClient.updateMount(mount, options_);
		if (isEcho()) {
			Mount mount2 = null; //documentsClient.getMount(mountId, null);
			printObj(mount2);			
		}
	}
	
	public void deleteMount(String[] cmds, Map<String, Object> options) {
		if (isHelp2()) {
			return;
		}
		String mountId = argId(op, cmds);
		debug("Deleting Mount: %s", mountId);
		//documentsClient.deleteMount(mountId, null);
		if (isEcho()) {
			listMounts(cmds, options);
		}
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