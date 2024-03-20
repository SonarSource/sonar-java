package checks;

import com.google.common.collect.Lists;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;

public class CollectionInappropriateCallsCheckSample {
  private void myMethod() {
    List<String> myList = new ArrayList<String>();
    Set<A> myASet = new HashSet<A>();
    ArrayList<B> myBList = new ArrayList<B>();
    List<Set<Integer>> mySetList = new ArrayList<Set<Integer>>();
    List<Number> myNumberList = new ArrayList<Number>();
    List notTypedList = new ArrayList();

    Integer myInteger = Integer.valueOf(1);
    String myString = "";
    String[] myArrayString = new String[] {"myString"};
    Integer[] myArrayInteger = new Integer[] {Integer.valueOf(1)};

    myList.contains(myInteger); // Noncompliant [[sc=12;ec=20]] {{A "List<String>" cannot contain a "Integer".}}
    myList.remove(myInteger); // Noncompliant {{A "List<String>" cannot contain a "Integer".}}
    myList.removeAll(myNumberList); // Noncompliant {{A "List<String>" cannot contain a "Number".}}
    myList.removeAll(Arrays.asList("a", "b"));
    mySetList.removeAll(mySetList);
    mySetList.removeAll(mySetList.get(0)); // Noncompliant {{A "List<Set>" cannot contain a "Integer".}}
    mySetList.removeAll(null);
    mySetList.removeAll(notTypedList);
    myList.contains(myString); // Compliant
    myBList.contains(myInteger); // Noncompliant {{A "ArrayList<B>" cannot contain a "Integer".}}
    mySetList.contains(myString); // Noncompliant {{A "List<Set>" cannot contain a "String".}}
    mySetList.contains(returnOne()); // Noncompliant {{A "List<Set>" cannot contain a "Integer".}}
    mySetList.remove(B.returnOne()); // Noncompliant {{A "List<Set>" cannot contain a "Integer".}}
    myBList.contains(new B()); // Compliant
    myBList.remove(new A()); // Compliant
    myList.contains(myArrayInteger); // Noncompliant {{A "List<String>" cannot contain a "Integer[]".}}
    myList.remove(myArrayInteger[0]); // Noncompliant {{A "List<String>" cannot contain a "Integer".}}
    myList.remove(myArrayString[0]); // Compliant
    myASet.contains(new C()); // Compliant
    myASet.remove(new B()); // Compliant
    myNumberList.contains(myInteger); // Compliant

    Map<A, String> mapAString = new HashMap<>();
    mapAString.containsKey(null);
    mapAString.containsKey("key"); // Noncompliant {{A "Map<A, String>" cannot contain a "String" in a "A" type.}}
    mapAString.containsValue("val");
    mapAString.get("key"); // Noncompliant {{A "Map<A, String>" cannot contain a "String" in a "A" type.}}
    mapAString.getOrDefault("key", "val"); // Noncompliant {{A "Map<A, String>" cannot contain a "String" in a "A" type.}}
    mapAString.remove("key"); // Noncompliant {{A "Map<A, String>" cannot contain a "String" in a "A" type.}}
    mapAString.remove(new A(), "val");
    mapAString.remove(new A(), new A()); // Noncompliant {{A "Map<A, String>" cannot contain a "A" in a "String" type.}}
    mapAString.remove("key", new A()); // Noncompliant
                                       // Noncompliant@-1
    mapAString.remove("key", "val"); // Noncompliant {{A "Map<A, String>" cannot contain a "String" in a "A" type.}}

    Map<String, A> mapStringA = new HashMap<>();
    mapStringA.containsKey("key");
    mapStringA.containsValue("val"); // Noncompliant {{A "Map<String, A>" cannot contain a "String" in a "A" type.}}
    mapStringA.get("key");
    mapStringA.getOrDefault("key", new A());
    mapStringA.remove("key");
    mapStringA.remove(new A(), "val"); // Noncompliant
                                       // Noncompliant@-1
    mapStringA.remove(new A(), new A()); // Noncompliant {{A "Map<String, A>" cannot contain a "A" in a "String" type.}}
    mapStringA.remove("key", new A());
    mapStringA.remove("key", "val"); // Noncompliant {{A "Map<String, A>" cannot contain a "String" in a "A" type.}}
  }

  void streamLambdaWithRawtypeCollection(String myString, Collection<String> myCol) {
    ((Collection) myCol).stream().filter(item -> {
      return myCol.contains(item);
    });
  }

  private static Integer returnOne() {
    return Integer.valueOf(1);
  }

  static class A {

  }

  static class B extends A {
    public String value;

    public static Integer returnOne() {
      return Integer.valueOf(1);
    }
  }

  static class C extends B {

    private void myOtherMethod() {
      Set mySet = new HashSet<B>();
      A myA = new A();

      mySet.contains(myA); // Compliant
      mySet.remove(new B()); // Compliant

      List<Integer> myIntegerList = new ArrayList<Integer>();
      myIntegerList.contains(0); // Compliant (boxing)
      myIntegerList.remove(0L); // Noncompliant {{A "List<Integer>" cannot contain a "long".}}
      myIntegerList.indexOf(0L); // Noncompliant {{A "List<Integer>" cannot contain a "long".}}
      myIntegerList.lastIndexOf(0L); // Noncompliant {{A "List<Integer>" cannot contain a "long".}}

      List<String> myStringList = new ArrayList<String>();
      myStringList.contains(0); // Noncompliant {{A "List<String>" cannot contain a "int".}}
      myStringList.contains(new Object()); // Compliant

      List<String[]> myListArrayString = new ArrayList<String[]>();
      myListArrayString.contains("myString"); // Noncompliant
    }
  }

  static class D {
    List myList = Lists.newArrayList(1);
    void myMethod() {
      myList.contains(1); // Compliant
    }
  }

  static class MyCollection<E> extends ArrayList<E> {

    @Override
    public boolean add(E e) {
      if (contains(e)) { // Compliant
        return false;
      }
      return super.add(e);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      MyCollection<D> myColl = new MyCollection<D>();
      myColl.add(new D());
      for (D d : myColl) {
        c.contains(d); // Compliant
      }
      return super.removeAll(c);
    }
  }

  static class mySet<E> extends AbstractSet<E> {

    LinkedList<E> elements;

    @Override
    public Iterator<E> iterator() {
      return null;
    }

    @Override
    public int size() {
      return 0;
    }

    @Override
    public boolean add(E e) {
      if (!elements.contains(e)) { // Compliant
        return elements.add(e);
      }
      return false;
    }

    void deepToString(Object[] a, Set<Object[]> dejaVu) {
      dejaVu.remove(a);
    }
  }

  static class F<T> {
    java.util.Vector<F<String>> vectors;
    Set<Class> set;
    void f(F f, Class<?> clazz) {
      vectors.contains(f);
      set.contains(clazz);
    }

  }

  static class G {
    <E> void foo(Set<? extends E> set, Object o) {
      set.contains(o); // Compliant
    }
    <E> void foo2(Set<? extends E> set, E o) {
      set.contains(o);
    }
  }

  static class H<K, V> {
    static class Entry<K, V> {
    }

    void foo(Entry<?, ?> entry, Set<Entry<K, V>> entries) {
      entries.remove(entry); // Compliant
    }

    void bar(Set<Entry<K, V>> entries, Entry<?, ?> entry) {
      entries.remove(qix(entry)); // Compliant
    }

    static <K, V> Entry<K, V> qix(Entry<? extends K, ? extends V> entry) {
      return null;
    }
  }

  static class J {
    void foo(Set<J> s) {
      gul(new J()).remove(new G()); // compliant
    }

    static <K> Set<K> gul(K k) {
      return null;
    }
  }
  static class SomeClass<T> {}
  static class WrongCollectionElement<K> extends ArrayList<SomeClass<K>> {
    SomeClass<K> field;
    void foo() {
      remove(field); // compliant type of collection is SomeClass<K>
    }
    @Override
    public boolean remove(Object o) {
      return true;
    }
  }

  static class ClassUsage {
    boolean example(java.util.Set<? extends Class<?>> s, Class<? extends ClassUsage> c) {
      return s.contains(c); // Compliant
    }
  }

  static class WildcardUsage {
    static class InPredicate<T> implements Predicate<T> {
      private Collection<?> target;

      @Override
      public boolean test(T t) {
        return target.contains(t); // Compliant
      }
    }

    abstract static class EntrySet<K, V> implements Set<Entry<K, V>> {
      Set<Entry<K, V>> esDelegate;

      @Override
      public boolean remove(Object object) {
        Entry<?, ?> entry = (Entry<?, ?>) object;
        Entry<K, V> e2 = (Entry<K, V>) object;
        return esDelegate.remove(entry); // Compliant
      }
    }
  }

  static class FlatMapUsage {
    public void test(Set<Integer> used, List<List<Integer>> allInts) {
      allInts.stream()
        .flatMap(List::stream)
        .filter(i -> !used.contains(i)); // Compliant - list of lists is flattened into a list of integers
    }
  }

  static class Animal {}
  static class Color {}
  abstract static class ColorList implements java.util.List<Color> {}
  abstract static class ColorAnimalMap implements java.util.Map<Color, Animal> {}
  abstract static class SpecificColorList extends ColorList {}

  static class TestColorList {
    void foo(ColorList colors, SpecificColorList specificColors, ColorAnimalMap colorAnimalMap, Color c, Animal a) {
      colors.remove(c);
      specificColors.remove(c);
      specificColors.remove(c);
      specificColors.contains(c);
      specificColors.indexOf(c);
      specificColors.lastIndexOf(c);
      colorAnimalMap.get(c);

      colors.remove(a); // Noncompliant {{"ColorList" is a "Collection<Color>" which cannot contain a "Animal".}}
      specificColors.remove(a); // Noncompliant {{"SpecificColorList" is a "Collection<Color>" which cannot contain a "Animal".}}
      specificColors.contains(a); // Noncompliant {{"SpecificColorList" is a "Collection<Color>" which cannot contain a "Animal".}}
      specificColors.indexOf(a); // Noncompliant {{"SpecificColorList" is a "List<Color>" which cannot contain a "Animal".}}
      specificColors.lastIndexOf(a); // Noncompliant {{"SpecificColorList" is a "List<Color>" which cannot contain a "Animal".}}
      colorAnimalMap.get(a); // Noncompliant {{"ColorAnimalMap" is a "Map<Color, Animal>" which cannot contain a "Animal" in a "Color" type.}}
    }
  }

  class AutoBoxingToNumber {
    void simpleList() {
      List<Integer> integerList = new ArrayList<>();
      integerList.contains(1); // Compliant, auto-boxing to Integer
      integerList.contains(Integer.valueOf(1)); // Compliant, auto-boxing to Integer
      integerList.contains(1.2); // Noncompliant {{A "List<Integer>" cannot contain a "double".}}
      integerList.contains(Double.valueOf(1.2)); // Noncompliant {{A "List<Integer>" cannot contain a "Double".}}

      List<Number> numberList = new ArrayList<>();
      numberList.contains(1); // Compliant, auto-boxing to Integer, which is a subtype of Number.
      numberList.contains(Integer.valueOf(1)); // Compliant
      numberList.contains(Double.valueOf(1.2)); // Compliant
      numberList.contains(1.2); // Compliant, auto-boxing to Double, which is a subtype of Number.
    }

    void mapWithNumberAsKey(int intArgs, double doubleArg) {
      Map<Number, String> testMap = new HashMap<>();
      testMap.put(Integer.valueOf(1), "one");
      if (testMap.containsKey(intArgs)) { // Compliant, auto-boxing to Integer, which is a subtype of Number.
        testMap.get(intArgs); // Compliant, auto-boxing to Integer, which is a subtype of Number.
        testMap.get(doubleArg); // Compliant, auto-boxing to Integer, which is a subtype of Number.
      }
    }
  }
}
