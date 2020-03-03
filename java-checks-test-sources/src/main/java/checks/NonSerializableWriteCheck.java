package checks;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

class NonSerializableWriteCheck {
  void myMethod(ObjectOutputStream out, Object x, byte[] array) throws IOException {
    out.writeObject(new Object());
    out.writeObject("x");
    out.writeObject(1);
    MySerializable mySerializable = new MySerializable();
    out.writeObject(mySerializable);
    MyNonSerializable myNonSerializable1 = new MyNonSerializable();
    MyNonSerializable myNonSerializable2 = new MyNonSerializable();
    if (myNonSerializable2 instanceof Runnable) {
      out.writeObject(myNonSerializable1); // Noncompliant {{Make the "checks.MyNonSerializable" class "Serializable" or don't write it.}}
    }
    if (myNonSerializable2 instanceof Serializable) {
      out.writeObject(myNonSerializable2);
    }
    if (x.toString() instanceof Serializable) {
      out.writeObject(new MyNonSerializable()); // Noncompliant [[sc=23;ec=46]] {{Make the "checks.MyNonSerializable" class "Serializable" or don't write it.}}
    }
    out.writeObject(array);

    out.writeObject(java.util.Arrays.asList("one", "two")); // Noncompliant - false positive

    Iterable<String> iterable = new ArrayList<>();
    out.writeObject(iterable); // Noncompliant - false positive

    Collection<String> collection = new ArrayList<>();
    out.writeObject(collection); // Noncompliant - false positive

    List<String> list = new ArrayList<>();
    out.writeObject(list); // Noncompliant - false positive

    Set<String> set = new HashSet<>();
    out.writeObject(set); // Noncompliant - false positive

    SortedSet<String> sortedSet = new TreeSet<>();
    out.writeObject(sortedSet); // Noncompliant - false positive

    NavigableSet<String> navigableSet = new TreeSet<>();
    out.writeObject(navigableSet); // Noncompliant - false positive

    Queue<String> queue = new LinkedList<>();
    out.writeObject(queue); // Noncompliant - false positive

    Deque<String> deque = new ArrayDeque<>();
    out.writeObject(deque); // Noncompliant - false positive

    Map<String, String> map = new HashMap<>();
    out.writeObject(map); // Noncompliant - false positive

    SortedMap<String, String> sortedMap = new TreeMap<>();
    out.writeObject(sortedMap); // Noncompliant - false positive

    NavigableMap<String, String> navigableMap = new TreeMap<>();
    out.writeObject(navigableMap); // Noncompliant - false positive

    Enumeration<String> enumeration = Collections.emptyEnumeration();
    out.writeObject(enumeration); // Noncompliant - false positive
  }
}

class MySerializable implements Serializable {
}

class MyNonSerializable implements Runnable {
  @Override
  public void run() {
  }
}

class ParameterizedSerializable<T> implements Serializable {

  T t;

  private void writeObject(ObjectOutputStream s) throws java.io.IOException {
    s.writeObject(t);
  }

}

class TypeOfAssignedExpressions {
  final java.util.Map<Integer, List<Object>> cacheProp = new HashMap<>();
  final java.util.Map<Integer, List<Object>> cacheVar;
  {
    cacheVar = null;
  }
  java.util.Map<Integer, List<Object>> cacheVar2 = new HashMap<>();

  void foo() {
    try (FileOutputStream fos = new FileOutputStream(""); ObjectOutputStream oos = new ObjectOutputStream(fos)) {
      oos.writeObject(cacheProp); // compliant, real type is hashmap
      oos.writeObject(cacheVar); // Noncompliant : no initializer
      oos.writeObject(cacheVar2); // Noncompliant not final, so we are unsure of concrete type
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }
}
