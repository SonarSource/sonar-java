package references;

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
