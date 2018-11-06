import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoublePredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

class A implements Supplier<Integer> { // Noncompliant [[sc=7;ec=8]] {{Refactor this code to use the more specialised Functional Interface 'IntSupplier'}}
  @Override
  public Integer get() {
    return null;
  }

  private class AA implements Consumer<Long> { // Noncompliant

    Consumer<Integer> a1 = new Consumer<Integer>() { // Noncompliant [[sc=5;ec=22]] {{Refactor this code to use the more specialised Functional Interface 'IntConsumer'}}
      @Override
      public void accept(Integer t) {
      }
    };
    Consumer<Double> a2 = new Consumer<Double>() { // Noncompliant [[sc=5;ec=21]] {{Refactor this code to use the more specialised Functional Interface 'DoubleConsumer'}}
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
    UnaryOperator<Double> a10 = (double1) -> double1; // Noncompliant [[sc=5;ec=26]] {{Refactor this code to use the more specialised Functional Interface 'DoubleUnaryOperator'}}
    UnaryOperator<Long> a11 = long1 -> long1; // Noncompliant
    BiConsumer<A, Integer> a12 = (aaa, int1) -> {}; // Noncompliant [[sc=5;ec=27]] {{Refactor this code to use the more specialised Functional Interface 'ObjIntConsumer<A>'}}
    BiConsumer<A, Long> a13 = (aaa, long1) -> {};// Noncompliant {{Refactor this code to use the more specialised Functional Interface 'ObjLongConsumer<A>'}}
    BiConsumer<A.AA, Double> a14 = (aaa, double1) -> {};; // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'ObjDoubleConsumer<AA>'}}
    BiConsumer<A,A> compl1 = (a,aa)-> {};  // Compliant
    Function<A, Integer> a15 = (aaa) -> 1; // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'ToIntFunction<A>'}}
    Function<A, Long> a16 = (aaa) -> new Long(1); // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'ToLongFunction<A>'}}
    Function<A, Double> a17 = new Function<A, Double>() { // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'ToDoubleFunction<A>'}}
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
    BiFunction<String, Double, Double> a43 = (x,y) -> 2.0; // Compliant

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
    Function<Entry<?, A.AA>,? extends A> a32 = new Function() { // Compliant
      @Override
      public Object apply(Object t) {
        return null;
      }
    };
    DoublePredicate a31;
    Supplier<A> a34 = () -> new A(); // Compliant
    Supplier<Integer> a44 = new Supplier<Integer>();
    IntSupplier a45 = new IntSupplier();  
    Consumer<A> a35= new Consumer<A>() { // Compliant
      @Override
      public void accept(A t) {
      }
    };
    DoubleBinaryOperator d = (x, y) -> x * y; // Compliant 
    IntFunction<A> a33= (x, y) -> x * 2; // Compliant
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
    Function<? extends A, ? extends A> a41 = new Function() { // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'UnaryOperator<? extends A>'}}
      @Override
      public Object apply(Object t) {
        return null;
      }
    };
    private class A3{}
    Function<A.AA.A3, A.AA.A3> foo1 = a3 -> new A3(); // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'UnaryOperator<A3>'}}    
  }

  Function<A.AA, A.AA> foo1 = aaaaaa -> new AA(); // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'UnaryOperator<AA>'}}
}

class A2 implements BinaryOperator<Integer> {// Noncompliant {{Refactor this code to use the more specialised Functional Interface 'IntBinaryOperator'}}
  @Override
  public Integer apply(Integer t, Integer u) {
    return null;
  }
}

class A3 {
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

  private static class A4 implements Function<Entry<String, LinkedHashMap<String, Long>>, Entry<String, LinkedHashMap<String, Long>>> { // Noncompliant [[sc=24;ec=26]] 
    @Override
    public Entry<String, LinkedHashMap<String, Long>> apply(Entry<String, LinkedHashMap<String, Long>> t) {
      return null;
    }

    private static String getDetails(Function<? super Integer, ? super Integer> function, Integer... inT) { // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'UnaryOperator<? super Integer>'}}
      Function<?, String> a; // Compliant
      Function<? super A, ? super A> foo1 = new Function() { // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'UnaryOperator<? super A>'}}
        @Override
        public Object apply(Object t) {
          return null;
        }
      };
      Function<?, ?> foo2 = new Function() { // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'UnaryOperator<?>'}}
        @Override
        public Object apply(Object t) {
          return null;
        }
      };
      Function<Entry<?, ? super A>,? extends A> foo3 = new Function() { // Compliant
        @Override
        public Object apply(Object t) {
          return null;
        }
      };
      return null;
    }
  }

}

class MySupplier implements Supplier<Integer>  { // Noncompliant [[sc=7;ec=17;secondary=183]]
  Supplier<Integer> mySupplier = new MySupplier(); // Compliant 
  @Override
  public Integer get() {
    return null;
  }
}

class MySupplier1 implements Supplier<Integer>, Runnable { // Noncompliant [sc=7;ec=18;secondary=191] {{Refactor this code to use the more specialised Functional Interface 'IntSupplier'}}
  Supplier<Integer> mySupplier = new MySupplier1(); // Compliant

  @Override
  public Integer get() {
    return null;
  }

  @Override
  public void run() {
  }
}

class MySupplier2 implements Supplier<Integer>, Consumer<Double> { // Noncompliant [[sc=7;ec=18;secondary=204, 204]] {{Refactor this code to use the more specialised Functional Interfaces 'IntSupplier', 'DoubleConsumer'}}
  Supplier<Integer> mySupplier = new MySupplier2(); // Compliant

  @Override
  public Integer get() {
    return null;
  }

  @Override
  public void accept(Double t) {
  }
}

class MySupplier3 implements Supplier<Integer>, Runnable, Consumer<Double> { // Noncompliant [[sc=7;ec=18;secondary=217, 217]] {{Refactor this code to use the more specialised Functional Interfaces 'IntSupplier', 'DoubleConsumer'}}
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

    com.google.common.base.Function<Integer, String> ff = new com.google.common.base.Function<Integer, String>(){ // Compliant
      @Override
      public String apply(Integer a) {
        return null;
      }
    };
  }
}

class BooleanFunctions implements Supplier<Boolean>, Consumer<Boolean> { // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'BooleanSupplier'}}

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

class UnknownTypes implements Supplier<MyFirstUnknownType> {

  @Override
  public MyFirstUnknownType get() {
    return null;
  }

  Supplier<MyFirstUnknownType> unknownTypeSupplier = () -> new MyFirstUnknownType();
  BiConsumer<A, MyFirstUnknownType> unknownTypeBiConsumer = (a, ut) -> {};
  Function<A, MyFirstUnknownType> unknownTypeFunction1 = x -> new MyFirstUnknownType(x);
  Function<MyFirstUnknownType, A> unknownTypeFunction2 = x -> new A();
  Function<MyFirstUnknownType, MySecondUnknownType> unknownTypeFunction3 = ut -> new MySecondUnknownType(ut);
  Function<MyFirstUnknownType, MyFirstUnknownType> unknownTypeFunction4 = ut -> ut;
  BiFunction<MyFirstUnknownType, MySecondUnknownType, MyThirdUnknownType> unknownTypesBiFunction = (a, b) -> new MyThirdUnknownType(a,b);
  BiFunction<MyFirstUnknownType, MySecondUnknownType, Boolean> unknownTypesBiFunction2 = (x, y) -> true;
}

class MethodParams {
  void call(Function<Integer, Integer> function) {  // Noncompliant {{Refactor this code to use the more specialised Functional Interface 'IntUnaryOperator'}}
  }
}

class InferedTypeTree {
  java.util.Optional<Function<String, String>> fun() {
    return null;
  }

  boolean test(String str) {
    return fun().map(f -> consume(f, str)).isPresent();
  }

  boolean test2(String str) {
    return fun().map((Function<String, String> f) -> consume(f, str)).isPresent();
  }

  java.util.Optional<Function<?, ?>> consume(Function<?, ?> f, String str) { // Noncompliant
    return null;
  }
}
