package com.agh.lab13;

import org.jcsp.lang.*;

final class Main {
    public static void main(String[] args) {
        new Main();
    }

    Main() {
        final One2OneChannelInt channel = new One2AnyCallChannel();
        CSProcess[] procList = {new Producer(channel, 100), new Consumer(channel, 100)};
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
            channel.write(item);
        }
    }
}

class Consumer implements CSProcess {
    private final One2OneChannelInt channel;
    private final int n;


    Consumer(One2OneChannelInt in, int operations) {
        channel = in;
        n = operations;
    }

    @Override
    public void run() {
        for (int i = 0; i < n; i++) {
            int item = channel.read();
            System.out.println(item);
        }
    }
}

class Buffer implements CSProcess {
    private final One2OneChannelInt[] in;
    private final One2OneChannelInt[] req;
    private final One2OneChannelInt[] out;
    private final int[] buffer = new int[10];
    int hd = -1;
    int tl = -1;

    Buffer(One2OneChannelInt[] in, One2OneChannelInt[] req, One2OneChannelInt[] out) {
        this.in = in;
        this.req = req;
        this.out = out;
    }

    public void run() {
        final Guard[] guards = {(Guard) in[0], (Guard) in[1], (Guard) req[0], (Guard) req[1]};
        final Alternative alt = new Alternative(guards);
        int countdown = 4;
        while (countdown > 0) {
            int index = alt.select();
            switch (index) {
                case 0:
                case 1:
                    if (hd < tl + 11) {
                        int item = in[index].read();
                        if (item < 0)
                            countdown--;
                        else {
                            hd++;
                            buffer[hd % buffer.length] = item;
                        }
                    }
                    break;
                case 2:
                case 3:
                    if (tl < hd) {
                        req[index - 2].read();
                        request
                        tl++;
                        int item = buffer[tl % buffer.length];
                        out[index - 2].write(item);
                    } else if (countdown <= 2) {
                        req[index - 2].read();
                        request
                        out[index - 2].write(-1);
                        countdown--;
                    }
                    break;
            }
        }
    }
}