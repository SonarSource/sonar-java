import com.google.common.collect.Lists;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

class A {
  private void myMethod() {
    List<String> myList = new ArrayList<String>();
    Set<A> myASet = new HashSet<A>();
    ArrayList<B> myBList = new ArrayList<B>();
    List<Set<Integer>> mySetList = new ArrayList<Set<Integer>>();
    List<Number> myNumberList = new ArrayList<Number>();

    Integer myInteger = Integer.valueOf(1);
    String myString = "";
    String[] myArrayString = new String[] {"myString"};
    Integer[] myArrayInteger = new Integer[] {Integer.valueOf(1)};

    myList.contains(myInteger); // Noncompliant. Always false.
    myList.remove(myInteger); // Noncompliant. list.add(iger) doesn't compile, so this will always return false
    myList.contains(myString); // Compliant
    myBList.contains(myInteger); // Noncompliant
    mySetList.contains(myString); // Noncompliant
    mySetList.contains(returnOne()); // Noncompliant
    mySetList.remove(B.returnOne()); // Noncompliant
    myBList.contains(new B()); // Compliant
    myBList.remove(new A()); // Compliant
    myList.contains(myArrayInteger); // Noncompliant - False negative
    myList.remove(myArrayInteger[0]); // Noncompliant
    myList.remove(myArrayString[0]); // Compliant
    myASet.contains(new C()); // Compliant
    myASet.remove(new B()); // Compliant
    myNumberList.contains(myInteger); // Compliant
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
    myIntegerList.remove(0L); // Noncompliant
    
    List<String> myStringList = new ArrayList<String>();
    myStringList.contains(0); // Noncompliant
    myStringList.contains(new Object()); // Compliant
    
    List<String[]> myListArrayString = new ArrayList<String[]>();
    myListArrayString.contains("myString"); // Compliant - False positive
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
}
