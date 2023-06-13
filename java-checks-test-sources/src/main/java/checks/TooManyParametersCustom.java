package checks;

import org.springframework.web.bind.annotation.RequestMapping;

public class TooManyParametersCustom {
  TooManyParametersCustom(int p1, int p2, int p3, int p4, int p5, int p6) { // Noncompliant {{Constructor has 6 parameters, which is greater than 5 authorized.}}
  }

  void method(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9) { // Noncompliant {{Method has 9 parameters, which is greater than 8 authorized.}}
  }
}

class MethodsUsingSpringRequestMappingCustom {
  @org.springframework.web.bind.annotation.RequestMapping
  void foo(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8, String p9) {} // Noncompliant - filtered out by SpringFilter

  @RequestMapping
  void bar(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8, String p9) {} // Noncompliant - filtered out by SpringFilter
}
