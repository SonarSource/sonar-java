package checks.tests;


import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import static org.mockito.AdditionalMatchers.gt;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.intThat;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MockitoArgumentMatchersUsedOnAllParameters {

  @Test
  public void nonCompliant() {
    Foo foo = mock(Foo.class);
    Integer i1 = null, i2 = null, val1 = null, val2 = null;
    given(foo.bar(
      anyInt(),
      i1, // Noncompliant [[sc=7;ec=9;secondary=+1]] {{Add an "eq()" argument matcher on these parameters.}}
      i2))
      .willReturn(null);
    when(foo.baz(eq(val1), val2)).thenReturn("hi");// Noncompliant [[sc=28;ec=32]] {{Add an "eq()" argument matcher on this parameter.}}
    doThrow(new RuntimeException()).when(foo).quux(intThat(x -> x >= 42), -1); // Noncompliant [[sc=75;ec=77]] {{Add an "eq()" argument matcher on this parameter.}}
    verify(foo).bar(
      i1, // Noncompliant [[sc=7;ec=9;secondary=+2]] {{Add an "eq()" argument matcher on these parameters.}}
      anyInt(),
      i2);

    ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
    verify(foo).bar(captor.capture(),
      i1, // Noncompliant [[sc=7;ec=9]] {{Add an "eq()" argument matcher on this parameter.}}
      any());

    verify(foo).bar(anyInt(),
      i1.toString(), // Noncompliant [[sc=7;ec=20]] {{Add an "eq()" argument matcher on this parameter.}}
      any());

    // FP Wrapped argument matcher is masked by behind a method call
    verify(foo).bar(
      wrapArgumentMatcher(42), // Noncompliant [[sc=7;ec=30]] {{Add an "eq()" argument matcher on this parameter.}}
      any(), any());
  }

  @Test
  public void compliant() {
    Foo foo = mock(Foo.class);
    Integer i1 = null, i2 = null, val1 = null, val2 = null;
    given(foo.bar(anyInt(), eq(i1), eq(i2))).willReturn(null);
    when(foo.baz(val1, val2)).thenReturn("hi");
    doThrow(new RuntimeException()).when(foo).quux(intThat(x -> x >= 42), eq(-1));
    verify(foo).bar(eq(i1), anyInt(), eq(i2));
    verify(foo).bop();

    ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
    verify(foo).bar(captor.capture(), any(), any());

    // Casting a matcher is compliant
    verify(foo).bar((Integer) anyInt(), any(), any());
    verify(foo).bar((Integer) captor.capture(), any(), any());
    verify(foo).bar((Integer) (Number) (Integer) captor.capture(), any(), any());
    verify(foo).bar(((Integer) ((Number) ((Integer) captor.capture()))), any(), any());

    // Mix argument checkers from top Mockito and ArgumentMatchers classes
    verify(foo).bar(Mockito.anyInt(), any(), any());
    verify(foo).bar(Mockito.intThat(x -> x >= 42), Mockito.any(), any());
    verify(foo).bar(eq(42), any(), Mockito.notNull());

    // Additional argument checkers
    verify(foo).bar(gt(42), any(), any());
    verify(foo).bar(intThat(x -> x >= 42), or(ArgumentMatchers.any(), notNull()), any());
    verify(foo).bar(eq(42), any(), not(null));

    anyInt();
    eq(42);
    anySet();
    anyList();
  }

  private int wrapArgumentMatcher(int value) {
    return eq(value);
  }

  static class Foo {
    Object bar(int a, Object b, Object c) {
      return null;
    }

    String baz(Object a, Object b) {
      return "hi";
    }

    boolean bop() {
      return false;
    }

    void quux(int a, int b) {
    }
  }
}
