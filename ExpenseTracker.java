import java.util.*;
import java.io.*;

public class ExpenseTracker {

    static ArrayList<Expense> expenses = new ArrayList<>();
    static Scanner sc = new Scanner(System.in);
    static final String FILE_NAME = "expenses.txt";

    // hello world

    public static void main(String[] args) {

        loadFromFile();   // 🔹 LOAD DATA AT START

        while (true) {
            System.out.println("\n===== EXPENSE TRACKER =====");
            System.out.println("1. Add Expense");
            System.out.println("2. View All Expenses");
            System.out.println("3. View Total Expense");
            System.out.println("4. Exit");

            int choice = readInt("Choose an option: ");

            switch (choice) {
                case 1:
                    addExpense();
                    saveToFile();   // 🔹 SAVE AFTER ADD
                    break;
                case 2:
                    viewExpenses();
                    break;
                case 3:
                    viewTotal();
                    break;
                case 4:
                    saveToFile();
                    System.out.println("Data saved. Goodbye!");
                    return;
                default:
                    System.out.println("Invalid choice!");
            }
        }
    }

    // Safe integer reader from the console
    static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine();
            try {
                return Integer.parseInt(line.trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }

    // Safe double reader from the console
    static double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine();
            try {
                return Double.parseDouble(line.trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    // ---------------- ADD EXPENSE ----------------
    static void addExpense() {
        double amount = readDouble("Enter amount: ");

        System.out.print("Enter category: ");
        String category = sc.nextLine();

        System.out.print("Enter description: ");
        String description = sc.nextLine();

        expenses.add(new Expense(amount, category, description));
        System.out.println("Expense added successfully!");
    }

    // ---------------- VIEW EXPENSES ----------------
    static void viewExpenses() {
        if (expenses.isEmpty()) {
            System.out.println("No expenses recorded.");
            return;
        }

        for (Expense e : expenses) {
            System.out.println("Amount: ₹" + e.amount +
                    " | Category: " + e.category +
                    " | Description: " + e.description);
        }
    }

    // ---------------- TOTAL ----------------
    static void viewTotal() {
        double total = 0;
        for (Expense e : expenses) {
            total += e.amount;
        }
        System.out.println("Total Expense: ₹" + total);
    }

    // ---------------- SAVE TO FILE ----------------
    static void saveToFile() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (Expense e : expenses) {
                pw.println(e.amount + "," + e.category + "," + e.description);
            }
        } catch (IOException e) {
            System.out.println("Error saving file.");
        }
    }

    // ---------------- LOAD FROM FILE ----------------
    static void loadFromFile() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return;

        try (Scanner fileScanner = new Scanner(file)) {
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine();
                // split into 3 parts so descriptions with commas are preserved
                String[] parts = line.split(",", 3);
                if (parts.length < 3) continue; // malformed line

                try {
                    double amount = Double.parseDouble(parts[0].trim());
                    String category = parts[1].trim();
                    String description = parts[2].trim();

                    expenses.add(new Expense(amount, category, description));
                } catch (NumberFormatException nfe) {
                    // skip malformed amount lines
                    continue;
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading file.");
        }
    }
}
