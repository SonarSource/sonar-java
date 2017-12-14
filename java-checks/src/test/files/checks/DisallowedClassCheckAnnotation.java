package org.foo;

@MyAnnotation // Noncompliant [[sc=2;ec=14]]
public class A {

  @org.foo.MyAnnotation( // Noncompliant [[sc=4;ec=24]] {{Remove the use of this forbidden class.}}
    field = "53"
  )
  void foo() {
    MyAnnotation annotation; // Noncompliant
  }
}

@interface MyAnnotation {
  String field() default "42";
}
