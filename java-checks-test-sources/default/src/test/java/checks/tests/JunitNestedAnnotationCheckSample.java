package checks.tests;

import java.util.function.Consumer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class JunitNestedAnnotationCheckSample { // Compliant, this class has some tests but it's not an inner class

  @Test
  void a_test_in_a_non_inner_class() {
  }

  class InnerClassWithoutTest { // Compliant, this inner class has no test
    void not_a_test() {
    }
  }

  class NotAnnotatedInnerClassWithOneTest { // Noncompliant {{Add @Nested to this inner test class}}
//      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    @ParameterizedTest
    @ValueSource(strings = { "a", "b", "c" })
    void test(String value) {
    }
  }

  class NotAnnotatedInnerClassWithTwoTests { // Noncompliant
    int field;
    @Test
    void test1() {
    }
    @Test
    void test2() {
    }
  }

  @Nested
  class AnnotatedInnerClassWithOneTest { // Compliant
    @Test
    void test() {
    }
  }

  // By default Maven Surefire Plugin does not execute static nested classes, but it's not a JUnit 5 problem,
  // it's a maven configuration problem that is not in the scope of this rule.
  static class NotAnnotatedStaticNestedClassWithTests { // Compliant
    @Test
    void test() {
    }
  }

  @Nested
  static class AnnotatedStaticNestedClassWithTests { // Noncompliant {{Remove @Nested from this static nested test class or convert it into an inner class}}
    @Test
    void test() {
    }
  }

  class OutOfScope {
    void foo() {
      new Object() { // Compliant, anonymous classes are out of scope of this rule
        @Test
        public String test() {
          return "";
        }
      };
      class A {
        @Test
        void test() { // Compliant, classes in method body are out of scope of this rule
        }
      }
      Consumer<String> c = value -> {
        class B {
          @Test
          void test() { // Compliant, classes in lambda body are out of scope of this rule
          }
        }
      };
    }
  }

  abstract class AbstractInner { // Compliant abstract classes do no not need to be annottated
    protected abstract String abstractMethod();

    @Test
    void test() {
    }
  }

}
