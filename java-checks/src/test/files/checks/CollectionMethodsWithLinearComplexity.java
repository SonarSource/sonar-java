import java.util.concurrent.*;
import java.util.*;


class A {
  ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue();

  void foo() {
    log.info("Queue contains " + queue.size() + " elements"); // Noncompliant [[sc=40;ec=44]] {{This call to "size()" may be a performance hot spot if the collection is large.}}
  }
}

class ArrayListUsage {

  ArrayList<Object> field;
  List<Object> iface = new ArrayList<>();

  ArrayListUsage() {
    local.contains(a);
    field.contains(a);
    iface.contains(a);
  }

  void arrayList(Object a) {
    ArrayList<Object> local = new ArrayList<>();
    local.contains(a);
    field.contains(a); // Noncompliant
    iface.contains(a); // Noncompliant
    local.remove(a);
    field.remove(a); // Noncompliant
    iface.remove(a); // Noncompliant
  }
}

class ConcurrentLinkedQueueUsage {

  ConcurrentLinkedQueue<Object> field;
  Queue<Object> iface = new ConcurrentLinkedQueue<>();

  ConcurrentLinkedQueueUsage() {
    local.contains(a);
    field.contains(a);
    iface.contains(a);
  }

  void linkedQeue(Object a) {
    ConcurrentLinkedQueue<Object> local = new ConcurrentLinkedQueue<>();
    local.contains(a);
    field.contains(a); // Noncompliant
    iface.contains(a); // Noncompliant
    local.size();
    field.size(); // Noncompliant
    iface.size(); // Noncompliant
  }
}

class ConcurrentLinkedDequeUsage {

  ConcurrentLinkedDeque<Object> field;
  Deque<Object> iface = new ConcurrentLinkedDeque<>();

  ConcurrentLinkedDequeUsage() {
    local.contains(a);
    field.contains(a);
    iface.contains(a);
  }

  void deque(Object a) {
    ConcurrentLinkedDeque<Object> local = new ConcurrentLinkedDeque<>();
    local.contains(a);
    field.contains(a); // Noncompliant
    iface.contains(a); // Noncompliant
    local.size();
    field.size(); // Noncompliant
    iface.size(); // Noncompliant
  }
}

class CopyOnWriteArrayListUsage {

  CopyOnWriteArrayList<Object> field;
  List<Object> iface = new CopyOnWriteArrayList<>();

  CopyOnWriteArrayListUsage() {
    local.contains(a);
    field.contains(a);
    iface.contains(a);
  }

  void cowList(Object a) {
    CopyOnWriteArrayList<Object> local = new CopyOnWriteArrayList<>();
    local.contains(a);
    field.contains(a); // Noncompliant
    iface.contains(a); // Noncompliant
    local.add(a);
    field.add(a); // Noncompliant
    iface.add(a); // Noncompliant
    local.remove(a);
    field.remove(a); // Noncompliant
    iface.remove(a); // Noncompliant
  }
}

class CopyOnWriteArraySetUsage {

  CopyOnWriteArraySet<Object> field;
  List<Object> iface = new CopyOnWriteArraySet<>();

  CopyOnWriteArraySetUsage() {
    local.contains(a);
    field.contains(a);
    iface.contains(a);
  }

  void cowSet(Object a) {
    CopyOnWriteArraySet<Object> local = new CopyOnWriteArrayList<>();
    local.contains(a);
    field.contains(a); // Noncompliant
    iface.contains(a); // Noncompliant
    local.add(a);
    field.add(a); // Noncompliant
    iface.add(a); // Noncompliant
    local.remove(a);
    field.remove(a); // Noncompliant
    iface.remove(a); // Noncompliant
  }
}

class LinkedListUsage {

  LinkedList<Object> field;
  List<Object> iface = new LinkedList<>();

  LinkedListUsage() {
    local.contains(a);
    field.contains(a);
    iface.contains(a);
  }

  void arrayList(Object a) {
    LinkedList<Object> local = new LinkedList<>();
    local.contains(a);
    field.contains(a); // Noncompliant
    iface.contains(a); // Noncompliant
    local.get(0);
    field.get(0); // Noncompliant
    iface.get(0); // Noncompliant
  }
}

abstract class Coverage extends AbstractCollection {

  abstract ConcurrentLinkedQueue getQueue();

  void f() {
    size();
    getQueue().size(); // FN
  }

}




