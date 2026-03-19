package backend;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileService {

    public static void appendToFile(String filePath, String data) throws IOException {
        FileWriter fw = new FileWriter(filePath, true);
        fw.write(data + "\n");
        fw.close();
    }

    // -------- SAVE USER --------
    public static void saveUser(String filePath, String username, String password) throws IOException {
        FileWriter fw = new FileWriter(filePath, true);
        fw.write(username + "|" + password + "\n");   // ✅ use |
        fw.close();
    }

    // -------- VALIDATE USER --------
    public static boolean validateUser(String filePath, String username, String password) throws IOException {

        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String line;

        while ((line = br.readLine()) != null) {

            String[] parts = line.split("\\|");

            if (parts.length >= 2 &&
                parts[0].equals(username) &&
                parts[1].equals(password)) {

                br.close();
                return true;
            }
        }

        br.close();
        return false;
    }

    // -------- GET USER EXPENSES --------
    public static List<String> getExpensesByUser(String filePath, String username) throws IOException {

        List<String> expenses = new ArrayList<>();

        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String line;

        while ((line = br.readLine()) != null) {

            String[] parts = line.split("\\|");

            if (parts.length > 0 && parts[0].equals(username)) {
                expenses.add(line);
            }
        }

        br.close();
        return expenses;
    }
}