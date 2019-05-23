import com.google.common.collect.Lists;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class A {
  private void myMethod() {
    List<String> myList = new ArrayList<String>();
    List<Unknown> unknownList = new ArrayList<Unknown>();
    Set<A> myASet = new HashSet<A>();
    ArrayList<B> myBList = new ArrayList<B>();
    List<Set<Integer>> mySetList = new ArrayList<Set<Integer>>();
    List<Number> myNumberList = new ArrayList<Number>();

    Integer myInteger = Integer.valueOf(1);
    String myString = "";
    String[] myArrayString = new String[] {"myString"};
    Integer[] myArrayInteger = new Integer[] {Integer.valueOf(1)};

    myList.contains(myInteger); // Noncompliant [[sc=12;ec=20]] {{A "List<String>" cannot contain a "Integer"}}
    unknownList.contains(myInteger);
    myList.remove(myInteger); // Noncompliant {{A "List<String>" cannot contain a "Integer"}}
    myList.contains(myString); // Compliant
    myBList.contains(myInteger); // Noncompliant {{A "ArrayList<B>" cannot contain a "Integer"}}
    mySetList.contains(myString); // Noncompliant {{A "List<Set>" cannot contain a "String"}}
    mySetList.contains(returnOne()); // Noncompliant {{A "List<Set>" cannot contain a "Integer"}}
    mySetList.remove(B.returnOne()); // Noncompliant {{A "List<Set>" cannot contain a "Integer"}}
    myBList.contains(new B()); // Compliant
    myBList.remove(new A()); // Compliant
    myList.contains(myArrayInteger); // Noncompliant {{A "List<String>" cannot contain a "Integer[]"}}
    myList.remove(myArrayInteger[0]); // Noncompliant {{A "List<String>" cannot contain a "Integer"}}
    myList.remove(myArrayString[0]); // Compliant
    myASet.contains(new C()); // Compliant
    myASet.remove(new B()); // Compliant
    myNumberList.contains(myInteger); // Compliant
    mySetList.contains(unknownMethod()); // Compliant

    myUnknownCollection.stream().filter(s -> myASet.contains(s.toString())).collect(Collectors.toSet()); // Compliant
  }

  private static Integer returnOne() {
    return Integer.valueOf(1);
  }
}

class B extends A {
  public String value;

  public static Integer returnOne() {
    return Integer.valueOf(1);
  }
}

class C extends B {

  private void myOtherMethod() {
    Set mySet = new HashSet<B>();
    A myA = new A();

    mySet.contains(myA); // Compliant
    mySet.remove(new B()); // Compliant
    
    List<Integer> myIntegerList = new ArrayList<Integer>();
    myIntegerList.contains(0); // Compliant (boxing)
    myIntegerList.remove(0L); // Noncompliant {{A "List<Integer>" cannot contain a "long"}}
    
    List<String> myStringList = new ArrayList<String>();
    myStringList.contains(0); // Noncompliant {{A "List<String>" cannot contain a "int"}}
    myStringList.contains(new Object()); // Compliant
    
    List<String[]> myListArrayString = new ArrayList<String[]>();
    myListArrayString.contains("myString"); // Noncompliant
  }
}

class D {
  List myList = Lists.newArrayList(1);
  void myMethod() {
    myList.contains(1); // Compliant
  }
}

class MyCollection<E> extends ArrayList<E> {

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

class mySet<E> extends AbstractSet<E> {

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

class F<T> {
  java.util.Vector<F<String>> vectors;
  Set<Class> set;
  void f() {
    F f;
    vectors.contains(f);
    Class<?> clazz;
    set.contains(clazz);
  }

}

class G {
  <E> void foo(Set<? extends E> set, Object o) {
    set.contains(o); // Compliant
  }
  <E> void foo2(Set<? extends E> set, E o) {
    set.contains(o);
  }
}

class H<K, V> {
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

class J {
  void foo(Set<J> s) {
    gul(new J()).remove(new G()); // compliant
  }

  static <K> set<K> gul(K k) {
    return null;
  }
}
class Test {
  private List<String> badGrades = null;

  public String testSplitArray() {
    badGrades = java.util.Arrays.asList("C", "D", "E");

    List<String> myGrades = Arrays.asList("A,B,A", "A,C,A", "A,D,E");
    String test = myGrades.stream().
      map(grade -> unknownMethod()).
      filter(grade -> badGrades.contains(grade)). // compliant, type inference is unable to resolve type of grade because of unknownMethod
      findFirst().orElse("Nothing");
  }
}
class SomeClass<T> {}
class WrongCollectionElement<K> extends ArrayList<SomeClass<K>> {
  SomeClass<K> field;
  void foo() {
    remove(field); // compliant type of collection is SomeClass<K>
  }
  @Override
  public boolean remove(Object o) {
    return true;
  }
}

class LombokVal {
  boolean foo(List<String> words) {
    lombok.val y = "Hello World";
    return  words.contains(y); // Noncompliant - FP - handled by lombok filter
  }
}
