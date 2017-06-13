public class StandardFunctionalInterfaceCheck {

  @FunctionalInterface
  interface NotStandard {
    static double toDouble(int a) { // ignore static
      return a;
    }
    default short toShort(int a) { // ignore default
      return (short) a;
    }
    String toLong(int a, short b); // unknown standard interface
  }

  @FunctionalInterface
  interface LikeBiFunctionIntegerLongString { // Noncompliant {{Drop this interface in favor of "java.util.function.BiFunction<Integer,Long,String>".}}
    String m(Integer x, Long y);
  }

  @FunctionalInterface
  interface BooleanSupplierWithAnExtraMember { // Noncompliant {{Make this interface extend "java.util.function.BooleanSupplier" and remove the functional method declaration.}}
    static int ANSWER = 42;
    boolean  m();
  }

  @FunctionalInterface
  interface ShortName extends java.util.function.BiFunction<Integer,Long,String>  { // Compliant, used to simplify the name
  }

  @FunctionalInterface
  interface ExtendedBooleanSupplier extends java.util.function.BooleanSupplier { // Compliant, used to add default method
    default boolean isFalse() {
      return !getAsBoolean();
    }
  }

  @FunctionalInterface
  interface BooleanSupplierWithException { // Compliant, add exception in signature
    boolean m() throws java.io.IOException;
  }

  @FunctionalInterface
  interface FunctionWithTypeParameter { // Compliant, usage example: its/sources/fluent-http/src/main/java/net/codestory/http/internal/Unwrappable.java
    <T> T m(Class<T> x);
  }

  @FunctionalInterface
  interface SerializableBooleanSupplier extends java.io.Serializable { // Compliant, has extensions
    boolean  m();
  }

  @FunctionalInterface
  interface LikeBiFunctionXLongString<X> { // Noncompliant {{Drop this interface in favor of "java.util.function.BiFunction<X,Long,String>".}}
    String m(X x, Long y);
  }

  @FunctionalInterface
  interface LikeBiFunctionXYString<X,Y> { // Noncompliant {{Drop this interface in favor of "java.util.function.BiFunction<X,Y,String>".}}
    String m(X x, Y y);
  }

  @FunctionalInterface
  interface LikeBiFunctionXYZ<X,Y,Z> { // Noncompliant {{Drop this interface in favor of "java.util.function.BiFunction<X,Y,Z>".}}
    Z m(X x, Y y);
  }

  @FunctionalInterface
  interface LikeBiFunctionTUString<T,U> { // Noncompliant {{Drop this interface in favor of "java.util.function.BiFunction<T,U,String>".}}
    String m(T x, U y);
  }

  @FunctionalInterface
  interface LikeBooleanSupplier { // Noncompliant {{Drop this interface in favor of "java.util.function.BooleanSupplier".}}
    boolean  m();
  }

  @FunctionalInterface
  interface LikeDoubleBinaryOperator { // Noncompliant {{Drop this interface in favor of "java.util.function.DoubleBinaryOperator".}}
    double m(double x, double y);
  }

  @FunctionalInterface
  interface LikeDoubleConsumer { // Noncompliant {{Drop this interface in favor of "java.util.function.DoubleConsumer".}}
    void m(double x);
  }

  @FunctionalInterface
  interface LikeDoublePredicate { // Noncompliant {{Drop this interface in favor of "java.util.function.DoublePredicate".}}
    boolean m(double x);
  }

  @FunctionalInterface
  interface LikeDoubleSupplier { // Noncompliant {{Drop this interface in favor of "java.util.function.DoubleSupplier".}}
    double  m();
  }

  @FunctionalInterface
  interface LikeDoubleToIntFunction { // Noncompliant {{Drop this interface in favor of "java.util.function.DoubleToIntFunction".}}
    int m(double x);
  }

  @FunctionalInterface
  interface LikeDoubleToLongFunction { // Noncompliant {{Drop this interface in favor of "java.util.function.DoubleToLongFunction".}}
    long m(double x);
  }

  @FunctionalInterface
  interface LikeDoubleUnaryOperator { // Noncompliant {{Drop this interface in favor of "java.util.function.DoubleUnaryOperator".}}
    double m(double x);
  }

  @FunctionalInterface
  interface LikeIntBinaryOperator { // Noncompliant {{Drop this interface in favor of "java.util.function.IntBinaryOperator".}}
    int m(int x, int y);
  }

  @FunctionalInterface
  interface LikeIntConsumer { // Noncompliant {{Drop this interface in favor of "java.util.function.IntConsumer".}}
    void m(int x);
  }

  @FunctionalInterface
  interface LikeIntPredicate { // Noncompliant {{Drop this interface in favor of "java.util.function.IntPredicate".}}
    boolean m(int x);
  }

  @FunctionalInterface
  interface LikeIntSupplier { // Noncompliant {{Drop this interface in favor of "java.util.function.IntSupplier".}}
    int  m();
  }

  @FunctionalInterface
  interface LikeIntToDoubleFunction { // Noncompliant {{Drop this interface in favor of "java.util.function.IntToDoubleFunction".}}
    double m(int x);
  }

  @FunctionalInterface
  interface LikeIntToLongFunction { // Noncompliant {{Drop this interface in favor of "java.util.function.IntToLongFunction".}}
    long m(int x);
  }

  @FunctionalInterface
  interface LikeIntUnaryOperator { // Noncompliant {{Drop this interface in favor of "java.util.function.IntUnaryOperator".}}
    int m(int x);
  }

  @FunctionalInterface
  interface LikeLongBinaryOperator { // Noncompliant {{Drop this interface in favor of "java.util.function.LongBinaryOperator".}}
    long m(long x, long y);
  }

  @FunctionalInterface
  interface LikeLongConsumer { // Noncompliant {{Drop this interface in favor of "java.util.function.LongConsumer".}}
    void m(long x);
  }

  @FunctionalInterface
  interface LikeLongPredicate { // Noncompliant {{Drop this interface in favor of "java.util.function.LongPredicate".}}
    boolean m(long x);
  }

  @FunctionalInterface
  interface LikeLongSupplier { // Noncompliant {{Drop this interface in favor of "java.util.function.LongSupplier".}}
    long  m();
  }

  @FunctionalInterface
  interface LikeLongToDoubleFunction { // Noncompliant {{Drop this interface in favor of "java.util.function.LongToDoubleFunction".}}
    double m(long x);
  }

  @FunctionalInterface
  interface LikeLongToIntFunction { // Noncompliant {{Drop this interface in favor of "java.util.function.LongToIntFunction".}}
    int m(long x);
  }

  @FunctionalInterface
  interface LikeLongUnaryOperator { // Noncompliant {{Drop this interface in favor of "java.util.function.LongUnaryOperator".}}
    long m(long x);
  }

  @FunctionalInterface
  interface LikeBinaryOperator<T> { // Noncompliant {{Drop this interface in favor of "java.util.function.BinaryOperator<T>".}}
    T m(T x, T y);
  }

  @FunctionalInterface
  interface LikeConsumer<T> { // Noncompliant {{Drop this interface in favor of "java.util.function.Consumer<T>".}}
    void m(T x);
  }

  @FunctionalInterface
  interface LikeDoubleFunction<R> { // Noncompliant {{Drop this interface in favor of "java.util.function.DoubleFunction<R>".}}
    R m(double x);
  }

  @FunctionalInterface
  interface LikeIntFunction<R> { // Noncompliant {{Drop this interface in favor of "java.util.function.IntFunction<R>".}}
    R m(int x);
  }

  @FunctionalInterface
  interface LikeLongFunction<R> { // Noncompliant {{Drop this interface in favor of "java.util.function.LongFunction<R>".}}
    R m(long x);
  }

  @FunctionalInterface
  interface LikeObjDoubleConsumer<T> { // Noncompliant {{Drop this interface in favor of "java.util.function.ObjDoubleConsumer<T>".}}
    void m(T x, double y);
  }

  @FunctionalInterface
  interface LikeObjIntConsumer<T> { // Noncompliant {{Drop this interface in favor of "java.util.function.ObjIntConsumer<T>".}}
    void m(T x, int y);
  }

  @FunctionalInterface
  interface LikeObjLongConsumer<T> { // Noncompliant {{Drop this interface in favor of "java.util.function.ObjLongConsumer<T>".}}
    void m(T x, long y);
  }

  @FunctionalInterface
  interface LikePredicate<T> { // Noncompliant {{Drop this interface in favor of "java.util.function.Predicate<T>".}}
    boolean m(T x);
  }

  @FunctionalInterface
  interface LikeSupplier<T> { // Noncompliant {{Drop this interface in favor of "java.util.function.Supplier<T>".}}
    T  m();
  }

  @FunctionalInterface
  interface LikeToDoubleFunction<T> { // Noncompliant {{Drop this interface in favor of "java.util.function.ToDoubleFunction<T>".}}
    double m(T x);
  }

  @FunctionalInterface
  interface LikeToIntFunction<T> { // Noncompliant {{Drop this interface in favor of "java.util.function.ToIntFunction<T>".}}
    int m(T x);
  }

  @FunctionalInterface
  interface LikeToLongFunction<T> { // Noncompliant {{Drop this interface in favor of "java.util.function.ToLongFunction<T>".}}
    long m(T x);
  }

  @FunctionalInterface
  interface LikeUnaryOperator<T> { // Noncompliant {{Drop this interface in favor of "java.util.function.UnaryOperator<T>".}}
    T m(T x);
  }

  @FunctionalInterface
  interface LikeBiConsumer<T,U> { // Noncompliant {{Drop this interface in favor of "java.util.function.BiConsumer<T,U>".}}
    void m(T x, U y);
  }

  @FunctionalInterface
  interface LikeBiPredicate<T,U> { // Noncompliant {{Drop this interface in favor of "java.util.function.BiPredicate<T,U>".}}
    boolean m(T x, U y);
  }

  @FunctionalInterface
  interface LikeFunction<T,R> { // Noncompliant {{Drop this interface in favor of "java.util.function.Function<T,R>".}}
    R m(T x);
  }

  @FunctionalInterface
  interface LikeToDoubleBiFunction<T,U> { // Noncompliant {{Drop this interface in favor of "java.util.function.ToDoubleBiFunction<T,U>".}}
    double m(T x, U y);
  }

  @FunctionalInterface
  interface LikeToIntBiFunction<T,U> { // Noncompliant {{Drop this interface in favor of "java.util.function.ToIntBiFunction<T,U>".}}
    int m(T x, U y);
  }

  @FunctionalInterface
  interface LikeToLongBiFunction<T,U> { // Noncompliant {{Drop this interface in favor of "java.util.function.ToLongBiFunction<T,U>".}}
    long m(T x, U y);
  }

  @FunctionalInterface
  interface LikeBiFunction<T,U,R> { // Noncompliant {{Drop this interface in favor of "java.util.function.BiFunction<T,U,R>".}}
    R m(T x, U y);
  }

  @FunctionalInterface
  interface UsingArrays { // Compliant - Array used as return type
    byte[] bar(int value);
  }

  @FunctionalInterface
  public interface InterfaceWithObjectMethodName { // Noncompliant {{Drop this interface in favor of "java.util.function.Consumer<String>".}}
    void notify(String param);
  }

  @FunctionalInterface // not really functional iface, just to test private methods
  public interface InterfaceWithPrivateMethod {
    private void notify(String param) {};
  }
}
