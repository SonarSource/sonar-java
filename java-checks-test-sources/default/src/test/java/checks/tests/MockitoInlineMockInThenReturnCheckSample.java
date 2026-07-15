package checks.tests;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MockitoInlineMockInThenReturnCheckSample {

  interface Foo {
    String method();
  }

  class MyClass {
    Foo foo() {
      return null;
    }
  }

  @Test
  void noncompliantInlineMock() {
    MyClass myMock = mock(MyClass.class);
    when(myMock.foo()).thenReturn(mock(Foo.class)); // Noncompliant {{Extract this mock creation to a local variable.}}
  //                              ^^^^^^^^^^^^^^^
  }

  @Test
  void compliantExtractedMock() {
    MyClass myMock = mock(MyClass.class);
    Foo fooMock = mock(Foo.class);
    when(myMock.foo()).thenReturn(fooMock); // Compliant
  }

  @Test
  void compliantLiteralArg() {
    MyClass myMock = mock(MyClass.class);
    when(myMock.foo()).thenReturn(null); // Compliant
  }
}
