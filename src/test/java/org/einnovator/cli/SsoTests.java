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
public class SsoTests extends TestsBase {

	
	@Autowired
	Sso sso;

	@Test
	public void userListTest() throws Exception {
		runner.dispatch("sso", "users", "list");
	}

	@Test
	public void userListColsTest() throws Exception {
		runner.dispatch("sso", "users", "list", "-o", "id,username,email");
	}

	@Test
	public void userListYamlTest() throws Exception {
		runner.dispatch("sso", "users", "list", "-o", "yaml");
	}

	@Test
	public void userListJsonTest() throws Exception {
		runner.dispatch("sso", "users", "list", "-o", "json");
	}

	@Test
	public void groupListTest() throws Exception {
		runner.dispatch("sso", "group");
	}

	@Test
	public void invitationListTest() throws Exception {
		runner.dispatch("sso", "invitation");
	}

	@Test
	public void memberListTest() throws Exception {
		runner.dispatch("sso", "member");
	}

	@Test
	public void roleListTest() throws Exception {
		runner.dispatch("sso", "role");
	}

	@Test
	public void clientListTest() throws Exception {
		runner.dispatch("sso", "client");
	}

}
