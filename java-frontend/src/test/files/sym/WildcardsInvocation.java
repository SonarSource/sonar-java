import java.util.Collection;

class WildcardsInvocation {

  class Callable<X> {}
  class Collection<Y> {}

  <S> Collection<Callable<S>> fun(Collection<? extends Callable<S>> tasks) {}

  <T> void meth(Collection<? extends Callable<T>> tasks) {
    fun(tasks);
  }


  class MyType<X, Y> {
  }
  <K extends Enum<K>, V> void foo(MyType<K, ? extends V> myType) {}
  void test(){
    MyType<?, ?> myType;
    foo(myType);
  }
}