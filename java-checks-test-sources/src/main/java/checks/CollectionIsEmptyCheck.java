package checks;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

class CollectionIsEmptyCheck {
  int[] size = new int[4];

  private void foo(Collection myCollection, Collection myCollection2, CollectionIsEmptyCheck foo, ByteArrayOutputStream baot) {
    boolean b;
    int i;

    b = myCollection.size() == 0; // Noncompliant [[sc=9;ec=33;quickfixes=qf1]] {{Use isEmpty() to check whether the collection is empty or not.}}
    // fix@qf1 {{Use "isEmpty()"}}
    // edit@qf1 [[sc=22;ec=33]] {{isEmpty()}}
    b = myCollection.size() != 0; // Noncompliant [[sc=9;ec=33;quickfixes=qf2]]
    // fix@qf2 {{Use "isEmpty()"}}
    // edit@qf2 [[sc=9;ec=9]] {{!}}
    // edit@qf2 [[sc=22;ec=33]] {{isEmpty()}}
    b = myCollection.size() > 0; // Noncompliant [[sc=9;ec=32;quickfixes=qf3]]
    // fix@qf3 {{Use "isEmpty()"}}
    // edit@qf3 [[sc=9;ec=9]] {{!}}
    // edit@qf3 [[sc=22;ec=32]] {{isEmpty()}}
    b = myCollection.size() >= 1; // Noncompliant [[sc=9;ec=33;quickfixes=qf4]]
    // fix@qf4 {{Use "isEmpty()"}}
    // edit@qf4 [[sc=9;ec=9]] {{!}}
    // edit@qf4 [[sc=22;ec=33]] {{isEmpty()}}
    b = myCollection.size() < 1; // Noncompliant [[sc=9;ec=32;quickfixes=qf5]]
    // fix@qf5 {{Use "isEmpty()"}}
    // edit@qf5 [[sc=22;ec=32]] {{isEmpty()}}
    b = myCollection.size() <= 0; // Noncompliant [[sc=9;ec=33;quickfixes=qf6]]
    // fix@qf6 {{Use "isEmpty()"}}
    // edit@qf6 [[sc=22;ec=33]] {{isEmpty()}}

    b = 0 == myCollection.size(); // Noncompliant [[sc=9;ec=33;quickfixes=qf7]]
    // fix@qf7 {{Use "isEmpty()"}}
    // edit@qf7 [[sc=9;ec=14]] {{}}
    // edit@qf7 [[sc=27;ec=33]] {{isEmpty()}}
    b = 0 != myCollection.size(); // Noncompliant [[sc=9;ec=33;quickfixes=qf8]]
    // fix@qf8 {{Use "isEmpty()"}}
    // edit@qf8 [[sc=9;ec=14]] {{!}}
    // edit@qf8 [[sc=27;ec=33]] {{isEmpty()}}
    b = 0 < myCollection.size(); // Noncompliant [[sc=9;ec=32;quickfixes=qf9]]
    // fix@qf9 {{Use "isEmpty()"}}
    // edit@qf9 [[sc=9;ec=13]] {{!}}
    // edit@qf9 [[sc=26;ec=32]] {{isEmpty()}}
    b = 1 <= myCollection.size(); // Noncompliant [[sc=9;ec=33;quickfixes=qf10]]
    // fix@qf10 {{Use "isEmpty()"}}
    // edit@qf10 [[sc=9;ec=14]] {{!}}
    // edit@qf10 [[sc=27;ec=33]] {{isEmpty()}}
    b = 1 > myCollection.size(); // Noncompliant [[sc=9;ec=32;quickfixes=qf11]]
    // fix@qf11 {{Use "isEmpty()"}}
    // edit@qf11 [[sc=9;ec=13]] {{}}
    // edit@qf11 [[sc=26;ec=32]] {{isEmpty()}}
    b = 0 >= myCollection.size(); // Noncompliant [[sc=9;ec=33;quickfixes=qf12]]
    // fix@qf12 {{Use "isEmpty()"}}
    // edit@qf12 [[sc=9;ec=14]] {{}}
    // edit@qf12 [[sc=27;ec=33]] {{isEmpty()}}

    b = 0 == Arrays.asList("a", "b").size(); // Noncompliant [[sc=9;ec=44;quickfixes=qf13]]
    // fix@qf13 {{Use "isEmpty()"}}
    // edit@qf13 [[sc=9;ec=14]] {{}}
    // edit@qf13 [[sc=38;ec=44]] {{isEmpty()}}

    b = Arrays.asList("a", "b").size() != 0; // Noncompliant [[sc=9;ec=44;quickfixes=qf14]]
    // fix@qf14 {{Use "isEmpty()"}}
    // edit@qf14 [[sc=9;ec=9]] {{!}}
    // edit@qf14 [[sc=33;ec=44]] {{isEmpty()}}

    b = myCollection.size() == +0; // Compliant - corner case should be covered by another rule

    b = myCollection.size() == myCollection2.size(); // Compliant

    b = foo instanceof Object; // Compliant

    b = myCollection.size() == 3; // Compliant
    b = myCollection.size() < 3; // Compliant
    b = myCollection.size() > 3; // Compliant

    b =  3 <= myCollection.size(); // Compliant
    b =  3 >= myCollection.size(); // Compliant
    b =  3 > myCollection.size(); // Compliant
    b =  3 < myCollection.size(); // Compliant
    b =  3 == myCollection.size(); // Compliant
    b =  3 != myCollection.size(); // Compliant

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
