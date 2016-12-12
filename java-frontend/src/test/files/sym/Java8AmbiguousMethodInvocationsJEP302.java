class Test {

  void test() {
    m(s -> false); // ambiguous - does not compile with javac, nor with eclipse
    m(Foo::g); // ambiguous - does not compile with javac, compile with eclipse
    m2(Bar::f); // ambiguous - does not compile with javac, compile with eclipse
  }

  void m(Pred<String> ps) { }
  void m(Func<String, String> fss) { }

  void m2(Func<Integer, String> fss) { }
  void m2(Pred<String> ps) { }
}

/**
 * Similar to java.util.function.Function
 */
@FunctionalInterface
interface Func<T, R> {
  R apply(T t);
}

/**
 * Similar to java.util.function.Predicate
 */
@FunctionalInterface
interface Pred<T> {
  boolean test(T t);
}

class Foo {
  static boolean g(String s) { return false; }
  static boolean g(Integer i) { return false; }
}

class Bar {
  static String f(Integer i) { return null; }
  static String f(Double d) { return null; }
}
