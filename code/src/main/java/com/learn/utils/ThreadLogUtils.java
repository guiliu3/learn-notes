package com.learn.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ThreadLogUtils {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private ThreadLogUtils() {
    }

    public static void print(String message) {
        Thread t = Thread.currentThread();

        System.out.printf("[%s][%s][%s] %s%n",
                LocalDateTime.now().format(FORMATTER),
                t.getName(),
                t.getState(),
                message);
    }
}