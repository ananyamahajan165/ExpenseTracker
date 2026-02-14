import java.util.*;
import java.io.*;
import java.security.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ExpenseTracker {

    static Scanner sc = new Scanner(System.in);
    static String loggedInUser = null;

    static final String USER_FILE = "users.txt";
    static final String EXPENSE_FILE = "expenses.txt";
    static final String BUDGET_FILE = "budget.txt";

    // ================= MAIN =================
    public static void main(String[] args) {
        while (true) {
            if (loggedInUser == null) {
                userMenu();
            } else {
                expenseMenu();
            }
        }
    }

    // ================= USER MENU =================
    static void userMenu() {
        System.out.println("\n=== USER MENU ===");
        System.out.println("1. Register");
        System.out.println("2. Login");
        System.out.println("3. Forgot Password");
        System.out.println("4. Exit");

        int ch = readInt("Choose option: ");

        switch (ch) {
            case 1:
                register();
                pause();
                break;

            case 2:
                login();
                pause();
                break;

            case 3:
                forgotPassword();
                pause();
                break;

            case 4:
                System.out.println("Bye 👋");
                System.exit(0);

            default:
                System.out.println("Invalid option!");
                pause();
        }
    }

    // ================= EXPENSE MENU =================
    static void expenseMenu() {
        ensureFileExists(EXPENSE_FILE);
        ensureFileExists(BUDGET_FILE);
        System.out.println("\n=== EXPENSE MENU ===");
        System.out.println("Logged in as: " + loggedInUser);
        System.out.println("1. Set Monthly Budget");
        System.out.println("2. Add Expense");
        System.out.println("3. View Expenses");
        System.out.println("4. Total Expense");
        System.out.println("5. Change Password");
        System.out.println("6. Logout");

        int ch = readInt("Choose option: ");

        switch (ch) {
            case 1:
                setBudget();
                break;

            case 2:
                addExpense();
                break;

            case 3:
                viewExpenses();
                break;

            case 4:
                viewTotal();
                break;

            case 5:
                changePassword();
                break;

            case 6:
                loggedInUser = null;
                System.out.println("Logged out successfully");
                break;

            default:
                System.out.println("Invalid option!");
        }
    }

    // ================= REGISTER =================
    static void register() {
        System.out.print("Choose username: ");
        String u = sc.nextLine().toLowerCase();

        if (userExists(u)) {
            System.out.println("❌ Username already exists");
            return;
        }

        String p = readPassword("Choose password: ");
        String hashed = hashPassword(p);

        try (FileWriter fw = new FileWriter(USER_FILE, true)) {
            fw.write(u + "|" + hashed + "\n");
            System.out.println("✅ Registration successful");
        } catch (Exception e) {
            System.out.println("❌ Error saving user");
        }
    }

    // ================= LOGIN =================
    static void login() {
        System.out.print("Username: ");
        String u = sc.nextLine().toLowerCase();

        String p = readPassword("Password: ");
        String hashed = hashPassword(p);

        try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split("\\|");
                if (d[0].equals(u) && d[1].equals(hashed)) {
                    loggedInUser = u;
                    System.out.println("✅ Login successful");
                    return;
                }
            }
        } catch (Exception e) {
        }

        System.out.println("❌ Invalid credentials");
    }

    // ================= FORGOT PASSWORD =================
    static void forgotPassword() {
        System.out.print("Enter username: ");
        String u = sc.nextLine().toLowerCase();

        List<String> users = new ArrayList<>();
        boolean found = false;

        try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split("\\|");
                if (d[0].equals(u)) {
                    users.add(u + "|" + hashPassword("1234"));
                    found = true;
                } else {
                    users.add(line);
                }
            }
        } catch (Exception e) {
        }

        if (!found) {
            System.out.println("❌ User not found");
            return;
        }

        try (FileWriter fw = new FileWriter(USER_FILE)) {
            for (String s : users)
                fw.write(s + "\n");
            System.out.println("✅ Password reset to default: 1234");
        } catch (Exception e) {
        }
    }

    // ================= CHANGE PASSWORD =================
    static void changePassword() {
        String oldPwd = readPassword("Enter old password: ");
        String oldHash = hashPassword(oldPwd);

        List<String> users = new ArrayList<>();
        boolean updated = false;

        try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split("\\|");
                if (d[0].equals(loggedInUser) && d[1].equals(oldHash)) {
                    String newPwd = readPassword("Enter new password: ");
                    users.add(d[0] + "|" + hashPassword(newPwd));
                    updated = true;
                } else {
                    users.add(line);
                }
            }
        } catch (Exception e) {
        }

        if (!updated) {
            System.out.println("❌ Old password incorrect");
            return;
        }

        try (FileWriter fw = new FileWriter(USER_FILE)) {
            for (String s : users)
                fw.write(s + "\n");
            System.out.println("✅ Password changed successfully");
        } catch (Exception e) {
        }
    }

    // ================= SET BUDGET =================
    static void setBudget() {
        System.out.print("Enter monthly budget: ");
        double budget = Double.parseDouble(sc.nextLine());

        try (FileWriter fw = new FileWriter(BUDGET_FILE)) {
            fw.write(String.valueOf(budget));
            System.out.println("✅ Budget saved");
        } catch (Exception e) {
        }
    }

    // ================= ADD EXPENSE =================
    static void addExpense() {
    try (FileWriter fw = new FileWriter(EXPENSE_FILE, true)) {

        System.out.print("Amount: ");
        double amt = Double.parseDouble(sc.nextLine());

        System.out.print("Category: ");
        String cat = sc.nextLine();

        System.out.print("Description: ");
        String desc = sc.nextLine();

        LocalDate date = LocalDate.now();              // 📅 date
        LocalDateTime timestamp = LocalDateTime.now(); // ⏰ time

        fw.write(
            loggedInUser + "|" +
            amt + "|" +
            cat + "|" +
            date + "|" +
            timestamp + "|" +
            desc + "\n"
        );

        System.out.println("✅ Expense added with date & time");
        checkBudgetAlert();

    } catch (Exception e) {
        System.out.println("❌ Error adding expense");
    }
}

    // ================= ALERT =================
    static void checkBudgetAlert() {
        double total = 0, budget;

        try (BufferedReader br = new BufferedReader(new FileReader(EXPENSE_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split("\\|");
                if (d[0].equals(loggedInUser)) {
                    total += Double.parseDouble(d[1]);
                }
            }
        } catch (Exception e) {
        }

        try (BufferedReader br = new BufferedReader(new FileReader(BUDGET_FILE))) {
            budget = Double.parseDouble(br.readLine());
        } catch (Exception e) {
            return;
        }

        if (total >= budget * 0.9) {
            System.out.println("⚠️ ALERT: You have used 90% of your budget!");
        }
    }

    // ================= VIEW =================
    static void viewExpenses() {
        System.out.println("\n--- Your Expenses ---");
        boolean found = false;

        try (BufferedReader br = new BufferedReader(new FileReader(EXPENSE_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split("\\|");
                if (d[0].equals(loggedInUser)) {
                    System.out.println("₹" + d[1] + " | " + d[2] + " | " + d[3]);
                    found = true;
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Error reading expenses file");
            return;
        }

        if (!found) {
            System.out.println("No expenses found.");
        }

        System.out.println("----------------------");
    }

    static void viewTotal() {
        ensureFileExists(EXPENSE_FILE);
        ensureFileExists(BUDGET_FILE);

        double totalExpense = 0;
        double budget = 0;
        boolean expenseFound = false;

        // 1️⃣ Read total expenses of logged-in user
        try (BufferedReader br = new BufferedReader(new FileReader(EXPENSE_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split("\\|");
                if (d.length >= 2 && d[0].equals(loggedInUser)) {
                    totalExpense += Double.parseDouble(d[1]);
                    expenseFound = true;
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Error reading expenses");
            return;
        }

        // 2️⃣ Read budget
        try (BufferedReader br = new BufferedReader(new FileReader(BUDGET_FILE))) {
            String line = br.readLine();
            if (line != null) {
                budget = Double.parseDouble(line);
            }
        } catch (Exception e) {
            System.out.println("❌ Monthly budget not set");
            return;
        }

        // 3️⃣ Calculate remaining
        double remaining = budget - totalExpense;

        // 4️⃣ Display result
        System.out.println("\n--- Monthly Expense Summary ---");
        System.out.println("Monthly Budget : ₹" + budget);
        System.out.println("Total Expense  : ₹" + totalExpense);
        System.out.println("Remaining      : ₹" + remaining);

        if (!expenseFound) {
            System.out.println("(No expenses recorded yet)");
        }

        if (remaining <= 0) {
            System.out.println("⚠️ Budget exceeded!");
        } else if (remaining <= budget * 0.1) {
            System.out.println("⚠️ Warning: Only 10% budget left!");
        }

        System.out.println("--------------------------------");
    }

    // ================= HELPERS =================
    static boolean userExists(String username) {
        try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(username + "|"))
                    return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    static int readInt(String msg) {
        while (true) {
            try {
                System.out.print(msg);
                return Integer.parseInt(sc.nextLine());
            } catch (Exception e) {
                System.out.println("Enter valid number");
            }
        }
    }

    static String readPassword(String msg) {
        Console c = System.console();
        if (c != null) {
            return new String(c.readPassword(msg));
        } else {
            System.out.print(msg);
            return sc.nextLine();
        }
    }

    static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte x : b)
                sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            return password;
        }
    }

    static void ensureFileExists(String fileName) {
        try {
            File f = new File(fileName);
            if (!f.exists()) {
                f.createNewFile();
            }
        } catch (Exception e) {
            System.out.println("❌ Error creating file: " + fileName);
        }
    }

    static void pause() {
        System.out.println("\nPress ENTER to continue...");
        sc.nextLine();
    }
}
