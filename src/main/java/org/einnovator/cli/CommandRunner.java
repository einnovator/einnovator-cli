/**
 * 
 */
package org.einnovator.cli;

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
	
	void init(String[] cmds, Map<String, Object> args, OAuth2RestTemplate template);

	void run(String type, String op, String[] args, Map<String, Object> options);

	void setEndpoints(Map<String, Object> endpoints);

}
