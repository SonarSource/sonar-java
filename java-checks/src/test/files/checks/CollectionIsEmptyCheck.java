import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;

class A {
  int[] size = new int[4];

  private void foo(Collection myCollection, Collection myCollection2, A foo, ByteArrayOutputStream baot) {
    boolean b;
    int i;
    
    b = myCollection.size() == 0; // Noncompliant {{Use isEmpty() to check whether the collection is empty or not.}} [[sc=9;ec=33]]
    b = myCollection.size() != 0; // Noncompliant
    b = myCollection.size() > 0; // Noncompliant
    b = myCollection.size() >= 1; // Noncompliant
    b = myCollection.size() < 1; // Noncompliant
    b = myCollection.size() <= 0; // Noncompliant [[sc=9;ec=33]]

    b = 0 == myCollection.size(); // Noncompliant
    b = 0 != myCollection.size(); // Noncompliant
    b = 0 < myCollection.size(); // Noncompliant
    b = 1 <= myCollection.size(); // Noncompliant
    b = 1 > myCollection.size(); // Noncompliant
    b = 0 >= myCollection.size(); // Noncompliant

    b = myCollection.size() == +0; // Compliant - corner case should be covered by another rule

    b = myCollection.size() == myCollection2.size(); // Compliant

    b = foo instanceof Object; // Compliant

    b = myCollection.size() == 3; // Compliant
    b = myCollection.size() < 3; // Compliant
    b = myCollection.size() > 3; // Compliant

    b = 0 < 3; // Compliant
    b = 1 + 1 < 3; // Compliant

    b = myCollection.isEmpty();
    b = !myCollection.isEmpty();
    b = myCollection.size() == 1;

    b = 1 + 1 == 0; // Compliant
    b = foo.size[0] == 0; // Compliant

    b = size() == 0; // Compliant
    b = foo.bar() == 0; // Compliant

    b = foo.col().size() == 0; // Noncompliant
    
    b = baot.size() == 0; // Compliant
    b = foo.size() == 0; // Compliant
    
    i = myCollection.size() & 0; // Compliant
  }
  
  private int bar() {
    return 0;
  }

  private Collection col() {
    return new ArrayList<>();
  }

  private int size() {
    return 0;
  }
}

class MyCollection<E> extends ArrayList<E> {
  boolean foo() {
    return size() == 0; // Compliant
  }
  
  class MyInnerClass {
    Collection myCollection;
    
    boolean bar() {
      return myCollection.size() == 0; // Noncompliant
    }
  }
}