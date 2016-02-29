package java.lang;

import org.springframework.web.bind.annotation.RequestMapping;

class TooManyParameters {
  TooManyParameters(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) { // Noncompliant {{Constructor has 8 parameters, which is greater than 7 authorized.}}
  }

  void method(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) { // Noncompliant [[sc=8;ec=14]] {{Method has 8 parameters, which is greater than 7 authorized.}}
  }

  void otherMethod(int p1) {}

  static void staticMethod(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) {} // Noncompliant
}

class TooManyParametersExtended extends TooManyParameters {
  @java.lang.Override
  void method(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) {}

  static void staticMethod(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) {} // Noncompliant
}

class MethodsUsingSpringRequestMapping {
  @org.springframework.web.bind.annotation.RequestMapping
  void foo(String p1, String p2, String p3, String p4, String p5, String p6, String p6, String p8) {} // Noncompliant

  @RequestMapping
  void bar(String p1, String p2, String p3, String p4, String p5, String p6, String p6, String p8) {} // Noncompliant
}
