package org.einnovator.cli;

import java.util.Arrays;
import java.util.List;

public class AppConfig {

	public List<CommandRunner> getAllRunners() {
		return Arrays.asList(
			new Generic(),
			new Sso(),
			new Devops(),
			new Notifications(),
			new Documents(),
			new Social(),
			new Payments()
		);
	}
}
