package checks.tests;


import org.junit.Before;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.MockitoRule;

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

  @RunWith(MockitoJUnitRunner.class)
  public class FooTest {
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

  public class PoorlyIntializedRule {
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

  public class SetupJUnit5 {
    @Mock // Compliant
    private Bar bar;

    @BeforeEach
    void setUp() {
      MockitoAnnotations.openMocks(this);
    }
  }

  public class FooTest3 {
    @Mock // Compliant
    private Bar bar;

    @BeforeEach
    void setUp() {
      MockitoAnnotations.initMocks(this);
    }
  }

  public class SetupWithLegacyAnnotation {
    @Mock // compliant
    private Bar bar;

    @Before
    void setUp() {
      MockitoAnnotations.initMocks(this);
    }
  }


  private class Bar {
  }

  private class Baz {
  }

  private class Foo {
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
