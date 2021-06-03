package checks;

import java.util.function.Predicate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Comparator;
import java.util.Set;

class Outer {
  class Nested {
  }
  class ExtendedNested extends Nested { }
  List list;
  List<String> foo() {
    Object obj = null;
    Object o1 = (List<String>) foo(); // Noncompliant [[sc=18;ec=30]] {{Remove this unnecessary cast to "List".}}
    Object o2 = (List<? extends String>) foo(); // Noncompliant {{Remove this unnecessary cast to "List".}}
    Object o3 = (List<? super String>) foo(); // Noncompliant {{Remove this unnecessary cast to "List".}}
    String s1 = (String) obj; // Compliant
    String s2 = (String) s1; // Noncompliant {{Remove this unnecessary cast to "String".}}
    Nested a = (Nested) new ExtendedNested(); // Noncompliant {{Remove this unnecessary cast to "Nested".}}
    Nested[][] as = (Nested[][]) new ExtendedNested[1][1]; // Noncompliant {{Remove this unnecessary cast to "Nested[][]".}}
    ExtendedNested b = null;
    fun(b);

    fun((ExtendedNested) b); // Noncompliant
    fun(((ExtendedNested) b)); // Noncompliant
    fun((((ExtendedNested) b))); // Noncompliant
    fun((Nested) a, (ExtendedNested) a); // Noncompliant [[sc=10;ec=16]]
    fun((Nested) b); // Compliant - exception to distinguish the method to call
    List<ExtendedNested> bees = new java.util.ArrayList<ExtendedNested>();
    List<Nested> aaas = (List) bees;
    C c = new C((Nested) null); // Compliant - exception to distinguish the constructor to call
    foo((List<List<Nested>>) (List<?>) foo2()); // compliant

    String[] stringList = (String[]) list.toArray(new String[0]); // Compliant
    return null;
  }

  List<String> foo2() {

    int a = 1;
    int b = 2;
    double d = (double) a / (double) b;
    int c = (int)a; // Noncompliant {{Remove this unnecessary cast to "int".}}
    int e = (int) d;
    return null;
  }

  private static int charConversion(char c) {
    return (char) ((c | 0x20) - 'a'); // Compliant
  }

  void foo(List<List<Nested>> a) {}

  void castInArguments(List<String> p) {
    Collection<String> v1 = Collections.emptyList();
    List<String> v2 = Collections.emptyList();
    castInArguments((List<String>) v1); // Compliant - cast needed
    castInArguments((List<String>) v2); // Noncompliant
  }

  List<List<ExtendedNested>> foo3() {
    return null;
  }
  void fun(Nested a) {
  }

  void fun(Nested a, ExtendedNested b) {
  }

  void fun(ExtendedNested b) {
  }

  void funBParameter(ExtendedNested b) {
  }

  class C {
    C(Nested a) {}
    C(ExtendedNested a) throws Exception {
      Object o = (Object) fun().newInstance(); // Noncompliant {{Remove this unnecessary cast to "Object".}}
    }
    Class fun() { return null;}
    public <T> T[] toArray(T[] a) {
      Object[] elementData = new Object[0];
      // Make a new array of a's runtime type, but my contents:
      return (T[]) Arrays.copyOf(elementData, 12, a.getClass()); // Compliant - The cast is mandatory!
    }
    String[] fun2(){
      return (String[]) null; // Noncompliant {{Remove this unnecessary cast to "String[]".}}
    }
  }
}

class RedundantTypeCastCheck_D {
  <T> List<T> genericCast() {
    List<Object> objectList = null;
    return (List<T>) objectList;
  }
}

class RedundantTypeCastCheck_E<T> {
  <K, V> Map<K, Set<V>> secondTypeChangeCast(Map<K, V> multimap) {
    return (Map<K, Set<V>>) (Map<K, ?>) multimap; // Compliant
  }
  RedundantTypeCastCheck_E<List<Object>> typeChangeCast(RedundantTypeCastCheck_E<List<String>> list) {
    return (RedundantTypeCastCheck_E<List<Object>>) (RedundantTypeCastCheck_E<?>) list; // Compliant
  }
}
interface Dto<T> {}
interface Index<DOMAIN, DTO extends Dto<KEY>, KEY extends java.io.Serializable> {}
class CastToRawType {
  void fun() {
    Object o1  = (Object) Object[].class; // Noncompliant
    Class o2  = (Class) Object[].class; // Noncompliant
  }
  private final Map<Class<?>, Index<?,?,?>> indexComponents = null;
  public <K extends Index> K get(Class<K> clazz){
    return (K) this.indexComponents.get(clazz);
  }

}

class RedundantTypeCastCheck_F<T, K, V> {
  class Inner<J> {
    J fun(T t) {
      return (J) t;
    }
  }
  public <T> T[] toArray(T[] a) {
    int size = size();
    if (a.length < size)
      a = (T[])java.lang.reflect.Array
        .newInstance(a.getClass().getComponentType(), size);
    java.util.Iterator<Map.Entry<K,V>> it = iterator();
    for (int i = 0; i < size; i++)
      a[i] = (T) new java.util.AbstractMap.SimpleEntry<K,V>(it.next());
    if (a.length > size)
      a[size] = null;
    return a;
  }
  public int size() {
    return 42;
  }
  public java.util.Iterator<Map.Entry<K,V>> iterator() {
    return null;
  }
}
class RedundantTypeCastCheck_G<T> {

  private RedundantTypeCastCheck_G<?> resolveSupertype() {
    return null;
  }

  void foo() {
    RedundantTypeCastCheck_G<? super T> plop = (RedundantTypeCastCheck_G<? super T>) resolveSupertype(); // this works but there is an issue in the returned type of resolveSupertype which returns raw type RedundantTypeCastCheck_G instead of RedundantTypeCastCheck_G<?>
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

  static class H {
    java.util.concurrent.Callable<H> c0 = () -> {
      return (H) getValue();
    };

    private Object getValue() {
      return null;
    }

    public <T> T getInstance(Runtime runtime, Object o, Class<T> clazz) throws Exception {
      String name = clazz.getName();
      Class<T> c = (Class<T>) Class.forName(name, true, o.getClass().getClassLoader());
      return null;
    }
    public static Number choose(Integer i, Float f, boolean takeFirst) {
      return takeFirst ? (Number) i : f;
    }

    public static Number choose2(Integer i, Float f, boolean takeFirst) {
      return takeFirst ? (int) i : f;
    }

    public static float choose3(int i, double f, boolean takeFirst) {
      return takeFirst ? i : (float)f;
    }
  }

  class I {
    <K> K getValue(K s) {
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

interface RedundantTypeCastCheck_J {
  default void foo() { }
  default void bar() { }

  interface K extends RedundantTypeCastCheck_J {
    void foo();
  }

  interface L extends RedundantTypeCastCheck_J {
    void foobar();
  }

  static void test() {
    RedundantTypeCastCheck_J j1 = (K) () -> { }; // compliant : cast is needed for it to be used as a lambda expression
    RedundantTypeCastCheck_J j2 = (L) () -> { }; // compliant : cast is needed for it to be used as a lambda expression
  }
}

interface RedundantTypeCastCheck_M {
  default void foo() { }
  default void bar() { }
  void foobar();

  interface N extends RedundantTypeCastCheck_M {
    default void foobar() { }
    void foo();
  }

  interface O extends RedundantTypeCastCheck_M { }

  interface P extends RedundantTypeCastCheck_M {
    void foobar();
  }

  interface Q extends RedundantTypeCastCheck_M {
    default void foo() { }
  }

  static void test() {
    RedundantTypeCastCheck_M m1 = () -> { };
    RedundantTypeCastCheck_M m2 = (RedundantTypeCastCheck_M) () -> { }; // FN because we now allow all casts on lambdas to avoid FPs
    RedundantTypeCastCheck_M m3 = (N) () -> { }; // compliant : cast changes method associated to lambda expression
    RedundantTypeCastCheck_M m6 = (Q) () -> { }; // compliant : cast changes default definition of method foo
  }
}

class RedundantTypeCastCheck_T {
  Predicate<Object> methodReferenceCastNeeded() {
    return ((Predicate<Object>) Objects::nonNull).negate(); // Compliant : cannot call Predicate#negate() without casting it first
  }

  Comparator<Integer> methodReferenceCastNeeded2() {
    return (((Comparator<Integer>) Integer::compare)).reversed();   // Compliant : cannot call Comparator#reversed() without casting it first
  }

  Predicate<Object> methodReferenceCastNotNeeded() {
    return (((Predicate<Object>) Objects::nonNull)); // FN because we now allow all casts on method references to avoid FPs
  }

  Comparator<Integer> methodReferenceCastNotNeeded2() {
    return (Comparator<Integer>) Integer::compare; // FN because we now allow all casts on method references to avoid FPs
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
    a = (byte) -a; // Compliant - cast is required
  }
}

class CastIntersectionType {
  public static final Comparator<Object> UNIQUE_ID_COMPARATOR =  (Comparator<Object> & java.io.Serializable) (o1, o2) -> o1.toString().compareTo(o2.toString());
}

abstract class Discuss {

  abstract <M> M getMeta();

  void foo() {
    int  i = ((String) getMeta()).length(); // Compliant - generic method correctly handled
  }
}

class ClassWithAnnotation {
  @MyAnnotation((int) (0L + 42))
  Object field;

  @interface MyAnnotation {
    int value() default 42;
  }
}

class ClassWithVariadicFunction {
  public ClassWithVariadicFunction(int i) {

  }

  void variadicFunction(int ... p) {}
  void fun() {
    variadicFunction(1, 2, (int) 3.4);
    variadicFunction(1, 2, ((int) 3.4));
    variadicFunction((int) 1, 2, 3); // Noncompliant
    variadicFunction(((int) 1), 2, 3); // Noncompliant
    variadicFunction(1, 2, 3, (int) 4); // Noncompliant

    ClassWithVariadicFunction c = new ClassWithVariadicFunction((int) 3.4); // recovered constructor
  }
}

class GenericClass<T> {
  private GenericClass<?> type() {
    return null;
  }

  void foo() {
    GenericClass<? super T> g = (GenericClass<? super T>) type(); // type of the method invocation is well-handled
  }
}

class CastRawType {
  public static void paramsErrorMessage(Class clazz) {
    Outer.Nested r = (Outer.Nested) clazz.getAnnotation(Outer.Nested.class); // Handle cast of raw types
  }
}
