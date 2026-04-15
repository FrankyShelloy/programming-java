package ru.nsu.ccfit.factory;

import java.util.ArrayList;
import java.util.List;

public class Storage<T>{
    private int     capacity;
    private List<T> items = new ArrayList<>();
    private Runnable onGetCallback;

    public Storage(int capacity){
        this.capacity = capacity;
    }

    public void setOnGetCallback(Runnable callback) {
        this.onGetCallback = callback;
    }

    public synchronized void put(T item) throws InterruptedException{
        while (items.size() >= capacity){
            wait();
        }
        items.add(item);
        notifyAll();
    }

    public synchronized T get() throws InterruptedException{
        while (items.isEmpty()){
            wait();
        }
        T item = items.remove(0);
        if (onGetCallback != null) {
            onGetCallback.run();
        }
        notifyAll();
        return item;
    }

    public synchronized int getItemCount() {
        return items.size();
    }

    public int getCapacity() {
        return capacity;
    }
}
