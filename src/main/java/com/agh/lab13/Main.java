package com.agh.lab13;

import org.jcsp.lang.*;

import java.util.ArrayList;
import java.util.List;

public final class Main {
    public static void main(String[] args) {
        final One2OneChannelInt prodChan = Channel.one2oneInt();
        final One2OneChannelInt consReq = Channel.one2oneInt();
        final One2OneChannelInt consChan = Channel.one2oneInt();

        CSProcess[] procList = {
                new Producer(prodChan, 3),
                new Consumer(consReq, consChan),
                new Buffer(prodChan, consReq, consChan)
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
        }
        System.out.println("Konsument konczy prace");
    }
}

class Buffer implements CSProcess {
    private final One2OneChannelInt producerInput;
    private final One2OneChannelInt consumerDataRequest;
    private final One2OneChannelInt consumerOutput;
    private final List<Integer> buffer = new ArrayList<>();
    private final int size = 10;

    Buffer(One2OneChannelInt in, One2OneChannelInt req, One2OneChannelInt out) {
        this.producerInput = in;
        this.consumerDataRequest = req;
        this.consumerOutput = out;
    }

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