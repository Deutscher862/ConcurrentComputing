package com.agh.lab8;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class Main{
    public static void main(String[] args) {
        int MAX_ITER = 570;
        ExecutorThread executorThread = new ExecutorThread(MAX_ITER);
        executorThread.start();
    }
}

class Mandelbrot extends JFrame {
    private final double ZOOM = 150;
    private final BufferedImage I;
    private double zx, zy, cX, cY, tmp;

    Mandelbrot(int MAX_ITER) {
        super("Mandelbrot Set");
        setBounds(100, 100, 800, 600);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        I = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
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

class ExecutorThread extends Thread{
    private final int MAX_ITER;

    ExecutorThread(int MAX_ITER){
        this.MAX_ITER = MAX_ITER;
    }

    @Override
    public void run() {
        new Mandelbrot(MAX_ITER).setVisible(false);
    }
}