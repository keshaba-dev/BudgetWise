package com.budgetwise.dao;

import com.budgetwise.model.Transaction;
import com.budgetwise.util.DBConnection;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/** Simple CRUD and aggregate SQL for a user's transactions. Every query is scoped by user_id. */
public class TransactionDao {
    public List<Transaction> list(int userId,String search,String type,String category,String from,String to) throws SQLException {
        StringBuilder sql=new StringBuilder("SELECT t.transaction_id,t.category_id,c.category_name,t.transaction_type,t.amount,t.transaction_date,t.description FROM transactions t JOIN categories c ON c.category_id=t.category_id WHERE t.user_id=?");
        List<Object> args=new ArrayList<>();args.add(userId);
        if(search!=null&&!search.isBlank()){sql.append(" AND t.description LIKE ?");args.add("%"+search+"%");}
        if(type!=null&&!type.isBlank()){sql.append(" AND t.transaction_type=?");args.add(type);}
        if(category!=null&&!category.isBlank()){sql.append(" AND t.category_id=?");args.add(Integer.parseInt(category));}
        if(from!=null&&!from.isBlank()){sql.append(" AND t.transaction_date>=?");args.add(from);}
        if(to!=null&&!to.isBlank()){sql.append(" AND t.transaction_date<=?");args.add(to);}
        sql.append(" ORDER BY t.transaction_date DESC,t.transaction_id DESC");
        List<Transaction> out=new ArrayList<>();try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(sql.toString())){for(int i=0;i<args.size();i++)p.setObject(i+1,args.get(i));try(ResultSet r=p.executeQuery()){while(r.next()){Transaction t=new Transaction();t.transactionId=r.getInt(1);t.categoryId=r.getInt(2);t.categoryName=r.getString(3);t.transactionType=r.getString(4);t.amount=r.getBigDecimal(5);t.transactionDate=r.getDate(6).toLocalDate();t.description=r.getString(7);out.add(t);}}}return out;
    }
    public void save(int userId,int id,int categoryId,String type,java.math.BigDecimal amount,LocalDate date,String description) throws SQLException {
        // A category can be shared or owned by this user; reject IDs belonging to another account.
        try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT 1 FROM categories WHERE category_id=? AND (user_id IS NULL OR user_id=?)")){p.setInt(1,categoryId);p.setInt(2,userId);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Choose one of your available categories.");}}
        if(amount.signum()<=0)throw new IllegalArgumentException("Amount must be greater than zero.");
        if(id==0){String s="INSERT INTO transactions(user_id,category_id,transaction_type,amount,transaction_date,description) VALUES(?,?,?,?,?,?)";try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(s)){p.setInt(1,userId);p.setInt(2,categoryId);p.setString(3,type);p.setBigDecimal(4,amount);p.setDate(5,java.sql.Date.valueOf(date));p.setString(6,description);p.executeUpdate();}}
        else {String s="UPDATE transactions SET category_id=?,transaction_type=?,amount=?,transaction_date=?,description=? WHERE transaction_id=? AND user_id=?";try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(s)){p.setInt(1,categoryId);p.setString(2,type);p.setBigDecimal(3,amount);p.setDate(4,java.sql.Date.valueOf(date));p.setString(5,description);p.setInt(6,id);p.setInt(7,userId);p.executeUpdate();}}
    }
    public void delete(int userId,int id) throws SQLException {try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("DELETE FROM transactions WHERE transaction_id=? AND user_id=?")){p.setInt(1,id);p.setInt(2,userId);p.executeUpdate();}}
    public Map<String,Object> summary(int userId,int month,int year) throws SQLException {
        Map<String,Object> m=new LinkedHashMap<>();String range="SELECT COALESCE(SUM(CASE WHEN transaction_type='INCOME' THEN amount ELSE 0 END),0) income,COALESCE(SUM(CASE WHEN transaction_type='EXPENSE' THEN amount ELSE 0 END),0) expenses,COUNT(*) count FROM transactions WHERE user_id=? AND MONTH(transaction_date)=? AND YEAR(transaction_date)=?";
        try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(range)){p.setInt(1,userId);p.setInt(2,month);p.setInt(3,year);try(ResultSet r=p.executeQuery()){r.next();m.put("income",r.getBigDecimal("income"));m.put("expenses",r.getBigDecimal("expenses"));m.put("count",r.getInt("count"));}}
        try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT category_id,category_name FROM categories WHERE user_id IS NULL OR user_id=? ORDER BY category_name")){p.setInt(1,userId);try(ResultSet r=p.executeQuery()){List<Map<String,Object>> categories=new ArrayList<>();while(r.next()){Map<String,Object>x=new LinkedHashMap<>();x.put("id",r.getInt(1));x.put("name",r.getString(2));categories.add(x);}m.put("categories",categories);}}
        try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT c.category_name,SUM(t.amount) total FROM transactions t JOIN categories c ON c.category_id=t.category_id WHERE t.user_id=? AND t.transaction_type='EXPENSE' AND MONTH(t.transaction_date)=? AND YEAR(t.transaction_date)=? GROUP BY c.category_id,c.category_name ORDER BY total DESC LIMIT 1")){p.setInt(1,userId);p.setInt(2,month);p.setInt(3,year);try(ResultSet r=p.executeQuery()){m.put("topCategory",r.next()?r.getString(1):"No spending yet");}}
        try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT c.category_name,SUM(t.amount) total FROM transactions t JOIN categories c ON c.category_id=t.category_id WHERE t.user_id=? AND t.transaction_type='EXPENSE' AND MONTH(t.transaction_date)=? AND YEAR(t.transaction_date)=? GROUP BY c.category_id,c.category_name ORDER BY total DESC")){p.setInt(1,userId);p.setInt(2,month);p.setInt(3,year);try(ResultSet r=p.executeQuery()){List<Map<String,Object>> totals=new ArrayList<>();while(r.next()){Map<String,Object>x=new LinkedHashMap<>();x.put("name",r.getString(1));x.put("amount",r.getBigDecimal(2));totals.add(x);}m.put("categorySpending",totals);}}
        return m;
    }
}
