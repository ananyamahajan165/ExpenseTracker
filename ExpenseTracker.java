import java.util.*;

public class ExpenseTracker {

    static Scanner sc = new Scanner(System.in);
    static String loggedInUser = null;

    public static void main(String[] args) {

        while (true) {
            if (loggedInUser == null) {
                System.out.println("\n=== USER MENU ===");
                System.out.println("1. Register");
                System.out.println("2. Login");
                System.out.println("3. Exit");
                System.out.print("Choose option: ");

                int ch = sc.nextInt();
                switch (ch) {
                    case 1: register(); break;
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

    String sql = "INSERT INTO users (username, password) VALUES (?, ?)";

    try (java.sql.Connection con = DBConnection.getConnection();
         java.sql.PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, u);
        ps.setString(2, p);
        ps.executeUpdate();

        System.out.println("✅ User registered in DATABASE");

    } catch (java.sql.SQLIntegrityConstraintViolationException e) {
        System.out.println("❌ Username already exists!");
    } catch (Exception e) {
        e.printStackTrace();
    }
}

    // ---------------- LOGIN ----------------
    static void login() {
    sc.nextLine();
    System.out.println("\n--- User Login ---");

    System.out.print("Username: ");
    String u = sc.nextLine();

    System.out.print("Password: ");
    String p = sc.nextLine();

    String sql = "SELECT id FROM users WHERE username = ? AND password = ?";

    try (java.sql.Connection con = DBConnection.getConnection();
         java.sql.PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, u);
        ps.setString(2, p);

        java.sql.ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            loggedInUser = u;
            System.out.println("✅ Login successful!");
        } else {
            System.out.println("❌ Invalid credentials!");
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
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
            case 1: addExpense(); break;
            case 2: viewMyExpenses(); break;
            case 3: deleteExpense(); break;
            case 4: viewTotal(); break;
            case 5: loggedInUser = null; break;
            default: System.out.println("Invalid option");
        }
    }

    // ---------------- ADD xEXPENSE ----------------
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

    String sql = "INSERT INTO expenses (user_id, amount, category, description, date) " +
                 "VALUES ((SELECT id FROM users WHERE username=?), ?, ?, ?, ?)";

    try (java.sql.Connection con = DBConnection.getConnection();
         java.sql.PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, loggedInUser);
        ps.setDouble(2, amt);
        ps.setString(3, cat);
        ps.setString(4, desc);
        ps.setDate(5, java.sql.Date.valueOf(date));

        ps.executeUpdate();
        System.out.println("✅ Expense saved in DATABASE");

    } catch (Exception e) {
        e.printStackTrace();
    }
}


    // ---------------- VIEW OWN EXPENSES ----------------
    static void viewMyExpenses() {

    String sql =
        "SELECT e.id, e.amount, e.category, e.description, e.date " +
        "FROM expenses e " +
        "JOIN users u ON e.user_id = u.id " +
        "WHERE u.username = ?";

    boolean found = false;

    try (java.sql.Connection con = DBConnection.getConnection();
         java.sql.PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, loggedInUser);
        java.sql.ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            System.out.println(
                "ID:" + rs.getInt("id") +
                " | ₹" + rs.getDouble("amount") +
                " | " + rs.getString("category") +
                " | " + rs.getString("description") +
                " | " + rs.getDate("date")
            );
            found = true;
        }

        if (!found) {
            System.out.println("No expenses found.");
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
}


    // ---------------- DELETE ----------------
    static void deleteExpense() {

    System.out.print("Enter ID to delete: ");
    int id = sc.nextInt();

    String sql =
        "DELETE e FROM expenses e " +
        "JOIN users u ON e.user_id = u.id " +
        "WHERE e.id = ? AND u.username = ?";

    try (java.sql.Connection con = DBConnection.getConnection();
         java.sql.PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setInt(1, id);
        ps.setString(2, loggedInUser);

        int rows = ps.executeUpdate();

        if (rows > 0) {
            System.out.println("✅ Expense deleted from DATABASE");
        } else {
            System.out.println("❌ Invalid ID or not your expense");
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
}


    // ---------------- TOTAL ----------------
static void viewTotal() {

    String sql =
        "SELECT SUM(e.amount) AS total " +
        "FROM expenses e " +
        "JOIN users u ON e.user_id = u.id " +
        "WHERE u.username = ?";

    try (java.sql.Connection con = DBConnection.getConnection();
         java.sql.PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, loggedInUser);
        java.sql.ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            double total = rs.getDouble("total");
            System.out.println("Total Expense: ₹" + total);
        } else {
            System.out.println("Total Expense: ₹0");
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
}
}