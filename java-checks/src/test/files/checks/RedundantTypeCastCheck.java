import java.util.function.Predicate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

  void fun() {
    Object test = new Object[]{"1"};
    String[] test2 = new String[]{"1"};
    Object[] test3 = new String[]{"1"};
    System.out.println(((Object[])test)[0]); // compliant : target type is array access
    System.out.println(((Object[])test2)[0]); // Noncompliant
    System.out.println(((String[])test3)[0]); // compliant
  }
}

interface J {
  default void foo() { }
  default void bar() { }

  interface K extends J {
    void foo();
  }

  interface L extends J {
    void foobar();
  }

  static void test() {
    J j1 = (K) () -> { }; // compliant : cast is needed for it to be used as a lambda expression
    J j2 = (L) () -> { }; // compliant : cast is needed for it to be used as a lambda expression
  }
}

interface M {
  default void foo() { }
  default void bar() { }
  void foobar();

  interface N extends M {
    default void foobar() { }
    void foo();
  }

  interface O extends M { }

  interface P extends M {
    void foobar();
  }

  interface Q extends M {
    default void foo() { }
  }

  static void test() {
    M m1 = () -> { };
    M m2 = (M) () -> { }; // Noncompliant {{Remove this unnecessary cast to "M".}}
    M m3 = (N) () -> { }; // compliant : cast changes method associated to lambda expression
    M m4 = (O) () -> { }; // Noncompliant {{Remove this unnecessary cast to "O".}}
    M m5 = (P) () -> { }; // Noncompliant {{Remove this unnecessary cast to "P".}}
    M m6 = (Q) () -> { }; // compliant : cast changes default definition of method foo
  }
}

interface R {
  void foo();
  void bar();

  interface S extends R {
    default void foo();
  }

  static void test() {
    R r1 = (S) () -> {  }; // compliant : cast is needed for it to be used as a lambda expression
  }
}

class T {
  Predicate<Object> methodReferenceCastNeeded() {
    return ((Predicate<Object>) Objects::nonNull).negate(); // Compliant : cannot call Predicate#negate() without casting it first
  }

  Comparator<Integer> methodReferenceCastNeeded2() {
    return (((Comparator<Integer>) Integer::compare)).reversed();   // Compliant : cannot call Comparator#reversed() without casting it first
  }

  Predicate<Object> methodReferenceCastNotNeeded() {
    return (((Predicate<Object>) Objects::nonNull)); // Noncompliant
  }

  Comparator<Integer> methodReferenceCastNotNeeded2() {
    return (Comparator<Integer>) Integer::compare; // Noncompliant
  }
}

interface U<A extends Iterable> {
  A foo(A param);

  default void test() {
    U u1 = (U<List>) (param) -> param.subList(0,1); // Compliant : cast needed to access sublist method
  }
}

abstract class MyClass {
  public String field;
  abstract <U extends MyClass> U foo();

  String qix() {
    return ((MyOtherClass) foo()).field; // Compliant - FN
  }

  String qix0() {
    return ((MyOtherClass) foo()).otherField; // Compliant
  }

  String qix1() {
    return ((MyOtherClass) foo()).bar(); // Compliant
  }

  String qix2() {
    return ((MyOtherClass) unknown()).bar(); // Compliant
  }

  MyClass qix3() {
    return ((MyOtherClass) foo()); // Noncompliant
  }
}
abstract class MyOtherClass extends MyClass {
  public String otherField;
  abstract String bar();
}

class AWT {
  private static java.awt.event.AWTEventListener deProxyAWTEventListener(java.awt.event.AWTEventListener l) {
    java.awt.event.AWTEventListener localL = l;

    if (localL == null) {
      return null;
    }
    if (l instanceof java.awt.event.AWTEventListenerProxy) {
      localL = (java.awt.event.AWTEventListener) // Noncompliant
        ((java.awt.event.AWTEventListenerProxy) l).getListener(); // Compliant
    }
    return localL;
  }

  void foo() {
    byte a = 42;
    a = (byte) -a; // FP, cast is required
  }
}

class CastIntersectionType {
  public static final Comparator<Object> UNIQUE_ID_COMPARATOR =  (Comparator<Object> & java.io.Serializable) (o1, o2) -> o1.toString().compareTo(o2.toString());
}
