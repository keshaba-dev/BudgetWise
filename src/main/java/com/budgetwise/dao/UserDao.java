package com.budgetwise.dao;

import com.budgetwise.util.DBConnection;
import java.sql.*;

/** User lookup and account creation. Passwords are stored as BCrypt hashes. */
public class UserDao {
    public int register(String name, String email, String passwordHash) throws SQLException {
        String sql = "INSERT INTO users(name,email,password) VALUES(?,?,?)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, name); p.setString(2, email); p.setString(3, passwordHash); p.executeUpdate();
            try (ResultSet keys = p.getGeneratedKeys()) { if (keys.next()) return keys.getInt(1); }
        }
        throw new SQLException("User was not created");
    }
    public UserRecord findByEmail(String email) throws SQLException {
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement("SELECT user_id,name,email,password FROM users WHERE email=?")) {
            p.setString(1, email); try (ResultSet r = p.executeQuery()) { return r.next() ? new UserRecord(r.getInt("user_id"), r.getString("name"), r.getString("email"), r.getString("password")) : null; }
        }
    }
    public void updateProfile(int id, String name, String email) throws SQLException {
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement("UPDATE users SET name=?,email=? WHERE user_id=?")) { p.setString(1,name); p.setString(2,email); p.setInt(3,id); p.executeUpdate(); }
    }
    public void updatePassword(int id, String hash) throws SQLException {
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement("UPDATE users SET password=? WHERE user_id=?")) { p.setString(1,hash); p.setInt(2,id); p.executeUpdate(); }
    }
    public record UserRecord(int id, String name, String email, String passwordHash) { }
}
