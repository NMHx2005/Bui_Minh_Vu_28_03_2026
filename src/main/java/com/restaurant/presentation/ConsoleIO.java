package com.restaurant.presentation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/**
 * Đọc nhập console: luôn thông báo tiếng Việt rõ ràng, không để crash khi nhập sai kiểu.
 */
public class ConsoleIO {

    private final Scanner scanner;

    public ConsoleIO(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    /**
     * Chuỗi không rỗng sau trim (lặp lại cho đến khi hợp lệ).
     */
    public String readNonBlankLine(String prompt) {
        while (true) {
            String s = readLine(prompt).trim();
            if (!s.isEmpty()) {
                return s;
            }
            System.out.println("Không được để trống hoặc chỉ nhập khoảng trắng.");
        }
    }

    public long readLong(String prompt) {
        while (true) {
            String s = readLine(prompt).trim();
            if (s.isEmpty()) {
                System.out.println("Không được để trống. Vui lòng nhập số nguyên.");
                continue;
            }
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                System.out.println("Không phải số nguyên hợp lệ. Ví dụ: 1, 42, 100.");
            }
        }
    }

    public int readIntInRange(String prompt, int min, int max) {
        while (true) {
            String s = readLine(prompt).trim();
            if (s.isEmpty()) {
                System.out.println("Không được để trống. Nhập một số trong khoảng " + min + " – " + max + ".");
                continue;
            }
            try {
                int v = Integer.parseInt(s);
                if (v >= min && v <= max) {
                    return v;
                }
                System.out.println("Số phải từ " + min + " đến " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("Không phải số nguyên. Nhập lại (chỉ chữ số, không gõ chữ).");
            }
        }
    }

    public BigDecimal readPositiveBigDecimal(String prompt) {
        while (true) {
            String s = readLine(prompt).trim();
            if (s.isEmpty()) {
                System.out.println("Giá không được để trống.");
                continue;
            }
            s = s.replace(',', '.');
            try {
                BigDecimal b = new BigDecimal(s);
                if (b.compareTo(BigDecimal.ZERO) <= 0) {
                    System.out.println("Giá phải lớn hơn 0.");
                    continue;
                }
                return b;
            } catch (NumberFormatException e) {
                System.out.println("Giá không hợp lệ. Chỉ nhập số (ví dụ 50000 hoặc 35000.5).");
            }
        }
    }

    public int readNonNegativeInt(String prompt) {
        while (true) {
            String s = readLine(prompt).trim();
            if (s.isEmpty()) {
                System.out.println("Không được để trống.");
                continue;
            }
            try {
                int v = Integer.parseInt(s);
                if (v >= 0) {
                    return v;
                }
                System.out.println("Số phải >= 0.");
            } catch (NumberFormatException e) {
                System.out.println("Vui lòng nhập số nguyên (0, 1, 2, …).");
            }
        }
    }

    /** Ngày ISO: yyyy-MM-dd (dùng báo cáo / thống kê). */
    public LocalDate readLocalDateIso(String prompt) {
        DateTimeFormatter f = DateTimeFormatter.ISO_LOCAL_DATE;
        while (true) {
            String s = readNonBlankLine(prompt + " (yyyy-MM-dd): ").trim();
            try {
                return LocalDate.parse(s, f);
            } catch (DateTimeParseException e) {
                System.out.println("Không đúng định dạng. Ví dụ: 2026-03-01");
            }
        }
    }

    public boolean readYesNo(String prompt) {
        while (true) {
            String s = readLine(prompt).trim().toLowerCase();
            if (s.isEmpty()) {
                System.out.println("Vui lòng trả lời Y (có) hoặc N (không).");
                continue;
            }
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
