package checks;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.chrono.JapaneseDate;
import java.util.Optional;

class ValueBasedObjectIdentityCheckSample {

  void nonCompliantIdentityChecks(LocalDateTime ldt1, LocalDateTime ldt2, JapaneseDate jd1, JapaneseDate jd2, Optional<Integer> opt, Integer i) {
    boolean b = ldt1 == ldt2; // Noncompliant {{Use ".equals()" instead of "==" or "!=" to compare objects of value-based types.}}
//              ^^^^^^^^^^^^
    boolean b1 = jd1 != jd2; // Noncompliant {{Use ".equals()" instead of "==" or "!=" to compare objects of value-based types.}}
//               ^^^^^^^^^^
    int hash = System.identityHashCode(opt); // Noncompliant {{Use ".hashCode()" instead of "System.identityHashCode()" to compute the hash code for objects of value-based types.}}
//             ^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    int hash2 = System.identityHashCode(i); // Noncompliant {{Use ".hashCode()" instead of "System.identityHashCode()" to compute the hash code for objects of value-based types.}}
//              ^^^^^^^^^^^^^^^^^^^^^^^^^^
  }

  void compliantIdentityChecks(int i, int j, String s, String t, Integer k, Integer l, Clock c1, Clock c2) {
    boolean b = i == j; // Compliant
    boolean b1 = s == t; // Compliant
    boolean b2 = k == l; // Compliant: already covered by S4973
    boolean b3 = c1 == c2; // Compliant
    int hash = System.identityHashCode(c1); // Compliant
  }

}
