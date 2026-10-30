package I2new.group2.dao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Creates the schema (schema.sql) and loads the demo scenario (data.sql).
 */
public final class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    /** Creates and seeds the database on first run. @return true if the database was (re)created */
    public static boolean initializeIfNeeded() throws SQLException {
        if (tablesExist()) {
            return false;
        }
        resetDemoData();
        return true;
    }

    /** Drops every table, recreates the schema and reloads the demo data. */
    public static void resetDemoData() throws SQLException {
        Connection connection = DatabaseConnection.getInstance().getConnection();
        runScript(connection, "schema.sql");
        runScript(connection, "data.sql");
    }

    private static boolean tablesExist() throws SQLException {
        Connection connection = DatabaseConnection.getInstance().getConnection();
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet rs = meta.getTables(connection.getCatalog(), null, "payment", new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private static void runScript(Connection connection, String resource) throws SQLException {
        String script = readResource(resource);
        try (Statement statement = connection.createStatement()) {
            for (String sql : script.split(";")) {
                if (!sql.isBlank()) {
                    statement.execute(sql);
                }
            }
        }
    }

    /** Reads a script and removes the full-line and end-of-line "--" comments. */
    private static String readResource(String resource) {
        try (InputStream in = DatabaseInitializer.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + resource);
            }
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return text.lines()
                    .map(DatabaseInitializer::stripComment)
                    .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + resource, e);
        }
    }

    private static String stripComment(String line) {
        boolean inString = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\'') {
                inString = !inString;
            } else if (!inString && c == '-' && i + 1 < line.length() && line.charAt(i + 1) == '-') {
                return line.substring(0, i);
            }
        }
        return line;
    }
}
