package checks;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoublePredicate;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SpecializedFunctionalInterfacesCheckSample {

  static class A implements Supplier<Integer> { // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'IntSupplier'}}
//             ^
    @Override
    public Integer get() {
      return null;
    }

    static class AA implements Consumer<Long> { // Noncompliant

      Consumer<Integer> a1 = new Consumer<Integer>() { // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'IntConsumer'}}
//    ^^^^^^^^^^^^^^^^^
        @Override
        public void accept(Integer t) {
        }
      };
      Consumer<Double> a2 = new Consumer<Double>() { // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'DoubleConsumer'}}
//    ^^^^^^^^^^^^^^^^
        @Override
        public void accept(Double t) {
        }
      };
      Consumer<Long> a3 = new Consumer<Long>() { // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'LongConsumer'}}
        @Override
        public void accept(Long t) {
        }
      };
      Supplier<Integer> a = () -> 1; // Noncompliant
      Supplier<Double> a6 = () -> 1.0; // Noncompliant
      Supplier<Long> a7 = () -> new Long(1); // Noncompliant
      Predicate<Integer> a4 = (int1) -> true; // Noncompliant
      Predicate<Double> a8 = (double1) -> true; // Noncompliant
      Predicate<Long> a9 = (long1) -> true; // Noncompliant
      UnaryOperator<Integer> a5 = (int1) -> int1; // Noncompliant
      UnaryOperator<Double> a10 = (double1) -> double1; // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'DoubleUnaryOperator'}}
//    ^^^^^^^^^^^^^^^^^^^^^
      UnaryOperator<Long> a11 = long1 -> long1; // Noncompliant
      BiConsumer<A, Integer> a12 = (aaa, int1) -> {}; // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'ObjIntConsumer<A>'}}
//    ^^^^^^^^^^^^^^^^^^^^^^
      BiConsumer<A, Long> a13 = (aaa, long1) -> {}; // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'ObjLongConsumer<A>'}}
      BiConsumer<A.AA, Double> a14 = (aaa, double1) -> {}; // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'ObjDoubleConsumer<AA>'}}
      BiConsumer<A, A> compl1 = (a, aa) -> {}; // Compliant
      Function<A, Integer> a15 = (aaa) -> 1; // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'ToIntFunction<A>'}}
      Function<A, Long> a16 = (aaa) -> new Long(1); // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'ToLongFunction<A>'}}
      Function<A, Double> a17 = new Function<A, Double>() { // Compliant, a17 is referenced bellow
        @Override
        public Double apply(A aaa) {
          return 1.0;
        }
      };
      Function<A, Double> a17_2 = a17; // Compliant
      Function<Integer, A> a18 = (int1) -> new A(); // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'IntFunction<A>'}}
      Function<Long, A> a19 = (long1) -> new A(); // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'LongFunction<A>'}}
      Function<Double, A2> a20 = (double1) -> new A2(); // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'DoubleFunction<A2>'}}

      Function<Double, Integer> a21 = (double1) -> 1; // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'DoubleToIntFunction'}}
      Function<Double, Long> a22 = (double1) -> new Long(1); // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'DoubleToLongFunction'}}
      Function<Long, Double> a23 = (long1) -> 1.0; // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'LongToDoubleFunction'}}
      Function<Long, Integer> a24 = (long1) -> 1; // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'LongToIntFunction'}}

      Function<A, A> a25 = (aaa) -> new A(); // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'UnaryOperator<A>'}}
      Function<Integer, Integer> lambda = value -> value * 2; // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'IntUnaryOperator'}}
      BiFunction<String, Integer, Double> a40 = (x, y) -> 2.0; // Compliant
      BiFunction<String, String, Integer> a42 = (x, y) -> 1; // Compliant
      BiFunction<String, Double, Double> a43 = (x, y) -> 2.0; // Compliant

      BiFunction<String, String, String> bi = (x, y) -> { // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'BinaryOperator<String>'}}
        return x + y;
      };

      Function<A.AA, A.AA> a39; // Compliant -- compliant no initialization
      Function<A, A2> a30 = new Function() { // Compliant
        @Override
        public Object apply(Object t) {
          return null;
        }
      };
      Function<Entry<?, A.AA>, ? extends A> a32 = new Function() { // Compliant
        @Override
        public Object apply(Object t) {
          return null;
        }
      };
      DoublePredicate a31;
      Supplier<A> a34 = () -> new A(); // Compliant
      IntSupplier a45 = new IntSupplier() {
        @Override
        public int getAsInt() {
          return 0;
        }
      };
      Consumer<A> a35 = new Consumer<A>() { // Compliant
        @Override
        public void accept(A t) {
        }
      };
      DoubleBinaryOperator d = (x, y) -> x * y; // Compliant
      IntFunction<A> a33 = (x) -> new A(); // Compliant
      Predicate<A> a36 = (aaa) -> true; // Compliant
      UnaryOperator<A> a37 = (aaa) -> new A(); // Compliant
      BinaryOperator<A> a38 = new BinaryOperator<A>() { // Compliant
        @Override
        public A apply(A t, A t1) {
          return null;
        }
      };

      @Override
      public void accept(Long t) {
      }

      Function<? extends A, ? extends A> a41 = new Function() { // Compliant because we don't apply the rule when wildcards are used
        @Override
        public Object apply(Object t) {
          return null;
        }
      };

      private static class A3 {
      }

      Function<A.AA.A3, A.AA.A3> foo1 = a3 -> new A3(); // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'UnaryOperator<A3>'}}
    }

    Function<A.AA, A.AA> foo1 = aaaaaa -> new AA(); // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'UnaryOperator<AA>'}}
  }

  static class A2 implements BinaryOperator<Integer> {// Noncompliant {{Refactor this code to use the more specialised Functional Interface 'IntBinaryOperator'}}
    @Override
    public Integer apply(Integer t, Integer u) {
      return null;
    }
  }

  static class A3 {
    void foo() {
      Supplier<Boolean> a = () -> true; // Noncompliant
      BinaryOperator<Double> var1 = new BinaryOperator<Double>() {// Noncompliant
        @Override
        public Double apply(Double t, Double u) {
          return null;
        }
      };
      BinaryOperator<Long> var2 = new BinaryOperator<Long>() { // Noncompliant
        @Override
        public Long apply(Long t, Long u) {
          return null;
        }
      };
    }

    private static class A4 implements Function<Entry<String, LinkedHashMap<String, Long>>, Entry<String, LinkedHashMap<String, Long>>> { // Noncompliant
//                       ^^
      @Override
      public Entry<String, LinkedHashMap<String, Long>> apply(Entry<String, LinkedHashMap<String, Long>> t) {
        return null;
      }

      private static String getDetails(Function<? super Integer, ? super Integer> function, Integer... inT) { // Compliant because we don't apply the rule when wildcards are used

        Function<?, String> a; // Compliant
        Function<? super A, ? super A> foo1 = new Function() { // Compliant because we don't apply the rule when wildcards are used
          @Override
          public Object apply(Object t) {
            return null;
          }
        };
        Function<?, ?> foo2 = new Function() { // Compliant because we don't apply the rule when wildcards are used
          @Override
          public Object apply(Object t) {
            return null;
          }
        };
        Function<Entry<?, ? super A>, ? extends A> foo3 = new Function() { // Compliant
          @Override
          public Object apply(Object t) {
            return null;
          }
        };
        return null;
      }
    }

  }

  static class MySupplier implements Supplier<Integer> { // Noncompliant
//             ^^^^^^^^^^
//                                   ^^^^^^^^^^^^^^^^^@-1<
    Supplier<Integer> mySupplier = new MySupplier(); // Compliant

    @Override
    public Integer get() {
      return null;
    }
  }

  static class MySupplier1 implements Supplier<Integer>, Runnable { // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'IntSupplier'}}
//             ^^^^^^^^^^^
//                                    ^^^^^^^^^^^^^^^^^@-1<
    Supplier<Integer> mySupplier = new MySupplier1(); // Compliant

    @Override
    public Integer get() {
      return null;
    }

    @Override
    public void run() {
    }
  }

  static class MySupplier2 implements Supplier<Integer>, Consumer<Double> { // Noncompliant {{Refactor this code to use the more specialised Functional Interfaces 'IntSupplier', 'DoubleConsumer'}}
//             ^^^^^^^^^^^
//                                    ^^^^^^^^^^^^^^^^^@-1<
//                                                       ^^^^^^^^^^^^^^^^@-2<
    Supplier<Integer> mySupplier = new MySupplier2(); // Compliant

    @Override
    public Integer get() {
      return null;
    }

    @Override
    public void accept(Double t) {
    }
  }

  static class MySupplier3 implements Supplier<Integer>, Runnable, Consumer<Double> { // Noncompliant {{Refactor this code to use the more specialised Functional Interfaces 'IntSupplier', 'DoubleConsumer'}}
//             ^^^^^^^^^^^
//                                    ^^^^^^^^^^^^^^^^^@-1<
//                                                                 ^^^^^^^^^^^^^^^^@-2<
    Supplier<Integer> mySupplier = new MySupplier2(); // Compliant

    @Override
    public Integer get() {
      return null;
    }

    @Override
    public void accept(Double t) {
    }

    @Override
    public void run() {
    }

    void foo4() {
      BiFunction<Integer[][][], Integer[][][], Integer[][][]> myBiFunc = new BiFunction<Integer[][][], Integer[][][], Integer[][][]>() { // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'BinaryOperator<Integer[][][]>'}}
        @Override
        public Integer[][][] apply(Integer[][][] t, Integer[][][] u) {
          return null;
        }
      };

      com.google.common.base.Function<Integer, String> ff = new com.google.common.base.Function<Integer, String>() { // Compliant
        @Override
        public String apply(Integer a) {
          return null;
        }
      };
    }
  }

  static class BooleanFunctions implements Supplier<Boolean>, Consumer<Boolean> { // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'BooleanSupplier'}}

    @Override
    public Boolean get() {
      return null;
    }

    @Override
    public void accept(Boolean t) {
    }

    Function<Boolean, Boolean> a1 = (x) -> true; // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'UnaryOperator<Boolean>'}}
    Function<A, Boolean> a2 = (x) -> true; // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'Predicate<A>'}}
    Function<Boolean, Integer> a3 = (x) -> null;
    Function<Boolean, A> a4 = (x) -> null;

    BiFunction<A, A2, Boolean> b1 = (x, y) -> true; // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'BiPredicate<A, A2>'}}
    BiFunction<Boolean, Boolean, Boolean> b2 = (x, y) -> true; // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'BinaryOperator<Boolean>'}}

    Supplier<Boolean> a = () -> true; // Noncompliant
    BiConsumer<A, Boolean> b3 = (aaa, bool1) -> {}; // Compliant - there is no ObjBooleanConsumer functional interface
    Consumer<Boolean> b4 = (bool1) -> {}; // Compliant - there is no BooleanConsumer functional interface
    Predicate<Boolean> b5 = (bool1) -> true; // Compliant - there is no BooleanPredicate functional interface
    UnaryOperator<Boolean> b6 = (bool1) -> true; // Compliant - there is no BooleanUnaryOperator functional interface
    BinaryOperator<Boolean> b7 = (bool1, bool2) -> true; // Compliant - there is no BooleanBinaryOperator functional interface
  }

  static class MethodParams {
    void call(Function<Integer, Integer> function) { // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'IntUnaryOperator'}}
    }
  }

  static class InferedTypeTree {
    java.util.Optional<Function<String, String>> fun() {
      return null;
    }

    boolean test(String str) {
      return fun().map(f -> consume(f, str)).isPresent();
    }

    boolean test2(String str) {
      return fun().map((Function<String, String> f) -> consume(f, str)).isPresent();
    }

    java.util.Optional<Function<?, ?>> consume(Function<?, ?> f, String str) { // Compliant because we don't apply the rule when wildcards are used
      return null;
    }
  }

  static int noIssueWhenUsedAsMethodReference(Stream<Object> objectStream, Object object) {
    // The usage of "keyMapperFunction1" it does not allow to replace its type with ToIntFunction<Object>,
    // it would not be assignable to "keyMapperFunction2"
    Function<Object, Integer> keyMapperFunction1 = Object::hashCode;

    // The usage of "keyMapperFunction2" it does not allow to replace its type with ToIntFunction<Object>,
    // it would not be compatible with the bellow "Collectors.toMap"
    Function<Object, Integer> keyMapperFunction2 = keyMapperFunction1;
    objectStream.collect(Collectors.toMap(keyMapperFunction2, Function.identity()));

    // "keyMapperFunction3" is not used as a reference, so it's OK to replace it with ToIntFunction<Object>
    Function<Object, Integer> keyMapperFunction3 = Object::hashCode;  // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'ToIntFunction<Object>'}}
    int value = keyMapperFunction3.apply(object);
    value = value + keyMapperFunction3.apply(value) + Math.abs(keyMapperFunction3.apply(value));
    value += keyMapperFunction3.apply(value);
    if (value > 0) {
      return keyMapperFunction3.apply(value);
    }

    BiConsumer<String, Integer> m1_1 = (a, b) -> System.setProperty(a, String.valueOf(b)); // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'ObjIntConsumer<String>'}}
    m1_1.accept("", 2);
    m1_1.accept("", 3);
    BiConsumer<String, Integer> m1_2 = (a, b) -> System.setProperty(a, String.valueOf(b));
    m1_2.accept("", 2);
    foo(m1_2);
    Consumer<Integer> m2_1 = (a) -> System.setProperty("x", String.valueOf(a)); // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'IntConsumer'}}
    m2_1.accept(2);
    Consumer<Integer> m2_2 = (a) -> System.setProperty("x", String.valueOf(a));
    foo(m2_2);
    Supplier<Integer> m3_1 = () -> 42; // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'IntSupplier'}}
    foo(m3_1.get());
    Supplier<Integer> m3_2 = () -> 42;
    foo(m3_2);
    Function<Integer, Integer> m4_1 = (a) -> a + 1; // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'IntUnaryOperator'}}
    int x4_1 = m4_1.apply(3) + 2;
    Function<Integer, Integer> m4_2 = (a) -> a + 1; // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'UnaryOperator<Integer>'}}
    Stream.of(1, 2, 3).map(m4_2).forEach(x -> foo(x));
    Function<String, Boolean> m5_1 = (a) -> "true".equals(a); // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'Predicate<String>'}}
    foo(m5_1.apply("") ? 1 : 2);
    Function<String, Boolean> m5_2 = (a) -> "true".equals(a);
    foo(m5_2);
    Function<Integer, Long> m6_1 = Long::valueOf; // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'IntToLongFunction'}}
    LongSupplier s = () -> m6_1.apply(12);
    Function<Integer, Long> m6_2 = Long::valueOf;
    foo(m6_2);
    return m4_1.apply(7);
  }

  static void foo(Object object) {

  }

  void functionsWithDefaultMethods() {
    Function<Long, Boolean> myFunctionLB = l -> l > 42L; // Compliant, uses compose/andThen
    myFunctionLB.andThen(s -> s && true);
    myFunctionLB.compose((Object o) -> o.hashCode() + 42L);
    
    Function<Integer, Boolean> myFunctionIB = i -> i > 42; // Compliant, uses compose/andThen
    myFunctionIB.andThen(s -> s && true);
    myFunctionIB.compose((Object o) -> o.hashCode() + 42);
    
    Function<Double, Boolean> myFunctionDB = i -> i > 42.5; // Compliant, uses compose/andThen
    myFunctionDB.andThen(s -> s && true);
    myFunctionDB.compose((Object o) -> (double)o.hashCode() + 42.8);
    
    Function<Long, String> myFunctionLS = l -> l > 42L ? "big" : "small"; // Compliant, uses compose/andThen
    myFunctionLS.andThen(s -> s.length() + 42);
    myFunctionLS.compose((Object o) -> o.hashCode() + 42L);
    
    Function<Long, Integer> myFunctionLI = l -> l > 42L ? l.compareTo(100L) : l.compareTo(1000L); // Compliant, uses compose/andThen
    myFunctionLI.andThen(s -> s + 42);
    myFunctionLI.compose((Object o) -> o.hashCode() + 42L);

    Function<String, Double> myFunctionSD = s -> s.length() > 42 ? 100.01 : 10.002; // Compliant, uses compose/andThen
    myFunctionSD.andThen(s -> s + 42);
    myFunctionSD.compose((Object o) -> o.hashCode() + "42L");
  }
  
  void biFunctionsWithDefaultMethods() {
    BiFunction<String, Long, Boolean> myFunctionLB = (s, l) -> l > 42L; // Compliant, uses andThen
    myFunctionLB.andThen(s -> s && true);

    BiFunction<String, Integer, Boolean> myFunctionIB = (s, i) -> i > 42; // Compliant, uses andThen
    myFunctionIB.andThen(s -> s && true);

    BiFunction<String, Double, Boolean> myFunctionDB = (s, d) -> d > 42.5; // Compliant, uses andThen
    myFunctionDB.andThen(s -> s && true);
  }
  
  void biConsumersWithDefaultMethods() {
    BiConsumer<String, Long> myConsumerLB = (s, l) -> System.out.println(s + l); // Compliant, uses andThen
    myConsumerLB.andThen((s, l) -> System.out.println("After" + s + l));

    BiConsumer<String, Integer> myConsumerIB = (s, i) -> System.out.println(s + i); // Compliant, uses andThen
    myConsumerIB.andThen((s, l) -> System.out.println("After" + s + l));

    BiConsumer<String, Double> myConsumerDB = (s, d) -> System.out.println(s + d); // Compliant, uses andThen
    myConsumerDB.andThen((s, l) -> System.out.println("After" + s + l));
  }

  void biConsumersWithWrappers() {
    BiConsumer<Integer, Integer> myConsumerII = (s, l) -> System.out.println(s + l); // Compliant
    BiConsumer<Integer, Long> myConsumerIL = (s, l) -> System.out.println(s + l); // Compliant
    BiConsumer<Integer, Double> myConsumerID = (s, l) -> System.out.println(s + l); // Compliant

    BiConsumer<Long, Integer> myConsumerLI = (s, l) -> System.out.println(s + l); // Compliant
    BiConsumer<Long, Long> myConsumerLL = (s, l) -> System.out.println(s + l); // Compliant
    BiConsumer<Long, Double> myConsumerLD = (s, l) -> System.out.println(s + l); // Compliant

    BiConsumer<Double, Integer> myConsumerDI = (s, l) -> System.out.println(s + l); // Compliant
    BiConsumer<Double, Long> myConsumerDL = (s, l) -> System.out.println(s + l); // Compliant
    BiConsumer<Double, Double> myConsumerDD = (s, l) -> System.out.println(s + l); // Compliant

    BiConsumer<Character, Double> myConsumerCD = (s, l) -> System.out.println(s + l); // Compliant
    BiConsumer<Float, Long> myConsumerFL = (s, l) -> System.out.println(s + l); // Compliant
    BiConsumer<Byte, Integer> myConsumerBI = (s, l) -> System.out.println(s + l); // Compliant
    BiConsumer<Short, Integer> myConsumerSI = (s, l) -> System.out.println(s + l); // Compliant
  }
}
