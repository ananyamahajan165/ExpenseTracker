package backend;

import java.io.BufferedReader;
import java.io.FileReader;

public class AuthDebug {
    public static void main(String[] args) throws Exception {
        String username = "codextestuser";
        String password = "secret123";
        String hashed = ExpenseTracker.hashPassword(password);

        System.out.println("hash=" + hashed);

        try (BufferedReader br = new BufferedReader(new FileReader(ExpenseTracker.USER_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split("\\|");
                if (data.length == 2) {
                    System.out.println(data[0] + " => " + data[1].equals(hashed));
                    if (data[0].equals(username) && data[1].equals(hashed)) {
                        System.out.println("MATCH");
                    }
                }
            }
        }
    }
}
