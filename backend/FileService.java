package backend;

import java.io.*;

public class FileService {

    public static void appendToFile(String filePath, String data) throws IOException {
        FileWriter fw = new FileWriter(filePath, true);
        fw.write(data + "\n");
        fw.close();
    }

    public static String searchExpensesByUser(String filePath, String username) throws IOException {

        BufferedReader br = new BufferedReader(new FileReader(filePath));
        StringBuilder result = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null) {

            String[] parts = line.split(",");

            if (parts.length > 0 && parts[0].equals(username)) {
                result.append(line).append("\n");
            }
        }

        br.close();
        return result.toString();
    }



    public static void saveUser(String filePath, String username, String password) throws IOException {
    FileWriter fw = new FileWriter(filePath, true);
    fw.write(username + "," + password + "\n");
    fw.close();
}



public static boolean validateUser(String filePath, String username, String password) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(filePath));
    String line;

    while ((line = br.readLine()) != null) {
        String[] parts = line.split(",");
        if (parts[0].equals(username) && parts[1].equals(password)) {
            br.close();
            return true;
        }
    }

    br.close();
    return false;
}

    
    public static java.util.List<String> getExpensesByUser(String filePath, String username) throws IOException {

    java.util.List<String> expenses = new java.util.ArrayList<>();

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



public static boolean validateUser(String filePath, String username, String hashedPassword) throws IOException {

    BufferedReader br = new BufferedReader(new FileReader(filePath));
    String line;

    while ((line = br.readLine()) != null) {

        String[] d = line.split("\\|");

        if (d.length >= 2 &&
            d[0].trim().equals(username) &&
            d[1].trim().equals(hashedPassword)) {

            br.close();
            return true;
        }
    }

    br.close();
    return false;
}
}