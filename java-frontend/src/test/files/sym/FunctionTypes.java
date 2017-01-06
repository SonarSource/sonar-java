interface FunctionTypes<T> {

  T fun(T t);

  FunctionTypes<? extends A> upperBound;
  FunctionTypes<? super A> lowerBound;
  FunctionTypes<?> unbounded;
  FunctionTypes<A> ref;
}
class A {}
