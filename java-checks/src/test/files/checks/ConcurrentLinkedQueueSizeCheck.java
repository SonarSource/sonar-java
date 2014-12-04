import java.util.concurrent.ConcurrentLinkedQueue;

class A {
  void foo() {
    ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue();
    log.info("Queue contains " + queue.size() + " elements");
  }
}