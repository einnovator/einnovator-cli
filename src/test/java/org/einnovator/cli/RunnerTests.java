package org.einnovator.cli;

import java.util.LinkedHashMap;
import java.util.Map;

import org.einnovator.devops.client.model.Deployment;
import org.junit.Test;

public class RunnerTests {

	@Test
	public void test() {
		CommandRunnerBase runner = new CommandRunnerBase() {

			@Override
			public String getName() {
				return null;
			}

			@Override
			public void run(String type, String op, String[] cmds, String[] extra, Map<String, Object> options) {
			}}
		;
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("stack", "boot");
		Deployment deploy  = runner.convert(map, Deployment.class);
		System.out.println(deploy);
	}

}
