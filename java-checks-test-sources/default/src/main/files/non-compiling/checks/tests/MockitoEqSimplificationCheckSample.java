package checks.tests;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.Matchers;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MockitoEqSimplificationCheckSample {

  @Captor
  ArgumentCaptor<Object> captor;

  @Test
  public void myTest() {
    Foo foo = mock(Foo.class);
    Object v1 = new Object();
    Object v2 = new Object();
    Object v3 = new Object();
    Object v4 = new Object();
    Object v5 = new Object();

    given(foo.bar(eq(v1), // Noncompliant {{Remove this and every subsequent useless "eq(...)" invocation; pass the values directly.}}
//                ^^
      eq(v2),
//    ^^<
      eq(v3))).willReturn(null);
//    ^^<
    when(foo.baz(eq(v4), // Noncompliant {{Remove this and every subsequent useless "eq(...)" invocation; pass the values directly.}}
//               ^^
      eq(v5))).thenReturn("foo");
//    ^^<
    when(foo.baz(Matchers.eq(v4), // Noncompliant
//               ^^^^^^^^^^^
      eq(v5))).thenReturn("foo");
//    ^^<
    doThrow(new RuntimeException()).when(foo).quux(eq(42)); // Noncompliant {{Remove this useless "eq(...)" invocation; pass the values directly.}}
//                                                 ^^
    doCallRealMethod().when(foo).baz(eq(v4), eq(v5)); // Noncompliant
    verify(foo).bar(eq(v1), eq(v2), eq(v3)); // Noncompliant
    verify(foo, never()).bar(eq(v1), eq(v2), eq(v3)); // Noncompliant

    InOrder inOrder = Mockito.inOrder(foo);
    inOrder.verify(foo).bar(eq(v1), eq(v2), eq(v3)); // Noncompliant
    inOrder.verify(foo).baz(eq(v1), eq(v2)); // Noncompliant

    given(foo.bar(v1, v2, v3)).willReturn(null);
    when(foo.baz(v4, v5)).thenReturn("foo");
    doThrow(new RuntimeException()).when(foo).quux(42);
    doCallRealMethod().when(foo).baz(v4, v5);
    verify(foo).bar(v1, v2, v3);
    verify(foo, never()).bar(v1, v2, v3);

    given(foo.noArg()).willReturn(null);
    verify(foo).noArg();

    InOrder inOrder2 = Mockito.inOrder(foo);
    inOrder2.verify(foo).bar(v1, v2, v3);
    inOrder2.verify(foo).baz(v1, v2);

    // No issue when mixed:

    given(foo.bar(eq(v1), endsWith(""), v3)).willReturn(null);
    given(foo.bar(eq(v1), any(), eq(v3))).willReturn(null);
    given(foo.baz(eq(v1), captor.capture())).willReturn(null);

    // When raw values are mixed with matchers, Mockito will throw InvalidUseOfMatchersException.
    // S6073 should cover this case.
    given(foo.bar(v1, eq(v2), v3)).willReturn(null); // Compliant
    given(foo.bar(v1, eq(v2), any())).willReturn(null); // Compliant

    when(foo).thenReturn(foo); // Compliant, does not make sense, for coverage

    // Extra parenthesis
    given((foo.bar(eq(v1), eq(v2), eq(v3)))).willReturn(null); // Noncompliant
    given(foo.bar((eq(v1)), ((eq(v2))), eq(v3))).willReturn(null); // Noncompliant
    given(foo.bar(eq(v1), (any()), eq(v3))).willReturn(null); // Compliant
    given(foo.bar((v1), ((v2)), (v3))).willReturn(null); // Compliant
  }

  class Foo {
    Foo t = this;

    Object bar(Object o1, Object o2, Object o3) {
      return null;
    }

    Object baz(Object o1, Object o2) {
      return null;
    }

    Object quux(int i) {
      return null;
    }

    Object noArg() {
      return null;
    }

  }

}
