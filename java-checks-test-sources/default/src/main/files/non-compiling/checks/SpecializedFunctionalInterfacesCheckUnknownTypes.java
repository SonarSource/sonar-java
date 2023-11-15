package checks;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.concurrent.Future;

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

class A implements Supplier<Integer> { // Noncompliant
  @Override
  public Integer get() {
    return null;
  }
}

// Test that the rule doesn't crash when encountering records
record Range(int lo, int hi) {
}
