package I2new.group2.dao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Single shared JDBC connection (singleton), configured by {@code db.properties}.
 * The file is looked up in the working directory first, then on the classpath
 * (src/main/resources/db.properties).
 */
public final class DatabaseConnection {

    /** A unit of work executed inside a transaction. */
    @FunctionalInterface
    public interface SqlWork<T> {
        T run(Connection connection) throws SQLException;
    }

    private static final String CONFIG_FILE = "db.properties";
    private static DatabaseConnection instance;

    private final String url;
    private final String user;
    private final String password;
    private Connection connection;

    private DatabaseConnection(Properties config) {
        this.url = require(config, "db.url");
        this.user = require(config, "db.user");
        this.password = config.getProperty("db.password", "");
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection(loadConfig());
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url, user, password);
        }
        return connection;
    }

    /**
     * Runs {@code work} atomically: commit on success, rollback on any exception.
     * When already inside a transaction, the work simply joins it.
     */
    public <T> T inTransaction(SqlWork<T> work) throws SQLException {
        Connection c = getConnection();
        if (!c.getAutoCommit()) {
            return work.run(c);
        }
        boolean previousAutoCommit = true;
        c.setAutoCommit(false);
        try {
            T result = work.run(c);
            c.commit();
            return result;
        } catch (SQLException | RuntimeException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(previousAutoCommit);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
            // shutting down anyway
        }
    }

    private static Properties loadConfig() {
        Properties config = new Properties();
        try {
            Path local = Path.of(CONFIG_FILE);
            if (Files.exists(local)) {
                try (InputStream in = Files.newInputStream(local)) {
                    config.load(in);
                }
                return config;
            }
            try (InputStream in = DatabaseConnection.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
                if (in == null) {
                    throw new IllegalStateException("Missing " + CONFIG_FILE + ": copy src/main/resources/"
                            + CONFIG_FILE + ".example to " + CONFIG_FILE + " and set your MySQL credentials");
                }
                config.load(in);
            }
            return config;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + CONFIG_FILE + ": " + e.getMessage(), e);
        }
    }

    private static String require(Properties config, String key) {
        String value = config.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Property '" + key + "' is missing in " + CONFIG_FILE);
        }
        return value.trim();
    }
}
