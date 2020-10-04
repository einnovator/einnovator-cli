package org.einnovator.cli;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class SsoTests {

	@Autowired
	Sso sso;
	
	@Autowired
	CliRunner runner;

	@Test
	public void ssoTests() throws Exception {
		runner.dispatch("sso", "users", "list", "-o", "id,username,email");
		/*
		runner.run("users", "list");		
		runner.run("groups", "list");		
		runner.run("invitations", "list");		
		runner.run("roles", "list");		
		runner.run("clients", "list");
		*/
	}

}
