package com.agh.lab5.grainblock;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Main {
    public static void main(String[] args) {
        ListWithNodeLocks listWithNodeLocks = new ListWithNodeLocks();
        ListWithGlobalLock listWithGlobalLock = new ListWithGlobalLock();

        int noThreads = 350;

        Instant start = Instant.now();
        List<MainThreadNodeLockList> threads = new ArrayList<>();
        for (int i = 0; i < noThreads; i++) {
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
        for (int i = 0; i < noThreads; i++) {
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
            output.append(String.valueOf(noThreads)).append(" ");
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
        for (int i = 0; i < 100; i++) {
            list.contains(i);
        }
        for (int i = 0; i < 100; i++) {
            list.remove(i);
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
        for (int i = 0; i < 100; i++) {
            list.contains(i);
        }
        for (int i = 0; i < 100; i++) {
            list.remove(i);
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

    @Override
    public boolean equals(Object o1) {
        if (this == o1) return true;
        if (o1 == null || getClass() != o1.getClass()) return false;
        LockNode lockNode = (LockNode) o1;
        return Objects.equals(o, lockNode.o) && Objects.equals(next, lockNode.next) && Objects.equals(lock, lockNode.lock);
    }

    @Override
    public int hashCode() {
        return Objects.hash(o, next, lock);
    }
}

class ListWithNodeLocks {
    private LockNode first = new LockNode(null);
    private LockNode last = new LockNode(null);

    ListWithNodeLocks() {
        first.setNext(last);
    }

    boolean contains(Object o) {
        LockNode prev, currentNode = first;
        first.lock();
        try {
            while (currentNode != null) {
                if (currentNode.getO().equals(o)) {
                    return true;
                }
                prev = currentNode;
                currentNode = currentNode.next;
                try {
                    if (currentNode != null) {
                        currentNode.lock();
                    }
                } finally {
                    prev.unlock();
                }
            }
        } finally {
            if (currentNode != null) {
                currentNode.unlock();
            }
        }
        return false;
    }

    boolean remove(Object o) {
        LockNode prevprev = null, prev = null, currentNode = first;
        currentNode.lock();
        try {
            while (currentNode != null) {
                if (currentNode.getO().equals(o)) {
                    if (prev != null) {
                        prev.next = currentNode.next;
                        currentNode.next = null;
                    }
                    return true;
                }
                prevprev = prev;
                prev = currentNode;
                currentNode = currentNode.next;
                try {
                    if (currentNode != null) {
                        currentNode.lock();
                    }
                } finally {
                    if (prevprev != null) {
                        prevprev.unlock();
                    }
                }
            }
        } finally {
            if (prev != prevprev) {
                prev.unlock();
            }
            if (currentNode != null) {
                currentNode.unlock();
            }
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
            return true;
        } else if (last.getO() == null) {
            last = newNode;
            first.setNext(last);
            first.unlock();
        } else {
            last.setNext(newNode);
            LockNode prev = last;
            last = newNode;
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
