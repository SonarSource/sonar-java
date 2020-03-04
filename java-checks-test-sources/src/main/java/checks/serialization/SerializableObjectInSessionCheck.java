package checks.serialization;

import java.io.Externalizable;
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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.dom4j.io.SAXEventRecorder;

import static java.util.Collections.EMPTY_LIST;

class SerializableObjectInSessionCheck {

  void foo(HttpServletRequest request) {
    HttpSession session = request.getSession();
    session.setAttribute("address", new Address()); // Noncompliant [[sc=37;ec=50]] {{Make "Address" serializable or don't store it in the session.}}
    session.setAttribute("person", new Person()); // Noncompliant {{Make "Person" serializable or don't store it in the session.}}
    session.setAttribute("person", 1);
    session.setAttribute("person", new Integer(1));
    session.setAttribute("addressString", "address");

    session.setAttribute("intArray", new int[] { 1, 2 });
    session.setAttribute("stringArray", new String[] { "one", "two" });
    session.setAttribute("personArray", new Person[] { new Person() }); // Noncompliant {{Make "Person[]" serializable or don't store it in the session.}}

    session.setAttribute("stringArrayList", new java.util.ArrayList<>(java.util.Arrays.asList("one", "two")));
    session.setAttribute("personArrayList", new java.util.ArrayList<>(java.util.Arrays.asList(new Person(), new Person()))); // Noncompliant {{Make "ArrayList" and its parameters serializable or don't store it in the session.}}

    session.setAttribute("stringList", java.util.Arrays.asList("one", "two"));

    session.setAttribute("nonSerializableParameterized", new CustomStack<String>()); // Noncompliant {{Make "CustomStack" and its parameters serializable or don't store it in the session.}}

    Iterable<String> iterable = new ArrayList<>();
    session.setAttribute("name", iterable);

    Collection<String> collection = new ArrayList<>();
    session.setAttribute("name", collection); // Compliant

    List<String> list = new ArrayList<>();
    session.setAttribute("name", list); // Compliant

    Set<String> set = new HashSet<>();
    session.setAttribute("name", set); // Compliant

    SortedSet<String> sortedSet = new TreeSet<>();
    session.setAttribute("name", sortedSet); // Compliant

    NavigableSet<String> navigableSet = new TreeSet<>();
    session.setAttribute("name", navigableSet); // Compliant

    Queue<String> queue = new LinkedList<>();
    session.setAttribute("name", queue); // Compliant

    Deque<String> deque = new ArrayDeque<>();
    session.setAttribute("name", deque); // Compliant

    Map<String, String> map = new HashMap<>();
    session.setAttribute("name", map);

    SortedMap<String, String> sortedMap = new TreeMap<>();
    session.setAttribute("name", sortedMap);

    NavigableMap<String, String> navigableMap = new TreeMap<>();
    session.setAttribute("name", navigableMap);

    Enumeration<String> enumeration = Collections.emptyEnumeration();
    session.setAttribute("name", enumeration);

    Externalizable externalizable = new SAXEventRecorder();
    session.setAttribute("name", externalizable);

    // EMPTY_LIST declaration is not in this file, symbol.declaration is null
    session.setAttribute("name", EMPTY_LIST);

    Class<?> serializableClass;
    serializableClass =  String.class;
    session.setAttribute("name",  serializableClass);

    Class<?> notSerializableClass;
    notSerializableClass = Void.class;
    session.setAttribute("name",  notSerializableClass); // Noncompliant {{Make "Class" and its parameters serializable or don't store it in the session.}}
  }

  public class Address {
  }
  public class Person {
  }
  public class CustomStack<E> {
  }
}
