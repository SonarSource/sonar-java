import com.google.common.collect.Lists;

import java.lang.Exception;

class A {

  int var;

  abstract int foo();

  int foo(int u) {
    int x = 0;// Noncompliant {{Remove this useless assignment to local variable "x".}} [[sc=11;ec=14]]
    x = 3;
    int y = x + 1; // Noncompliant {{Remove this useless assignment to local variable "y".}} [[sc=11;ec=18]]
    x = 2; // Noncompliant {{Remove this useless assignment to local variable "x".}} [[sc=7;ec=10]]
    x = 3;
    y = 2;
    foo(y);
    foo(x);
    Object a = new Object();
    System.out.println(a);
    a = null; // Noncompliant [[sc=7;ec=13]]
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

  Object anonymous_class() {
    int a,b = 0; // Noncompliant
    a = 42;
    if(a == 42) {
      b = 12; // Noncompliant
    }
    return new Object() {
      @Override
      public String toString() {
        b = 14; // Noncompliant
        return a;
      }
    };
  }

  void lambdas_using_local_vars() {
    int a;
    if(cond) {
      a = 42;
    }
    plop(y -> a + y);
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
    class A {
      int fun() {
        return a;
      }
    }
    return new A();
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

  Object try_with_resource() {
    String path = ""; // compliant
    try (BufferedReader br = new BufferedReader(new FileReader(path))) {
      br.readLine();
    }

    try ( FileInputStream in = new FileInputStream( storageFile );
          FileLock lock = in.getChannel().lock(0, Long.MAX_VALUE, true) ) // compliant, this will be closed in the implicit finally
    {
      in.read();
    }

  }

  public class MyClass {
    private static class Foo {
      void bar(int p){
      }
    }

    public static void main(String... args) {
      Foo x = new Foo(); // compliant
      List<Integer> list = new ArrayList<>();
      list.forEach(x::bar);
    }
    int foo() {
      int i,j;
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

  private void foo(int y) {
    final int x = 1;
    switch (y) {
      case x:
        System.out.println("1");
      default:
        System.out.println("2");
    }
  }
}

class Stuff {
  void foo(boolean b1, boolean b2) {
    boolean x = false;  // Noncompliant
    x = b1 && b2;       // Noncompliant
    ((x)) = b1 && b2;   // Noncompliant
  }

  void assertStatement(boolean x) {
    boolean y = !x; // compliant, y is used in assert statement.
    assert y;
  }

}
