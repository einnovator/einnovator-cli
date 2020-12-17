package org.einnovator.cli;

import org.springframework.beans.factory.annotation.Autowired;

import static org.einnovator.cli.LoginTests.*;

import org.junit.Before;

public class TestsBase {

	@Autowired
	CliRunner runner;

	@Before
	public void setup() {
		login();
	}
	
	public void login() {
		runner.dispatch("login", "-u", DEFAULT_USERNAME, "-p", DEFAULT_PASSWORD, "-a", DEFAULT_API);
	}
}
