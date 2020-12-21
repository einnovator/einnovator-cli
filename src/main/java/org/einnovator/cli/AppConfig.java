package org.einnovator.cli;

import java.util.ArrayList;
import java.util.List;

public class AppConfig {

	public List<CommandRunner> getAllRunners() {
		List<CommandRunner> runners = new ArrayList<>();
		runners.add(new Sso());
		runners.add(new Devops());
		runners.add(new Notifications());
		runners.add(new Documents());
		runners.add(new Social());
		runners.add(new Payments());
		return runners;
	}
}
