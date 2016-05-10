import java.util.Collection;
import java.util.List;
import java.util.Set;

class A<T> {
  <E extends T> Object[] foo(List<String> list, Set rawSet, Collection<E> col) {
    String[] a1 = (String[]) list.toArray(); // Noncompliant [[sc=30;ec=44]] {{Pass "new String[0]" as argument to "toArray".}}
    Number[] a2 = (Number[]) rawSet.toArray(); // Noncompliant [[sc=30;ec=46]] {{Pass "new Number[0]" as argument to "toArray".}}
    Object[] a3 = list.toArray(); // Compliant
    String[] a4 = list.toArray(new String[0]); // Compliant
    Object[] a5 = (Object[]) list.toArray(); // Compliant
    E[] a6 = (E[]) col.toArray(); // Compliant
    Object o = (Object) list.toArray(); // Compliant

    return list.toArray(); // Compliant
  }
}