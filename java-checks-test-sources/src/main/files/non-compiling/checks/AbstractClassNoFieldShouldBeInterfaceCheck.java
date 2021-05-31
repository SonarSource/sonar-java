import com.google.auto.value.AutoOneOf;
import com.google.auto.value.AutoValue;
import org.immutables.value.Value;

abstract class A {
  private int b;

  abstract void method();
}
abstract class B { // Noncompliant [[sc=16;ec=17]] {{Convert the abstract class "B" into an interface.}}
  int method(){
    return 1;
  }
  class F {}
}
class C {
  int method(){
    return 1;
  }
}

abstract class D {
  protected void method() {

  }
}

abstract class E extends A {
}

public abstract class F {
  public abstract double v();

  @Override
  public String toString() {
    return ":";
  }
}

public abstract class G {
  public abstract double v();

  public String toString() {
    return ":";
  }
}

public abstract class Car { // Compliant - has private methods
  public void start() {
      turnOnLights();
      startEngine();
  }

  public abstract void stop();

  private void turnOnLights() {}
  private void startEngine() {}
}

// Issue will be filtered by GoogleAutoFilter
@AutoValue
abstract class Foo { // Noncompliant
  static Foo create(String name) {
    return new AutoValue_Foo(name);
  }
  abstract String name();
  @AutoValue.Builder
  abstract static class Builder { // Noncompliant
    abstract Builder namer(String name);
  }
}

// Issue will be filtered by GoogleAutoFilter
@AutoOneOf(StringOrInteger.Kind.class)
abstract class StringOrInteger { // Noncompliant
  public enum Kind {
    STRING, INTEGER
  }
}

@Value.Immutable
abstract class Bar { // Compliant
  abstract String name();
}

@org.immutables.value.Value.Immutable
abstract class BarWithFullAnnotation { // Compliant
  abstract String name();
}

@creedthoughts.org.immutables.value.Value.Immutable
abstract class BarWithFullAnnotation { // Noncompliant
  abstract String name();
}
