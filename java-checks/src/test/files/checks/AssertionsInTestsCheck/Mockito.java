import org.junit.Test;

import org.mockito.InOrder;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

class MockitoTest {

  @Test
  public void nothing() { // Noncompliant
    int myInt = 42;
  }

  @Test
  public void verify_single_argument() { // Compliant
    A a = mock(A.class);
    Object myParam = new Object();

    a.add(myParam);

    verify(a).add(myParam);
  }

  @Test
  public void verify_multiple_argument() { // Compliant
    A a = mock(A.class);
    Object myParam = new Object();
    Object myOtherParam = new Object();

    a.add(myParam);

    org.mockito.Mockito.verify(a, never()).add(myOtherParam);
  }

  @Test
  public void verify_zero_interaction() { // Compliant
    A a = mock(A.class);
    verifyZeroInteractions(a);
  }

  @Test
  public void verify_zero_interaction_multiple_params() { // Compliant
    A a = mock(A.class);
    MockitoTest m = mock(MockitoTest.class);
    org.mockito.Mockito.verifyZeroInteractions(a, m, mock(Object.class));
  }

  @Test
  public void inOrder() {
    A firstMock = mock(A.class);
    A secondMock = mock(A.class);
    InOrder inOrder = inOrder(firstMock, secondMock);

    inOrder.verify(firstMock).add("was called first");
    inOrder.verify(secondMock).add("was called second");

  }

  static class A {
    void add(Object o);
  }

}
