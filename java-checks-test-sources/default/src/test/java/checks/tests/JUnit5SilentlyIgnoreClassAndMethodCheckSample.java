package checks.tests;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class JUnit5SilentlyIgnoreClassAndMethodCheckSample {

  @Test
  private void testPrivate() {} // Noncompliant {{Remove this 'private' modifier.}}
//^^^^^^^

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
  private class PrivateWithOneTest { // Noncompliant {{Remove this 'private' modifier.}}
//^^^^^^^
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
  static void staticTest() {} // Noncompliant {{Remove this 'static' modifier.}}
//^^^^^^

  @Test
  int testReturningValue() { return 0; } // Noncompliant {{Replace the return type by void.}}
//^^^

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
    private void testPrivate() {} // Noncompliant [[quickfixes=qf4]]
//  ^^^^^^^
    // fix@qf4 {{Remove modifier}}
    // edit@qf4 [[sc=5;ec=12]] {{}}

    @Test
    static void staticTest() {} // Noncompliant [[quickfixes=qf5]]
//  ^^^^^^
    // fix@qf5 {{Remove modifier}}
    // edit@qf5 [[sc=5;ec=11]] {{}}

    @Nested
    private class PrivateWithOneTest { // Noncompliant [[quickfixes=qf3]]
//  ^^^^^^^
      // fix@qf3 {{Remove modifier}}
      // edit@qf3 [[sc=5;ec=12]] {{}}
      @Test
      void test() {}
    }

    @Test
    List<String> quickFixes() { return Collections.emptyList(); } // Noncompliant [[quickfixes=qf1]]
//  ^^^^^^^^^^^^
    // fix@qf1 {{Replace with void}}
    // edit@qf1 [[sc=5;ec=17]] {{void}}
    // edit@qf1 [[sc=40;ec=63]] {{}}
    
    @Test
    Object bar(boolean b, Object o) { // Noncompliant [[quickfixes=qf2]]
//  ^^^^^^
      // fix@qf2 {{Replace with void}}
      // edit@qf2 [[sc=5;ec=11]] {{void}}
      // edit@qf2 [[sl=+6;sc=16;el=+6;ec=39]] {{}}
      // edit@qf2 [[sl=+8;sc=14;el=+8;ec=15]] {{}}
      if (b) {
        return Collections.emptyList();
      }
      return o;
    }
  }
}
