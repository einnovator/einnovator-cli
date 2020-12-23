package org.einnovator.cli;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.einnovator.util.StringUtil;

public abstract class RunnerBase {

	protected List<CommandRunner> runners;
	
	
	protected boolean interactive;
	
	public RunnerBase() {
	}

	public void init(boolean interactive, ResourceBundle bundle) {
		this.interactive = interactive;
		this.bundle = bundle;
	}
	
	/**
	 * Get the value of property {@code runners}.
	 *
	 * @return the value of {@code runners}
	 */
	public List<CommandRunner> getRunners() {
		return runners;
	}

	/**
	 * Set the value of property {@code runners}.
	 *
	 * @param runners the value of {@code runners}
	 */
	public void setRunners(List<CommandRunner> runners) {
		this.runners = runners;
	}

	/**
	 * Get the value of property {@code interactive}.
	 *
	 * @return the value of {@code interactive}
	 */
	public boolean isInteractive() {
		return interactive;
	}

	/**
	 * Set the value of property {@code interactive}.
	 *
	 * @param interactive the value of {@code interactive}
	 */
	public void setInteractive(boolean interactive) {
		this.interactive = interactive;
	}

	/**
	 * Get the value of property {@code bundle}.
	 *
	 * @return the value of {@code bundle}
	 */
	public ResourceBundle getBundle() {
		return bundle;
	}

	/**
	 * Set the value of property {@code bundle}.
	 *
	 * @param bundle the value of {@code bundle}
	 */
	public void setBundle(ResourceBundle bundle) {
		this.bundle = bundle;
	}

	public CommandRunner getRunnerByName(String name) {
		for (CommandRunner runner: runners) {
			String rNAME = runner.getName();
			if (name.equalsIgnoreCase(rNAME)) {
				return runner;
			}
		}
		return null;
	}
	

	public CommandRunner getRunnerByCommand(String NAME) {
			for (CommandRunner runner: runners) {
			if (runner.supports(NAME)) {
				return runner;
			}
		}
		return null;
	}
	

	protected void exit(int code) {
		if (interactive) {
			throw new InteractiveException();
		}
		System.exit(code);		
	}
	
	protected void error(String msg, Object... args) {
		System.err.println(String.format("ERROR: " + msg, args));
	}

	protected void error(Exception e) {
		String s = e.toString();
		System.err.println("ERROR: " + (!s.isEmpty() ? s : e.getClass().getSimpleName()));
	}
	
	public String resolve(String[] cmds_) {
		for (String cmd: cmds_) {
			String s = resolve(cmd);
			if (StringUtil.hasText(s)) {
				return s.trim();
			}
		}
		return "";
	}

	public String resolve(String key) {
		return resolve(key, true);
	}


	public String resolve(String key, boolean required) {
		return resolve(key, required ? "?" + key + "?" : null);
	}
	
	public String resolve(String key, String defaultValue) {
		ResourceBundle bundle = getResourceBundle();
		if (bundle!=null) {
			try {
				String s = bundle.getString(key);	
				if (s!=null) {
					s = s.trim();
					if (!s.isEmpty()) {
						int i = s.indexOf("$");
						if (i>=0) {
							String prefix = i>0 ? s.substring(0, i).trim() : null;
							s = s.substring(i+1).trim();
							if (s.isEmpty()) {
								if (prefix==null || prefix.isEmpty()) {
									return defaultValue;									
								}
								return prefix;
							}
							if (!key.equals(s)) {
								s = resolve(s, defaultValue);								
								if (s==null || s.isEmpty()) {
									if (prefix==null || prefix.isEmpty()) {
										return defaultValue;									
									}
									return prefix;
								} else {
									if (prefix==null || prefix.isEmpty()) {
										return s;									
									}
									return prefix + s;
								}
							}
						}
						return s;						
					}
				}
			} catch (RuntimeException e) {
			}			
		}
		return defaultValue;
	}


	protected ResourceBundle bundle;
	
	public ResourceBundle getResourceBundle() {
		if (bundle==null) {
			bundle = ResourceBundle.getBundle("messages", getLocale());
		}
		return bundle;
	}
	
	public Locale getLocale() {
		return Locale.getDefault();
	}
}
