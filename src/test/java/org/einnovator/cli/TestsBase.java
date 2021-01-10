package org.einnovator.cli;

import static org.einnovator.cli.LoginTests.API;
import static org.einnovator.cli.LoginTests.PASSWORD;
import static org.einnovator.cli.LoginTests.USERNAME;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

public class TestsBase {

	@Autowired
	CliRunner runner;

	@Before
	public void setup() throws Exception {
		LoginTests.setup();
		login();
	}
	
	public void login() {
		runner.dispatch("login", "-u", USERNAME, "-p", PASSWORD, "-a", API);
	}
}
