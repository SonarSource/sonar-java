class A {
  List a = new Vector();         // Non-Compliant
  Vector a = new Vector();       // Non-Compliant
  Hashtable a = new Hashtable(); // Non-Compliant
  Map a = new Hashtable();       // Non-Compliant
  Hashtable a = foo();           // Non-Compliant
  HashMap a = new HashMap();     // Compliant
  ArrayList a = new ArrayList(); // Compliant
}
