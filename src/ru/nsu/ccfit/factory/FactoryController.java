package ru.nsu.ccfit.factory;

import ru.nsu.ccfit.threadpool.ThreadPool;

public class FactoryController {
    private final Storage<Body>       bodyStorage;
    private final Storage<Motor>      motorStorage;
    private final Storage<Accessory>  accessoryStorage;
    private final Storage<Auto>       autoStorage;
    private final ThreadPool          threadPool;

    public FactoryController(Storage<Body> bs, Storage<Motor> ms, Storage<Accessory> as, Storage<Auto> aus, ThreadPool tp) {
        this.bodyStorage =      bs;
        this.motorStorage =     ms;
        this.accessoryStorage = as;
        this.autoStorage =      aus;
        this.threadPool =       tp;
        autoStorage.setOnGetCallback(this::requestBuildIfNecessary);
        requestBuildIfNecessary();
    }

    private synchronized void requestBuildIfNecessary() {
        int targetTasks = autoStorage.getCapacity() - (autoStorage.getItemCount() + threadPool.getTaskCount());
        for (int i = 0; i < targetTasks; i++) {
            threadPool.execute(new BuildAutoTask(bodyStorage, motorStorage, accessoryStorage, autoStorage));
        }
    }
}
