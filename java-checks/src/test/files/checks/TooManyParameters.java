import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import com.fasterxml.jackson.annotation.JsonCreator;

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
  void foo(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant

  @RequestMapping
  void bar(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
}

class MethodsUsingSpringGetMapping {
  @org.springframework.web.bind.annotation.GetMapping
  void foo(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant

  @GetMapping
  void bar(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
}

class MethodsUsingSpringPostMapping {
  @org.springframework.web.bind.annotation.PostMapping
  void foo(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant

  @PostMapping
  void bar(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
}

class MethodsUsingSpringPutMapping {
  @org.springframework.web.bind.annotation.PutMapping
  void foo(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant

  @PutMapping
  void bar(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
}

class MethodsUsingSpringDeleteMapping {
  @org.springframework.web.bind.annotation.DeleteMapping
  void foo(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant

  @DeleteMapping
  void bar(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
}

class MethodsUsingSpringPatchMapping {
  @org.springframework.web.bind.annotation.PatchMapping
  void foo(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant

  @PatchMapping
  void bar(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
}

class MethodsUsingJsonCreator {
  @JsonCreator
  void foo(String p1, String p2, String p3, String p4, String p5, String p6, String p7, String p8) {} // Compliant
}
