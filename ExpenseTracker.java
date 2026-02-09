import java.util.*;
import java.io.*;
import java.security.*;
import java.io.Console;

public class ExpenseTracker {

    static Scanner sc = new Scanner(System.in);
    static String loggedInUser = null;

    static final String USER_FILE = "users.txt";
    static final String EXPENSE_FILE = "expenses.txt";

    // ================= MAIN =================
    public static void main(String[] args) {

        while (true) {
            if (loggedInUser == null) {
                System.out.println("\n=== USER MENU ===");
                System.out.println("1. Register");
                System.out.println("2. Login");
                System.out.println("3. Exit");

                int ch = readInt("Choose option: ");

                switch (ch) {
                    case 1: register(); break;
                    case 2: login(); break;
                    case 3: System.out.println("Bye 👋"); return;
                    default: System.out.println("Invalid option!");
                }
            } else {
                expenseMenu();
            }
        }
    }

    // ================= REGISTER =================
    static void register() {
        System.out.print("Choose username: ");
        String u = sc.nextLine();

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
        String u = sc.nextLine();

        String p = readPassword("Password: ");
        String hashed = hashPassword(p);

        try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split("\\|");
                if (data[0].equals(u) && data[1].equals(hashed)) {
                    loggedInUser = u;
                    System.out.println("✅ Login successful");
                    return;
                }
            }
        } catch (Exception e) {}

        System.out.println("❌ Invalid credentials");
    }

    // ================= EXPENSE MENU =================
    static void expenseMenu() {
        System.out.println("\n=== EXPENSE MENU ===");
        System.out.println("User: " + loggedInUser);
        System.out.println("1. Add Expense");
        System.out.println("2. View Expenses");
        System.out.println("3. Total Expense");
        System.out.println("4. Logout");

        int ch = readInt("Choose option: ");

        switch (ch) {
            case 1: addExpense(); break;
            case 2: viewExpenses(); break;
            case 3: viewTotal(); break;
            case 4: loggedInUser = null; break;
            default: System.out.println("Invalid option!");
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

            fw.write(loggedInUser + "|" + amt + "|" + cat + "|" + desc + "\n");
            System.out.println("✅ Expense added");

        } catch (Exception e) {
            System.out.println("❌ Error adding expense");
        }
    }

    // ================= VIEW EXPENSES =================
    static void viewExpenses() {
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
        } catch (Exception e) {}

        if (!found) System.out.println("No expenses found");
    }

    // ================= TOTAL =================
    static void viewTotal() {
        double total = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(EXPENSE_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split("\\|");
                if (d[0].equals(loggedInUser)) {
                    total += Double.parseDouble(d[1]);
                }
            }
        } catch (Exception e) {}

        System.out.println("Total Expense: ₹" + total);
    }

    // ================= HELPERS =================
    static boolean userExists(String username) {
        try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(username + "|")) return true;
            }
        } catch (Exception e) {}
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

    // ===== PASSWORD HIDE =====
    static String readPassword(String msg) {
        Console console = System.console();
        if (console != null) {
            char[] pwd = console.readPassword(msg);
            return new String(pwd);
        } else {
            // VS Code fallback
            System.out.print(msg);
            return sc.nextLine();
        }
    }

    // ===== PASSWORD HASH =====
    static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(password.getBytes());

            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (Exception e) {
            return password;
        }
    }
}
