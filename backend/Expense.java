package backend;

import java.time.LocalDate;

public class Expense {

    private String username;
    private double amount;
    private String category;
    private LocalDate date;

    public Expense(String username, double amount, String category, LocalDate date) {
        this.username = username;
        this.amount = amount;
        this.category = category;
        this.date = date;
    }

    public String getUsername() {
        return username;
    }

    public double getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public LocalDate getDate() {
        return date;
    }
}