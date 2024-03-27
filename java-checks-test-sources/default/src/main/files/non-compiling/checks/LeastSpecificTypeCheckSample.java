package checks;

import java.util.*;
import javax.annotation.Resource;
import javax.inject.Inject;
import org.springframework.beans.factory.annotation.Autowired;

class LeastSpecificTypeCheckSample {
  @SomethingUnknown
  public void resourceAnnotatedMethod1(List<Object> list) { // Compliant - Unknown annotation, could be Spring, we do not report anything
    for (Object o : list) {
      o.toString();
    }
  }

  @SomethingUnknown
  public void resourceAnnotatedMethod2(Collection<Object> list) { // Compliant - Unknown annotation, could be Spring, we do not report anything
    for (Object o : list) {
      o.toString();
    }
  }

  public interface II1 {}

  public interface I1 extends II1 {
    void m();
  }

  public interface I2 extends I1 {
    void m();
  }

  public static class T1 {
    void m() {}
    void mt1() {}
  }

  public static class T2 extends T1 implements I2 {
    void ma();
  }

  public interface IMB {
    void ma();
    void ma_1(Unknown u);

    interface Inner {}
    void ma_2(Inner i);
  }

  public interface IMA {
    void ma();
    void ma_1(Unknown u);

    interface Inner {}
    void ma_2(Inner i);
  }

  static class T extends T2 implements IMB, IMA {}

  public static void bla(T t) { // Noncompliant  {{Use 'checks.LeastSpecificTypeCheckSample.I1' here; it is a more general type than 'T'.}}
    t.m();
  }

  public static void foo(T t) { // Noncompliant  {{Use 'checks.LeastSpecificTypeCheckSample.T1' here; it is a more general type than 'T'.}}
    t.m();
    t.mt1();
  }

  public static void ma(T t) { // Noncompliant  {{Use 'checks.LeastSpecificTypeCheckSample.IMA' here; it is a more general type than 'T'.}}
    // defined in both T2 and IMA, interface is preferred
    t.ma();
  }

  public static void ma_1(T t) { // Compliant - ambiguous as Unknown type is unknown
    // defined in both T2 and IMA, unknow makes it indecidable
    t.ma_1(null);
  }

  public static void ma_2(T t) { // Noncompliant {{Use 'checks.LeastSpecificTypeCheckSample.IMB' here; it is a more general type than 'T'.}}
    // defined in both T2 and IMA, but call is ambiguous
    t.ma_2(null);
  }

  public interface IG<T> {
    T get();
  }

  public abstract class GImpl implements IG<GImpl> {
    GImpl get();
  }

  class GImplSub extends GImpl {

  }

  public static void generics(GImplSub s) { // Noncompliant  {{Use 'checks.LeastSpecificTypeCheckSample.GImpl' here; it is a more general type than 'GImplSub'.}}
    s.get();
  }

  class Generic<T> implements IG<T> {

  }

  public static void generics2(Generic<Object> g) { // Noncompliant  {{Use 'checks.LeastSpecificTypeCheckSample.IG' here; it is a more general type than 'Generic'.}}
    g.get();
  }

  public static void stringBuilder(final StringBuilder name) {
    name.charAt(0);
    name.substring(0, 1);
  }

}
