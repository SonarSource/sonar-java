import java.util.concurrent.*;
import java.util.*;


class A {
  final ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue();

  void foo() {
    log.info("Queue contains " + queue.size() + " elements"); // Noncompliant [[sc=40;ec=44]] {{This call to "size()" may be a performance hot spot if the collection is large.}}
  }
}

class ArrayListUsage {

  ArrayList<Object> field;
  private List<Object> iface = new ArrayList<>();
  ArrayList<Object> accessible;

  ArrayListUsage(Object a) { // Compliant - usage in constructor
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
  final Queue<Object> iface = new ConcurrentLinkedQueue<>();

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
  private Deque<Object> iface = new ConcurrentLinkedDeque<>();

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
  private List<Object> iface = new CopyOnWriteArrayList<>();

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
  private List<Object> iface = new CopyOnWriteArraySet<>();

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
  private List<Object> iface = new LinkedList<>();

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
    this.size();
    super.size();
  }

  private List<Object> field1 = new ArrayList<>();
  ArrayList<Object> field2;
  void g(Object o) {
    field2 = (ArrayList<Object>) field1;
    field2 = this.field1;
    field1.contains(o); // Noncompliant
  }

}

class Assignments {

  private List<Object> list1 = new LinkedList();
  private List<Object> list2;
  List<Object> list3;
  private List<Object> list4;
  private List<Object> list5 = new LinkedList();

  void f(Object a) {
    list1 = new ArrayList();
    list2 = new ArrayList();
    list1.contains(a); // Noncompliant
    list2.contains(a); // Noncompliant
    list3.contains(a);
    list2.remove(a);
    list2 = new LinkedList();
    this.list4 = new ArrayList();
    this.list4.get(0).x = new LinkedList();
    list4.contains(a); // Noncompliant
    this.list5 = new ArrayList();
    list5.remove(a); // Compliant
  }
}




