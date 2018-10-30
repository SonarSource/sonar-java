import java.util.stream.Stream;
import java.util.List;
class A {
  void foo(){
    List<Object> objs = Lists.asList(new Object());
    objs.stream().map(o -> o.toString());
    Stream<Object> s = objs.stream();
    s.map(v -> v.toString());
    s.map(v -> v.toString());
    bar(t -> t + "");
  }

  F bar(F f){
    return s -> this.toString() + s;
  }

  F2 qix() {
    return s -> { return s -> s;};
  }

  F field = s -> s;

  void fun() {
    field = s -> s;
    B b = new B(s->s);
  }

  F cond(boolean a) {
    return a ? s->"" : s->s;
  }

  F parenth() {
    return (((s->s)));
  }
}


class B {
  B(F f){}
}

interface F {
  String apply(String s);
}
interface F2 {
  F apply(String s);
}
class test {
  interface BiFunction<T, U, R> {
    R apply(T t, U u);

    default <V> BiFunction<T,U,V> andThen(Function<? super R,? extends V> after){
      return null;
    }

  }
  interface Function<T,R> {
    R apply(T t);
  }

  private final List<BiFunction<String, Function<String, Integer>, Integer>> operations;
  void addAfterOperation(BiFunction<String, Integer, Integer> operation) {
    operations.add((context, payloadSupplier) -> operation.apply(context, payloadSupplier.apply(context)));
  }


  private class MyMap<K, V> {
    V computeIfAbsent(K key, Function<? super K,? extends V> mappingFunction) {
      return null;
    }
  }
  void stringParamMethod(String s) {

  }
  void test() {
    MyMap<String, Integer> stringIntegerMyMap = new MyMap<>();
    stringIntegerMyMap.computeIfAbsent("foo", myStringParam -> {stringParamMethod(myStringParam);return 1;});
  }

}

class Overload<T> {
  // Overloaded method resolved by lambdas can lead to IOOBException if arity is not checked.
  void foo(java.util.function.Consumer<T> c) {}
  void foo(Runnable r) {}
  void test() {
    new Overload<String>().foo(() -> sout("")).foo(s -> sout(s));
  }
}

class deferedInference {
  void fun(List<String> l) {
    l.stream().collect(java.util.stream.Collectors.toMap( s1 -> foo(s1), s2 -> s2 + "-"));
  }
  private void foo(String s){}

  class MyClass {
    static <X, Y> G<Y, List<X>> myFoo(java.util.function.Function<X, Y> f) {
      return null;
    }

    void myBar(G<String, List<Integer>> g) {
      myBar(myFoo(x -> x.toString()));
    }
  }

  class G<A, B> {}

  <K> K getField(String s) {
    return null;
  }
  java.util.Map<String, String> myField() {
    return getField("");
  }
}


abstract class Test {
  void test(java.util.function.Supplier<?> s) {
    this.call(Object::new);
    this.call(() -> new Object());
    this.call(s);
  }
  static void call(java.util.Set<?> set) {}
  abstract void call(java.util.function.Supplier<?> supplier);
}
