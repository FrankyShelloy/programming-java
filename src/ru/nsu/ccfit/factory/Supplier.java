package ru.nsu.ccfit.factory;

public class Supplier<T extends Component> implements Runnable {
    private final Storage<T>   storage;
    private final Class<T>     componentClass;
    private int                delay;
    private static long        idCounter = 0;
    private long               totalProduced = 0;

    public Supplier(Storage<T> storage, Class<T> componentClass, int delay) {
        this.storage =        storage;
        this.componentClass = componentClass;
        this.delay =          delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(delay);
                T item = createItem();
                storage.put(item);
                synchronized (this) {
                    totalProduced++;
                }
            }
        } catch (InterruptedException e) {
        }
    }

    public synchronized long getTotalProduced() {
        return totalProduced;
    }

    private synchronized T createItem() {
        try {
            return componentClass.getConstructor(long.class).newInstance(idCounter++);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}