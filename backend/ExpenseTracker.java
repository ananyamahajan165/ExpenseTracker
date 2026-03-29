package backend;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;


public class ExpenseTracker {

    static Scanner sc = new Scanner(System.in);
    static String loggedInUser = null;
    static final Map<String, String> activeSessions = new HashMap<>();

    static final String USER_FILE = "data/users.txt";
    static final String EXPENSE_FILE = "data/expenses.txt";
    static final String BUDGET_FILE = "data/budget.txt";
    enum Category {
    FOOD,
    TRAVEL,
    RENT,
    SHOPPING,
    UTILITIES,
    OTHER
}




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




static void startServer() {

    
    try {
        ensureFileExists(USER_FILE);
        ensureFileExists(EXPENSE_FILE);
        ensureFileExists(BUDGET_FILE);

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "9080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // -------- HEALTH CHECK --------

        

server.createContext("/", exchange -> {

    String path = exchange.getRequestURI().getPath();

    if (path.equals("/")) {
        path = "/index.html";
    }

    File file = new File("frontend" + path);

    if (!file.exists()) {
        exchange.sendResponseHeaders(404, -1);
        return;
    }

    String contentType = "text/html";

    if (path.endsWith(".js")) {
        contentType = "application/javascript";
    } else if (path.endsWith(".css")) {
        contentType = "text/css";
    }

    byte[] response = Files.readAllBytes(file.toPath());

    exchange.getResponseHeaders().set("Content-Type", contentType);
    exchange.sendResponseHeaders(200, response.length);
    exchange.getResponseBody().write(response);
    exchange.close();
});



server.createContext("/set-budget", exchange -> {

    enableCors(exchange);

    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
        sendJson(exchange, 405,
            "{\"status\":\"error\",\"message\":\"Method Not Allowed\"}");
        return;
    }

    String username = getAuthenticatedUser(exchange);
    if (username == null) {
        sendJson(exchange, 401,
            "{\"status\":\"error\",\"message\":\"Please login first\"}");
        return;
    }

    String body = new String(exchange.getRequestBody().readAllBytes()).trim();

    if (body.isEmpty()) {
        sendJson(exchange, 400,
            "{\"status\":\"error\",\"message\":\"Budget is required\"}");
        return;
    }

    try {
        String[] parts = body.split("\\|");
        double budgetValue;
        String month;

        if (parts.length >= 2) {
            month = normalizeMonth(parts[0]);
            budgetValue = Double.parseDouble(parts[1].trim());
        } else {
            month = YearMonth.now().toString();
            budgetValue = Double.parseDouble(body);
        }

        upsertBudget(username, month, budgetValue);
    } catch (NumberFormatException e) {
        sendJson(exchange, 400,
            "{\"status\":\"error\",\"message\":\"Budget must be a number\"}");
        return;
    } catch (IllegalArgumentException e) {
        sendJson(exchange, 400,
            "{\"status\":\"error\",\"message\":\"Invalid month\"}");
        return;
    } catch (IOException e) {
        sendJson(exchange, 500,
            "{\"status\":\"error\",\"message\":\"Failed to save budget\"}");
        return;
    }

    sendJson(exchange, 200,
        "{\"status\":\"success\",\"message\":\"Budget updated\"}");

});




server.createContext("/summary", exchange -> {

    enableCors(exchange);

    if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
        sendJson(exchange, 405,
            "{\"status\":\"error\",\"message\":\"Method Not Allowed\"}");
        return;
    }

    String username = getAuthenticatedUser(exchange);
    if (username == null) {
        sendJson(exchange, 401,
            "{\"status\":\"error\",\"message\":\"Please login first\"}");
        return;
    }

    String monthFilter;
    try {
        monthFilter = getRequestedMonth(exchange);
    } catch (IllegalArgumentException e) {
        sendJson(exchange, 400,
            "{\"status\":\"error\",\"message\":\"Invalid month\"}");
        return;
    }

    try {

        Map<String, Double> categoryTotals = new HashMap<>();
        double total = 0;

        // -------- READ EXPENSES --------
        try (BufferedReader br = new BufferedReader(new FileReader(EXPENSE_FILE))) {

            String line;

            while ((line = br.readLine()) != null) {

                String[] parts = line.split("\\|");

                if (parts.length < 6 || !parts[0].equals(username)) continue;
                if (!parts[3].startsWith(monthFilter)) continue;

                try {

                    double amount = Double.parseDouble(parts[1]);
                    String category = parts[2];

                    total += amount;

                    categoryTotals.put(
                        category,
                        categoryTotals.getOrDefault(category, 0.0) + amount
                    );

                } catch (NumberFormatException e) {
                    continue;
                }
            }
        }

        // -------- READ BUDGET --------
        double budget = getBudgetForUser(username, monthFilter);

        // -------- BUILD JSON RESPONSE --------
        StringBuilder json = new StringBuilder();

        json.append("{");
        json.append("\"total\":").append(total).append(",");
        json.append("\"budget\":").append(budget).append(",");
        json.append("\"byCategory\":{");

        boolean first = true;

        for (String cat : categoryTotals.keySet()) {

            if (!first) json.append(",");
            first = false;

            json.append("\"").append(cat).append("\":")
                .append(categoryTotals.get(cat));
        }

        json.append("}}");

        sendJson(exchange, 200, json.toString());

    } catch (Exception e) {
        e.printStackTrace();
        sendJson(exchange, 500,
            "{\"status\":\"error\",\"message\":\"Internal Server Error\"}");
    }
});



server.createContext("/register", exchange -> {

    enableCors(exchange);

    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
        sendJson(exchange, 405,
            "{\"status\":\"error\",\"message\":\"Method Not Allowed\"}");
        return;
    }

    try {

        String body = new String(exchange.getRequestBody().readAllBytes());
        String[] parts = body.split("\\|");

        if (parts.length < 2) {
            sendJson(exchange, 400,
                "{\"status\":\"error\",\"message\":\"Invalid data\"}");
            return;
        }

        String username = parts[0].trim().toLowerCase();
        String password = parts[1].trim();

        if (username.isBlank() || password.isBlank()) {
            sendJson(exchange, 400,
                "{\"status\":\"error\",\"message\":\"Username and password are required\"}");
            return;
        }

        if (userExists(username)) {
            sendJson(exchange, 400,
                "{\"status\":\"error\",\"message\":\"User already exists\"}");
            return;
        }

        String hashed = hashPassword(password);

        try (FileWriter fw = new FileWriter(USER_FILE, true)) {
            fw.write(username + "|" + hashed + "\n");
        }

        String token = createSession(username);
        sendJson(exchange, 200,
            "{\"status\":\"success\",\"message\":\"Registration successful\",\"username\":\""
            + escapeJson(username) + "\",\"token\":\"" + token + "\"}");

    } catch (Exception e) {
        e.printStackTrace();
        sendJson(exchange, 500,
            "{\"status\":\"error\",\"message\":\"Registration failed\"}");
    }
});




server.createContext("/login", exchange -> {

    enableCors(exchange);

    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
        sendJson(exchange, 405,
            "{\"status\":\"error\",\"message\":\"Method Not Allowed\"}");
        return;
    }

    try {

        String body = new String(exchange.getRequestBody().readAllBytes());
        String[] parts = body.split("\\|");

        if (parts.length < 2) {
            sendJson(exchange, 400,
                "{\"status\":\"error\",\"message\":\"Invalid data\"}");
            return;
        }

        String username = parts[0].trim().toLowerCase();
        String password = parts[1].trim();
        String hashed = hashPassword(password);

        boolean valid = false;

        try (BufferedReader br = new BufferedReader(new FileReader(USER_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split("\\|");
                if (data.length == 2 &&
                    data[0].equals(username) &&
                    data[1].equals(hashed)) {
                    valid = true;
                    break;
                }
            }
        }

        if (!valid) {
    sendJson(exchange, 401,
        "{\"status\":\"error\",\"message\":\"Invalid credentials\"}");
    return;
}

        String token = createSession(username);
        sendJson(exchange, 200,
    "{\"status\":\"success\",\"message\":\"Login successful\",\"username\":\""
    + escapeJson(username) + "\",\"token\":\"" + token + "\"}");

    } catch (Exception e) {
        e.printStackTrace();
        sendJson(exchange, 500,
            "{\"status\":\"error\",\"message\":\"Login failed\"}");
    }
});





server.createContext("/expenses", exchange -> {

    enableCors(exchange);

    if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
        sendJson(exchange, 405,
            "{\"status\":\"error\",\"message\":\"Method Not Allowed\"}");
        return;
    }

    String username = getAuthenticatedUser(exchange);
    if (username == null) {
        sendJson(exchange, 401,
            "{\"status\":\"error\",\"message\":\"Please login first\"}");
        return;
    }

    String monthFilter;
    try {
        monthFilter = getRequestedMonth(exchange);
    } catch (IllegalArgumentException e) {
        sendJson(exchange, 400,
            "{\"status\":\"error\",\"message\":\"Invalid month\"}");
        return;
    }

    try {
        StringBuilder json = new StringBuilder();
        json.append("[");

        boolean first = true;

        try (BufferedReader br = new BufferedReader(new FileReader(EXPENSE_FILE))) {
            String line;

while ((line = br.readLine()) != null) {

    String[] parts = line.split("\\|");

    if (parts.length >= 6) {
        if (!parts[0].equals(username)) {
            continue;
        }

        if (!parts[3].startsWith(monthFilter)) {
            continue;
        }

        if (!first) json.append(",");
        first = false;

        json.append("{")
            .append("\"amount\":").append(parts[1]).append(",")
            .append("\"category\":\"").append(escapeJson(parts[2])).append("\",")
            .append("\"date\":\"").append(escapeJson(parts[3])).append("\",")
            .append("\"timestamp\":\"").append(escapeJson(parts[4])).append("\",")
            .append("\"description\":\"").append(escapeJson(parts[5])).append("\"")
            .append("}");
    }
}
        }

        json.append("]");

        sendJson(exchange, 200, json.toString());

    } catch (Exception e) {
        e.printStackTrace();
        sendJson(exchange, 500,
            "{\"status\":\"error\",\"message\":\"Failed to load expenses\"}");
    }
});




server.createContext("/deleteExpense", exchange -> {

    enableCors(exchange);

    if (!exchange.getRequestMethod().equalsIgnoreCase("DELETE")) {
        sendJson(exchange, 405,
            "{\"status\":\"error\",\"message\":\"Method Not Allowed\"}");
        return;
    }

    String username = getAuthenticatedUser(exchange);
    if (username == null) {
        sendJson(exchange, 401,
            "{\"status\":\"error\",\"message\":\"Please login first\"}");
        return;
    }

    try {
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");

        if (parts.length < 3) {
            sendJson(exchange, 400,
                "{\"status\":\"error\",\"message\":\"Invalid ID\"}");
            return;
        }

        String timestampToDelete = parts[2];

        File inputFile = new File(EXPENSE_FILE);
        List<String> updatedLines = new ArrayList<>();

        BufferedReader br = new BufferedReader(new FileReader(inputFile));
        String line;

        while ((line = br.readLine()) != null) {

            String[] data = line.split("\\|");

            if (data.length >= 6) {
                if (!data[0].equals(username)) {
                    updatedLines.add(line);
                    continue;
                }

                String timestamp = data[4];

                if (!timestamp.equals(timestampToDelete)) {
                    updatedLines.add(line);
                }
            }
        }

        br.close();

        FileWriter fw = new FileWriter(inputFile);

        for (String l : updatedLines) {
            fw.write(l + "\n");
        }

        fw.close();

        sendJson(exchange, 200,
            "{\"status\":\"success\",\"message\":\"Expense deleted\"}");

    } catch (Exception e) {
        e.printStackTrace();
        sendJson(exchange, 500,
            "{\"status\":\"error\",\"message\":\"Delete failed\"}");
    }
});









server.createContext("/add-expense", exchange -> {

    enableCors(exchange);

    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
        sendJson(exchange, 405,
            "{\"status\":\"error\",\"message\":\"Method Not Allowed\"}");
        return;
    }

    String username = getAuthenticatedUser(exchange);
    if (username == null) {
        sendJson(exchange, 401,
            "{\"status\":\"error\",\"message\":\"Please login first\"}");
        return;
    }

    try {

        String body = new String(exchange.getRequestBody().readAllBytes());

        String[] parts = body.split("\\|");

        if (parts.length < 3) {
            sendJson(exchange, 400,
                "{\"status\":\"error\",\"message\":\"Invalid data\"}");
            return;
        }

        double amount = Double.parseDouble(parts[0]);
        String category = parts[1];
        String description = parts[2];
        LocalDate date = parts.length >= 4 && !parts[3].trim().isEmpty()
            ? LocalDate.parse(parts[3].trim())
            : LocalDate.now();
        LocalDateTime timestamp = LocalDateTime.now();

        String record =
                username + "|" +
                amount + "|" +
                category + "|" +
                date + "|" +
                timestamp + "|" +
                description + "\n";

        try (FileWriter fw = new FileWriter(EXPENSE_FILE, true)) {
            fw.write(record);
        }

        sendJson(exchange, 200,
            "{\"status\":\"success\",\"message\":\"Expense added successfully\"}");

    } catch (Exception e) {
        e.printStackTrace();
        sendJson(exchange, 500,
            "{\"status\":\"error\",\"message\":\"Failed to add expense\"}");
    }
});





server.createContext("/logout", exchange -> {

    enableCors(exchange);

    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
        sendJson(exchange, 405,
            "{\"status\":\"error\",\"message\":\"Method Not Allowed\"}");
        return;
    }

    String token = getAuthToken(exchange);
    if (token != null) {
        activeSessions.remove(token);
    }

    sendJson(exchange, 200,
        "{\"status\":\"success\",\"message\":\"Logged out successfully\"}");
});


server.setExecutor(null);
server.start();
        System.out.println("🚀 Server started at http://localhost:9080");

    } catch (Exception e) {
        e.printStackTrace();
        System.out.println("❌ Failed to start server");
        e.printStackTrace();
    }
    
}

static void enableCors(HttpExchange exchange) {
    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
    exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
}



static void saveExpense(double amount, String category, String description) {

    String data = amount + "|" + category + "|" + description;

    try {
        FileService.appendToFile(EXPENSE_FILE, data);
    } catch (IOException e) {
        e.printStackTrace();
    }
}


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
    startServer();   // start HTTP server
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
        System.out.println("5. View Monthly Expenses");
        System.out.println("6. Category-wise Monthly Report");
        System.out.println("7. Search Expenses");
        System.out.println("8. Sort Expenses");
        System.out.println("9. Change Password");
        System.out.println("10. Logout");

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
                viewExpenses();
                    break;

            case 6:
                categoryWiseMonthlyReport();
                break;

            case 7:
                searchExpenses();
                break;

            case 8:
                sortExpensesMenu();
                break;

            case 9:
                changePassword();
                break;

            case 10:
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

        try {
    boolean valid = FileService.validateUser(USER_FILE, u, hashed);

    if (valid) {
        loggedInUser = u;
        System.out.println("✅ Login successful");
        return;
    }

} catch (IOException e) {
    e.printStackTrace();
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
    try {
        System.out.print("Enter monthly budget: ");
        double budget = Double.parseDouble(sc.nextLine());

        if (budget <= 0) {
            throw new InvalidInputException("Budget must be greater than 0");
        }

        try (FileWriter fw = new FileWriter(BUDGET_FILE)) {
            fw.write(String.valueOf(budget));
            System.out.println("✅ Budget saved");
        }

    } catch (InvalidInputException e) {
        System.out.println("❌ " + e.getMessage());
    } catch (Exception e) {
        System.out.println("❌ Invalid budget input");
    }
}




    // ================= ADD EXPENSE =================
    static void addExpense() {
    try (FileWriter fw = new FileWriter(EXPENSE_FILE, true)) {

        System.out.print("Amount: ");
        double amt = Double.parseDouble(sc.nextLine());

        if (amt <= 0) {
            throw new InvalidExpenseException("Amount must be greater than 0");
        }

        Category cat = null;

while (cat == null) {
    System.out.println("Choose Category:");
    Category[] categories = Category.values();

    for (int i = 0; i < categories.length; i++) {
        System.out.println((i + 1) + ". " + categories[i]);
    }

    int choice = readInt("Enter category number: ");

    if (choice >= 1 && choice <= categories.length) {
        cat = categories[choice - 1];
    } else {
        System.out.println("❌ Invalid choice. Try again.");
    }

        }

        System.out.print("Description: ");
        String desc = sc.nextLine();

        if (desc.trim().isEmpty()) {
            throw new InvalidExpenseException("Description cannot be empty");
        }

        LocalDate date = LocalDate.now();
        LocalDateTime timestamp = LocalDateTime.now();

        fw.write(
                loggedInUser + "|" +
                amt + "|" +
                cat.name() + "|" +
                date + "|" +
                timestamp + "|" +
                desc + "\n"
        );

        System.out.println("✅ Expense added successfully");
        checkBudgetAlert();

    } catch (InvalidExpenseException e) {
        System.out.println("❌ " + e.getMessage());
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
    System.out.println("\n--- All Expenses ---");
    boolean found = false;

    try (BufferedReader br = new BufferedReader(new FileReader(EXPENSE_FILE))) {
        String line;
        while ((line = br.readLine()) != null) {
            String[] d = line.split("\\|");

            if (d.length >= 6 && d[0].equals(loggedInUser)) {
                System.out.println(
                        "₹" + d[1] +
                        " | " + d[2] +
                        " | Date: " + d[3] +
                        " | Time: " + d[4] +
                        " | " + d[5]
                );
                found = true;
            }
        }
    } catch (Exception e) {
        System.out.println("❌ Error reading expenses");
        return;
    }

    if (!found) {
        System.out.println("No expenses found.");
    }

    System.out.println("----------------------");
}





    static void categoryWiseMonthlyReport() {
    System.out.print("Enter month (YYYY-MM): ");
    String monthInput = sc.nextLine();

    // Initialize totals for all categories
    Map<Category, Double> categoryTotals = new EnumMap<>(Category.class);
    for (Category c : Category.values()) {
        categoryTotals.put(c, 0.0);
    }

    boolean found = false;
    double grandTotal = 0;

    try (BufferedReader br = new BufferedReader(new FileReader(EXPENSE_FILE))) {
        String line;
        while ((line = br.readLine()) != null) {
            String[] d = line.split("\\|");

            // username | amount | category | date | timestamp | description
            if (d.length >= 6 && d[0].equals(loggedInUser)) {
                LocalDate expenseDate = LocalDate.parse(d[3]);
                String expenseMonth = expenseDate.getYear() + "-" +
                        String.format("%02d", expenseDate.getMonthValue());

                if (expenseMonth.equals(monthInput)) {
                    Category cat = Category.valueOf(d[2]);
                    double amt = Double.parseDouble(d[1]);

                    categoryTotals.put(cat, categoryTotals.get(cat) + amt);
                    grandTotal += amt;
                    found = true;
                }
            }
        }
    } catch (Exception e) {
        System.out.println("❌ Error generating report");
        return;
    }

    if (!found) {
        System.out.println("No expenses found for " + monthInput);
        return;
    }

    System.out.println("\n--- Category-wise Report (" + monthInput + ") ---");
    for (Category c : Category.values()) {
        System.out.println(c + " : ₹" + categoryTotals.get(c));
    }
    System.out.println("--------------------------------");
    System.out.println("TOTAL : ₹" + grandTotal);
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
                if (d.length >= 6 && d[0].equals(loggedInUser)) {
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

    static String createSession(String username) {
        String token = UUID.randomUUID().toString();
        activeSessions.put(token, username);
        return token;
    }

    static String getAuthToken(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null) {
            return null;
        }

        if (header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }

        return header.trim();
    }

    static String getAuthenticatedUser(HttpExchange exchange) {
        String token = getAuthToken(exchange);
        if (token == null || token.isEmpty()) {
            return null;
        }
        return activeSessions.get(token);
    }

    static double getBudgetForUser(String username, String month) {
        try (BufferedReader br = new BufferedReader(new FileReader(BUDGET_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 3 &&
                    parts[0].equals(username) &&
                    parts[1].equals(month)) {
                    return Double.parseDouble(parts[2]);
                }
            }
        } catch (Exception e) {
            return 0;
        }

        return 0;
    }

    static void upsertBudget(String username, String month, double budgetValue) throws IOException {
        List<String> budgets = new ArrayList<>();
        boolean updated = false;

        try (BufferedReader br = new BufferedReader(new FileReader(BUDGET_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                String[] parts = line.split("\\|");
                if (parts.length == 3 &&
                    parts[0].equals(username) &&
                    parts[1].equals(month)) {
                    budgets.add(username + "|" + month + "|" + budgetValue);
                    updated = true;
                } else {
                    budgets.add(line);
                }
            }
        } catch (FileNotFoundException e) {
            // File is created later when writing.
        }

        if (!updated) {
            budgets.add(username + "|" + month + "|" + budgetValue);
        }

        try (FileWriter fw = new FileWriter(BUDGET_FILE)) {
            for (String line : budgets) {
                fw.write(line + "\n");
            }
        }
    }

    static String escapeJson(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }

    static String getRequestedMonth(HttpExchange exchange) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null || query.isBlank()) {
            return YearMonth.now().toString();
        }

        for (String part : query.split("&")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2 && pair[0].equals("month")) {
                return normalizeMonth(pair[1]);
            }
        }

        return YearMonth.now().toString();
    }

    static String normalizeMonth(String month) {
        return YearMonth.parse(month.trim()).toString();
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
    System.out.print(msg);
    return sc.nextLine().trim();   // 🔥 TRIM IS IMPORTANT
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




static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, json.getBytes().length);
    exchange.getResponseBody().write(json.getBytes());
    exchange.close();
}





    static void pause() {
        System.out.println("\nPress ENTER to continue...");
        sc.nextLine();
    }




    static void searchExpenses() {
    System.out.print("Enter keyword (category/description): ");
    String keyword = sc.nextLine().toLowerCase();

    boolean found = false;

    try (BufferedReader br = new BufferedReader(new FileReader(EXPENSE_FILE))) {
        String line;
        while ((line = br.readLine()) != null) {
            String[] d = line.split("\\|");

            // username | amount | category | date | timestamp | description
            if (d.length >= 6 && d[0].equals(loggedInUser)) {
                String category = d[2].toLowerCase();
                String desc = d[5].toLowerCase();

                if (category.contains(keyword) || desc.contains(keyword)) {
                    System.out.println(
                        "₹" + d[1] +
                        " | " + d[2] +
                        " | " + d[3] +
                        " | " + d[5]
                    );
                    found = true;
                }
            }
        }
    } catch (Exception e) {
        System.out.println("❌ Error searching expenses");
        return;
    }

    if (!found) {
        System.out.println("No matching expenses found.");
    }
}



    static void sortExpensesMenu() {
    System.out.println("Sort By:");
    System.out.println("1. Amount (High → Low)");
    System.out.println("2. Date (Newest → Oldest)");

    int choice = readInt("Choose option: ");

    switch (choice) {
        case 1:
            sortByAmount();
            break;
        case 2:
            sortByDate();
            break;
        default:
            System.out.println("Invalid option");
    }
}




static List<Expense> loadUserExpenses() {
    List<Expense> list = new ArrayList<>();

    try (BufferedReader br = new BufferedReader(new FileReader(EXPENSE_FILE))) {
        String line;
        while ((line = br.readLine()) != null) {
            String[] d = line.split("\\|");
            if (d.length >= 6 && d[0].equals(loggedInUser)) {
                list.add(new Expense(
                        Double.parseDouble(d[1]),
                        d[2],
                        LocalDate.parse(d[3]),
                        d[5]
                ));
            }
        }
    } catch (Exception e) {
        System.out.println("❌ Error loading expenses");
    }
    return list;
}



static void sortByAmount() {
    List<Expense> list = loadUserExpenses();

    list.sort((a, b) -> Double.compare(b.amount, a.amount));

    for (Expense e : list) {
        System.out.println("₹" + e.amount + " | " + e.category + " | " + e.date + " | " + e.description);
    }
}




static void sortByDate() {
    List<Expense> list = loadUserExpenses();

    list.sort((a, b) -> b.date.compareTo(a.date));

    for (Expense e : list) {
        System.out.println("₹" + e.amount + " | " + e.category + " | " + e.date + " | " + e.description);
    }

    }




    static Map<String, String> parseJson(String json) {
    Map<String, String> map = new HashMap<>();

    if (json == null || json.trim().isEmpty()) {
        return map;
    }

    json = json.trim().replaceAll("[{}\"]", "");
    String[] pairs = json.split(",");

    for (String pair : pairs) {
        String[] kv = pair.split(":");
        if (kv.length == 2) {
            map.put(kv[0].trim(), kv[1].trim());
        }
    }

    return map;
}
}
