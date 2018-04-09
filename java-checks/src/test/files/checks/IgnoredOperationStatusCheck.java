import java.io.File;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;

class A {
  boolean fileOp(File f) {
    f.delete(); // Noncompliant [[sc=5;ec=16]] {{Do something with the "boolean" value returned by "delete".}}
    boolean b1 = f.delete(); // Noncompliant [[sc=5;ec=29]] {{Do something with the "boolean" value returned by "delete".}}
    boolean b2 = f.delete(); // Compliant
    if (b2 || f.delete()) {} // Compliant

    f.exists(); // Noncompliant
    f.renameTo(new File("")); // Noncompliant
    f.createNewFile(); // Noncompliant
    f.mkdir(); // Compliant : true if it was created, false if it existed, using it just to make sure we created the dir is a common idiom

    f.canWrite(); // Noncompliant
    f.isHidden(); // Noncompliant
    f.setLastModified(Long.MAX_VALUE); // Noncompliant

    return f.canExecute();
  }

  boolean lockOp(Lock l) {
    l.tryLock(); // Noncompliant [[sc=5;ec=17]] {{Do something with the "boolean" value returned by "tryLock".}}
    l.tryLock(0L, TimeUnit.DAYS); // Compliant
    return l.tryLock(); // Compliant
  }

  void iteratorOp(Iterator<String> i, List<Object> l) {
    i.hasNext(); // Noncompliant
    l.iterator().hasNext(); // Noncompliant
    while(i.hasNext()) { // Compliant
      // do something
    }
  }

  void enumerationOp(Enumeration<String> e, Hashtable<String, Object> h) {
    e.hasMoreElements(); // Noncompliant
    h.elements().hasMoreElements(); // Noncompliant
    while(i.hasNext()) { // Compliant
      // do something
    }
  }

  boolean conditionOp(Condition c) {
    c.awaitNanos(Long.MAX_VALUE); // Noncompliant {{Do something with the "long" value returned by "awaitNanos".}}
    if (c.await(42, TimeUnit.SECONDS)) { // Compliant
      return true;
    }
    c.await(42, TimeUnit.DAYS); // Noncompliant
    c.awaitUntil(new Date()); // Noncompliant
    return false;
  }

  boolean countdownOp(CountDownLatch c) {
    c.await(42L, TimeUnit.HOURS); // Noncompliant
    return c.await(0L, TimeUnit.DAYS); // Compliant
  }

  boolean semaphoreOp(Semaphore s) {
    s.tryAcquire(42); // Noncompliant
    s.tryAcquire(); // Noncompliant
    s.tryAcquire(42L, TimeUnit.HOURS); // Noncompliant
    s.tryAcquire(42,  42L, TimeUnit.SECONDS); // Noncompliant
    return s.tryAcquire(); // Compliant
  }

  boolean blockingQueueOp(BlockingQueue<String> b, ArrayBlockingQueue<Object> a, List<String> l, List<Object> lo) {
    a.remove("hello"); // Noncompliant
    b.remove(); // Noncompliant {{Do something with the "String" value returned by "remove".}}
    b.remove(new Object()); // Noncompliant

    b.drainTo(l); // not a status code
    a.drainTo(lo, 42); // not a status code

    a.offer(new Object()); // Noncompliant
    b.offer("hello", 42L, TimeUnit.SECONDS); // Noncompliant
    return a.offer(new Object()); // Compliant
  }
}
