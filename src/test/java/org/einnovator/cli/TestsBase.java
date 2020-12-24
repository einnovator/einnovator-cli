package org.einnovator.cli;

import org.springframework.beans.factory.annotation.Autowired;

import static org.einnovator.cli.LoginTests.*;

import org.junit.Before;

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
