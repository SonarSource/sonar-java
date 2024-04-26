import java.io.IOException;
import java.sql.SQLException;

public class ThrowsSeveralCheckedException extends Base {

  public void foo1() {
  }

  public void foo2() throws Throwable {
  }

  public void foo3() throws Error {
  }

  public void foo4() throws MyException {
  }

  public void foo5() throws RuntimeException {
  }

  public void foo6() throws IllegalArgumentException {
  }

  public void foo7() throws MyRuntimeException {
  }

  public void foo8() throws IllegalArgumentException, MyException, NullPointerException {
  }

  public void foo9() throws IOException, MyException { // Noncompliant {{Refactor this method to throw at most one checked exception instead of: java.io.IOException, ThrowsSeveralCheckedException$MyException}}
//            ^^^^
  }

  public void foo10() throws IOException, IOException, SQLException { // Noncompliant {{Refactor this method to throw at most one checked exception instead of: java.io.IOException, java.io.IOException, java.sql.SQLException}}
  }

  void foo11() throws IOException, IOException, SQLException {
  }

  public void foo12() throws IOException, UnknownException { // Compliant - in order to avoid false positives, we do not raise issue for unknown exceptions
  }

  public class MyException extends Exception {
  }

  public class MyRuntimeException extends RuntimeException {
  }

  @Override
  public void overridenMethod() throws IOException, SQLException { // Compliant - overriden methods
  }

}

class Base {

  public void overridenMethod() throws IOException, SQLException { // Noncompliant
  }

}

class Implements implements I {

  @Override
  public void foo() {
  }

  @Override
  public void bar() throws IOException, SQLException { // Compliant - overriden
  }

  public void baz() {
  }

  public void qux() throws IOException, SQLException { // Noncompliant
  }

}

interface I {
  public void foo();

  public void bar() throws IOException, SQLException; // Noncompliant

}

class BaseClass<T> {
  public T meth() throws IOException, SQLException{return null;} // Noncompliant
}
class BaseChildClass<T> extends BaseClass<T>{}
class Synth extends BaseChildClass<Integer> {
  @Override
  public Integer meth() throws IOException { return 1;}
}

class J {
  public void method(int a, String b) throws IOException, SQLException{} // Noncompliant
  public J method2(int a, String b) throws IOException, SQLException{return null;} // Noncompliant
  public String method3(int a, String b) throws IOException, SQLException{return null;} // Noncompliant
}
class K extends J {
  public void method(String a, String b) throws IOException, SQLException{} // Noncompliant
  public void method(int a, String b) throws IOException, SQLException{}
  public K method2(int a, String b) throws IOException, SQLException{return null;}
  public String method3(int a) throws IOException, SQLException{return null;} // Noncompliant
}
interface L {
  void method(String a, String b) throws IOException, SQLException; // Noncompliant
}
interface M extends L {}
class O implements M {
  public void method(String a, String b) throws IOException, SQLException {}
}
class P {
  public void method(Q a, String b) throws IOException, SQLException {} // Noncompliant
  private void privateMethod(Q a, String b) throws IOException, SQLException {}
  class Q{}
}
class R extends P {
  public void method(Q a, String b) throws IOException, SQLException {}
  public static void foo(Q a, String b) throws IOException, SQLException { // Noncompliant
    P p = new P() { // Ignore anonymous classes: false negative SONARJAVA-645
      public void method(Q a, String b) throws IOException, SQLException {}
    };
  }
}
class S<T> {
  public T method(T a) throws IOException, SQLException { return null;} // Noncompliant
}
class U extends S<String> {
  public String method(String a) throws IOException, SQLException { return null; } // false positive
}
class V extends P {
  public void privateMethod(Q a, String b) throws IOException, SQLException {} // Noncompliant
}
