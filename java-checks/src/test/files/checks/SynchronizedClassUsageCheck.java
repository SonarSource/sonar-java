import java.util.Hashtable;     // Compliant
import java.util.Vector;        // Compliant

class A {
  List a = new Vector();         // Non-Compliant
  Vector a = new Vector();       // Non-Compliant
  Hashtable a = new Hashtable(); // Non-Compliant
  Map a = new Hashtable();       // Non-Compliant
  Hashtable a = foo();           // Non-Compliant
  HashMap a = new HashMap();     // Compliant
  ArrayList a = new ArrayList(); // Compliant
  Vector<Integer> a;             // Non-Compliant
  StringBuffer a = new StringBuffer(); // Non-Compliant
  Stack a = new Stack();         // Non-Compliant

  private void f() {
    System.out.println(Vector.class); // Non-Compliant
    System.out.println(new java.util.Vector()); // Non-Compliant
    java.util.Vector<Integer> result = null; // Non-Compliant
    List result = new java.util.Vector<Integer>(); // Non-Compliant
  }

  public Vector a; // Non-Compliant

  public Vector f() { // Non-Compliant
  }

  public void f(Vector a) { // Non-Compliant
  }

  @Override
  public Vector f(Vector a) { // Compliant
    Vector a; // Non-Compliant

    try (Vector a = null) { // Non-Compliant
    }
  }
}

interface A {
  void f(Vector a); // Non-Compliant
}

enum A {
  ;

  Vector a; // Non-Compliant

  Vector a() { // Non-Compliant
  }

  @Override a(Vector a) { // Compliant
  }
}
