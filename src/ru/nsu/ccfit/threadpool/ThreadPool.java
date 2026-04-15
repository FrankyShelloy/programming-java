package ru.nsu.ccfit.threadpool;

import java.util.LinkedList;
import java.util.Queue;

public class ThreadPool {
    private final Queue<Runnable>  taskQueue = new LinkedList<>();
    private final WorkerThread[]   threads;

    public ThreadPool(int threadCount) {
        threads = new WorkerThread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new WorkerThread();
            threads[i].start();
        }
    }

    public synchronized void execute(Runnable task) {
        taskQueue.add(task);
        notifyAll();
    }

    public synchronized int getTaskCount() {
        return taskQueue.size();
    }

    private class WorkerThread extends Thread {
        @Override
        public void run() {
            while (!isInterrupted()) {
                Runnable task;
                synchronized (ThreadPool.this) {
                    while (taskQueue.isEmpty()) {
                        try {
                            ThreadPool.this.wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                    task = taskQueue.poll();
                }
                try {
                    task.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void shutdown() {
        for (WorkerThread thread : threads) {
            thread.interrupt();
        }
    }
}
