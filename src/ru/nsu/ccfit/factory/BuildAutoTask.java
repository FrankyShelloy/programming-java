package ru.nsu.ccfit.factory;

public class BuildAutoTask implements Runnable {
    private final Storage<Body>      bodyStorage;
    private final Storage<Motor>     motorStorage;
    private final Storage<Accessory> accessoryStorage;
    private final Storage<Auto>      autoStorage;
    private static long              autoIdCounter = 0;
    private static long              totalBuilt = 0;

    public BuildAutoTask(Storage<Body> bs, Storage<Motor> ms, Storage<Accessory> as, Storage<Auto> aus) {
        this.bodyStorage = bs;
        this.motorStorage = ms;
        this.accessoryStorage = as;
        this.autoStorage = aus;
    }

    @Override
    public void run() {
        try {
            Body body = bodyStorage.get();
            Motor motor = motorStorage.get();
            Accessory accessory = accessoryStorage.get();

            long id;
            synchronized (BuildAutoTask.class) {
                id = autoIdCounter++;
                totalBuilt++;
            }
            Auto newAuto = new Auto(id, body, motor, accessory);

            autoStorage.put(newAuto);

        } catch (InterruptedException e) {
            // я хз что тут кидать, run выполняется один раз, ничего случится не должно
        }
    }

    public static synchronized long getTotalBuilt() {
        return totalBuilt;
    }
}