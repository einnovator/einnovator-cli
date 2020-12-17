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
public class LoginTests {

	public static String DEFAULT_USERNAME = "jsimao71@gmail.com";
	public static String DEFAULT_USERNAME2 = "tdd@gmail.com";
	public static String DEFAULT_PASSWORD = "Einnovator123!!";
	public static String DEFAULT_API = "localhost";

	@Autowired
	Sso sso;
	
	@Autowired
	CliRunner runner;

	@Test
	public void loginTests() throws Exception {
		runner.dispatch("login", "-u", DEFAULT_USERNAME, "-p", DEFAULT_PASSWORD, "-a", DEFAULT_API);
	}

}
