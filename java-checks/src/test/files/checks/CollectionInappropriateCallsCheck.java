import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class A {
  private void myMethod() {
    List<String> myList = new ArrayList<String>();
    Set<A> myASet = new HashSet<A>();
    ArrayList<B> myBList = new ArrayList<B>();
    List<Set<Integer>> mySetList = new ArrayList<Set<Integer>>();

    Integer myInteger = Integer.valueOf(1);
    String myString = "";
    String[] myArrayString = new String[] {"myString"};
    Integer[] myArrayInteger = new Integer[] {Integer.valueOf(1)};

    if (myList.contains(myInteger)) { // Noncompliant. Always false.
      myList.remove(myInteger); // Noncompliant. list.add(iger) doesn't compile, so this will always return false
    }

    if (myList.contains(myString)) { // Compliant
      myList.remove(myString); // Compliant
    }

    if (myBList.contains(myInteger)) { // Noncompliant
      myBList.remove(myInteger); // Noncompliant
    }

    if (mySetList.contains(myString)) { // Noncompliant
      mySetList.remove(myString); // Noncompliant
    }

    if (mySetList.contains(returnOne())) { // Noncompliant
      mySetList.remove(B.returnOne()); // Noncompliant
    }

    if (myBList.contains(new B())) { // Compliant
      myBList.remove(new A()); // Noncompliant
    }

    if (myList.contains(myArrayInteger)) { // Noncompliant - False negative
      myList.remove(myArrayInteger[0]); // Noncompliant
      myList.remove(myArrayString[0]); // Compliant
    }

    if (myASet.contains(new C())) { // Compliant
      myASet.remove(new B()); // Compliant
    }
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

    if (mySet.contains(myA)) { // Compliant
      mySet.remove(new B()); // Compliant
    }
    
    List<Integer> myIntegerList = new ArrayList<Integer>();
    if (myIntegerList.contains(0)) { // Compliant (boxing)
      myIntegerList.remove(0); // Compliant (boxing)
      myIntegerList.remove(0L); // Noncompliant
    }
    
    List<String> myStringList = new ArrayList<String>();
    if (myStringList.contains(0)) { // Noncompliant
      // do nothing
    }
  }
}

class D {
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
      if (c.contains(d)) { // Compliant
        // do nothing
      }
    }
    return super.removeAll(c);
  }

}
