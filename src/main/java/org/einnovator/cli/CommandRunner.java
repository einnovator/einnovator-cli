/**
 * 
 */
package org.einnovator.cli;

import java.util.List;
import java.util.Map;

import org.springframework.security.oauth2.client.OAuth2RestTemplate;

/**
 * A {@code CommandRunner}.
 *
 * @author support@einnovator.org
 *
 */
public interface CommandRunner {

	String getPrefix();

	boolean supports(String cmd);

	void printUsage();
	
	void init(String[] cmds, Map<String, Object> args, OAuth2RestTemplate template, boolean interactive);

	void run(String type, String op, String[] args, Map<String, Object> options);

	void setEndpoints(Map<String, Object> endpoints);


	void setRunners(List<CommandRunner> runners);

	Map<String, Object> getSettings();

	void loadSettings(Map<String, Object> settings);

}
