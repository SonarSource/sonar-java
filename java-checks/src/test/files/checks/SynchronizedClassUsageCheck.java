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
}
