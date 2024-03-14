package checks.serialization;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateful;
import javax.inject.Inject;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.stereotype.Component;

class Address {
}
class Person implements Serializable {
  Address address; // Noncompliant [[sc=11;ec=18]] {{Make "address" transient or serializable.}}
  SerializableFieldInSerializableClassCheckA a;
  static Address address2;//Compliant : static field
  transient Address address3;
}
enum SerializableFieldInSerializableClassCheckA {
  B;
  Address address;
  Address[][] addressArray;
}

class Person2 implements Serializable {
  Address address; //Compliant: read/write methods are implemented
  transient Address address2;
  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {}
  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {}
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

class SerializableFieldInSerializableClassCheckB<T> {}

class Person6<E, F extends Serializable> implements Serializable {
  private SerializableFieldInSerializableClassCheckB<Object> bs; // Noncompliant
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

class Person6_1<T extends MySimpleInterface & Serializable> implements Serializable {
  T t; //Compliant
}

class Person6_2<T extends Serializable & MySimpleInterface> implements Serializable {
  T t; //Compliant
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
    if (ok.isEmpty()) {
      Object myVar = ok;
    }
  }

  Map bar() {
    return null;
  }
}

class Person8 implements Serializable {
  @Inject Address address; // Compliant field is injected
  @jakarta.inject.Inject Address jakartaAddress; // Compliant

  @EJB Address address2; // Compliant

  @Inject Address address3; // Compliant
  @jakarta.inject.Inject Address jakartaAddress4; // Compliant

  @EJB Address address4; // Compliant

  @Deprecated Address address5; // Noncompliant
}

class Person9999 implements Serializable {
  Address address; // Compliant field is injected
  @EJB Address address2; // Compliant

  Address address3; // Compliant
  @EJB Address address4; // Compliant

  @Deprecated Address address5; // Noncompliant

  @Inject
  public Person9999(Address address) {
    this.address = address;
  }

  @Inject
  public void setAddress3(Address address3) {
    this.address3 = address3;
  }
  @jakarta.inject.Inject
  public void jakartaSetAddress3(Address address3) {
    this.address3 = address3;
  }
}

class Person999 implements Serializable {
  Address address; // Noncompliant

  @Inject
  public Person999(Address address) {
  }
}

class JakartaPerson999 implements Serializable {
  Address address; // Noncompliant

  @jakarta.inject.Inject
  public JakartaPerson999(Address address) {
  }
}

class Person99 implements Serializable {
  Address address; // Compliant

  @Inject
  public Person99(Address _address) {
    int i = 0;
    address = _address;
    i = 5;
  }
}

class JakartaPerson99 implements Serializable {
  Address address; // Compliant

  @jakarta.inject.Inject
  public JakartaPerson99(Address _address) {
    int i = 0;
    address = _address;
    i = 5;
  }
}

class Person777 implements Serializable {
  Address address; // Compliant
  Address address1; // Noncompliant

  public Person777(Address _address, Address _address1) {
    int i = 0;
    address = _address;
    address1 = _address1;
    i = 5;
  }

  @Inject
  public Person777(Address _address) {
    int i = 0;
    address = _address;
    i = 5;
  }
}

class JakartaPerson777 implements Serializable {
  Address address; // Compliant
  Address address1; // Noncompliant

  public JakartaPerson777(Address _address, Address _address1) {
    int i = 0;
    address = _address;
    address1 = _address1;
    i = 5;
  }

  @jakarta.inject.Inject
  public JakartaPerson777(Address _address) {
    int i = 0;
    address = _address;
    i = 5;
  }
}

class IncompleteSerializableMethods1 implements Serializable {
  Address address; // Noncompliant - read/write methods are not exactly matching signatures (throwing wrong types)
  private void writeObject(java.io.ObjectOutputStream out) {}
  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException {}
}

class IncompleteSerializableMethods2 implements Serializable {
  Address address; // Noncompliant - write methods is wrongly implemented
  private void writeObject(java.io.ObjectOutputStream out) throws ClassCastException {} // wrong thrown type
  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {}
}
class MyServlet extends javax.servlet.http.HttpServlet {
  private Map<String, String> nok1 = new MyNonSerializableMap<>();
}

class Person9 implements Serializable {
  private HashMap<Object, Object> both2; // Noncompliant
  private ArrayList<Object> objects2; // Noncompliant
  private ArrayList<String> lines = null; // Compliant: ArrayList, String, and null are serializable
}

class MyObject {

}

interface MySimpleInterface {

}

class MyNonSerializableList<E> implements List<E> {
  MyNonSerializableList() {}

  @Override
  public int size() {
    return 0;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public boolean contains(Object o) {
    return false;
  }

  @Override
  public Iterator<E> iterator() {
    return null;
  }

  @Override
  public Object[] toArray() {
    return new Object[0];
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return null;
  }

  @Override
  public boolean add(E e) {
    return false;
  }

  @Override
  public boolean remove(Object o) {
    return false;
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return false;
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    return false;
  }

  @Override
  public boolean addAll(int index, Collection<? extends E> c) {
    return false;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return false;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return false;
  }

  @Override
  public void clear() {

  }

  @Override
  public E get(int index) {
    return null;
  }

  @Override
  public E set(int index, E element) {
    return null;
  }

  @Override
  public void add(int index, E element) {

  }

  @Override
  public E remove(int index) {
    return null;
  }

  @Override
  public int indexOf(Object o) {
    return 0;
  }

  @Override
  public int lastIndexOf(Object o) {
    return 0;
  }

  @Override
  public ListIterator<E> listIterator() {
    return null;
  }

  @Override
  public ListIterator<E> listIterator(int index) {
    return null;
  }

  @Override
  public List<E> subList(int fromIndex, int toIndex) {
    return null;
  }
}

class MyNonSerializableMap<K, V> implements Map<K, V> {
  MyNonSerializableMap() {}

  @Override
  public int size() {
    return 0;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public boolean containsKey(Object key) {
    return false;
  }

  @Override
  public boolean containsValue(Object value) {
    return false;
  }

  @Override
  public V get(Object key) {
    return null;
  }

  @Override
  public V put(K key, V value) {
    return null;
  }

  @Override
  public V remove(Object key) {
    return null;
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {

  }

  @Override
  public void clear() {

  }

  @Override
  public Set<K> keySet() {
    return null;
  }

  @Override
  public Collection<V> values() {
    return null;
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return null;
  }
}

class MyAbstractNonSerializableMap<K,V> extends MyNonSerializableMap<K,V> {
  static MyAbstractNonSerializableMap foo() {
    return null;
  }
}

class A implements java.io.Serializable {
  private NonSerializableInterface field = new SerializableImpl(); // Noncompliant
  private final NonSerializableInterface field1 = new SerializableImpl(); // Compliant, is final
  private NonSerializableInterface field2; // Noncompliant
  private NonSerializableInterface field3 = init(); // Noncompliant
  private NonSerializableInterface field4 = initWithSerializable(); // Noncompliant, is not final
  private final NonSerializableInterface field5 = initWithSerializable(); // Compliant, is final
  private final NonSerializableInterface field6 = init(); // Noncompliant

  private NonSerializableInterface init() {
    throw new RuntimeException();
  }

  private SerializableImpl initWithSerializable() {
    throw new RuntimeException();
  }

  void mess() {
    field = null;
    field4 = null;
  }
}

class SerializableImpl implements NonSerializableInterface, java.io.Serializable {
  @Override public void doSomething() { }
}

interface NonSerializableInterface {
  void doSomething();
}

@Component
class MyBean {
}

class WicketComponentWithSpringBean extends GenericPanel<String> {

  @SpringBean
  private MyBean myBean; // Compliant

  public WicketComponentWithSpringBean(String id) {
    super(id);
  }
}

class GuavaImmutable implements Serializable {
  ImmutableList<String> immutableList; // Compliant
  ImmutableSet<String> immutableSet; // Compliant
  ImmutableCollection<String> immutableCollection; // Compliant
  ImmutableMap<String, String> immutableMap; // Compliant
  ImmutableBiMap<String, String> immutableBiMap; // Compliant
  ImmutableMultiset<String> immutableMultiset; // Compliant

  ImmutableRangeMap<String, String> immutableRangeMap; // Compliant
  ImmutableRangeSet<String> immutableRangeSet; // Compliant
  ImmutableTable<String, String, String> immutableTable; // Compliant
  ImmutableMultimap<String, String> immutableMultimap; // Compliant
}

@Stateful
class ResourceAnnotationMakesClassSerializable implements Serializable
{
  @Resource
  private Object first; // Compliant
  @javax.annotation.Resource
  private Object second; // Compliant

  @jakarta.annotation.Resource
  private Object third; // Compliant
}
