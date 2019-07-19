import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ejb.EJB;

class Address {
}
class Person implements Serializable {
  Address address; // Noncompliant [[sc=11;ec=18]] {{Make "address" transient or serializable.}}
  A a;
  UnknownField unknownField;
  static Address address2;//Compliant : static field
  transient Address address3;
}
enum A {
  B;
  Address address;
  Address[][] addressArray;
}

class Person2 implements Serializable {
  Address address; //Compliant: read/write methods are implemented
  transient Address address2;
  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {}
  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException {}
}
interface MyCustomInterface extends Serializable {}
class Person3 implements MyCustomInterface {
  Address address; // Noncompliant {{Make "address" transient or serializable.}}
}
class Person4<T extends Serializable, S extends Address> implements MyCustomInterface {
  T t; //Compliant
  S s; // Noncompliant {{Make "s" transient or serializable.}}
}
class Person5 implements Serializable {
  int[][] matrix; //Compliant
  Integer integer; //Compliant
}

class B<T> {}

class Person6<E, F extends Serializable> implements Serializable {
  private B<Objects> bs; // Noncompliant
  private List<Person6> persons; // Compliant
  private List things; // Noncompliant {{Make "things" transient or serializable.}}
  private List<MyObject> objects; // Noncompliant {{Make "objects" transient or serializable.}}
  private List<? extends MyObject> otherObjects; // Noncompliant {{Make "otherObjects" transient or serializable.}}
  private List<? extends Person6> otherPersons; // Compliant
  private List<? extends E> otherThings; // Noncompliant {{Make "otherThings" transient or serializable.}}
  private List<? extends F> otherSerializableThings; // Compliant
  private List<?> otherUnknown; // Noncompliant {{Make "otherUnknown" transient or serializable.}}
  private List<? super F> super1;
  private List<? super E> super2; // Noncompliant

  public List<Person6> persons1; // Noncompliant {{Make "persons1" private or transient.}}
  transient public List<Person6> persons2; // Compliant - transient
  private List<Person6> persons3 = new ArrayList<>(); // Compliant - ArrayList is serializable
  private List<Person6> persons4 = new MyNonSerializableList<>(); // Noncompliant
}

class Person7 implements Serializable {
  private Map<Object, Object> both; // Noncompliant {{Make "both" transient or serializable.}}
  private Map<String, Object> right; // Noncompliant {{Make "right" transient or serializable.}}
  private Map<Object, String> left; // Noncompliant {{Make "left" transient or serializable.}}
  private Map<String, String> ok; // Compliant

  private Map<String, List<String>> nestedOk; // Compliant
  private Map<String, List<Object>> nestedLeft; // Noncompliant {{Make "nestedLeft" transient or serializable.}}

  private Map<String, String> nok1 = new MyNonSerializableMap<>(); // Noncompliant
  private MyNonSerializableMap<String, String> nok2; // Noncompliant

  void foo() {
    ok = new MyNonSerializableMap<>(); // Noncompliant
    nok2 = new MyNonSerializableMap<>(); // Noncompliant
    ok = nok2; // Noncompliant
    ok = null; // Compliant
    ok = bar(); // Compliant
    ok = MyAbstractNonSerializableMap.foo(); // Noncompliant
    ok = new HashMap<>(); // Compliant
    ok = unknown(); // Compliant
    if (ok.isEmpty()) {
      Object myVar = ok;
    }
  }

  Map bar() {
    return null;
  }
}

class Person8 implements Serializable {
  @javax.inject.Inject Address address; // Compliant field is injected
  @javax.ejb.EJB Address address2; // Compliant

  @Inject Address address3; // Compliant
  @EJB Address address4; // Compliant

  @Deprecated Address address5; // Noncompliant
}

class MyObject {

}

class MyNonSerializableList<E> implements List<E> {
  MyNonSerializableList() {}
}

class MyNonSerializableMap<K, V> implements Map<K, V> {
  MyNonSerializableMap() {}
}

abstract class MyAbstractNonSerializableMap<K,V> extends MyNonSerializableMap<K,V> {
  static MyAbstractNonSerializableMap foo() {
    return null;
  }
}

class IncompleteSerializableMethods1 implements Serializable {
  Address address; // Noncompliant - read/write methods are not exactly matching signatures (throwing wrong types)
  private void writeObject(java.io.ObjectOutputStream out) {}
  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException {}
}

class IncompleteSerializableMethods2 implements Serializable {
  Address address; // Noncompliant - write methods is wrongly implemented
  private void writeObject(java.io.ObjectOutputStream out) throws java.lang.ClassCastException {} // wrong thrown type
  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException {}
}
public class MyServlet extends javax.servlet.http.HttpServlet {
  private Map<String, String> nok1 = new MyNonSerializableMap<>();
}

class test implements Serializable {
  private HashMap<Object, Object> both2; // Noncompliant
  private ArrayList<Object> objects2; // Noncompliant
  private ArrayList<String> lines = null; // Compliant: ArrayList, String, and null are serializable
}
