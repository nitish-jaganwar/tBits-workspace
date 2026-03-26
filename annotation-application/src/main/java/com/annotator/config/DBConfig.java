package com.annotator.config;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class DBConfig {

	public static Connection getConnection() throws Exception {
		Properties props = new Properties();

		try (InputStream input = DBConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
			if (input == null) {
				throw new Exception("Sorry, unable to find config.properties");
			}
			props.load(input);
		}

		Class.forName(props.getProperty("db.driver"));

		return DriverManager.getConnection(props.getProperty("db.url"), props.getProperty("db.user"),
				props.getProperty("db.password"));
	}
}