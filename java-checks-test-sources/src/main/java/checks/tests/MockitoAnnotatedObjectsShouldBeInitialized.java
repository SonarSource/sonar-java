package checks.tests;

import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.MockitoRule;
import org.mockito.junit.jupiter.MockitoExtension;

public class MockitoAnnotatedObjectsShouldBeInitialized {
  class MixedNonCompliant {
    @Mock // Noncompliant {{Initialize mocks before using them.}}
    private Bar bar;

    @Spy // Issue is only reported on the first annotation
    private Baz baz;

    @InjectMocks // Issue is only reported on the first annotation
    private Foo fooUnderTest;

    @Test
    void someTest() {
      // test something ...
    }
  }

  @RunWith(EmptyRunner.class)
  public class RunWithBadParameter {
    @Mock // Compliant FN is a runner that does not initialize anything
    private Bar bar;
  }

  @RunWith(Runner.class)
  public class RunWithAbstractRunner {
    @Mock // Compliant FN is a runner that but not a concrete one
    private Bar bar;
  }

  @Nullable
  public class IrrelevantAnnotationOnClass {
    @Mock // Noncompliant
    private Bar bar;
  }

  @RunWith(MockitoJUnitRunner.class)
  public class JUnit4AnnotatedTest {
    @Mock // Compliant
    private Bar bar;
  }

  @ExtendWith(EmptyExtension.class)
  public class ExtendWithEmptyParameter {
    @Mock // Compliant FN is an extension that does not initialize anything
    private Bar bar;
  }

  @ExtendWith(Extension.class)
  public class ExtendWithAbstractExtension {
    @Mock // Compliant FN use the base interface
    private Bar bar;
  }

  @ExtendWith(MockitoExtension.class)
  public class JUnit5AnnotatedTest {
    @Mock // Compliant
    private Bar bar;
  }


  public class UntaggedRule {
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock // Noncompliant {{Initialize mocks before using them.}}
    private Bar bar;
  }

  public class UninitializedRule {
    @Rule
    public MockitoRule rule;

    @Mock // Noncompliant {{Initialize mocks before using them.}}
    private Bar bar;
  }

  public class NullInitializedRule {
    @Rule
    public MockitoRule rule = null;

    @Mock // Noncompliant {{Initialize mocks before using them.}}
    private Bar bar;
  }

  public class PoorlyInitializedRule {
    @Rule
    public MockitoRule rule = FakeRule.returnNull();

    @Mock // Noncompliant {{Initialize mocks before using them.}}
    private Bar bar;
  }

  public class FalseNegativeRule {
    @Rule
    public MockitoRule rule = FakeRule.rule();

    @Mock // Compliant FN
    private Bar bar;
  }

  public class FooTest2 {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock // Compliant
    private Bar bar;
  }

  public class UntaggedSetup {
    @Mock // Noncompliant {{Initialize mocks before using them.}}
    private Bar bar;

    void setUp() {
      MockitoAnnotations.initMocks(this);
    }
  }

  public class SetupWithoutInitMocks {
    @Mock // Noncompliant {{Initialize mocks before using them.}}
    private Bar bar;

    @BeforeEach
    void setUp() {
      System.out.println("No call to initMocks");
    }
  }

  public class EmptySetup {
    @Mock // Noncompliant {{Initialize mocks before using them.}}
    private Bar bar;

    @BeforeEach
    void setUp() {
    }
  }

  public class SetupJunit4 {
    @Mock // compliant
    private Bar bar;

    @Before
    void setUp() {
      MockitoAnnotations.initMocks(this);
    }
  }

  public class SetupJUnit5 {
    @Mock // Compliant
    private Bar bar;

    @BeforeEach
    void setUp() {
      MockitoAnnotations.openMocks(this);
    }
  }

  public class SetupMix {
    @Mock // Compliant
    private Bar bar;

    @BeforeEach
    void setUp() {
      MockitoAnnotations.initMocks(this);
    }
  }

  class SetupInBaseClass {
    @BeforeEach
    void baseSetUp() {
      MockitoAnnotations.initMocks(this);
    }
  }

  class MockInChild extends SetupInBaseClass {
    @Mock // Compliant
    private Bar bar;
  }

  class NoSetup {
  }

  class MockInGrandChild extends NoSetup {
    @Mock // Compliant FN Super classes are not explored
    private Bar bar;
  }

  @ExtendWith(MockitoExtension.class)
  public class Nesting {
    @org.junit.jupiter.api.Nested
    public class Nested {
      @Mock
      private Bar bar;
    }

    public class NestedAsWell {
      @Mock // Noncompliant
      private Bar bar;

      @org.junit.jupiter.api.Nested
      public class NestedFurther {
        @Mock
        private Bar bar;
      }
    }
  }

  @RunWith(MockitoJUnitRunner.class)
  public class NestingWithWrongAnnotation {
    @Nested
    public class NestedButNotAnnotated {
      @Mock // Noncompliant
      private Bar bar;
    }

    public class NestedAsWellButAnnotated {
      @Nested
      public class NestedFurther {
        @Mock // Noncompliant
        private Bar bar;
      }
    }
  }

  public class NestingButNotAnnotated {
    public class NestedButNotAnnotated {
      @Mock // Noncompliant
      private Bar bar;
    }

    @ExtendWith(MockitoExtension.class)
    public class NestedAsWellButAnnotated {
      @org.junit.jupiter.api.Nested
      public class NestedFurther {
        @Mock
        private Bar bar;
      }
    }
  }

  private class Bar {
  }

  private class Baz {
  }

  private class Foo {
  }

  private static class EmptyRunner extends org.junit.runner.Runner {

    @Override
    public Description getDescription() {
      return null;
    }

    @Override
    public void run(RunNotifier runNotifier) {
    }
  }

  private static class EmptyExtension implements Extension {

  }

  private static class FakeRule {
    static MockitoRule rule() {
      return null;
    }

    static MockitoRule returnNull() {
      return null;
    }
  }
}
