package com.agh.lab2;

class Counter {
    private int _val;
    private int op = 0;

    public Counter(int n) {
        _val = n;
    }

    public void inc() {
        _val++;
        op++;
    }

    public void dec() {
        _val--;
        op++;
    }

    public int value() {
        return _val;
    }

    public int getOp() {
        return op;
    }
}

//class countingSemaphore{
//    private final Queue<Semafor> waitingThreads = new LinkedList<>();
//    private int _czeka = 0;
//
//    public synchronized void P(Semafor semafor) {
//        waitingThreads.add(semafor);
//        _czeka++;
//        while (!_stan) {
//            try {
//                wait();
//            } catch (InterruptedException e) {
//                System.out.println(e.getMessage());
//            }
//        }
//        _czeka--;
//        _stan = false;
//    }
//
//    public synchronized void V() {
//        if (_czeka > 0) {
//            this.notify();
//        }
//        _stan = true;
//    }
//}`

class Semafor {
    private boolean _stan = true;
    private int _czeka = 0;

    public synchronized void P() {
        _czeka++;
        while (!_stan) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        _stan = false;
        _czeka--;
    }

    public synchronized void V() {
        if (_czeka > 0) {
            this.notify();
        }
        _stan = true;
    }
}

class IThread extends Thread {
    private final Counter counter;
    private final Semafor semafor;

    IThread(Counter counter, Semafor semafor) {
        this.counter = counter;
        this.semafor = semafor;
    }

    @Override
    public void run() {
        for (int i = 0; i < 100000; i++) {
            this.semafor.P();
            counter.inc();
            this.semafor.V();
        }
    }
}

class DThread extends Thread {
    private final Counter counter;
    private final Semafor semafor;

    DThread(Counter counter, Semafor semafor) {
        this.counter = counter;
        this.semafor = semafor;
    }

    @Override
    public void run() {
        for (int i = 0; i < 100000; i++) {
            this.semafor.P();
            counter.dec();
            this.semafor.V();
        }
    }
}

class Race {
    public static void main(String[] args) {
        Counter cnt;
        IThread iThread;
        DThread dThread;
        cnt = new Counter(0);
        Semafor semafor = new Semafor();
        iThread = new IThread(cnt, semafor);
        dThread = new DThread(cnt, semafor);
        iThread.start();
        dThread.start();
        try {
            iThread.join();
            dThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Stan= " + cnt.value());
        System.out.println("OP: " + cnt.getOp());
    }
}

