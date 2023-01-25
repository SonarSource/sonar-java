package checks.tests;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class JUnit5SilentlyIgnoreClassAndMethodCheck {

  @Test
  private void testPrivate() {} // Noncompliant [[sc=3;ec=10]] {{Remove this 'private' modifier.}}

  @Test
  void testDefault() {} // Compliant

  @Test
  protected void testProtected() {} // Compliant

  @Test
  public void testPublic() {} // Compliant

  private void notATest() {} // Compliant

  @org.junit.Test
  private void testWithJUnit4Annotation() {} // Compliant, this rule does not target JUnit4

  @Test
  final void finalTestMethod() {} // Compliant

  @Nested
  private class PrivateWithoutTest { // Compliant

  }

  @Nested
  private class PrivateWithOneTest { // Noncompliant [[sc=3;ec=10]] {{Remove this 'private' modifier.}}
    @Test
    void test() {}
  }

  @Nested
  public class PublicWithOneTest { // Compliant
    @Test
    void test() {}
  }

  @Nested
  class DefaultWithOneTest { // Compliant
    @Test
    void test() {}
  }

  @Test
  static void staticTest() {} // Noncompliant [[sc=3;ec=9]] {{Remove this 'static' modifier.}}

  @Test
  int testReturningValue() { return 0; } // Noncompliant [[sc=3;ec=6]] {{Replace the return type by void.}}

  @TestFactory
  Collection<DynamicTest> testFactory() { // Compliant, TestFactory returning a value
    return Collections.emptyList();
  }

  @Nested
  static class AnnotatedStaticNestedClassWithTests {  // Compliant, bug raises by S5790
    @Test
    void test() {
    }
  }

  @Nested
  class Quickfixes {
    @Test
    List<String> quickFixes() { return Collections.emptyList(); } // Noncompliant [[sc=5;ec=17;quickfixes=qf1]]
    // fix@qf1 {{Replace with void}}
    // edit@qf1 [[sc=5;ec=17]] {{void}}
    @Test
    String toEmptyString() { return ""; } // Noncompliant [[sc=5;ec=11;quickfixes=qf2]]
    // fix@qf2 {{Replace with void}}
    // edit@qf2 [[sc=5;ec=11]] {{void}}
  }
}
