package com.qa.gorest.manager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigManager {
	
	
	private static Properties properties = new Properties();

	static {
		try (InputStream input = ConfigManager.class.getClassLoader().getResourceAsStream("config/config.properties")) {
			if(input != null)
			{
				properties.load(input);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns the value for the given key.
	 * Resolution order (highest to lowest priority):
	 *   1. JVM system property  -> -Dgorest_bearerToken=xyz
	 *   2. Environment variable -> GOREST_BEARER_TOKEN=xyz
	 *   3. config.properties file (git-ignored local value)
	 * This lets you inject secrets on CI / Jenkins without committing them.
	 */
	public static String Get(String key) {
		// 1. System property provided at runtime, e.g. mvn test -Dgorest_bearerToken=xyz
		String sysProp = System.getProperty(key);
		if (sysProp != null && !sysProp.trim().isEmpty())
			return sysProp;

		// 2. Environment variable, e.g. export GOREST_BEARER_TOKEN=xyz
		String envVar = camelCaseToScreamingSnake(key);
		String env = System.getenv(envVar);
		if (env != null && !env.trim().isEmpty())
			return env;

		// 3. Fallback to the config.properties file
		return properties.getProperty(key);
	}

	/**
	 * Convert a key such as "gorest_bearerToken" to "GOREST_BEARER_TOKEN"
	 * so it can be matched to a conventional environment variable name.
	 */
	private static String camelCaseToScreamingSnake(String key) {
		StringBuilder sb = new StringBuilder();
		for (char c : key.toCharArray()) {
			if (Character.isUpperCase(c)) {
				sb.append('_');
			}
			sb.append(Character.toUpperCase(c));
		}
		return sb.toString();
	}

	public static void setProperty(String key, String value) {
		 properties.setProperty(key, value);
		
	}
	
	public static String getProperty(String key)
	{
		return properties.getProperty(key);
	}

}
