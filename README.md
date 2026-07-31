# Expense Tracker

A full-stack expense tracker built with a Java backend and a simple HTML/CSS/JavaScript frontend. The app supports user authentication, month-wise expense tracking, monthly budgets, CSV export, and password reset.

## Features

- User registration and login
- Forgot password flow
- Add and delete expenses
- Month-wise expense management
- Month-wise budget tracking
- Expense filtering, searching, and sorting
- CSV export for the selected month
- Separate data per user
  
## Tech Stack
- Backend: Core Java with `HttpServer`
- Frontend: HTML, CSS, JavaScript
- Storage: Plain text files in `data/`

## Project Structure

```text
ExpenseTracker/
├── backend/
│   ├── ExpenseTracker.java
│   ├── FileService.java
│   ├── Expense.java
│   └── User.java
├── frontend/
│   ├── index.html
│   ├── style.css
│   └── script.js
├── data/
│   ├── users.txt
│   ├── expenses.txt
│   └── budget.txt
└── README.md
```

## How It Works

### Authentication

- `Register` creates a new user
- `Login` signs the user in
- `Forgot Password` lets the user reset their password by entering a username and a new password
- Passwords are stored as SHA-256 hashes in `data/users.txt`

### Expenses

- Users can add expenses with:
  - amount
  - category
  - description
  - date
- Expenses are stored in `data/expenses.txt`
- Each expense is linked to the logged-in user

### Monthly View

- The dashboard includes a month selector
- Expenses, totals, budgets, and CSV export are all based on the selected month
- This means you do not need to re-enter old data every month just to continue using the app

### Budget

- Budget is saved separately for each user and each month
- Example:
  - March budget and April budget are treated separately

## API Endpoints

The Java server currently exposes these routes:

- `GET /` - serves the frontend
- `POST /register`
- `POST /login`
- `POST /forgot-password`
- `POST /logout`
- `GET /expenses?month=YYYY-MM`
- `POST /add-expense`
- `DELETE /deleteExpense/{timestamp}`
- `GET /summary?month=YYYY-MM`
- `POST /set-budget`

## How to Run

### 1. Compile the backend

```bash
javac backend/*.java
```

### 2. Start the server

```bash
java backend.ExpenseTracker
```

The app starts on:

```text
http://localhost:9080
```

### 3. Open the app

Open this URL in your browser:

[http://localhost:9080](http://localhost:9080)

## Data Files

### `data/users.txt`

Stores user credentials in this format:

```text
username|hashed_password
```

### `data/expenses.txt`

Stores expenses in this format:

```text
username|amount|category|date|timestamp|description
```

### `data/budget.txt`

Stores monthly budgets in this format:

```text
username|YYYY-MM|budget
```

## Usage Flow

1. Register a new account
2. Login with your credentials
3. Select the month you want to work on
4. Set that month’s budget
5. Add expenses with date, category, and description
6. View total, remaining amount, and filtered expense list
7. Export that month’s data to CSV if needed

## Notes

- The project still contains some older console-based logic in the Java file, but the current app flow is browser-based
- Data is stored in text files, so this project is best suited for learning, demos, and small local use
- The forgot password flow is basic and does not use email or OTP verification

## Future Improvements

- Edit expense support in the web UI
- Better validation and error handling
- Safer password recovery flow
- Move from text files to a database
- Cleaner backend separation into controllers/services/models

## Author

Ananya Mahajan
