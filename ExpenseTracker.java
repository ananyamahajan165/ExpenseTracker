import java.util.*;
import java.io.*;

public class ExpenseTracker {

    static ArrayList<Expense> expenses = new ArrayList<>();
    static Scanner sc = new Scanner(System.in);
    static final String FILE = "expenses.txt";
    static int counter = 1;

    public static void main(String[] args) {

        loadFromFile();

        while (true) {
            System.out.println("\n===== EXPENSE TRACKER =====");
            System.out.println("1. Add Expense");
            System.out.println("2. View All Expenses");
            System.out.println("3. View Total Expense");
            System.out.println("4. Delete Expense by ID");
            System.out.println("5. Category-wise Total");
            System.out.println("6. Exit");

            System.out.print("Choose option: ");
            int choice = sc.nextInt();

            switch (choice) {
                case 1: addExpense(); saveToFile(); break;
                case 2: viewExpenses(); break;
                case 3: viewTotal(); break;
                case 4: deleteExpense(); saveToFile(); break;
                case 5: categoryTotal(); break;
                case 6: System.out.println("Goodbye!"); return;
                default: System.out.println("Invalid option!");
            }
        }
    }

    // ---------------- ADD ----------------
    static void addExpense() {
        System.out.print("Amount: ");
        double amount = sc.nextDouble();
        sc.nextLine();

        System.out.print("Category: ");
        String category = sc.nextLine();

        System.out.print("Description: ");
        String desc = sc.nextLine();

        System.out.print("Date (YYYY-MM-DD): ");
        String date = sc.nextLine();

        expenses.add(new Expense(counter++, amount, category, desc, date));
        System.out.println("Expense added!");
    }

    // ---------------- VIEW ----------------
    static void viewExpenses() {
        if (expenses.isEmpty()) {
            System.out.println("No expenses.");
            return;
        }

        for (Expense e : expenses) {
            System.out.println(
                "ID: " + e.id +
                " | ₹" + e.amount +
                " | " + e.category +
                " | " + e.description +
                " | " + e.date
            );
        }
    }

    // ---------------- TOTAL ----------------
    static void viewTotal() {
        double total = 0;
        for (Expense e : expenses) total += e.amount;
        System.out.println("Total Expense: ₹" + total);
    }

    // ---------------- DELETE ----------------
    static void deleteExpense() {
        System.out.print("Enter ID to delete: ");
        int id = sc.nextInt();

        boolean removed = expenses.removeIf(e -> e.id == id);
        if (removed) System.out.println("Expense deleted!");
        else System.out.println("ID not found!");
    }

    // ---------------- CATEGORY TOTAL ----------------
    static void categoryTotal() {
        sc.nextLine();
        System.out.print("Enter category: ");
        String cat = sc.nextLine();

        double total = 0;
        for (Expense e : expenses) {
            if (e.category.equalsIgnoreCase(cat)) {
                total += e.amount;
            }
        }

        System.out.println("Total for " + cat + ": ₹" + total);
    }

    // ---------------- SAVE ----------------
    static void saveToFile() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE))) {
            for (Expense e : expenses) {
                pw.println(e.id + "," + e.amount + "," + e.category + "," +
                           e.description + "," + e.date);
            }
        } catch (Exception e) {
            System.out.println("Error saving file");
        }
    }

    // ---------------- LOAD ----------------
    static void loadFromFile() {
        File f = new File(FILE);
        if (!f.exists()) return;

        try (Scanner fs = new Scanner(f)) {
            while (fs.hasNextLine()) {
                String[] p = fs.nextLine().split(",");
                int id = Integer.parseInt(p[0]);
                double amt = Double.parseDouble(p[1]);

                expenses.add(new Expense(id, amt, p[2], p[3], p[4]));
                counter = Math.max(counter, id + 1);
            }
        } catch (Exception e) {
            System.out.println("Error loading file");
        }
    }
}
