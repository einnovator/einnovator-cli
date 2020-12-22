/**
 * 
 */
package org.einnovator.cli;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.springframework.web.client.RestTemplate;

/**
 * A {@code CommandRunner}.
 *
 * @author support@einnovator.org
 *
 */
public interface CommandRunner {

	String getName();

	boolean supports(String cmd);
	
	boolean supports(String cmd, Map<String, Object> options);


	void printUsage();
	
	void init(String[] cmds, Map<String, Object> options, RestTemplate template, boolean interactive, ResourceBundle bundle);

	void run(String type, String op, String[] cmds, Map<String, Object> options);

	void setEndpoints(Map<String, Object> endpoints);


	void setRunners(List<CommandRunner> runners);

	Map<String, Object> getSettings();

	void loadSettings(Map<String, Object> settings);

}
