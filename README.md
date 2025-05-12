# Expense Tracker Application
A Java Swing application for tracking personal expenses with MySQL backend and data visualization.

![Screenshot 2025-05-12 222949](https://github.com/user-attachments/assets/775c7d9f-a757-422e-9620-94845adc1f17)
![Screenshot 2025-05-12 223124](https://github.com/user-attachments/assets/3b374c6f-3a93-4e31-8d41-21f53a2a3495)
![Screenshot 2025-05-12 223114](https://github.com/user-attachments/assets/b66b0a53-394a-4b58-a268-5d8e2a4f962a)
![Screenshot 2025-05-12 223107](https://github.com/user-attachments/assets/ba38ad1b-af9b-403b-a642-7334811e2c66)
![Screenshot 2025-05-12 223038](https://github.com/user-attachments/assets/c2851a1d-a543-441f-aead-b988bc0e97a4)

## Features

- Add, view, and delete expenses
- Categorized spending analysis
- Monthly expense tracking
- Budget limit warnings
- Interactive charts and sortable tables

## Prerequisites

- Java JDK 8+
- MySQL Server 5.7+
- MySQL Connector/J

## Installation

1. **Set up the database**:
   ```bash
   mysql -u root -p < setup_database.sql

   
## 3. setup_database.sql

```sql
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
