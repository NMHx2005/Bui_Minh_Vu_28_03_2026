package com.restaurant;

import com.restaurant.presentation.RestaurantApp;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        try {
            new RestaurantApp(scanner).run();
        } finally {
            scanner.close();
        }
    }
}
