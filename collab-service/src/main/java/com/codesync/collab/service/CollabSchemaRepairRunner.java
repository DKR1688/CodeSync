package com.codesync.collab.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Locale;

@Component
public class CollabSchemaRepairRunner implements ApplicationRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(CollabSchemaRepairRunner.class);

	private final DataSource dataSource;
	private final JdbcTemplate jdbcTemplate;

	public CollabSchemaRepairRunner(DataSource dataSource, JdbcTemplate jdbcTemplate) {
		this.dataSource = dataSource;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!isMySqlFamilyDatabase()) {
			return;
		}

		String currentContentType = jdbcTemplate.query(
				"""
						SELECT LOWER(DATA_TYPE)
						FROM INFORMATION_SCHEMA.COLUMNS
						WHERE TABLE_SCHEMA = DATABASE()
						  AND TABLE_NAME = 'collab_sessions'
						  AND COLUMN_NAME = 'current_content'
						""",
				rs -> rs.next() ? rs.getString(1) : null);

		if (currentContentType == null || "longtext".equals(currentContentType)) {
			return;
		}

		LOGGER.info("Repairing collab_sessions.current_content column from {} to LONGTEXT", currentContentType);
		jdbcTemplate.execute("ALTER TABLE collab_sessions MODIFY COLUMN current_content LONGTEXT NOT NULL");
	}

	private boolean isMySqlFamilyDatabase() {
		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData metadata = connection.getMetaData();
			String productName = metadata.getDatabaseProductName();
			if (productName == null) {
				return false;
			}
			String normalized = productName.toLowerCase(Locale.ROOT);
			return normalized.contains("mysql") || normalized.contains("mariadb");
		} catch (SQLException exception) {
			LOGGER.warn("Unable to inspect the database product while checking collaboration schema", exception);
			return false;
		}
	}
}
