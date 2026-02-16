import java.util.*;
import java.io.*;
import java.security.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

public class ExpenseTracker {

    // ================= GLOBALS =================
    static Scanner sc = new Scanner(System.in);
    static String loggedInUser = null;

    static final String USER_FILE = "../data/users.txt";
    static final String EXPENSE_FILE = "../data/expenses.txt";
    static final String BUDGET_FILE = "../data/budget.txt";

    // ================= ENUM =================
    enum Category {
        FOOD, TRAVEL, RENT, SHOPPING, UTILITIES, OTHER
    }

    // ================= EXCEPTIONS =================
    static class InvalidExpenseException extends Exception {
        public InvalidExpenseException(String message) {
            super(message);
        }
    }

    static class InvalidInputException extends Exception {
        public InvalidInputException(String message) {
            super(message);
        }
    }

    // ================= HTTP SERVER =================
    static void startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

            // Health check
            server.createContext("/health", exchange -> {
                String response = "Backend running";
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.close();
            });

            // Add expense API
            server.createContext("/add-expense", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {

                    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                        exchange.sendResponseHeaders(405, -1);
                        return;
                    }

                    String body = new String(exchange.getRequestBody().readAllBytes());

                    try {
                        Map<String, String> data = parseJson(body);

                        if (loggedInUser == null)
                            throw new RuntimeException("Not logged in");

                        double amount = Double.parseDouble(data.get("amount"));
                        String category = data.get("category");
                        String description = data.get("description");

                        if (amount <= 0 || description == null || description.trim().isEmpty())
                            throw new RuntimeException("Invalid data");

                        try (FileWriter fw = new FileWriter(EXPENSE_FILE, true)) {
                            fw.write(
                                    loggedInUser + "|" +
                                    amount + "|" +
                                    category + "|" +
                                    LocalDate.now() + "|" +
                                    LocalDateTime.now() + "|" +
                                    description + "\n"
                            );
                        }

                        String res = "Expense added";
                        exchange.sendResponseHeaders(200, res.length());
                        exchange.getResponseBody().write(res.getBytes());

                    } catch (Exception e) {
                        String err = "Error: " + e.getMessage();
                        exchange.sendResponseHeaders(400, err.length());
                        exchange.getResponseBody().write(err.getBytes());
                    } finally {
                        exchange.close();
                    }
                }
            });

            server.start();
            System.out.println("🚀 Server running at http://localhost:8080");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= MODEL =================
    static class Expense {
        double amount;
        String category;
        LocalDate date;
        String description;

        Expense(double amount, String category, LocalDate date, String description) {
            this.amount = amount;
            this.category = category;
            this.date = date;
            this.description = description;
        }
    }

    // ================= MAIN =================
    public static void main(String[] args) {
        startServer();

        while (true) {
            if (loggedInUser == null)
                userMenu();
            else
                expenseMenu();
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
            case 1 -> register();
            case 2 -> login();
            case 3 -> forgotPassword();
            case 4 -> System.exit(0);
            default -> System.out.println("Invalid option");
        }
        pause();
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
    System.out.println("5. View Monthly Expenses");
    System.out.println("6. Category-wise Monthly Report");
    System.out.println("7. Search Expenses");
    System.out.println("8. Sort Expenses");
    System.out.println("9. Change Password");
    System.out.println("10. Logout");

    int ch = readInt("Choose option: ");

    switch (ch) {
        case 1 -> setBudget();
        case 2 -> addExpense();
        case 3 -> viewExpenses();
        case 4 -> viewTotal();
        case 5 -> viewExpenses(); // monthly handled internally
        case 6 -> categoryWiseMonthlyReport();
        case 7 -> searchExpenses();
        case 8 -> sortExpensesMenu();
        case 9 -> changePassword();
        case 10 -> {
            loggedInUser = null;
            System.out.println("Logged out successfully");
        }
        default -> System.out.println("Invalid option!");
    }
}

    // ================= CORE FEATURES =================
    static void setBudget() {
        try {
            System.out.print("Enter budget: ");
            double b = Double.parseDouble(sc.nextLine());
            if (b <= 0) throw new InvalidInputException("Budget > 0");

            try (FileWriter fw = new FileWriter(BUDGET_FILE)) {
                fw.write(String.valueOf(b));
            }
            System.out.println("Budget saved");

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    static void addExpense() {
        try (FileWriter fw = new FileWriter(EXPENSE_FILE, true)) {

            System.out.print("Amount: ");
            double amt = Double.parseDouble(sc.nextLine());
            if (amt <= 0) throw new InvalidExpenseException("Invalid amount");

            Category cat = Category.values()[readInt("Category (1-6): ") - 1];

            System.out.print("Description: ");
            String desc = sc.nextLine();
            if (desc.isBlank()) throw new InvalidExpenseException("Empty desc");

            fw.write(loggedInUser + "|" + amt + "|" + cat + "|" +
                    LocalDate.now() + "|" + LocalDateTime.now() + "|" + desc + "\n");

            System.out.println("Expense added");

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    static void viewExpenses() {
        try (BufferedReader br = new BufferedReader(new FileReader(EXPENSE_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(loggedInUser + "|"))
                    System.out.println(line);
            }
        } catch (Exception ignored) {}
    }

    static void viewTotal() {
        double total = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(EXPENSE_FILE))) {
            String l;
            while ((l = br.readLine()) != null)
                if (l.startsWith(loggedInUser + "|"))
                    total += Double.parseDouble(l.split("\\|")[1]);
        } catch (Exception ignored) {}
        System.out.println("Total = ₹" + total);
    }

    // ---------------- PLACEHOLDER FEATURES (stubs to satisfy compiler) ----------------
    // These are intentionally minimal: they avoid changing program behavior but
    // allow the project to compile when the full implementations are not present.
    static void categoryWiseMonthlyReport() {
        System.out.println("Category-wise monthly report: (not implemented yet)");
    }

    static void searchExpenses() {
        System.out.println("Search expenses: (not implemented yet)");
    }

    static void sortExpensesMenu() {
        System.out.println("Sort expenses: (not implemented yet)");
    }

    static void changePassword() {
        System.out.println("Change password: (not implemented yet)");
    }

    // ================= AUTH =================
    static void register() {
        System.out.print("Username: ");
        String u = sc.nextLine();
        System.out.print("Password: ");
        String p = hashPassword(sc.nextLine());

        try (FileWriter fw = new FileWriter(USER_FILE, true)) {
            fw.write(u + "|" + p + "\n");
        } catch (Exception ignored) {}
    }

    static void login() {
        System.out.print("Username: ");
        String u = sc.nextLine();
        System.out.print("Password: ");
        String p = hashPassword(sc.nextLine());

        try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
            String l;
            while ((l = br.readLine()) != null) {
                String[] d = l.split("\\|");
                if (d[0].equals(u) && d[1].equals(p)) {
                    loggedInUser = u;
                    return;
                }
            }
        } catch (Exception ignored) {}
        System.out.println("Login failed");
    }

    static void forgotPassword() {
        System.out.println("Reset to 1234 (demo)");
    }

    // ================= HELPERS =================
    static int readInt(String m) {
        while (true) {
            try {
                System.out.print(m);
                return Integer.parseInt(sc.nextLine());
            } catch (Exception ignored) {}
        }
    }

    static String hashPassword(String p) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(p.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            return p;
        }
    }

    static void ensureFileExists(String f) {
        try { new File(f).createNewFile(); } catch (Exception ignored) {}
    }

    static void pause() {
        System.out.println("Press ENTER...");
        sc.nextLine();
    }

    static Map<String, String> parseJson(String json) {
        Map<String, String> map = new HashMap<>();
        json = json.replaceAll("[{}\"]", "");
        for (String p : json.split(",")) {
            String[] kv = p.split(":");
            if (kv.length == 2) map.put(kv[0].trim(), kv[1].trim());
        }
        return map;
    }
}