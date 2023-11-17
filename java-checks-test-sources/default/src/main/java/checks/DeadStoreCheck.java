package checks;

import com.google.common.collect.Lists;

import java.lang.Exception;
import java.io.*;
import java.util.function.IntFunction;

class DeadStoreCheck {

  int var;

  int foo(int u) {
    int x = 0;// Compliant - default value
    x = 3; // Noncompliant
    x = 4;
    int y = x + 1; // Noncompliant {{Remove this useless assignment to local variable "y".}} [[sc=11;ec=18]]
    x = 2; // Noncompliant {{Remove this useless assignment to local variable "x".}} [[sc=7;ec=10]]
    x = 3;
    y = 2;
    foo(y);
    foo(x);
    Object a = new Object();
    System.out.println(a);
    a = null; // Noncompliant [[sc=7;ec=13]]
    return 0;
  }

  void fields() {
    this.var = 2; // Compliant
    var = 3; // Compliant - do not check fields
  }

  void foo2() {
    Object a = "";
    for (int i = 0; i < 10; i++) {
      a = "";
    }
    System.out.println(a);
  }

  void lambdas_using_local_vars(IntFunction lambda) {
    int a = 42;
    lambdas_using_local_vars(y -> a + y);
  }

  void ignore_try_finally() {
    int a;
    a = 12; //false negative excluded by try finally
    try {

    }finally {
      a = a + 1 ;
    }
  }

  void ignore_multiple_try_finally() {
    int a;
    a = 12; //false negative excluded by try finally
    try {

    }finally {
      a = a + 1 ;
    }
    try {

    }finally {
    }
  }



  public boolean try_finally_return(boolean satisfied) {
    try {
      return satisfied = true; // compliant but by exclusion of try catch - CFG of try finally should be fixed.
    } finally {
      if (!satisfied) {
      }
    }
  }

  void try_finally_in_inner_class_should_not_exclude_method() {
    int a;
    a = 12; // Noncompliant
    class inner {
      void foo() {
        try {

        }finally {

        }
      }
    }
  }
  void for_each_statement() {
    int a = 0;
    for (String elem: Lists.newArrayList(" ", "")) {
      System.out.println(a);
      a = 2;
    }
    System.out.println(a);
  }

  int read_var_in_catch() {
    int a = -1;
    int b = 2;
    try {
      a = 2;
      b = raisingExceptionMethod();
      System.out.println(a);
    }catch (Exception e) {
      System.out.println(a);
    }
    return b;
  }

  Object inner_class() {
    int a = 12;
    class B {
      int fun() {
        return a;
      }
    }
    return new B();
  }

  int increment_operator() {
    int i = 0;
    int b = 12;
    ++b; // Noncompliant [[sc=5;ec=8]]
    int c = 0;
    foo(++c); // compliant not last element of block
    int j = -1;
    while ((j = foo(++j)) != -1) {
      System.out.println("");
    }
    if(i != 0) {
      return i++; // Noncompliant
    } else {
      return ++i;
    }
  }

  void parenthesis_identifier_in_assignement() {
    int i = 0;
    System.out.println(i);
    (i) = 12; // Noncompliant [[sc=9;ec=13]]
  }
  int parenthesis_identifier_in_postfix_increment() {
    int j = 0;
    for (int i = 0; i < 10; ++j, ++i) ;
    int b = 0;
    return (b)++; // Noncompliant [[sc=12;ec=17]]
  }
  void foo() {
    int i = 0;
    ++i; // Noncompliant
    System.out.println("");
    return;
  }

  Object variable_initialized() {
    Object foo = null; // compliant variable should be initialized
    try {
      foo = raisingExceptionMethod();
    } catch (Exception e){
    }
    return foo;
  }

  private static int raisingExceptionMethod() throws Exception { 
    return 0;
  }

  void try_with_resource(File storageFile) throws Exception {
    String path = ""; // compliant
    try (BufferedReader br = new BufferedReader(new FileReader(path))) {
      br.readLine();
    }

    try ( FileInputStream in = new FileInputStream( storageFile );
          java.nio.channels.FileLock lock = in.getChannel().lock(0, Long.MAX_VALUE, true) ) // compliant, this will be closed in the implicit finally
    {
      in.read();
    }

  }

  void try_with_resource_java9() throws Exception {
    final FileInputStream fis = new FileInputStream("..."); // compliant
    try (fis) {

    }
  }

  public static class MyClass {
    private static class Foo {
      void bar(int p){
      }
    }

    public static void main(String... args) {
      Foo x = new Foo(); // compliant
      java.util.List<Integer> list = new java.util.ArrayList<>();
      list.forEach(x::bar);
    }
    int foo() {
      int i = 0;
      int j = 0;
      i = i + 1; // Noncompliant
      j += 1; // Noncompliant
      int k = 0;
      k += 2;
      System.out.println(k);
      int n;
      n = 2;
      n += 12; // Noncompliant
      int order = 0;
      return (short) (order &= 12);
    }
  }

  private void foo3(int y) {
    final int x = 1;
    switch (y) {
      case x:
        System.out.println("1");
      default:
        System.out.println("2");
    }
  }
}

class DeadStoreCheckStuff {
  void foo(boolean b1, boolean b2) {
    boolean x = false;  // Compliant
    x = b1 && b2;       // Noncompliant
    ((x)) = b1 && b2;   // Noncompliant
  }

  void assertStatement(boolean x) {
    boolean y = !x; // compliant, y is used in assert statement.
    assert y;
  }

}

class DeadStoreCheckNoIssueOnInitializers {

  // no issue if variable initializer is 'true' or 'false'
  boolean testBoolean(boolean arg0) {
    boolean b1 = true; // Compliant
    b1 = false;        // Noncompliant
    b1 = arg0;

    boolean b2 = false; // Compliant
    b2 = true;          // Noncompliant
    b2 = arg0;

    boolean b3 = arg0;  // Noncompliant
    b3 = arg0;

    return b1 && b2 && b3;
  }

  // no issue if initializer is 'null'
  Object testNull(boolean b, Object o) {
    Object o1 = null;  // Compliant
    o1 = new Object(); // Noncompliant
    o1 = null;         // Noncompliant
    o1 = o;

    Object o2 = o; // Noncompliant
    o2 = null;

    return b ? o1 : o2;
  }

  //no issue if initializer is the empty String
  String testNull(String s) {
    String s1 = ""; // Compliant
    s1 = "yolo";    // Noncompliant
    s1 = "hello";

    String s2 = "world"; // Noncompliant
    s2 = "moto";

    return s1 + s2;
  }

  // no issue if variable initializer is '-1', '0', or '1'
  int testIntLiterals() {

    int a = +42;  // Noncompliant

    int b = (0);  // Compliant
    b = -1;       // Noncompliant - Only taken into consideration when used in initializer
    b = 0;        // Noncompliant
    b = 1;        // Noncompliant
    int c = +1;   // Compliant
    int d = (-1); // Compliant
    int e = -1;   // Compliant

    // Only int literals are excluded
    long myLong = -1L;       // Noncompliant
    double myDouble = -1.0d; // Noncompliant
    float myFloat = -1.0f;   // Noncompliant

    short myShort = -1; // Compliant
    byte myByte = 1; //Compliant

    return 0;
  }
}
class DeadStoreCheckB {
  void foo() {
    int attemptNumber = 0;
    while (true) {
      try {
        attemptNumber++; // compliant this is handled in the catch block
        throw new MyException();
      } catch (MyException e) {
        if (attemptNumber >= 10) {
          System.exit(1);
        }
      }
    }
  }
  static class MyException extends Exception {  }
}

abstract class DeadStoreCheckC {

  public void testCodeWithForLoop2() {
    RuntimeException e = null;
    for (;;) {
      try {
        e = new RuntimeException();
        break;
      } finally {
        doSomething();
      }
    }
    throw e;
  }

  public void testCodeWithForLoop3() {
    RuntimeException e = null;
    for (int i = 0; i < 2; ) {
      try {
        e = new RuntimeException();
        break;
      } finally {
        doSomething();
      }
    }
    throw e;
  }

  public void testCodeWithWhileLoop() {
    RuntimeException e = null;
    while(true) {
      try {
        e = new RuntimeException();
        break;
      } finally {
        doSomething();
      }
    }
    throw e;
  }

  abstract void doSomething();

  public class FooException extends Exception { }
}
