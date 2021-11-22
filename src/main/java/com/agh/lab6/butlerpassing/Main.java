package com.agh.lab6.butlerpassing;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.agh.lab6.butlerpassing.Fil5mon.NO_PICKING;

class Fork {
    boolean state = false;

    synchronized void pick() {
        state = true;
    }

    synchronized void put() {
        state = false;
    }
}

class Philosopher extends Thread {
    private int _counter = 0;
    private final int id;
    private final Random random = new Random();
    private final Butler butler;

    Philosopher(int id, Butler butler) {
        this.id = id;
        this.butler = butler;
    }

    public void run() {
        while (true) {
            butler.pass(id - 1, id % 5);
//            System.out.println(id + ": podnosze " + id + " " + ((id % 5) + 1));
            ++_counter;
            try {
                int randInt = random.nextInt();
                TimeUnit.MILLISECONDS.sleep(randInt % 100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            butler.put(id - 1, id % 5);
//            System.out.println(id + ": opuszczam " + id + " " + ((id % 5) + 1));
            if (_counter % NO_PICKING == 0) {
//                System.out.println("Filozof: " + id +
//                        " jadlem " + _counter + " razy");
                break;
            }
        }
    }
}

class Butler {
    private final List<Fork> forks;

    Butler(List<Fork> forks) {
        this.forks = forks;
    }

    synchronized void pass(int leftFork, int rightFork) {
        while (forks.get(leftFork).state || forks.get(rightFork).state) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        forks.get(leftFork).pick();
        forks.get(rightFork).pick();
    }

    synchronized void put(int leftFork, int rightFork) {
        forks.get(leftFork).put();
        forks.get(rightFork).put();
        notifyAll();
    }
}

class Fil5mon {
    public static int NO_PICKING;

    public static void main(String[] args) {
        for (int j = 1; j <= 100; j++) {
            NO_PICKING = j;
            System.out.println(NO_PICKING);
            List<Fork> forks = new ArrayList<>();
            List<Philosopher> philosophers = new ArrayList<>();

            for (int i = 0; i < 5; i++) {
                forks.add(new Fork());
            }

            Butler butler = new Butler(forks);

            try (Writer output = new BufferedWriter(new FileWriter("results.txt", true))) {
                Instant start = Instant.now();
                for (int i = 0; i < 5; i++) {
                    Philosopher philosopher = new Philosopher(i + 1, butler);
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

                Instant finish = Instant.now();
                double timeElapsed = Duration.between(start, finish).toMillis();
//                System.out.println("Czas działania: " + timeElapsed / 1000 + "s");

                output.append(String.valueOf(NO_PICKING)).append(" ");
                output.append(String.valueOf(timeElapsed / 1000)).append("\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

