import ru.nsu.ccfit.factory.*;
import ru.nsu.ccfit.threadpool.ThreadPool;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Main {
    public static void main(String[] args) {
        Properties properties = new Properties();


        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                return;
            }
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        int storageBodySize = Integer.parseInt(properties.getProperty("StorageBodySize", "100"));
        int storageMotorSize = Integer.parseInt(properties.getProperty("StorageMotorSize", "100"));
        int storageAccessorySize = Integer.parseInt(properties.getProperty("StorageAccessorySize", "100"));
        int storageAutoSize = Integer.parseInt(properties.getProperty("StorageAutoSize", "100"));
        int accessorySuppliers = Integer.parseInt(properties.getProperty("AccessorySuppliers", "5"));
        int workers = Integer.parseInt(properties.getProperty("Workers", "10"));
        int dealers = Integer.parseInt(properties.getProperty("Dealers", "10"));
        boolean logSale = Boolean.parseBoolean(properties.getProperty("LogSale", "true"));

        Storage<Body> bodyStorage = new Storage<>(storageBodySize);
        Storage<Motor> motorStorage = new Storage<>(storageMotorSize);
        Storage<Accessory> accessoryStorage = new Storage<>(storageAccessorySize);
        Storage<Auto> autoStorage = new Storage<>(storageAutoSize);

        ThreadPool threadPool = new ThreadPool(workers);

        java.util.List<Thread> allThreads = new java.util.ArrayList<>();

        new FactoryController(bodyStorage, motorStorage, accessoryStorage, autoStorage, threadPool);

        Supplier<Body> bodySupplier = new Supplier<>(bodyStorage, Body.class, 1000);
        Thread bodyThread = new Thread(bodySupplier, "BodySupplier");
        allThreads.add(bodyThread);
        bodyThread.start();
        
        Supplier<Motor> motorSupplier = new Supplier<>(motorStorage, Motor.class, 1000);
        Thread motorThread = new Thread(motorSupplier, "MotorSupplier");
        allThreads.add(motorThread);
        motorThread.start();
        
        java.util.List<Supplier<Accessory>> accessorySuppliersList = new java.util.ArrayList<>();
        for (int i = 0; i < accessorySuppliers; i++) {
            Supplier<Accessory> s = new Supplier<>(accessoryStorage, Accessory.class, 1000);
            accessorySuppliersList.add(s);
            Thread t = new Thread(s, "AccessorySupplier-" + i);
            allThreads.add(t);
            t.start();
        }

        java.util.List<Dealer> dealersList = new java.util.ArrayList<>();
        for (int i = 0; i < dealers; i++) {
            Dealer d = new Dealer(autoStorage, i, 2000, logSale);
            dealersList.add(d);
            Thread t = new Thread(d, "Dealer-" + i);
            allThreads.add(t);
            t.start();
        }

        new FactoryGUI(bodyStorage, motorStorage, accessoryStorage, autoStorage, threadPool, 
                       bodySupplier, motorSupplier, accessorySuppliersList, dealersList, allThreads);
    }
}
