/**
 * 
 */
package org.einnovator.cli;

import java.util.Map;

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
	
	void init0(Map<String, Object> args);
	void init(Map<String, Object> args);

	void run(String type, String op, Map<String, Object> argsMap, String[] args);

}
