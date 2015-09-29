import com.google.common.collect.Lists;

import java.lang.Exception;

class A {

  abstract int foo();

  int foo(int u) {
    int x = 0;// Noncompliant {{Remove this useless assignment to local variable "x".}}
    x = 3;
    int y = x + 1; // Noncompliant {{Remove this useless assignment to local variable "y".}}
    x = 2; // Noncompliant {{Remove this useless assignment to local variable "x".}}
    x = 3;
    y = 2;
    foo(y);
    foo(x);
    Object a = new Object();
    System.out.println(a);
    a = null; // Noncompliant
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
    //false positive : read in catch is not considered and exceptional path with no assignment not considered
    int b = 2;// Noncompliant
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
    ++b; // Noncompliant
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
    (i) = 12; // Noncompliant
  }
  int parenthesis_identifier_in_postfix_increment() {
    int j = 0;
    for (int i = 0; i < 10; ++j, ++i) ;
    int b = 0;
    return (b)++; // Noncompliant
  }
  void foo() {
    int i = 0;
    ++i; // Noncompliant
    System.out.println("");
    return;
  }

}
