package com.agh.lab8;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

class Main {
    public static void main(String[] args) {
        int MAX_ITER = 500;

        try (Writer output = new BufferedWriter(new FileWriter("results.txt", true))) {
            for (int no_threads = 1; no_threads <= 100; no_threads++) {
                System.out.println(no_threads);
                double[] values = {0, 0, 0, 0};
                for (int j = 0; j < 100; j++) {
                    List<Future<Double>> futures = new ArrayList<>();
                    ExecutorService executor = Executors.newFixedThreadPool(4);
                    futures.add(executor.submit(new NewSingleThreadExecutorTest(MAX_ITER, no_threads)));
                    futures.add(executor.submit(new ThreadPoolTest(MAX_ITER, no_threads, Executors.newFixedThreadPool(no_threads))));
                    futures.add(executor.submit(new ThreadPoolTest(MAX_ITER, no_threads, Executors.newCachedThreadPool())));
                    futures.add(executor.submit(new ThreadPoolTest(MAX_ITER, no_threads, Executors.newWorkStealingPool(no_threads))));

                    executor.shutdown();

                    boolean executed = false;
                    try {
                        while (!executed)
                            if (executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS))
                                executed = true;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    for (int k = 0; k < 4; k++) {
                        values[k] += futures.get(k).get();
                    }
                }

                output.append(String.valueOf(no_threads)).append(" ");

                for (int k = 0; k < 4; k++) {
                    output.append(String.valueOf(values[k] / 100.0)).append(" ");
                }
                output.append("\n");
            }
        } catch (IOException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

abstract class ExecutorTest implements Callable<Double> {
    protected final int MAX_ITER;
    protected final int no_threads;
    protected final Mandelbrot mandelbrot;

    protected ExecutorTest(int max_iter, int no_threads) {
        MAX_ITER = max_iter;
        this.no_threads = no_threads;
        mandelbrot = new Mandelbrot(MAX_ITER);
        mandelbrot.setVisible(false);
    }

    @Override
    abstract public Double call();
}

class NewSingleThreadExecutorTest extends ExecutorTest {
    NewSingleThreadExecutorTest(int MAX_ITER, int no_threads) {
        super(MAX_ITER, no_threads);
    }

    @Override
    public Double call() {
        Instant start = Instant.now();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new ExecutorThread(1, 1, mandelbrot));
        executor.shutdown();
        boolean executed = false;
        try {
            while (!executed)
                if (executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS))
                    executed = true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Instant finish = Instant.now();
        return (double) Duration.between(start, finish).toMillis() / 1000;
    }
}

class ThreadPoolTest extends ExecutorTest {
    private final ExecutorService executor;

    ThreadPoolTest(int MAX_ITER, int no_threads, ExecutorService executor) {
        super(MAX_ITER, no_threads);
        this.executor = executor;
    }

    @Override
    public Double call() {
        Instant start = Instant.now();
        for (int i = 0; i < no_threads; i++) {
            executor.execute(new ExecutorThread(i, no_threads, mandelbrot));
        }
        executor.shutdown();
        boolean executed = false;
        try {
            while (!executed)
                if (executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS))
                    executed = true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Instant finish = Instant.now();
        return (double) Duration.between(start, finish).toMillis() / 1000;
    }
}

class Mandelbrot extends JFrame {
    private final BufferedImage I;
    private final int MAX_ITER;

    Mandelbrot(int MAX_ITER) {
        super("Mandelbrot Set");
        setBounds(100, 100, 800, 600);
        setResizable(false);
        I = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.MAX_ITER = MAX_ITER;
    }

    void count(int first_x, int step) {
        for (int y = 0; y < getHeight(); y++) {
            for (int x = first_x; x < getWidth(); x += step) {
                double zy;
                double zx = zy = 0;
                double ZOOM = 150;
                double cX = (x - 400) / ZOOM;
                double cY = (y - 300) / ZOOM;
                int iter = MAX_ITER;
                while (zx * zx + zy * zy < 4 && iter > 0) {
                    double tmp = zx * zx - zy * zy + cX;
                    zy = 2.0 * zx * zy + cY;
                    zx = tmp;
                    iter--;
                }
                I.setRGB(x, y, iter | (iter << 8));
            }
        }
    }

    @Override
    public void paint(Graphics g) {
        g.drawImage(I, 0, 0, this);
    }
}

class ExecutorThread extends Thread {
    private final int first_x;
    private final int step;
    private final Mandelbrot mandelbrot;

    ExecutorThread(int first_x, int step, Mandelbrot mandelbrot) {
        this.first_x = first_x;
        this.step = step;
        this.mandelbrot = mandelbrot;
    }

    @Override
    public void run() {
        mandelbrot.count(first_x, step);
    }
}
