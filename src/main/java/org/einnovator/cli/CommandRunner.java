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
	
	void init(Map<String, Object> args, OAuth2RestTemplate template);

	void run(String type, String op, Map<String, Object> argsMap, String[] args);

}
