package references; import java.util.List;import java.util.Collection;

@SuppressWarnings("all")
class MethodCall extends Parent {

  void target() {
  }

  void method() {
    target();
    foo();
  }

}

class Parent {
  void foo() {
  }
}
//Overloading in class hierarchy
class C1 {
  void fun(String a) {
  }
}

class C2 extends C1 {
  void fun(Object a) {
  }
  void method() {
    fun("");
  }
}
//Overloading between class and interface
interface I1 {
  void bar(String s);
}
class D1 {
  void bar(Object j){}
}
abstract class D2 extends D1 implements I1 {
  void method(){
    bar("");
  }
}

abstract class D3 implements I1 {}
interface I2 {
  void bar(int i);
}
abstract class D4 extends D3 implements I2{
  void method(){
    bar("");
  }
}

class D5 extends D1 {
  void bar(Object j){}
}
class D6 extends D5 {
  void method(){
    bar("");
  }
}
interface I3 extends I1 {}
class D7 implements I3 {
  void method(){
    bar("");
  }
}
//default methods
class ADefault {
  public void defaultMethod() {}
}
interface IDefault {
  default void defaultMethod(){ }
}
class CDefault extends ADefault implements IDefault {
  void fun(){
    defaultMethod();
  }
}

class Outer {
  void func() {
  }
  class Inner {
    void a() {
      func(); // this is not resolved properly
    }
  }
}

class NumericalPromotion{
  void num(long l){
    num(1+2);
  }
}

class VariableArity {
  void varargs(int a, String... s){}
  void bar() {
    varargs(1, "");
    varargs(1, "", "");
    varargs(1, new String[] {""});
    varargs(1);
    varargs();
  }



  void varargs(String... s);
}

class Autoboxing {
  void fun1(Integer i){}
  void fun2(Object i){}
  void fun3(int i){}
  void fun4(Boolean b){}
  void fun5(char c, Object... o){}
  void bar(){
    fun1(1);
    fun2(1);
    fun3(new Integer(2));
    fun4(true);
    fun5('c', 1, 2l, 3.0f);
  }
}

class GenericErasure<T extends CharSequence> {

  void fun(T charseq) {
    fun("");
    T var;
    fun(var);
  }

}

class OverloadingAutoboxing {
  abstract void process(int i);
  abstract void process(Integer i);
  void overloading(int int1, Integer integer1) {
    process(int1);
    process(integer1);
  }
  abstract void process2(Integer i);
  abstract void process2(int i);
  void overloading2(int int1, Integer integer1) {
    process2(int1);
    process2(integer1);
  }

  abstract void process3(int i);
  abstract void process3(Object o);
  void overloading3(int int1, Integer integer1) {
    process3(int1);
    process3(integer1);
  }
}

class VarargsMostSpecific {
  void varargs(String first, String second, Object... objects) {
  }

  void varargs(String... strings) {
  }

  void varargs_usage() {
    varargs("", "", new Object());
    varargs("", "", "");
  }

  void varargs2(Object... objects) {
  }

  void varargs2(String string, String... strings) {
  }

  void varargs_usage_2() {
    varargs2("", "", new Object());
    varargs2("", "", "");
  }
}

class GenericClass {

  class NestedGenericClass<T> {
    private NestedGenericClass(T argument) {
    }

    private void genericMethod(T argument) {
    }
  }

  class ComplexNestedGenericClass<T extends java.util.Collection<Object>> {
    private ComplexNestedGenericClass(T argument) {
    }

    private void complexGenericMethod(T argument) {
    }
  }
  class U extends java.util.ArrayList<Object> { }
  public void test() {
    NestedGenericClass<List<Object>> nestedGenericClass = new NestedGenericClass<List<Object>>(new java.util.ArrayList<Object>());
    nestedGenericClass.genericMethod(new java.util.LinkedList<Object>());
    ComplexNestedGenericClass<List<Object>> complexNestedGenericClass = new ComplexNestedGenericClass<List<Object>>(new java.util.ArrayList<Object>());
    complexNestedGenericClass.complexGenericMethod(new java.util.LinkedList<Object>());
    ComplexNestedGenericClass v1 = new ComplexNestedGenericClass(new java.util.ArrayList<Object>());
    ComplexNestedGenericClass<U> v2 = new ComplexNestedGenericClass<U>(new U());
    ComplexNestedGenericClass<? extends List<Object>> v3 = new ComplexNestedGenericClass<List<Object>>(new java.util.ArrayList<Object>());
  }
}

class VarArgsNotInvoked {
  void varargs3(String s, Object... o){}
  void varargs3(String s){}
  void varargs4(Object... o){}
  void varargs4(){}

  void test() {
    varargs3("");
    varargs4();
  }
}

class MyVarArgs {
  void varargs5(Object o) {}
  void varargs6(String s, Object... o) {}
}

class MyVarArgsTest extends MyVarArgs {
  void varargs5(String s, Object... o) {}
  void varargs6(Object o) {}

  void test() {
    varargs5(null);
    varargs5("");
    varargs6(null);
    varargs6("");
  }
}

class HidingOfStaticMethod {
  static class A<T> {
    static <E> void of(E e1, E e2) { }
    static void by(Object o1, Object o2) { }
  }

  static class B<T> extends A<T> {
    static <E extends Comparable<? super E>> void of(E e1, E e2) { }
    static void by(Object o1, Object o2) { }

    void tstFromB() {
      B.by("hello", "world"); // call to B.by(), as A.by() is hidden

      B.of("hello", "world"); // explicit call to B.of()
      B.of(new C(), new C()); // call to inherited method A.of() through B
    }
  }

  static class C {}

  void tstFromOutsideHierarchy() {
    A.by("hello", "world"); // explicit call to A.of()
    B.by("hello", "world"); // explicit call to B.of()

    A.of("hello", "world"); // explicit call to A.of()
    B.of("hello", "world"); // explicit call to B.of()
    B.of(new C(), new C()); // call to inherited method A.of() through B
  }
}

class ParametrizedCall {
  <T extends B> void foo() {}

  void tst() {
    foo();
    this.<C>foo();
  }

  static class B {}
  static class C extends B {}
}

class variadicGenericMethods {
  class S<T> {

  }
  static <T> S<T> to(T... values) {return null;}
  static <T> S<T> to(T t) {return null;}

  void fun() {
    String[] strings = new String[12];
    String string = "";
    to(string);
    to(strings);
    from(string);
    from(strings);
  }

  static <T> S<T> from(T t) {return null;}
  static <T> S<T> from(T... values) {return null;}

}

public static final class Builder<B> {
  public <T extends B> Builder<B> putAll() {
    Class<? extends T> type;
    T value;
    cast(type, value);
    return this;
  }

  private static <B, T extends B> T cast(Class<T> type, B value) {
    return null;
  }
}

class Predicate<S> {
  public static <T> Predicate<T> in(Collection<? extends T> inParam) {
    return null;
  }
}
class Maps<V> {
  private boolean removeIf(Predicate<? super V> valuePredicate) {
    return true;
  }

 public boolean removeAll(Collection<?> collection) {
    return removeIf(Predicate.in(collection));
  }

}
class MostSpecificArgType {
  class Parent<A> {}
  class Child<B> extends Parent<B> {}

  private <K> void myMethod(Parent<K> c){}
  private <T> void myMethod(Child<T> c){}

  void plop() {
    myMethod(new Child<String>());
  }
}
class OUTER {
  class A {

    void bar() {
      B<Integer> b = new B<>();
      b.foo(this::add);
    }

    private Integer add(Integer a, Integer b) {
      return a + b;
    }
  }

  class B<U> {
    void foo(java.util.function.BinaryOperator<U> op) {
    }
  }
}

class UnknownTypeMatching {
  void someFun(Unknown_type_1 param) {
    Unknown_type_2 localVar = null;
    someFun(localVar);
  }
}
