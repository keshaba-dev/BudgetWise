package com.budgetwise.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Opens MySQL connections using environment variables so credentials stay out of source control. */
public final class DBConnection {
    private DBConnection() { }

    public static Connection getConnection() throws SQLException {
        String url = System.getenv().getOrDefault("BUDGETWISE_DB_URL", "jdbc:mysql://localhost:3306/budgetwise_db?serverTimezone=UTC");
        String user = System.getenv().getOrDefault("BUDGETWISE_DB_USER", "root");
        String password = System.getenv().getOrDefault("BUDGETWISE_DB_PASSWORD", "");
        return DriverManager.getConnection(url, user, password);
    }
}
