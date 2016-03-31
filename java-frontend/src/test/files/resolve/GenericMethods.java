abstract class A<X> implements I2<Class<? extends X>, X> {
  <B, T extends B> T cast(Class<T> type, B value) {
    return null;
  }

  <T extends X> T foo(Class<T> type, T value) {
    return cast(type, put(type, value));
  }

  <T extends X> T bar(Class<T> type) {
    return cast(type, get(type));
  }

  I1 myI = new I1() {
    @Override
    public void foo(Class<?> key, Object value) {
     cast(key, value);
    }
  };
}

interface I1 {
  void foo(Class<?> key, Object value);
}
interface I2<K, V> {
  V put(K key, V value);
  V get(Object key);
}

class B {
  void print(String s) { }

  <T> void myMethod(T param) {
    print(param.toString());
  }

  <T extends C> void myMethod1(T param) {
    print(param.bar());
  }

  <T extends I3> void myMethod2(T param) {
    print(param.foo());
  }

  <T extends C & I3> void myMethod3(T param) {
    print(param.bar() + param.foo() + param.toString());
  }
}

class C {
  String bar() { return ""; }
}

interface I3 {
  String foo();
}
