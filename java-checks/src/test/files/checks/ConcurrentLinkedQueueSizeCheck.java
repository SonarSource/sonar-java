import java.util.concurrent.ConcurrentLinkedQueue;

class A {
  void foo() {
    ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue();
    log.info("Queue contains " + queue.size() + " elements"); // Noncompliant [[sc=40;ec=44]] {{Remove this call to "ConcurrentLinkedQueue.size()"}}
  }
}
