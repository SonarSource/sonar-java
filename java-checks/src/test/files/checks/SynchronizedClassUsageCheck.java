import java.util.Hashtable;     // Compliant
import java.util.Vector;        // Compliant

class A {
  List a = new Vector();         // Noncompliant {{Replace the synchronized class "Vector" by an unsynchronized one such as "ArrayList" or "LinkedList".}}
  Vector a = new Vector();       // Noncompliant {{Replace the synchronized class "Vector" by an unsynchronized one such as "ArrayList" or "LinkedList".}}
  Hashtable a = new Hashtable(); // Noncompliant {{Replace the synchronized class "Hashtable" by an unsynchronized one such as "HashMap".}}
  Map a = new Hashtable();       // Noncompliant {{Replace the synchronized class "Hashtable" by an unsynchronized one such as "HashMap".}}
  Hashtable a = foo();           // Noncompliant {{Replace the synchronized class "Hashtable" by an unsynchronized one such as "HashMap".}}
  HashMap a = new HashMap();     // Compliant
  ArrayList a = new ArrayList(); // Compliant
  Vector<Integer> a;             // Noncompliant {{Replace the synchronized class "Vector" by an unsynchronized one such as "ArrayList" or "LinkedList".}}
  StringBuffer a = new StringBuffer(); // Noncompliant {{Replace the synchronized class "StringBuffer" by an unsynchronized one such as "StringBuilder".}}
  java.util.Stack a = new java.util.Stack();         // Noncompliant {{Replace the synchronized class "Stack" by an unsynchronized one such as "Deque".}}

  private void f() {
    System.out.println(Vector.class); // OK
    a.call(new java.util.Vector()); // OK
    java.util.Vector<Integer> result = null; // Noncompliant
    List result = new java.util.Vector<Integer>(); // Noncompliant
  }

  public Vector a; // Noncompliant {{Replace the synchronized class "Vector" by an unsynchronized one such as "ArrayList" or "LinkedList".}}

  public java.util.Stack f() { // Noncompliant {{Replace the synchronized class "Stack" by an unsynchronized one such as "Deque".}}
  }

  public void f(Vector a) { // Noncompliant {{Replace the synchronized class "Vector" by an unsynchronized one such as "ArrayList" or "LinkedList".}}
  }

  @Override
  public Vector f(Vector a) { // Compliant
    Vector a; // Noncompliant {{Replace the synchronized class "Vector" by an unsynchronized one such as "ArrayList" or "LinkedList".}}

    try (Vector a = null) { // Noncompliant {{Replace the synchronized class "Vector" by an unsynchronized one such as "ArrayList" or "LinkedList".}}
    }
  }
  public void f(Integer i) { // Compliant
  }
}

interface AInterface {
  void f(Vector a); // Noncompliant {{Replace the synchronized class "Vector" by an unsynchronized one such as "ArrayList" or "LinkedList".}}
}

enum AEnum {
  ;

  Vector a; // Noncompliant {{Replace the synchronized class "Vector" by an unsynchronized one such as "ArrayList" or "LinkedList".}}

  Vector a() { // Noncompliant {{Replace the synchronized class "Vector" by an unsynchronized one such as "ArrayList" or "LinkedList".}}
  }

  @Override a(Vector a) { // Compliant
  }
}

class B {
  class Stack {}
  B() {}
  void foo(Stack stack) { // Compliant
  }
}
