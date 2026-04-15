package ru.nsu.ccfit.factory;


import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Dealer implements Runnable {
    private final Storage<Auto> autoStorage;
    private int                 delay;
    private final int           id;
    private final boolean       logEnabled;
    private static PrintWriter  fileLogger;

    static {
        try {
            fileLogger = new PrintWriter(new FileWriter("factory_log.txt", true));
        } catch (IOException e) {
            System.err.println("Could not create log file: " + e.getMessage());
        }
    }

    public Dealer(Storage<Auto> autoStorage, int id, int delay, boolean logEnabled) {
        this.autoStorage = autoStorage;
        this.id = id;
        this.delay = delay;
        this.logEnabled = logEnabled;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Auto auto = autoStorage.get();
                if (logEnabled) {
                    logPurchase(auto);
                }
                Thread.sleep(delay);
            }
        } catch (InterruptedException e) {
        }
    }

    private void logPurchase(Auto auto) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
        String logMessage = String.format("%s: Dealer %d: Auto %d (Body: %d, Motor: %d, Accessory: %d)",
                time, id, auto.getId(),
                auto.getBody().getId(),
                auto.getMotor().getId(),
                auto.getAccessory().getId());

        System.out.println(logMessage);
        synchronized (Dealer.class) {
            if (fileLogger != null) {
                fileLogger.println(logMessage);
                fileLogger.flush();
            }
        }
    }

    public static void closeLogger() {
        if (fileLogger != null) {
            fileLogger.close();
        }
    }
}
