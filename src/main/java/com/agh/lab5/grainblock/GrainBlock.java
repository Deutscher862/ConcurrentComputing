package com.agh.lab5.grainblock;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Main {
    public static void main(String[] args) {
        ListWithNodeLocks listWithNodeLocks = new ListWithNodeLocks();
        ListWithGlobalLock listWithGlobalLock = new ListWithGlobalLock();

        Instant start = Instant.now();
        List<MainThreadNodeLockList> threads = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            MainThreadNodeLockList newThread = new MainThreadNodeLockList(listWithNodeLocks);
            threads.add(newThread);
            newThread.start();
        }
        for (MainThreadNodeLockList thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Instant halftime = Instant.now();

        List<MainThreadGlobalLockList> threads2 = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            MainThreadGlobalLockList newThread = new MainThreadGlobalLockList(listWithGlobalLock);
            threads2.add(newThread);
            newThread.start();
        }
        for (MainThreadGlobalLockList thread : threads2) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Instant finish = Instant.now();
        double nodeLockListTime = Duration.between(start, halftime).toMillis();
        double listLockList = Duration.between(halftime, finish).toMillis();
        System.out.println("Czas działania pierwszej listy: " + nodeLockListTime / 1000 + "s");
        System.out.println("Czas działania drugiej listy: " + listLockList / 1000 + "s");

        try (Writer output = new BufferedWriter(new FileWriter("results.txt", true))) {
            output.append(String.valueOf(nodeLockListTime / 1000)).append(" ");
            output.append(String.valueOf(listLockList / 1000)).append("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class MainThreadNodeLockList extends Thread {
    ListWithNodeLocks list;

    MainThreadNodeLockList(ListWithNodeLocks list) {
        this.list = list;
    }

    @Override
    public void run() {
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }
    }
}

class MainThreadGlobalLockList extends Thread {
    ListWithGlobalLock list;

    MainThreadGlobalLockList(ListWithGlobalLock list) {
        this.list = list;
    }

    @Override
    public void run() {
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }
    }
}

class LockNode {
    Object o;
    LockNode next;
    private final Lock lock = new ReentrantLock();

    LockNode(Object o) {
        this.o = o;
    }

    void lock() {
        lock.lock();
    }

    void unlock() {
        lock.unlock();
    }

    public void setNext(LockNode next) {
        this.next = next;
    }

    public Object getO() {
        return o;
    }
}

class ListWithNodeLocks {
    private LockNode first = new LockNode(null);
    private LockNode last = new LockNode(null);
    private final Lock sizeLock = new ReentrantLock();
    private int size = 0;

    ListWithNodeLocks() {
        first.setNext(last);
    }

    boolean contains(Object o) {
        if (size == 0) return false;
        if (size == 1) {
            first.lock();
            boolean result = first.getO().equals(o);
            first.unlock();
            return result;
        }
        LockNode currentNode = first;
        while (currentNode.next.getO() != null) {
            currentNode.lock();
            currentNode.next.lock();
            if (currentNode.getO().equals(o)) {
                currentNode.unlock();
                currentNode.next.unlock();
                return true;
            }
            currentNode.unlock();
            currentNode = currentNode.next;
        }
        return false;
    }

    boolean remove(Object o) {
        if (!contains(o)) return false;
        LockNode currentNode = first, prev = null;

        if (currentNode != null && currentNode.getO().equals(o)) {
            first.lock();
            first = currentNode.next;
            first.unlock();
            sizeLock.lock();
            size--;
            sizeLock.unlock();
            return true;
        }

        while (true) {
            assert currentNode != null;
            if (!(currentNode.next.getO() != null && !currentNode.getO().equals(o))) break;
            currentNode.lock();
            currentNode.next.lock();
            if (prev != null)
                prev.unlock();
            prev = currentNode;
            prev.lock();
            if (currentNode.getO().equals(o)) {
                prev.setNext(currentNode.next);
                currentNode.unlock();
                prev.unlock();
                sizeLock.lock();
                size--;
                sizeLock.unlock();
                return true;
            }
            currentNode.unlock();
            currentNode = currentNode.next;
        }
        return false;
    }

    boolean add(Object o) {
        LockNode newNode = new LockNode(o);
        first.lock();
        last.lock();
        if (first.getO() == null) {
            first = newNode;
            newNode.setNext(last);
            sizeLock.lock();
            size++;
            sizeLock.unlock();
            return true;
        } else if (last.getO() == null) {
            last = newNode;
            first.setNext(last);
            sizeLock.lock();
            size++;
            sizeLock.unlock();
            first.unlock();
        } else {
            last.setNext(newNode);
            LockNode prev = last;
            last = newNode;
            sizeLock.lock();
            size++;
            sizeLock.unlock();
            first.unlock();
            prev.unlock();
        }
        return true;
    }
}

class PlainNode {
    Object o;
    PlainNode next;

    PlainNode(Object o) {
        this.o = o;
    }

    public void setNext(PlainNode next) {
        this.next = next;
    }

    public Object getO() {
        return o;
    }
}

class ListWithGlobalLock {
    private PlainNode first;
    private PlainNode last;
    private final Lock sizeLock = new ReentrantLock();
    private final Lock listLock = new ReentrantLock();
    private int size = 0;

    boolean contains(Object o) {
        if (size == 0) return false;
        listLock.lock();
        PlainNode currentNode = first;
        while (currentNode != null) {
            if (currentNode.getO().equals(o)) {
                listLock.unlock();
                return true;
            }
            currentNode = currentNode.next;
        }
        listLock.unlock();
        return false;
    }

    boolean remove(Object o) {
        if (!contains(o)) return false;
        PlainNode currentNode = first, prev = null;

        listLock.lock();

        if (currentNode != null && currentNode.getO().equals(o)) {
            first = currentNode.next;
            listLock.unlock();
            return true;
        }

        while (currentNode != null && !currentNode.getO().equals(o)) {
            prev = currentNode;
            currentNode = currentNode.next;
        }

        listLock.unlock();
        assert prev != null;
        assert currentNode != null;
        prev.next = currentNode.next;
        return true;
    }

    void add(Object o) {
        PlainNode newNode = new PlainNode(o);
        listLock.lock();
        if (first == null) {
            first = newNode;
        } else if (last == null) {
            last = newNode;
            first.setNext(last);
        } else {
            last.setNext(newNode);
            last = newNode;
        }
        listLock.unlock();
        sizeLock.lock();
        size++;
        sizeLock.unlock();
    }
}
