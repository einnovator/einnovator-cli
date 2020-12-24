package org.einnovator.cli;

import static org.junit.Assert.fail;

import org.einnovator.util.StringUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class LoginTests {

	public static String USERNAME;
	public static String PASSWORD;
	public static String API;
	
	public static String ENV_USERNAME = "username";
	public static String ENV_PASSWORD = "password";
	public static String ENV_API = "api";

	@Autowired
	Sso sso;
	
	@Autowired
	CliRunner runner;

	@Test
	public void loginTests() throws Exception {
		setup();
		runner.dispatch("login", "-u", USERNAME, "-p", PASSWORD, "-a", API);
	}
	
	public static void setup() throws Exception {
		USERNAME = System.getenv(ENV_USERNAME);
		PASSWORD = System.getenv(ENV_PASSWORD);
		API = System.getenv(ENV_API);
		if (!StringUtil.hasText(USERNAME)) {
			fail("missing username");
		}
		if (!StringUtil.hasText(PASSWORD)) {
			fail("missing password");
		}
		if (!StringUtil.hasText(API)) {
			fail("missing api");
		}
	}

}
