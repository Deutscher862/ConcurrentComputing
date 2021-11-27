package com.agh.lab7;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

class Main {
    public static void main(String[] argv) {
        ActiveObject activeObject = new ActiveObject(50);
        Proxy proxy = activeObject.getProxy();
        List<Thread> threads = new ArrayList<>();
        int no_consumers = 25;
        int no_producers = 10;

        activeObject.runScheduler();

        for (int i = 0; i < no_consumers + no_producers; i++) {
            Thread newThread = i < no_producers ? new Producer(i + 1, proxy) : new Consumer(i - no_producers + 1, proxy);
            threads.add(newThread);
            newThread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class Consumer extends Thread {
    private final int id;
    private final Proxy proxy;

    Consumer(int id, Proxy proxy) {
        this.id = id;
        this.proxy = proxy;
    }

    @Override
    public void run() {
        while (true) {
            CustomFuture returnedValue = proxy.remove();
            while (!returnedValue.isReady()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Konsument " + id
                    + " dostal: " + returnedValue.getObject());
            try {
                TimeUnit.MILLISECONDS.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class Producer extends Thread {
    private final int id;
    private final Proxy proxy;
    private final Random rand = new Random();

    Producer(int id, Proxy proxy) {
        this.id = id;
        this.proxy = proxy;
    }

    @Override
    public void run() {
        while (true) {
            int producedValue = rand.nextInt(1000);
            proxy.add(producedValue);
            System.out.println("Producent " + id + " dodal: " + producedValue);
            try {
                TimeUnit.MILLISECONDS.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class ActiveObject {
    private final Scheduler scheduler;
    private final Proxy proxy;

    ActiveObject(int queueSize) {
        Buffer buffer = new Buffer(queueSize);
        ActivationQueue activationQueue = new ActivationQueue();
        scheduler = new Scheduler(activationQueue);
        proxy = new Servant(buffer, activationQueue);
    }

    Proxy getProxy() {
        return this.proxy;
    }

    void runScheduler() {
        scheduler.start();
    }
}

class Buffer {
    private final int maxSize;
    private final Queue<Object> buffer = new LinkedList<>();

    Buffer(int bufSize) {
        this.maxSize = bufSize;
    }

    void add(Object object) {
        if (this.isFull())
            return;
        buffer.add(object);
    }

    Object remove() {
        if (this.isEmpty())
            return null;
        return buffer.remove();
    }

    boolean isFull() {
        return buffer.size() == maxSize;
    }

    boolean isEmpty() {
        return buffer.isEmpty();
    }
}

interface Proxy {
    void add(Object object);

    CustomFuture remove();
}

class Servant implements Proxy {
    private final Buffer buffer;
    private final ActivationQueue activationQueue;

    Servant(Buffer buffer, ActivationQueue activationQueue) {
        this.buffer = buffer;
        this.activationQueue = activationQueue;
    }

    @Override
    public void add(Object object) {
        activationQueue.enqueue(new AddRequest(buffer, object));
    }

    @Override
    public CustomFuture remove() {
        CustomFuture customFuture = new CustomFuture();
        activationQueue.enqueue(new RemoveRequest(buffer, customFuture));
        return customFuture;
    }
}

interface MethodRequest {
    void call();

    boolean guard();
}

class AddRequest implements MethodRequest {
    private final Buffer buffer;
    private final Object object;

    AddRequest(Buffer buffer, Object object) {
        this.buffer = buffer;
        this.object = object;
    }

    @Override
    public void call() {
        buffer.add(object);
    }

    @Override
    public boolean guard() {
        return !buffer.isFull();
    }
}

class RemoveRequest implements MethodRequest {
    private final Buffer buffer;
    private final CustomFuture customFuture;

    RemoveRequest(Buffer buffer, CustomFuture customFuture) {
        this.buffer = buffer;
        this.customFuture = customFuture;
    }

    @Override
    public void call() {
        customFuture.setObject(buffer.remove());
    }

    @Override
    public boolean guard() {
        return !buffer.isEmpty();
    }
}

class ActivationQueue {
    private final Queue<MethodRequest> queue = new ConcurrentLinkedQueue<>();

    void enqueue(MethodRequest request) {
        queue.add(request);
    }

    MethodRequest dequeue() {
        return queue.poll();
    }

    boolean isEmpty() {
        return queue.isEmpty();
    }
}

class Scheduler extends Thread {
    private final ActivationQueue activationQueue;

    Scheduler(ActivationQueue activationQueue) {
        this.activationQueue = activationQueue;
    }

    @Override
    public void run() {
        while (true) {
            if (!activationQueue.isEmpty()) {
                MethodRequest methodRequest = activationQueue.dequeue();
                if (methodRequest.guard()) {
                    methodRequest.call();
                } else {
                    activationQueue.enqueue(methodRequest);
                }
            }
        }
    }
}

class CustomFuture {
    private Object object;

    void setObject(Object object) {
        this.object = object;
    }

    Object getObject() {
        return object;
    }

    boolean isReady() {
        return object != null;
    }
}
