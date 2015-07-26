import java.util.concurrent.ConcurrentLinkedQueue;

class A {
  void foo() {
    ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue();
    log.info("Queue contains " + queue.size() + " elements"); // Noncompliant {{Remove this call to "ConcurrentLinkedQueue.size()"}}
  }
}