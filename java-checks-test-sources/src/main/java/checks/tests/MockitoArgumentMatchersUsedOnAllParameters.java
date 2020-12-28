package checks.tests;


import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.intThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MockitoArgumentMatchersUsedOnAllParameters {
  static class Foo {
    Object bar(int a, Object b, Object c) {
      return null;
    }

    String baz(Object a, Object b) {
      return "hi";
    }

    void quux(int a, int b) {
    }
  }

  @Test
  public void nonCompliant() {
    Foo foo = mock(Foo.class);
    Integer i1 = null, i2 = null, val1 = null, val2 = null;
    given(foo.bar(
      anyInt(),
      i1, // Noncompliant [[sc=7;ec=9;secondary=+1]] {{Add an "eq()" argument matcher on this/these parameters}}
      i2))
      .willReturn(null);
    given(foo.bar(
      anyInt(),
      i1, // Noncompliant [[sc=7;ec=9;secondary=+1]] {{Add an "eq()" argument matcher on this/these parameters}}
      i2))
      .willReturn(null);
    when(foo.baz(eq(val1), val2)).thenReturn("hi");// Noncompliant [[sc=28;ec=32]] {{Add an "eq()" argument matcher on this/these parameters}}
    doThrow(new RuntimeException()).when(foo).quux(intThat(x -> x >= 42), -1); // Noncompliant [[sc=75;ec=77]] {{Add an "eq()" argument matcher on this/these parameters}}
    verify(foo).bar(
      i1, // Noncompliant [[sc=7;ec=9;secondary=+2]] {{Add an "eq()" argument matcher on this/these parameters}}
      anyInt(),
      i2);
  }

  @Test
  public void compliant() {
    Foo foo = mock(Foo.class);
    Integer i1 = null, i2 = null, val1 = null, val2 = null;
    given(foo.bar(anyInt(), eq(i1), eq(i2))).willReturn(null);
    when(foo.baz(val1, val2)).thenReturn("hi");
    doThrow(new RuntimeException()).when(foo).quux(intThat(x -> x >= 42), eq(-1));
    verify(foo).bar(eq(i1), anyInt(), eq(i2));

    anyInt();
    eq(42);
    anySet();
    anyList();
  }
}
