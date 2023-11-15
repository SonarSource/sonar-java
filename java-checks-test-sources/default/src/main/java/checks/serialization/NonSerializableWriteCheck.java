package checks.serialization;

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
      out.writeObject(myNonSerializable1); // Noncompliant {{Make the "checks.serialization.MyNonSerializable" class "Serializable" or don't write it.}}
    }
    if (myNonSerializable2 instanceof Serializable) {
      out.writeObject(myNonSerializable2);
    }
    if (x.toString() instanceof Serializable) {
      out.writeObject(new MyNonSerializable()); // Noncompliant [[sc=23;ec=46]] {{Make the "checks.serialization.MyNonSerializable" class "Serializable" or don't write it.}}
    }
    out.writeObject(array);

    out.writeObject(java.util.Arrays.asList("one", "two"));

    Iterable<String> iterable = new ArrayList<>();
    out.writeObject(iterable);

    Collection<String> collection = new ArrayList<>();
    out.writeObject(collection);

    List<String> list = new ArrayList<>();
    out.writeObject(list);

    Set<String> set = new HashSet<>();
    out.writeObject(set);

    SortedSet<String> sortedSet = new TreeSet<>();
    out.writeObject(sortedSet);

    NavigableSet<String> navigableSet = new TreeSet<>();
    out.writeObject(navigableSet);

    Queue<String> queue = new LinkedList<>();
    out.writeObject(queue);

    Deque<String> deque = new ArrayDeque<>();
    out.writeObject(deque);

    Map<String, String> map = new HashMap<>();
    out.writeObject(map);

    SortedMap<String, String> sortedMap = new TreeMap<>();
    out.writeObject(sortedMap);

    NavigableMap<String, String> navigableMap = new TreeMap<>();
    out.writeObject(navigableMap);

    Enumeration<String> enumeration = Collections.emptyEnumeration();
    out.writeObject(enumeration);
  }

  void shouldNotLeadToStackoverflow(ObjectOutputStream out, Object obj1) throws IOException {
    obj1 = obj1;
    out.writeObject(obj1);
  }

  void shouldNotLeadToStackoverflow2(ObjectOutputStream out, Object obj1, Object obj2) throws IOException {
    Object tmp = obj1;
    obj1 = obj2;
    obj2 = tmp;
    out.writeObject(obj1);
  }
}

class MySerializable implements Serializable {
}

class MyNonSerializable implements Runnable {
  @Override
  public void run() {
  }
}

class ParameterizedObject<T> implements Serializable {

  T t;

  private void writeObject(ObjectOutputStream s) throws java.io.IOException {
    s.writeObject(t);
  }

}

class ParameterizedMyNonSerializable<T extends MyNonSerializable> implements Serializable {

  T t;

  private void writeObject(ObjectOutputStream s) throws java.io.IOException {
    s.writeObject(t); // Noncompliant {{Make the "T" class "Serializable" or don't write it.}}
  }

}

class ParameterizedSerializable<T extends Serializable> implements Serializable {

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
      oos.writeObject(cacheProp); // Compliant, real type is HashMap
      oos.writeObject(cacheVar); // Compliant, real type is null
      oos.writeObject(cacheVar2); // Compliant, real type is HashMap
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }
}
