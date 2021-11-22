package com.agh.lab6.parallelpicking;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

class Fork {
    boolean state = false;
    private final int id;

    Fork(int id) {
        this.id = id;
    }

    synchronized void pick() {
        while (state) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        state = true;
    }

    synchronized void put() {
        state = false;
        notifyAll();
    }

    public int getId() {
        return id;
    }
}

class Philosopher extends Thread {
    private int _counter = 0;
    private final Fork leftFork;
    private final Fork rightFork;
    private final int id;
    private final Random random = new Random();
    private final Object synchObject;

    Philosopher(Fork leftFork, Fork rightFork, int id, Object synchObject) {
        this.leftFork = leftFork;
        this.rightFork = rightFork;
        this.id = id;
        this.synchObject = synchObject;
    }

    public void run() {
        while (true) {
            synchronized (synchObject) {
                leftFork.pick();
                System.out.println(id + ": podnosze " + leftFork.getId() + " widelec");
                rightFork.pick();
                System.out.println(id + ": podnosze " + rightFork.getId() + " widelec");
            }
            ++_counter;
            try {
                int randInt = random.nextInt() * 100;
                TimeUnit.MILLISECONDS.sleep(randInt);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            leftFork.put();
            System.out.println(id + ": odkladam " + leftFork.getId() + " widelec");
            rightFork.put();
            System.out.println(id + ": odkladam " + rightFork.getId() + " widelec");
            if (_counter % 100 == 0) {
                System.out.println("Filozof: " + Thread.currentThread() +
                        "jadlem " + _counter + " razy");
                break;
            }
        }
    }
}

class Fil5mon {
    public static void main(String[] args) {
        List<Fork> forks = new ArrayList<>();
        List<Philosopher> philosophers = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            forks.add(new Fork(i + 1));
        }

        Object synchObject = new Object();

        for (int i = 0; i < 5; i++) {
            Fork leftFork = forks.get(i);
            Fork rightFork = forks.get((i + 1) % 5);
            Philosopher philosopher = new Philosopher(leftFork, rightFork, i + 1, synchObject);
            philosophers.add(philosopher);
            philosopher.start();
        }

        for (Philosopher philosopher : philosophers) {
            try {
                philosopher.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

