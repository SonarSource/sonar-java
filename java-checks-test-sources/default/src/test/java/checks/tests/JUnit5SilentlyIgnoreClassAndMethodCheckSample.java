package checks.tests;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class JUnit5SilentlyIgnoreClassAndMethodCheckSample {

  @BeforeAll
  void beforeAll() {} // Compliant, limitation: we don't report missing "static" modifier.

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

    @BeforeAll
    private static void beforeAll() {} // Noncompliant {{Remove this 'private' modifier.}} [[quickfixes=qf1]]
//  ^^^^^^^
    // fix@qf1 {{Remove "private" modifier}}
    // edit@qf1 [[sc=5;ec=13]] {{}}

    @AfterAll
    private static void afterAll() {} // Noncompliant {{Remove this 'private' modifier.}} [[quickfixes=qf2]]
//  ^^^^^^^
    // fix@qf2 {{Remove "private" modifier}}
    // edit@qf2 [[sc=5;ec=13]] {{}}

    @BeforeEach
    static void beforeEach() {} // Noncompliant {{Remove this 'static' modifier.}} [[quickfixes=qf3]]
//  ^^^^^^
    // fix@qf3 {{Remove "static" modifier}}
    // edit@qf3 [[sc=5;ec=12]] {{}}

    @AfterEach
    static void afterEach() {} // Noncompliant {{Remove this 'static' modifier.}} [[quickfixes=qf4]]
//  ^^^^^^
    // fix@qf4 {{Remove "static" modifier}}
    // edit@qf4 [[sc=5;ec=12]] {{}}

    @Test
    private void testPrivate() {} // Noncompliant [[quickfixes=qf5]]
//  ^^^^^^^
    // fix@qf5 {{Remove "private" modifier}}
    // edit@qf5 [[sc=5;ec=13]] {{}}

    @Test
    static void staticTest() {} // Noncompliant [[quickfixes=qf6]]
//  ^^^^^^
    // fix@qf6 {{Remove "static" modifier}}
    // edit@qf6 [[sc=5;ec=12]] {{}}

    @Nested
    private class PrivateWithOneTest { // Noncompliant [[quickfixes=qf7]]
//  ^^^^^^^
      // fix@qf7 {{Remove "private" modifier}}
      // edit@qf7 [[sc=5;ec=13]] {{}}
      @Test
      void test() {}
    }

    @Test
    List<String> quickFixes() { return Collections.emptyList(); } // Noncompliant [[quickfixes=qf8]]
//  ^^^^^^^^^^^^
    // fix@qf8 {{Replace with void}}
    // edit@qf8 [[sc=5;ec=17]] {{void}}
    // edit@qf8 [[sc=40;ec=63]] {{}}
    
    @Test
    Object bar(boolean b, Object o) { // Noncompliant [[quickfixes=qf9]]
//  ^^^^^^
      // fix@qf9 {{Replace with void}}
      // edit@qf9 [[sc=5;ec=11]] {{void}}
      // edit@qf9 [[sl=+7;sc=16;el=+7;ec=39]] {{}}
      // edit@qf9 [[sl=+9;sc=14;el=+9;ec=15]] {{}}
      if (b) {
        return Collections.emptyList();
      }
      return o;
    }
  }
}
