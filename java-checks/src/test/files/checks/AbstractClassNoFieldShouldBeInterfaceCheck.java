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

@AutoValue
abstract class Foo { // Compliant
  static Foo create(String name) {
    return new AutoValue_Foo(name);
  }
  abstract String name();
}

@Value.Immutable
abstract class Bar { // Compliant
  abstract String name();
}
