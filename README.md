# BudgetWise — Smart Personal Expense & Budget Tracker

BudgetWise is a beginner-friendly personal finance web application. Users can record income and expenses, organize entries with categories, set monthly spending targets, and review simple financial summaries. The browser frontend is plain HTML, CSS and JavaScript; a small Java server exposes JSON endpoints and uses JDBC to store data in MySQL.

## Features

- Registration, login and logout with session-based authentication and BCrypt password hashes
- Private, account-scoped transaction data with add, edit, delete, search and filters
- Shared built-in categories and custom personal categories
- Monthly budget, remaining amount, progress indicator, 80% notice and exceeded-budget alert
- Dashboard with monthly income, expenses, balance, budget and recent activity
- Financial insights with a spending-by-category summary
- Profile and password updates
- Responsive landing, about, registration, login, dashboard, transactions, add/edit transaction, categories, budget, insights and profile pages

## Screenshots

Add screenshots here after running the application:

| Landing page | Dashboard |
| --- | --- |
| `screenshots/landing.png` | `screenshots/dashboard.png` |

## Technology stack

- Java 17+, Maven, Jetty 11
- JDBC and MySQL Connector/J
- MySQL 8+
- HTML, CSS and vanilla JavaScript
- Git and GitHub

## Project structure

```text
BudgetWise/
├── database/database.sql              # Database, tables, keys and default categories
├── frontend/                           # Static pages served by the Java app
│   ├── css/styles.css                  # Shared responsive design system
│   ├── js/app.js                       # Navigation, rendering, forms and API calls
│   └── *.html                          # Landing and feature pages
├── src/main/java/com/budgetwise/
│   ├── BudgetWiseApplication.java      # Starts Jetty and serves frontend/API
│   ├── controller/ApiServlet.java      # HTTP routes, validation and sessions
│   ├── dao/                            # Prepared JDBC statements and SQL queries
│   ├── model/                          # Simple data objects
│   └── util/DBConnection.java          # MySQL connection settings
├── .env.example                        # Safe example configuration
└── pom.xml                             # Maven dependencies and run configuration
```

## Database schema

- `users` stores a user's name, unique email and BCrypt password hash.
- `categories` stores shared defaults (`user_id` is NULL) and user-owned categories.
- `transactions` links each income/expense row to its owner and category. Dates and amounts support monthly summaries.
- `budgets` stores one amount per user, month and year. A unique key prevents duplicate monthly budgets.

Foreign keys connect transactions and budgets to users; transactions also link to categories. Deleting a user cascades their own data. A category referenced by transactions cannot be deleted.

## Database setup

1. Install MySQL 8 and make sure the MySQL server is running.
2. Run `database/database.sql` in MySQL Workbench, or run `mysql -u root -p < database/database.sql` from the project folder.
3. Set `BUDGETWISE_DB_URL`, `BUDGETWISE_DB_USER` and `BUDGETWISE_DB_PASSWORD` in your terminal. The default URL is `jdbc:mysql://localhost:3306/budgetwise_db?serverTimezone=UTC`.
4. Never put a real password into Java source or commit it to Git.

PowerShell example for the current terminal:

```powershell
$env:BUDGETWISE_DB_USER = "root"
$env:BUDGETWISE_DB_PASSWORD = "your-local-password"
$env:BUDGETWISE_DB_URL = "jdbc:mysql://localhost:3306/budgetwise_db?serverTimezone=UTC"
```

## Run locally

Install JDK 17+ and Maven 3.9+, configure MySQL, then run:

```bash
mvn clean compile exec:java
```

Open [http://localhost:8080](http://localhost:8080). The server uses `PORT` when provided and defaults to 8080. Create an account, then add transactions and a monthly budget. Maven downloads the dependencies listed in `pom.xml` on the first run.

## Important JDBC concepts

- `DBConnection.getConnection()` opens a connection from environment-based settings.
- DAO classes group straightforward SQL operations by data type.
- `PreparedStatement` binds user input separately from SQL text, helping prevent SQL injection.
- Try-with-resources closes connections, statements and result sets reliably.
- The session's `userId` is added to private queries so one account cannot list another account's records.

## Key SQL queries

- Monthly income and expense totals use `SUM(CASE WHEN transaction_type=...)`.
- Transaction counts use `COUNT(*)`.
- Category spending uses `GROUP BY category_id, category_name` and sorts by total.
- Budget upsert uses `INSERT ... ON DUPLICATE KEY UPDATE` with the unique user/month/year key.

## Frontend and backend communication

The frontend JavaScript uses `fetch('/api/...')` to send and receive JSON. The Java servlet validates the request, reads the signed-in user from the HTTP session, calls the relevant DAO, and returns a JSON response. Jetty serves the static `frontend/` files and the `/api` servlet from the same origin.

## GitHub Pages and deployment

GitHub Pages can host the static landing/about pages, but it cannot run this Java server or connect securely to MySQL. For the full application, deploy the Java service and MySQL database to a server that supports them; do not publish credentials. If publishing only the presentation pages, keep API-dependent account pages on the Java server.

## Future improvements

- Add CSV export, recurring transactions and downloadable monthly reports
- Add stronger email verification and password reset for a production deployment
- Add pagination for very large transaction histories
- Add automated integration tests against a disposable MySQL database
- Add charts with an accessible charting library

## Interview overview

The user enters a page served by Jetty. JavaScript sends a JSON request to `ApiServlet`. The servlet checks the session and input, then calls a DAO. The DAO executes a prepared SQL statement through `DBConnection`. The response returns as JSON and JavaScript updates the page. The `users` table owns private transactions and budgets; categories are shared defaults or are owned by one user. The app computes monthly totals in SQL and compares monthly expense totals with the budget to produce progress and warning states.
