-- BudgetWise schema. Run this file once in MySQL Workbench or with mysql < database.sql.
CREATE DATABASE IF NOT EXISTS budgetwise_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE budgetwise_db;

CREATE TABLE IF NOT EXISTS users (
  user_id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(190) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- NULL user_id means the category is shared as a built-in default.
CREATE TABLE IF NOT EXISTS categories (
  category_id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NULL,
  category_name VARCHAR(40) NOT NULL,
  CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  CONSTRAINT uq_category_owner_name UNIQUE (user_id, category_name)
);

CREATE TABLE IF NOT EXISTS transactions (
  transaction_id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  category_id INT NOT NULL,
  transaction_type ENUM('INCOME','EXPENSE') NOT NULL,
  amount DECIMAL(12,2) NOT NULL CHECK (amount > 0),
  transaction_date DATE NOT NULL,
  description VARCHAR(250) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_transactions_category FOREIGN KEY (category_id) REFERENCES categories(category_id),
  INDEX idx_transactions_owner_date (user_id, transaction_date),
  INDEX idx_transactions_category (category_id)
);

CREATE TABLE IF NOT EXISTS budgets (
  budget_id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  `month` TINYINT UNSIGNED NOT NULL,
  `year` SMALLINT UNSIGNED NOT NULL,
  budget_amount DECIMAL(12,2) NOT NULL CHECK (budget_amount > 0),
  CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  CONSTRAINT uq_budget_period UNIQUE (user_id, `month`, `year`)
);

INSERT INTO categories(user_id,category_name)
SELECT NULL, defaults.category_name FROM (
  SELECT 'Food' category_name UNION ALL SELECT 'Travel' UNION ALL SELECT 'Shopping' UNION ALL
  SELECT 'Entertainment' UNION ALL SELECT 'Bills' UNION ALL SELECT 'Health' UNION ALL
  SELECT 'Education' UNION ALL SELECT 'Other'
) defaults WHERE NOT EXISTS (
  SELECT 1 FROM categories existing WHERE existing.user_id IS NULL AND existing.category_name=defaults.category_name
);

-- Optional example aggregate query: monthly totals for one user.
-- SELECT transaction_type, SUM(amount) AS total FROM transactions
-- WHERE user_id=1 AND MONTH(transaction_date)=MONTH(CURRENT_DATE())
-- AND YEAR(transaction_date)=YEAR(CURRENT_DATE()) GROUP BY transaction_type;
