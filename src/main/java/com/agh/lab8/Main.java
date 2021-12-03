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
            for (int no_threads = 1; no_threads < 2; no_threads++) {
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

                output.append(String.valueOf(no_threads)).append(" ");

                for (Future<Double> future : futures) {
                    output.append(String.valueOf(future.get())).append(" ");
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

    protected ExecutorTest(int max_iter, int no_threads) {
        MAX_ITER = max_iter;
        this.no_threads = no_threads;
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
        executor.execute(new ExecutorThread(MAX_ITER));
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
            executor.execute(new ExecutorThread(MAX_ITER));
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
    private final double ZOOM = 150;
    private final BufferedImage I;
    private double zx, zy, cX, cY, tmp;
    private final int MAX_ITER;

    Mandelbrot(int MAX_ITER) {
        super("Mandelbrot Set");
        setBounds(100, 100, 800, 600);
        setResizable(false);
        I = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.MAX_ITER = MAX_ITER;
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                zx = zy = 0;
                cX = (x - 400) / ZOOM;
                cY = (y - 300) / ZOOM;
                int iter = MAX_ITER;
                while (zx * zx + zy * zy < 4 && iter > 0) {
                    tmp = zx * zx - zy * zy + cX;
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
    private final int MAX_ITER;

    ExecutorThread(int MAX_ITER) {
        this.MAX_ITER = MAX_ITER;
    }

    @Override
    public void run() {
        new Mandelbrot(MAX_ITER).setVisible(false);
    }
}