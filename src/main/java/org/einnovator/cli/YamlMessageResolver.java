package org.einnovator.cli;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.einnovator.util.ResourceUtils;
import org.einnovator.util.StringUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class YamlMessageResolver {
	protected static YAMLFactory yamlFactory = new YAMLFactory();

	public static final String RESOURCE_FILE_NAME = "messages";
	public static final String RESOURCE_FILE_SUFFIX = ".yml";
	
	public String locale;
	
	public YamlMessageResolver() {
	}
	
	
	/**
	 * Get the value of property {@code locale}.
	 *
	 * @return the value of {@code locale}
	 */
	public String getLocale() {
		return locale;
	}


	/**
	 * Set the value of property {@code locale}.
	 *
	 * @param locale the value of {@code locale}
	 */
	public void setLocale(String locale) {
		this.locale = locale;
	}


	public Map<String, String> loadMessages() {
		Map<String, String> msgs = loadMessages(makeFilename(""));
		String locale = getLocale();
		if (StringUtil.hasText(locale)) {
			Map<String, String> msgs2 = loadMessages(makeFilename(locale));		
			msgs = merge(msgs, msgs2);
		}		
		return msgs;
	}

	private Map<String, String> merge(Map<String, String> msgs, Map<String, String> msgs2) {
		if (msgs2==null) {
			return msgs;
		}
		if (msgs==null) {
			return msgs2;
		}
		msgs.putAll(msgs2);
		return msgs;
	}

	private String makeFilename(String infix) {
		return RESOURCE_FILE_NAME + infix + RESOURCE_FILE_SUFFIX;
	}
	
	public Map<String, String> loadMessages(String path) {
		return flatMap(loadMessagesTree(path));
	}

	public Map<String, String> flatMap(Map<String, Object> map) {
		return flatMap(map, new LinkedHashMap<>(), null);
	}

	@SuppressWarnings("unchecked")
	public Map<String, String> flatMap(Map<String, Object> map, Map<String, String> out, String NAME) {
		if (map==null) {
			return null;
		}
		Map<String, String> map2 = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e: map.entrySet()) {
			Object value = e.getValue();
			if (value==null) {
				continue;
			}
			String key = NAME!=null ? NAME + "." + e.getKey() : e.getKey();
			if (value instanceof Map) {
				flatMap((Map<String, Object>)value, out, key);
			} else {
				map2.put(key, format(value));
			}
		}
		return map2;
	}
	private String format(Object value) {
		if (value==null) {
			return "";
		}
		return value.toString();
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> loadMessagesTree(String path) {
		String s = ResourceUtils.readResource(path, true);
		if (!StringUtil.hasText(s)) {
			return null;
		}
		return readYaml(s, Map.class);
	}


	public static <T> T readYaml(String content, Class<T> type) {
		if (content==null) {
			return null;
		}
		ObjectMapper mapper = new ObjectMapper(yamlFactory);
	    try {
			return mapper.readValue(content, type);
		} catch (IOException e) {
			return null;
		}		
	}
}
