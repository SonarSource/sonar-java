package checks.tests;


import java.util.Random;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;

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

    verify(foo).bar(
      returnRawValue(), // Noncompliant [[sc=7;ec=23]] {{Add an "eq()" argument matcher on this parameter.}}
      any(), any()
    );

    verify(foo).bar(
      new Integer("42"), // Noncompliant
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

    // Cases where the method called returns an ArgumentMatcher
    verify(foo).bar(
      wrapArgumentMatcher(42), // Compliant
      any(), any());

    verify(foo).bar(
      wrapArgThat(1), // Compliant
      any(), any());

    verify(foo).bar(
      wrapThroughLayers(1), // Compliant
      any(), any());

    verify(foo).bar(
      wrapThroughLayers(42), // Compliant
      any(), any());

    verify(foo).bar(
      returnRawValueThroughLayers(), // Compliant FN, if the method invoked calls another method, we don't go deeper and consider it eventually returns an argument matcher
      any(), any()
    );

    verify(foo).bar(
      recursiveCall(), // Compliant, if the method invoked calls another method, we don't go deeper and consider it eventually returns an argument matcher
      any(), any());


    verify(foo).bar(
      staticallyWrapArgThat(41), // Compliant, if the method invoked calls another method, we don't go deeper and consider it eventually returns an argument matcher
      any(), any());

    MockitoArgumentMatchersUsedOnAllParameters obj = new MockitoArgumentMatchersUsedOnAllParameters();

    verify(foo).bar(
      obj.wrapArgThat(41), // Compliant
      any(), any());

    verify(foo).bar(
      obj.staticallyWrapArgThat(41), // Compliant
      any(), any());
  }


  private int returnRawValue() {
    return 42;
  }

  private int returnRawValueThroughLayers() {
    return returnRawValue();
  }

  private int wrapArgumentMatcher(int value) {
    return eq(value);
  }

  private int wrapArgThat(int lowerBound) {
    return argThat(number -> lowerBound < number);
  }

  private int wrapThroughLayers(int value) {
    return wrapArgThat(value);
  }

  private int recursiveCall() {
    Random random = new Random();
    if ((random.nextInt() % 2) == 0) {
      return argThat(number -> number >= 0);
    }
    return recursiveCall();
  }

  private static int staticallyWrapArgThat(int lowerBound) {
    return argThat(number -> lowerBound < number);
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

  public abstract class SomeAbstractTest {
    interface SthToMock {
      String command(String exec);
    }

    @Mock
    private SthToMock sthToMock;

    public abstract String getCommand();

    @Test
    public void shouldReturnSystemDateTime() {
      when(sthToMock.command(getCommand())).thenReturn("test"); // Compliant as sthToMock is abstract we assume its implementation will be compliant
    }
  }
}
