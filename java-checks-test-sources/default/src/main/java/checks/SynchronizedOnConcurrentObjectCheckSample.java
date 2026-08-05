package checks;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class SynchronizedOnConcurrentObjectCheckSample {

  private final ReentrantLock reentrantLock = new ReentrantLock();
  private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
  private final Lock lock = new ReentrantLock();
  private final Semaphore semaphore = new Semaphore(1);
  private final CountDownLatch latch = new CountDownLatch(1);
  private final CyclicBarrier barrier = new CyclicBarrier(2);
  private final BlockingQueue<String> blockingQueue = new ArrayBlockingQueue<>(10);
  private final ArrayBlockingQueue<String> arrayBlockingQueue = new ArrayBlockingQueue<>(10);
  private final LinkedBlockingQueue<String> linkedBlockingQueue = new LinkedBlockingQueue<>();
  private final AtomicBoolean atomicBoolean = new AtomicBoolean();
  private final AtomicInteger atomicInteger = new AtomicInteger();
  private final CustomLock customLock = new CustomLock();

  private final Object objectLock = new Object();
  private final ConcurrentHashMap<String, String> concurrentMap = new ConcurrentHashMap<>();
  private Future<?> future;

  void noncompliant() {
    synchronized (reentrantLock) { // Noncompliant {{Use the "ReentrantLock" API for synchronization instead of a "synchronized" block.}}
    //            ^^^^^^^^^^^^^
    }

    synchronized (lock) { // Noncompliant {{Use the "Lock" API for synchronization instead of a "synchronized" block.}}
    //            ^^^^
    }

    synchronized (rwLock) { // Noncompliant {{Use the "ReentrantReadWriteLock" API for synchronization instead of a "synchronized" block.}}
    //            ^^^^^^
    }

    synchronized (semaphore) { // Noncompliant {{Use the "Semaphore" API for synchronization instead of a "synchronized" block.}}
    //            ^^^^^^^^^
    }

    synchronized (latch) { // Noncompliant {{Use the "CountDownLatch" API for synchronization instead of a "synchronized" block.}}
    //            ^^^^^
    }

    synchronized (barrier) { // Noncompliant {{Use the "CyclicBarrier" API for synchronization instead of a "synchronized" block.}}
    //            ^^^^^^^
    }

    synchronized (blockingQueue) { // Noncompliant {{Use the "BlockingQueue" API for synchronization instead of a "synchronized" block.}}
    //            ^^^^^^^^^^^^^
    }

    synchronized (arrayBlockingQueue) { // Noncompliant {{Use the "ArrayBlockingQueue" API for synchronization instead of a "synchronized" block.}}
    //            ^^^^^^^^^^^^^^^^^^
    }

    synchronized (linkedBlockingQueue) { // Noncompliant {{Use the "LinkedBlockingQueue" API for synchronization instead of a "synchronized" block.}}
    //            ^^^^^^^^^^^^^^^^^^^
    }

    synchronized (atomicBoolean) { // Noncompliant {{Use the "AtomicBoolean" API for synchronization instead of a "synchronized" block.}}
    //            ^^^^^^^^^^^^^
    }

    synchronized (atomicInteger) { // Noncompliant {{Use the "AtomicInteger" API for synchronization instead of a "synchronized" block.}}
    //            ^^^^^^^^^^^^^
    }

    synchronized (customLock) { // Noncompliant {{Use the "CustomLock" API for synchronization instead of a "synchronized" block.}}
    //            ^^^^^^^^^^
    }
  }

  void compliant() {
    synchronized (objectLock) {
      // ...
    }

    synchronized (concurrentMap) {
      // ...
    }

    synchronized (future) {
      // ...
    }

    reentrantLock.lock();
    try {
      // ...
    } finally {
      reentrantLock.unlock();
    }

    rwLock.writeLock().lock();
    try {
      // ...
    } finally {
      rwLock.writeLock().unlock();
    }
  }

  static class CustomLock extends ReentrantLock {
  }
}
