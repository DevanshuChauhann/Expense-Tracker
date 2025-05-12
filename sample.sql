-- Create database
CREATE DATABASE IF NOT EXISTS ExpenseTracker;
USE ExpenseTracker;

-- Create expenses table
CREATE TABLE IF NOT EXISTS expenses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    date DATE NOT NULL,
    category VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    amount DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create user and grant privileges
CREATE USER IF NOT EXISTS 'expense_user'@'localhost' IDENTIFIED BY 'securepassword';
GRANT ALL PRIVILEGES ON ExpenseTracker.* TO 'expense_user'@'localhost';
FLUSH PRIVILEGES;

-- Sample data (optional)
INSERT INTO expenses (date, category, description, amount) VALUES
('2023-01-15', 'Food', 'Grocery shopping', 85.50),
('2023-01-16', 'Transportation', 'Gas', 45.00),
('2023-01-17', 'Entertainment', 'Movie tickets', 24.00);