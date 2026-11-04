# BudgetWise: interview study guide

## 1. How the application works

BudgetWise has two parts that run together on one local server:

1. Jetty serves the HTML pages, shared CSS and JavaScript from `frontend/`.
2. Jetty sends `/api/...` requests to the Java `ApiServlet`.
3. The servlet validates the request and reads the signed-in user's ID from the HTTP session.
4. A DAO builds a prepared SQL statement and runs it through JDBC.
5. The servlet returns JSON. The browser's JavaScript uses that response to update the page.

For example, saving an expense sends its type, category, amount, date and description as JSON. The servlet checks the session and values, `TransactionDao` executes an `INSERT`, and the browser returns to the transaction list.

## 2. What each major folder does

- `frontend/`: HTML page shells, shared responsive styles and browser-side JavaScript. Keeping the files separate makes the presentation easier to adjust without changing SQL.
- `src/main/java/com/budgetwise/controller/`: `ApiServlet` handles HTTP routes, sessions, input checks and JSON responses.
- `src/main/java/com/budgetwise/dao/`: DAO means Data Access Object. Each DAO keeps the SQL for a type of data near its insert, select, update and delete code.
- `src/main/java/com/budgetwise/model/`: simple Java objects that hold data such as a transaction.
- `src/main/java/com/budgetwise/util/`: small shared utilities. `DBConnection` reads the local database settings and opens connections.
- `database/`: the SQL script used to create the schema and default categories.
- `docs/`: learning and project documentation.

## 3. Database tables and relationships

- `users`: one row for each account. Its generated `user_id` is the owner's ID elsewhere. Passwords contain BCrypt hashes, not the original password.
- `categories`: labels that group transactions. A NULL `user_id` identifies a shared built-in category; otherwise the category belongs to that user.
- `transactions`: each row belongs to one user and references one category. `transaction_type` is either `INCOME` or `EXPENSE`.
- `budgets`: one monthly amount for one user. A unique key on user, month and year allows the app to update the existing monthly budget.

Relationships: one user can have many transactions, custom categories and budgets. One category can label many transactions. The shared categories have no single user owner. The app includes the current user's ID in private data queries so a different session cannot read or change those records through these routes.

## 4. How JDBC connects Java to MySQL

`DBConnection.getConnection()` calls `DriverManager.getConnection(...)`. The URL, username and password come from environment variables. That keeps the local password out of the source files. A DAO then uses try-with-resources to open a `Connection`, prepare a statement, bind values, execute it and close resources automatically.

`PreparedStatement` is important because the SQL structure is separate from the values typed by the user. For instance, a transaction description is bound with `setString`; it is not pasted into the SQL command.

## 5. Important SQL examples

Monthly totals use conditional sums:

```sql
SELECT
  SUM(CASE WHEN transaction_type = 'INCOME' THEN amount ELSE 0 END) AS income,
  SUM(CASE WHEN transaction_type = 'EXPENSE' THEN amount ELSE 0 END) AS expenses,
  COUNT(*) AS transaction_count
FROM transactions
WHERE user_id = ? AND MONTH(transaction_date) = ? AND YEAR(transaction_date) = ?;
```

Spending by category uses `GROUP BY`:

```sql
SELECT c.category_name, SUM(t.amount) AS total
FROM transactions t
JOIN categories c ON c.category_id = t.category_id
WHERE t.user_id = ? AND t.transaction_type = 'EXPENSE'
  AND MONTH(t.transaction_date) = ? AND YEAR(t.transaction_date) = ?
GROUP BY c.category_id, c.category_name
ORDER BY total DESC;
```

The budget query uses `INSERT ... ON DUPLICATE KEY UPDATE`. The unique key for user/month/year makes saving twice update the same budget row instead of creating duplicates.

## 6. How to run it locally

Install JDK 17 or later, Maven 3.9 or later and MySQL 8 or later. Run `database/database.sql`, configure `BUDGETWISE_DB_URL`, `BUDGETWISE_DB_USER` and `BUDGETWISE_DB_PASSWORD`, then run `mvn clean compile exec:java` in the project folder. Visit `http://localhost:8080` and register a new account.

## Interview-sized summary

“BudgetWise is a Java and MySQL expense tracker with a plain JavaScript frontend. Jetty serves the pages and a JSON servlet. The servlet stores the login in a session, validates requests and calls small JDBC DAOs. Prepared statements handle database input. Every private finance query is scoped to the signed-in user's ID. SQL aggregates summarize monthly income and expenses, while the app compares monthly expenses with a saved budget to show progress and warnings.”
