package checks.tests;


import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import not.a.real.SomeHelper;

import static org.mockito.AdditionalMatchers.gt;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.intThat;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static not.a.real.thing.unresolvableMethod;
import static not.a.real.Helper.wrapArgThat;

public class MockitoArgumentMatchersUsedOnAllParameters {

  @Test
  public void nonCompliant() {
    verify(foo).bar(
      returnRawValue(), // Noncompliant [[sc=7;ec=23]] {{Add an "eq()" argument matcher on this parameter.}}
      any(), any()
    );
  }

  @Test
  public void compliant() {
    Foo foo = mock(Foo.class);
    given(foo.bar(
      unresolvableMethod(), // Compliant, in case of incomplete semantic, we consider that the method invocation eventually returns an argument matcher
      any(),
      any()))
      .willReturn(null);

    given(foo.bar(
      wrapUnresolvableMethod(), // Compliant, in case of incomplete semantic, we consider that the method invocation eventually returns an argument matcher
      any(),
      any()))
      .willReturn(null);

    given(foo.bar(
      wrapThroughLayers(), // Compliant, in case of incomplete semantic, we consider that the method invocation eventually returns an argument matcher
      any(),
      any()))
      .willReturn(null);

    verify(foo).bar(
      returnRawValueThroughLayers(),  // Compliant FN, if the method invoked calls another method, we don't go deeper and consider it eventually returns an argument matcher
      any(), any()
    );

    verify(foo).bar(
      wrapArgThat(), // Compliant
      any(), any()
    );

    verify(foo).bar(
      SomeHelper.wrapArgThat(), // Compliant
      any(), any()
    );
  }

  private int returnRawValue() {
    return 42;
  }

  private int returnRawValueThroughLayers() {
    return returnRawValue();
  }

  private wrapUnresolvableMethod() {
    return unresolvableMethod();
  }

  private wrapUnresolvableMethodThroughLayers() {
    return wrapThroughLayers();
  }

  private int wrapThroughLayers(int value) {
    return wrapArgThat(value);
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
