interface A extends B {}
interface B extends C<String> {}
interface C<T> extends D<T> {}
interface D<E> {
  E get();
}
class X extends Y{}
class Y extends W<String> {}
class W<T> extends Z<T> {}
class Z<E> {
  E get() {
    return null;
  }
}
class Foo { W<String> foo(){} }
interface Bar { W<String> bar();}

class Test {
  A a;
  X x;
  Foo foo;
  Bar bar;

  void fun(String s) {
    fun(a.get());
    fun(x.get());
    fun(foo.foo().get());
    fun(bar.bar().get());
  }
}
abstract class NotDefered {
  abstract <T, U> T plop(T t);

  void gul(Integer i) {}

  void gul(String s) {}

  void test() {
    gul(plop("hello"));
  }
}
class MostSpecificArgTypeWithGenerics {
  class Parent<A> {}
  class Child<C, B> extends Parent<Parent<B>> implements IParent<B> {}
  interface IParent<A> {}


  private void myMethod(Parent<Parent<String>> c){}
  private void myMethod2(Parent<?> c){}
  private void myMethod3(IParent<String> c){}

  void plop() {
    Child<Integer, String> param = new Child<Integer, String>();
    myMethod(param);
    myMethod2(param);
    myMethod3(param);
  }
}
class RawTypeBound<C extends Comparable> {
  C field;
  <S extends Comparable<?>> void meth(S s){
    meth(field);
  }
}
