package ru.vspochernin.jerrgen.utils;

import java.util.Random;

public class CommonUtils {

    private static final Random RANDOM = new Random();

    private CommonUtils() {
    }

    public static void safeSleep(int millis) {
        try {
            Thread.sleep(RANDOM.nextInt(millis));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static int getNextInt() {
        return RANDOM.nextInt();
    }

    public static double getNextDouble() {
        return RANDOM.nextDouble();
    }
}
