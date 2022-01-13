package com.agh.lab13;

import org.jcsp.lang.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class Main {
    public static void main(String[] args) {
        int size = 3;
        One2OneChannelInt prodChan = Channel.one2oneInt();
        One2OneChannelInt consReq = Channel.one2oneInt();
        One2OneChannelInt consChan = Channel.one2oneInt();

        CSProcess[] procList = {
                new Producer(prodChan, 5),
                new Consumer(consReq, consChan),
                new DisorderBuffer(prodChan, consReq, consChan, size)
        };
        Parallel par = new Parallel(procList);
        par.run();
    }
}

class Producer implements CSProcess {
    private final One2OneChannelInt channel;
    private final int n;

    Producer(One2OneChannelInt out, int operations) {
        channel = out;
        n = operations;
    }

    @Override
    public void run() {
        for (int i = 0; i < n; i++) {
            int item = (int) (Math.random() * 100) + 1;
            System.out.println("Wpisuję: " + item);
            channel.out().write(item);
        }
        channel.out().write(-1);
        System.out.println("Producent konczy prace");
    }
}

class Consumer implements CSProcess {
    private final One2OneChannelInt in;
    private final One2OneChannelInt req;

    public Consumer(final One2OneChannelInt req, final One2OneChannelInt in) {
        this.req = req;
        this.in = in;
    }

    @Override
    public void run() {
        while (true) {
            req.out().write(0);
            int item = in.in().read();
            if (item == 0)
                continue;
            if (item == -1) {
                break;
            }
            System.out.println("Czytam: " + item);
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Konsument konczy prace");
    }
}

class OrderBuffer implements CSProcess {
    private final One2OneChannelInt producerInput;
    private final One2OneChannelInt consumerDataRequest;
    private final One2OneChannelInt consumerOutput;
    private final List<Integer> buffer = new ArrayList<>();
    private final int size;

    OrderBuffer(One2OneChannelInt in, One2OneChannelInt req, One2OneChannelInt out, int size) {
        this.producerInput = in;
        this.consumerDataRequest = req;
        this.consumerOutput = out;
        this.size = size;
    }

    @Override
    public void run() {
        final Guard[] guards = {producerInput.in(), consumerDataRequest.in()};
        final Alternative alt = new Alternative(guards);
        int countdown = 2;
        while (countdown > 0) {
            int index = alt.select();
            if (index == 0 && buffer.size() < size) {
                int item = producerInput.in().read();
                if (item < 0) {
                    countdown--;
                    buffer.add(-1);
                } else {
                    buffer.add(item);
                }
            } else {
                consumerDataRequest.in().read();
                if (buffer.isEmpty()) {
                    consumerOutput.out().write(0);
                } else {
                    int item = buffer.remove(0);
                    if (item == -1)
                        countdown--;
                    consumerOutput.out().write(item);
                }
            }
        }
        System.out.println("Bufor konczy prace");
    }
}

class DisorderBuffer implements CSProcess {
    private final One2OneChannelInt producerInput;
    private final One2OneChannelInt consumerDataRequest;
    private final One2OneChannelInt consumerOutput;
    private final int[] buffer;
    private final int maxSize;
    private int currentSize = 0;

    DisorderBuffer(One2OneChannelInt in, One2OneChannelInt req, One2OneChannelInt out, int size) {
        this.producerInput = in;
        this.consumerDataRequest = req;
        this.consumerOutput = out;
        this.maxSize = size;
        this.buffer = new int[size];
        for (int i = 0; i < size; i++) {
            this.buffer[i] = 0;
        }
    }

    public void run() {
        final Guard[] guards = {producerInput.in(), consumerDataRequest.in()};
        final Alternative alt = new Alternative(guards);
        int countdown = 2;
        while (countdown > 0) {
            int index = alt.fairSelect();
            if (index == 0 && currentSize < maxSize) {
                int item = producerInput.in().read();
                if (item < 0) {
                    countdown--;
                    item = -1;
                }
                int pointer = 0;
                while (pointer < maxSize && buffer[pointer] != 0) {
                    pointer++;
                }
                buffer[pointer] = item;
                currentSize++;
            } else {
                consumerDataRequest.in().read();
                if (currentSize == 0) {
                    consumerOutput.out().write(0);
                } else {
                    int pointer = 0;
                    while (pointer < maxSize && buffer[pointer] == 0) {
                        pointer++;
                    }
                    int item = buffer[pointer];
                    if (item == -1 && currentSize > 1) {
                        pointer++;
                        while (pointer < maxSize && buffer[pointer] == 0) {
                            pointer++;
                        }
                    }
                    if (pointer < maxSize) {
                        item = buffer[pointer];
                        buffer[pointer] = 0;
                        currentSize--;
                    } else {
                        countdown--;
                    }
                    consumerOutput.out().write(item);
                }
            }
        }
        System.out.println("Bufor konczy prace");
    }
}