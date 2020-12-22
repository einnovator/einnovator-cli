package org.einnovator.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.einnovator.util.StringUtil;
import org.einnovator.util.model.ObjectBase;
import org.einnovator.util.model.ToStringCreator;

public class Context extends ObjectBase {
	private String api;
	private Boolean singleuser;
	private List<String> servers;
	private Map<String, Object> endpoints;

	public Context() {
	}

	/**
	 * Get the value of property {@code api}.
	 *
	 * @return the value of {@code api}
	 */
	public String getApi() {
		return api;
	}

	/**
	 * Set the value of property {@code api}.
	 *
	 * @param api the value of {@code api}
	 */
	public void setApi(String api) {
		this.api = api;
	}

	/**
	 * Get the value of property {@code singleuser}.
	 *
	 * @return the value of {@code singleuser}
	 */
	public Boolean getSingleuser() {
		return singleuser;
	}

	/**
	 * Set the value of property {@code singleuser}.
	 *
	 * @param singleuser the value of {@code singleuser}
	 */
	public void setSingleuser(Boolean singleuser) {
		this.singleuser = singleuser;
	}

	/**
	 * Get the value of property {@code servers}.
	 *
	 * @return the value of {@code servers}
	 */
	public List<String> getServers() {
		return servers;
	}

	/**
	 * Set the value of property {@code servers}.
	 *
	 * @param servers the value of {@code servers}
	 */
	public void setServers(List<String> servers) {
		this.servers = servers;
	}

	/**
	 * Get the value of property {@code endpoints}.
	 *
	 * @return the value of {@code endpoints}
	 */
	public Map<String, Object> getEndpoints() {
		return endpoints;
	}

	/**
	 * Set the value of property {@code endpoints}.
	 *
	 * @param endpoints the value of {@code endpoints}
	 */
	public void setEndpoints(Map<String, Object> endpoints) {
		this.endpoints = endpoints;
	}
	
	@Override
	public ToStringCreator toString1(ToStringCreator creator) {
		return super.toString1(creator
				.append("api", api)
				.append("singleuser", singleuser)
				.append("servers", servers)
				.append("endpoints", endpoints)
				);
	}

	@SuppressWarnings("unchecked")
	public static Context make(Map<String, Object> map) {
		if (map==null) {
			return null;
		}
		Context context = new Context();
		context.api = (String)map.get(Sso.KEY_API);
		context.endpoints = (Map<String, Object>) map.get(Sso.KEY_ENDPOINTS);
		Boolean singleuser = (Boolean)map.get(Sso.KEY_SINGLEUSER);
		context.singleuser = Boolean.TRUE.equals(singleuser);
		if (context.endpoints!=null) {
			context.servers = new ArrayList<String>();
			for (Map.Entry<String, Object> e: context.endpoints.entrySet()) {
				Map<String, Object> data = (Map<String, Object>)e.getValue();
				if (data!=null) {
					String server = (String)data.get("server");
					if (StringUtil.hasText(server)) {
						context.servers.add(server);					
					}					
				}
			}
		}
		return context;
		
	}
	
	public static List<Context> make(List<Map<String, Object>> mapList) {
		List<Context> contexts = new ArrayList<>();
		for (Map<String, Object> map: mapList) {
			contexts.add(make(map));
		}
		return contexts;
	}

}