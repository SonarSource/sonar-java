package checks.unused;

import java.util.Map;

class UnusedTypeParameterCheck<S> { // Compliant
  Iterable<Something.Unknown<S>> f;
  private static <T> String doSomething(Iterable<Something.Unknown<T>> currentPath) { // Compliant, semantic is broken in this case
    return "";
  }

  private static <K, V> void doDifference(Map<? extends K, ? extends V> left, Map<K, Something.Unknown<V>> differences) { // Compliant
    for (Entry<? extends K, ? extends V> entry : left.entrySet()) {
      K leftKey = entry.getKey();
      V leftValue = entry.getValue();
    }
  }

}
