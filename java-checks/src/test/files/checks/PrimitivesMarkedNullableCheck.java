import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

@interface AnotherAnnotation {
}

abstract class A {

  @CheckForNull
  abstract int getInt0(); // Noncompliant [[sc=12;ec=15]] {{"@CheckForNull" annotation should not be used on primitive types}}

  abstract int getInt1();

  @AnotherAnnotation
  abstract int getInt2();

  @Nullable
  protected abstract boolean isBool(); // Noncompliant [[sc=22;ec=29]] {{"@Nullable" annotation should not be used on primitive types}}

  @javax.annotation.CheckForNull
  public double getDouble0() { return 0.0; } // Noncompliant {{"@CheckForNull" annotation should not be used on primitive types}}

  @javax.annotation.Nullable
  public double getDouble1() { return 0.0; } // Noncompliant {{"@Nullable" annotation should not be used on primitive types}}

  public double getDouble2() { return 0.0; }

  @javax.annotation.Nullable
  public Double getDouble3() { return 0.0; }

  @Nullable
  public Double getDouble4() { return 0.0; }

  @Nullable
  public Object getObj0() { return null; }

  @CheckForNull
  public Object getObj1() { return null; }

  public Object getObj2() { return null; }

}
