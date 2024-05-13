package org.foo;

@MyAnnotation // Noncompliant
//^[sc=2;ec=13]
public class A {

  @org.foo.MyAnnotation( // Noncompliant {{Remove the use of this forbidden class.}}
// ^^^^^^^^^^^^^^^^^^^^
    field = "53"
  )
  void foo() {
    MyAnnotation annotation; // Noncompliant
  }
}

@interface MyAnnotation {
  String field() default "42";
}
