package org.einnovator.cli;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class DevopsTests extends TestsBase {

	@Autowired
	Devops devops;

	@Test
	public void clusterListTest() throws Exception {
		run("devops", "cluster");
	}
	
	@Test
	public void clusterListTest2() throws Exception {
		run("devops", "cluster", "schema");
		run("devops", "cluster", "list", "-o", "+");
		run("devops", "cluster", "list", "-o", "id,name,displayName,provider,region,sandbox,shared,fallback,enabled,master,credentialsType,caCertData,caCertUri,clientCertData,clientCertUri,clientKeyData,clientKeyUri,clientKeyAlgo,username,key,secret,svcacc,token,owner,ownerType,lastModified,lastModifiedFormatted,lastModifiedBy,creationDate,creationDateFormatted,createdBy,img");
	}
	@Test
	public void spaceListTest() throws Exception {
		run("devops", "space");
	}

	@Test
	public void spaceListColsTest() throws Exception {
		run("devops", "space", "list", "-o", "id,name,displayName,cluster.name,owner,ownerType,lastModified,lastModifiedFormatted,lastModifiedBy,creationDate,creationDateFormatted,createdBy,img");
	}

	@Test
	public void domainListTest() throws Exception {
		run("devops", "domain", "list");
		run("devops", "domain", "list", "-o", "id,name,dns,tls");
		run("devops", "domain", "list", "-o", "id,name,displayName,dns,sandbox,verified,parent,root,enabled,tls,certificate,owner,ownerTypelastModified,lastModifiedFormatted,lastModifiedBy,creationDate,creationDateFormatted,createdBy,img");
	}

	@Test
	public void registryListTest() throws Exception {
		run("devops", "registry", "list");
		run("devops", "registry", "list", "-o", "id,name,displayName,server,credentialsType,username,owner,ownerType,lastModified,lastModifiedFormatted,lastModifiedBy,creationDate,creationDateFormatted,createdBy,img");
	}

	@Test
	public void vcsListTest() throws Exception {
		run("devops", "vcs", "list");
		run("devops", "vcs", "list", "-o", "id,name,displayName,url,credentialsType,username,owner,ownerType,lastModified,lastModifiedFormatted,lastModifiedBy,creationDate,creationDateFormatted,createdBy,img");
	}
	
	@Test
	public void deployListTest() throws Exception {
		run("devops", "deploy", "schema");
		String space = "us-central/test"; //"einnovator"; //"test-uscentral";
		run("devops", "deploy", "list", "-n", space);
		run("devops", "deploy", "list", "-n", space, "-o", "id,name,displayName,owner,ownerType,lastModified,lastModifiedFormatted,lastModifiedBy,creationDate,creationDateFormatted,createdBy,img");
	}

	@Test
	public void deployGetTest() throws Exception {
		String space = "us-central/test"; //"einnovator"; //"test-uscentral";
		String name = "superheros";
		run("devops", "deploy", "get", name, "-n", space);
	}

	@Test
	public void deployGetWideTest() throws Exception {
		String space = "us-central/test"; //"einnovator"; //"test-uscentral";
		String name = "superheros";
		run("devops", "deploy", "get", name, "-n", space, "-o", "wide");
	}

	@Test
	public void deployListFilterKindTest() throws Exception {
		String space = "us-central/test"; //"einnovator"; //"test-uscentral";
		run("devops", "deploy", "-n", space, "--kind=Deployment");
	}

	@Test
	public void deployListCicdTest() throws Exception {
		String space = "us-central/test"; //"einnovator"; //"test-uscentral";
		run("devops", "deploy", "-n", space, "-o", "cicd");
	}

	public void run(String... args) {
		System.out.println("-------------------");
		System.out.println(String.join(" ", args));
		runner.dispatch(args);
	}

}
