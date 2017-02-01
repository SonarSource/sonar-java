import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.Set;

class Outer {
  class A {
  }
  class B extends A { }
  List list;
  List<String> foo() {
    Object obj;
    Object o1 = (List<String>) foo(); // Noncompliant [[sc=18;ec=30]] {{Remove this unnecessary cast to "List".}}
    Object o2 = (List<? extends String>) foo(); // Noncompliant {{Remove this unnecessary cast to "List".}}
    Object o3 = (List<? super String>) foo(); // Noncompliant {{Remove this unnecessary cast to "List".}}
    String s1 = (String) obj; //Compliant
    String s2 = (String) s1; // Noncompliant {{Remove this unnecessary cast to "String".}}
    A a = (A) new B(); // Noncompliant {{Remove this unnecessary cast to "A".}}
    A[][] as = (A[][]) new B[1][1]; // Noncompliant {{Remove this unnecessary cast to "A[][]".}}
    B b;
    fun(b);
    fun((A) b);
    List<B> bees = new java.util.ArrayList<B>();
    java.util.List<A> aaas = (java.util.List) bees;
    C c = new C((A)null);
    foo((List<List<A>>) (List<?>) foo2()); // compliant
    obj = (Plop<String>) bar;
    String[] stringList = (String[]) list.toArray(new String[0]); // Compliant
  }
  List<String> foo2() {

    int a = 1;
    int b = 2;
    double d = (double) a / (double) b;
    int c = (int)a; // Noncompliant {{Remove this unnecessary cast to "int".}}
    int e = (int) d;
  }
  
  void foo(List<List<A>> a) {}
  
  List<List<B>> foo2() {
    return null;
  }
  void fun(A a) {
  }

  void fun(B b) {
  }

  class C {
    C(A a) {}
    C(B a) {
      Object o = (Object) fun().newInstance(); // Noncompliant {{Remove this unnecessary cast to "Object".}}
    }
    Class fun() { return null;}
    public <T> T[] toArray(T[] a) {
      Object[] elementData;
      // Make a new array of a's runtime type, but my contents:
      return (T[]) Arrays.copyOf(elementData, 12, a.getClass()); // Compliant - The cast is mandatory!
    }
    String[] fun2(){
      return (String[]) null; // Noncompliant {{Remove this unnecessary cast to "String[]".}}
    }
    void fun3() {
      Object[] a = null;
      java.util.Collection<C> c = (java.util.Collection<C>) Arrays.asList(a);
    }
  }
}

class D {
  <T> List<T> genericCast() {
    List<Object> objectList;
    return (List<T>) objectList;
  }
}

class E<T> {
  <K, V> Map<K, Set<V>> secondTypeChangeCast(Map<K, V> multimap) {
    return (Map<K, Set<V>>) (Map<K, ?>) multimap; // Compliant
  }
  E<List<Object>> typeChangeCast(E<List<String>> list) {
    return (E<List<Object>>) (E<?>) list; // Compliant
  }
}
public interface Index<DOMAIN, DTO extends Dto<KEY>, KEY extends Serializable> {}
class CastToRawType {
  void fun() {
    Object o1  = (Object) Object[].class; // Noncompliant
    Class o2  = (Class) Object[].class; // Noncompliant
  }
  private final Map<Class<?>, Index<?,?,?>> indexComponents;
  public <K extends Index> K get(Class<K> clazz){
    return (K) this.indexComponents.get(clazz);
  }

}

class F<T> {
  class Inner<K> {
    K fun(T t) {
      return (K) t;
    }
  }
  public <T> T[] toArray(T[] a) {
    int size = size();
    if (a.length < size)
      a = (T[])java.lang.reflect.Array
        .newInstance(a.getClass().getComponentType(), size);
    Iterator<Map.Entry<K,V>> it = iterator();
    for (int i = 0; i < size; i++)
      a[i] = (T) new java.util.AbstractMap.SimpleEntry<K,V>(it.next());
    if (a.length > size)
      a[size] = null;
    return a;
  }
}
class G<T> {

  private G<?> resolveSupertype() {
    return null;
  }

  void foo() {
    G<? super T> plop = (G<? super T>) resolveSupertype(); // this works but there is an issue in the returned type of resolveSupertype which returns raw type G instead of G<?>
  }

  private int unsafeCompare(Object k1, Object k2) {
    Comparator<? super T> comparator = null;
    if (comparator == null) {
      return ((Comparable<Object>) k1).compareTo(k2);
    } else {
      return ((Comparator<Object>) comparator).compare(k1, k2);
    }
  }

  String foo(Object a) {
    return a == null ? (String) null : a.toString(); // Noncompliant
  }

  class H {
    java.util.concurrent.Callable<H> c0 = () -> {
      return (H) getValue();
    };

    private Object getValue() {
      return null;
    }

    public <T> T getInstance(Ruby runtime, Object o, Class<T> clazz) {
      String name = clazz.getName();
      Class<T> c = (Class<T>) Class.forName(name, true, o.getClass().getClassLoader());
    }
    public static Number choose(Integer i, Float f, boolean takeFirst) {
      return takeFirst ? (Number) i : f;
    }
  }

  class I {
    <K> K getValue(String s) {
      return s;
    }
    String foo() {
      return (String) getValue(""); // Noncompliant
    }
  }

}
