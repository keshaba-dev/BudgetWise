package com.budgetwise.controller;

import com.budgetwise.dao.*;
import com.google.gson.*;
import jakarta.servlet.http.*;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.DateTimeException;
import java.util.*;
import org.mindrot.jbcrypt.BCrypt;
import com.budgetwise.util.DBConnection;

/** Beginner-friendly JSON API. The session's user id is used for every private query. */
public class ApiServlet extends HttpServlet {
    private final Gson gson=new Gson(); private final UserDao users=new UserDao(); private final TransactionDao transactions=new TransactionDao(); private final CategoryDao categories=new CategoryDao();
    private JsonObject body(HttpServletRequest req)throws IOException{return JsonParser.parseReader(req.getReader()).getAsJsonObject();}
    private void json(HttpServletResponse res,int status,Object value)throws IOException{res.setStatus(status);res.setContentType("application/json;charset=UTF-8");res.getWriter().write(gson.toJson(value));}
    private int user(HttpServletRequest req){Object id=req.getSession(false)==null?null:req.getSession(false).getAttribute("userId");return id==null?0:(int)id;}
    private String str(JsonObject o,String key){return o.has(key)&&!o.get(key).isJsonNull()?o.get(key).getAsString():"";}
    private Map<String,Object> msg(String value){return Map.of("message",value);}
    @Override protected void service(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        res.setHeader("Cache-Control","no-store"); String path=req.getPathInfo()==null?"/":req.getPathInfo(); String method=req.getMethod();
        try {
            if(path.equals("/auth/register")&&method.equals("POST")){JsonObject b=body(req);String name=str(b,"name").trim(),email=str(b,"email").trim().toLowerCase(),pass=str(b,"password");if(name.isBlank()||!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")||pass.length()<8||!pass.equals(str(b,"confirmPassword"))){json(res,400,msg("Enter a name, valid email, matching passwords and at least 8 password characters."));return;}int id=users.register(name,email,BCrypt.hashpw(pass,BCrypt.gensalt()));req.getSession(true).setAttribute("userId",id);json(res,201,Map.of("id",id,"name",name,"email",email));return;}
            if(path.equals("/auth/login")&&method.equals("POST")){JsonObject b=body(req);UserDao.UserRecord u=users.findByEmail(str(b,"email").trim().toLowerCase());if(u==null||!BCrypt.checkpw(str(b,"password"),u.passwordHash())){json(res,401,msg("Email or password is incorrect."));return;}req.getSession(true).setAttribute("userId",u.id());req.changeSessionId();json(res,200,Map.of("id",u.id(),"name",u.name(),"email",u.email()));return;}
            if(path.equals("/auth/logout")){if(req.getSession(false)!=null)req.getSession(false).invalidate();json(res,200,msg("Signed out."));return;}
            if(path.equals("/auth/me")){int id=user(req);if(id==0){json(res,401,msg("Please sign in."));return;}UserDao.UserRecord u=profile(id);json(res,200,Map.of("id",id,"name",u.name(),"email",u.email()));return;}
            int uid=user(req);if(uid==0){json(res,401,msg("Please sign in to continue."));return;}
            if(path.equals("/transactions")){if(method.equals("GET")){json(res,200,transactions.list(uid,req.getParameter("search"),req.getParameter("type"),req.getParameter("category"),req.getParameter("from"),req.getParameter("to")));return;}if(method.equals("POST")){JsonObject b=body(req);transactions.save(uid,b.has("id")?b.get("id").getAsInt():0,b.get("categoryId").getAsInt(),type(str(b,"type")),new BigDecimal(str(b,"amount")),LocalDate.parse(str(b,"date")),str(b,"description").trim());json(res,200,msg("Transaction saved."));return;}}
            if(path.startsWith("/transactions/")&&method.equals("DELETE")){transactions.delete(uid,Integer.parseInt(path.substring(path.lastIndexOf('/')+1)));json(res,200,msg("Transaction deleted."));return;}
            if(path.equals("/categories")){if(method.equals("GET")){json(res,200,categories.list(uid));return;}JsonObject b=body(req);String name=str(b,"name").trim();if(name.isBlank())throw new IllegalArgumentException();if(method.equals("POST")){categories.add(uid,name);json(res,201,msg("Category added."));return;}if(method.equals("PUT")){categories.update(uid,b.get("id").getAsInt(),name);json(res,200,msg("Category updated."));return;}}
            if(path.startsWith("/categories/")&&method.equals("DELETE")){categories.delete(uid,Integer.parseInt(path.substring(path.lastIndexOf('/')+1)));json(res,200,msg("Category deleted if unused."));return;}
            if(path.equals("/budget")){int month=Integer.parseInt(req.getParameter("month")!=null?req.getParameter("month"):String.valueOf(LocalDate.now().getMonthValue()));int year=Integer.parseInt(req.getParameter("year")!=null?req.getParameter("year"):String.valueOf(LocalDate.now().getYear()));if(method.equals("GET")){json(res,200,budget(uid,month,year));return;}JsonObject b=body(req);saveBudget(uid,b.get("month").getAsInt(),b.get("year").getAsInt(),new BigDecimal(str(b,"amount")));json(res,200,msg("Monthly budget saved."));return;}
            if(path.equals("/dashboard")||path.equals("/insights")){int month=Integer.parseInt(req.getParameter("month")!=null?req.getParameter("month"):String.valueOf(LocalDate.now().getMonthValue()));int year=Integer.parseInt(req.getParameter("year")!=null?req.getParameter("year"):String.valueOf(LocalDate.now().getYear()));Map<String,Object> result=transactions.summary(uid,month,year);Map<String,Object> bud=budget(uid,month,year);result.putAll(bud);result.put("recent",transactions.list(uid,null,null,null,null,null).stream().limit(5).toList());json(res,200,result);return;}
            if(path.equals("/profile")){if(method.equals("GET")){UserDao.UserRecord u=profile(uid);json(res,200,Map.of("name",u.name(),"email",u.email()));return;}JsonObject b=body(req);if(!str(b,"password").isBlank()){if(str(b,"password").length()<8){json(res,400,msg("New password must be at least 8 characters."));return;}users.updatePassword(uid,BCrypt.hashpw(str(b,"password"),BCrypt.gensalt()));}users.updateProfile(uid,str(b,"name").trim(),str(b,"email").trim().toLowerCase());json(res,200,msg("Profile updated."));return;}
            json(res,404,msg("API route not found."));
        }catch(SQLIntegrityConstraintViolationException e){json(res,409,msg("That email or category already exists, or the item is still in use."));}catch(NumberFormatException|DateTimeException e){json(res,400,msg("Check the amount, date and selected options."));}catch(IllegalArgumentException e){json(res,400,msg("Please check the information you entered."));}catch(SQLException e){e.printStackTrace();json(res,500,msg("Database error. Check your MySQL setup and try again."));}
    }
    private String type(String t){if(!t.equals("INCOME")&&!t.equals("EXPENSE"))throw new IllegalArgumentException();return t;}
    private UserDao.UserRecord profile(int id)throws SQLException{try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT user_id,name,email,password FROM users WHERE user_id=?")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("User not found");return new UserDao.UserRecord(id,r.getString(2),r.getString(3),r.getString(4));}}}
    private Map<String,Object> budget(int uid,int month,int year)throws SQLException{BigDecimal amount=BigDecimal.ZERO,spent=BigDecimal.ZERO;try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT budget_amount FROM budgets WHERE user_id=? AND month=? AND year=?")){p.setInt(1,uid);p.setInt(2,month);p.setInt(3,year);try(ResultSet r=p.executeQuery()){if(r.next())amount=r.getBigDecimal(1);}}try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT COALESCE(SUM(amount),0) FROM transactions WHERE user_id=? AND transaction_type='EXPENSE' AND MONTH(transaction_date)=? AND YEAR(transaction_date)=?")){p.setInt(1,uid);p.setInt(2,month);p.setInt(3,year);try(ResultSet r=p.executeQuery()){r.next();spent=r.getBigDecimal(1);}}Map<String,Object> m=new LinkedHashMap<>();m.put("budget",amount);m.put("spent",spent);m.put("remaining",amount.subtract(spent));m.put("progress",amount.signum()==0?0:spent.multiply(BigDecimal.valueOf(100)).divide(amount,1,java.math.RoundingMode.HALF_UP));m.put("warning",amount.signum()>0&&spent.compareTo(amount.multiply(new BigDecimal("0.8")))>=0&&spent.compareTo(amount)<=0);m.put("exceeded",spent.compareTo(amount)>0);return m;}
    private void saveBudget(int uid,int month,int year,BigDecimal amount)throws SQLException{if(month<1||month>12||year<2000||year>2100||amount.signum()<=0)throw new IllegalArgumentException();String s="INSERT INTO budgets(user_id,month,year,budget_amount) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE budget_amount=VALUES(budget_amount)";try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(s)){p.setInt(1,uid);p.setInt(2,month);p.setInt(3,year);p.setBigDecimal(4,amount);p.executeUpdate();}}
}
