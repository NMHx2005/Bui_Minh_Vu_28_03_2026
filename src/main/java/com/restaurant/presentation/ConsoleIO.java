package com.restaurant.presentation;

import java.math.BigDecimal;
import java.util.Scanner;

public class ConsoleIO {

    private final Scanner scanner;

    public ConsoleIO(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    public long readLong(String prompt) {
        while (true) {
            String s = readLine(prompt).trim();
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                System.out.println("Vui lòng nhập số nguyên hợp lệ.");
            }
        }
    }

    public int readIntInRange(String prompt, int min, int max) {
        while (true) {
            String s = readLine(prompt).trim();
            try {
                int v = Integer.parseInt(s);
                if (v >= min && v <= max) {
                    return v;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Chọn số từ " + min + " đến " + max + ".");
        }
    }

    public BigDecimal readPositiveBigDecimal(String prompt) {
        while (true) {
            String s = readLine(prompt).trim().replace(',', '.');
            try {
                BigDecimal b = new BigDecimal(s);
                if (b.compareTo(BigDecimal.ZERO) > 0) {
                    return b;
                }
                System.out.println("Giá phải lớn hơn 0.");
            } catch (NumberFormatException e) {
                System.out.println("Giá không hợp lệ (nhập số, ví dụ 50000 hoặc 50000.5).");
            }
        }
    }

    public int readNonNegativeInt(String prompt) {
        while (true) {
            String s = readLine(prompt).trim();
            try {
                int v = Integer.parseInt(s);
                if (v >= 0) {
                    return v;
                }
                System.out.println("Số phải >= 0.");
            } catch (NumberFormatException e) {
                System.out.println("Vui lòng nhập số nguyên.");
            }
        }
    }

    public boolean readYesNo(String prompt) {
        while (true) {
            String s = readLine(prompt).trim().toLowerCase();
            if (s.equals("y") || s.equals("c") || s.equals("có") || s.equals("co")) {
                return true;
            }
            if (s.equals("n") || s.equals("k") || s.equals("không") || s.equals("khong")) {
                return false;
            }
            System.out.println("Nhập Y/C (có) hoặc N/K (không).");
        }
    }
}
