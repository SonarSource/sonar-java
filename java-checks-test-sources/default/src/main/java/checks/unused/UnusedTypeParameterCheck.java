package checks.unused;

import com.google.common.collect.MapDifference;
import java.util.Map;

class UnusedTypeParameterCheck<T, S> { // Noncompliant [[sc=35;ec=36]] {{S is not used in the class.}}
  T field;
  <W,X> void fun(X x) {} // Noncompliant {{W is not used in the method.}}
}
interface UnusedTypeParameterCheckB<U, V> { // Noncompliant {{V is not used in the interface.}}
  U foo();
}
class UnusedTypeParameterCheckC {
  void fun(){}
  private static <K, V> void doDifference(Map<? extends K, ? extends V> left, Map<K, MapDifference.ValueDifference<V>> differences) { // Compliant
    for (Map.Entry<? extends K, ? extends V> entry : left.entrySet()) {
      K leftKey = entry.getKey();
      V leftValue = entry.getValue();
    }
  }
}

record UnusedRecordTypeParameter<T, U> (T t, String s) { // Noncompliant [[sc=37;ec=38]] {{U is not used in the record.}}
  void foo() { }
}
