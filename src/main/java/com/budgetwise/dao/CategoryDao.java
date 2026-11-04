package com.budgetwise.dao;

import com.budgetwise.util.DBConnection;
import java.sql.*;
import java.util.*;

/** Category queries include shared defaults and the current user's categories. */
public class CategoryDao {
    public List<Map<String,Object>> list(int userId) throws SQLException {
        String sql="SELECT category_id,category_name,(user_id IS NULL) AS is_default FROM categories WHERE user_id IS NULL OR user_id=? ORDER BY category_name";
        List<Map<String,Object>> out=new ArrayList<>();
        try(Connection c=DBConnection.getConnection(); PreparedStatement p=c.prepareStatement(sql)){p.setInt(1,userId);try(ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getInt(1));m.put("name",r.getString(2));m.put("default",r.getBoolean(3));out.add(m);}}}
        return out;
    }
    public void add(int userId,String name) throws SQLException { try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("INSERT INTO categories(user_id,category_name) VALUES(?,?)")){p.setInt(1,userId);p.setString(2,name);p.executeUpdate();} }
    public void update(int userId,int id,String name) throws SQLException { try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("UPDATE categories SET category_name=? WHERE category_id=? AND user_id=?")){p.setString(1,name);p.setInt(2,id);p.setInt(3,userId);p.executeUpdate();} }
    public void delete(int userId,int id) throws SQLException { try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("DELETE FROM categories WHERE category_id=? AND user_id=? AND NOT EXISTS(SELECT 1 FROM transactions WHERE category_id=?)")){p.setInt(1,id);p.setInt(2,userId);p.setInt(3,id);p.executeUpdate();} }
}
