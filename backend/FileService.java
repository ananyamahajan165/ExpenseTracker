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
}