import java.util.*;
import java.io.*;

public class ExpenseTracker {

    static ArrayList<User> users = new ArrayList<>();
    static ArrayList<Expense> expenses = new ArrayList<>();

    static Scanner sc = new Scanner(System.in);
    static String loggedInUser = null;
    static int counter = 1;

    static final String USER_FILE = "users.txt";
    static final String EXPENSE_FILE = "expenses.txt";

    public static void main(String[] args) {

        loadUsers();
        loadExpenses();

        while (true) {
            if (loggedInUser == null) {
                System.out.println("\n=== USER MENU ===");
                System.out.println("1. Register");
                System.out.println("2. Login");
                System.out.println("3. Exit");
                System.out.print("Choose option: ");

                int ch = sc.nextInt();
                switch (ch) {
                    case 1: register(); saveUsers(); break;
                    case 2: login(); break;
                    case 3: return;
                    default: System.out.println("Invalid option!");
                }
            } else {
                expenseMenu();
            }
        }
    }

    // ---------------- REGISTER ----------------
    static void register() {
        sc.nextLine();
        System.out.println("\n--- Register New User ---");
        System.out.print("Choose username: ");
        String u = sc.nextLine();

        System.out.print("Choose password: ");
        String p = sc.nextLine();

        users.add(new User(u, p));
        System.out.println("User registered successfully!");
    }

    // ---------------- LOGIN ----------------
    static void login() {
        sc.nextLine();
        System.out.println("\n--- User Login ---");

        System.out.print("Username: ");
        String u = sc.nextLine();

        System.out.print("Password: ");
        String p = sc.nextLine();

        for (User user : users) {
            if (user.username.equals(u) && user.password.equals(p)) {
                loggedInUser = u;
                System.out.println("Login successful!");
                return;
            }
        }
        System.out.println("Invalid credentials!");
    }

    // ---------------- EXPENSE MENU ----------------
    static void expenseMenu() {
        System.out.println("\n=== EXPENSE MENU ===");
        System.out.println("Logged in as: " + loggedInUser);
        System.out.println("1. Add Expense");
        System.out.println("2. View My Expenses");
        System.out.println("3. Delete Expense by ID");
        System.out.println("4. View Total Expense");
        System.out.println("5. Logout");
        System.out.print("Choose option: ");
        int ch = sc.nextInt();

        switch (ch) {
            case 1: addExpense(); saveExpenses(); break;
            case 2: viewMyExpenses(); break;
            case 3: deleteExpense(); saveExpenses(); break;
            case 4: viewTotal(); break;
            case 5: loggedInUser = null; break;
            default: System.out.println("Invalid option");
        }
    }

    // ---------------- ADD EXPENSE ----------------
    static void addExpense() {
        System.out.print("Amount: ");
        double amt = sc.nextDouble();
        sc.nextLine();

        System.out.print("Category: ");
        String cat = sc.nextLine();

        System.out.print("Description: ");
        String desc = sc.nextLine();

        System.out.print("Date (YYYY-MM-DD): ");
        String date = sc.nextLine();

        expenses.add(new Expense(counter++, loggedInUser, amt, cat, desc, date));
        System.out.println("Expense added!");
    }

    // ---------------- VIEW OWN EXPENSES ----------------
    static void viewMyExpenses() {
        boolean found = false;
        for (Expense e : expenses) {
            if (e.username.equals(loggedInUser)) {
                System.out.println(
                    "ID:" + e.id +
                    " | ₹" + e.amount +
                    " | " + e.category +
                    " | " + e.description +
                    " | " + e.date
                );
                found = true;
            }
        }
        if (!found) System.out.println("No expenses found.");
    }

    // ---------------- DELETE ----------------
    static void deleteExpense() {
        System.out.print("Enter ID to delete: ");
        int id = sc.nextInt();

        boolean removed = expenses.removeIf(
            e -> e.id == id && e.username.equals(loggedInUser)
        );

        if (removed) System.out.println("Expense deleted!");
        else System.out.println("Invalid ID!");
    }

    // ---------------- TOTAL ----------------
    static void viewTotal() {
        double total = 0;
        for (Expense e : expenses) {
            if (e.username.equals(loggedInUser)) {
                total += e.amount;
            }
        }
        System.out.println("Total Expense: ₹" + total);
    }

    // ---------------- SAVE / LOAD USERS ----------------
    static void saveUsers() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(USER_FILE))) {
            for (User u : users) {
                pw.println(u.username + "," + u.password);
            }
        } catch (Exception e) {}
    }

    static void loadUsers() {
        File f = new File(USER_FILE);
        if (!f.exists()) return;

        try (Scanner fs = new Scanner(f)) {
            while (fs.hasNextLine()) {
                String[] p = fs.nextLine().split(",");
                users.add(new User(p[0], p[1]));
            }
        } catch (Exception e) {}
    }

    // ---------------- SAVE / LOAD EXPENSES ----------------
    static void saveExpenses() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(EXPENSE_FILE))) {
            for (Expense e : expenses) {
                pw.println(e.id + "," + e.username + "," + e.amount + "," +
                           e.category + "," + e.description + "," + e.date);
            }
        } catch (Exception e) {}
    }

    static void loadExpenses() {
        File f = new File(EXPENSE_FILE);
        if (!f.exists()) return;

        try (Scanner fs = new Scanner(f)) {
            while (fs.hasNextLine()) {
                String[] p = fs.nextLine().split(",");
                int id = Integer.parseInt(p[0]);
                expenses.add(new Expense(id, p[1],
                        Double.parseDouble(p[2]), p[3], p[4], p[5]));
                counter = Math.max(counter, id + 1);
            }
        } catch (Exception e) {}
    }
}
