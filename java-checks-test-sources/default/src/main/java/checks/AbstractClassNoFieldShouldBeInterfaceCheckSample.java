package checks;

import com.google.auto.value.AutoOneOf;
import com.google.auto.value.AutoValue;
import org.immutables.value.Value;

abstract class AbstractClassNoFieldShouldBeInterfaceCheckSampleA {
  private int b;

  abstract void method();
}
abstract class AbstractClassNoFieldShouldBeInterfaceCheckSampleB { // Noncompliant [[sc=16;ec=65]] {{Convert the abstract class "AbstractClassNoFieldShouldBeInterfaceCheckSampleB" into an interface.}}
  int method(){
    return 1;
  }
  class AbstractClassNoFieldShouldBeInterfaceCheckSampleF {}
}
class AbstractClassNoFieldShouldBeInterfaceCheckSampleC {
  int method(){
    return 1;
  }
}

abstract class AbstractClassNoFieldShouldBeInterfaceCheckSampleD {
  protected void method() {

  }
}

abstract class AbstractClassNoFieldShouldBeInterfaceCheckSampleE extends AbstractClassNoFieldShouldBeInterfaceCheckSampleA {
}

abstract class AbstractClassNoFieldShouldBeInterfaceCheckSampleF {
  public abstract double v();

  @Override
  public String toString() {
    return ":";
  }
}

abstract class AbstractClassNoFieldShouldBeInterfaceCheckSampleG {
  public abstract double v();

  public String toString() {
    return ":";
  }
}

abstract class AbstractClassNoFieldShouldBeInterfaceCheckSampleCar { // Compliant - has private methods
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
abstract class AbstractClassNoFieldShouldBeInterfaceCheckSampleFoo { // Noncompliant
  static AbstractClassNoFieldShouldBeInterfaceCheckSampleFoo create(String name) {
    return new AbstractClassNoFieldShouldBeInterfaceCheckSampleFooImplem();
  }
  abstract String name();
  @AutoValue.Builder
  abstract static class Builder { // Noncompliant
    abstract Builder namer(String name);
  }
}

class AbstractClassNoFieldShouldBeInterfaceCheckSampleFooImplem extends AbstractClassNoFieldShouldBeInterfaceCheckSampleFoo {
  @Override
  String name() {
    return null;
  }
}

// Issue will be filtered by GoogleAutoFilter
@AutoOneOf(AbstractClassNoFieldShouldBeInterfaceCheckSampleStringOrInteger.Kind.class)
abstract class AbstractClassNoFieldShouldBeInterfaceCheckSampleStringOrInteger { // Noncompliant
  public enum Kind {
    STRING, INTEGER
  }
}

@Value.Immutable
abstract class AbstractClassNoFieldShouldBeInterfaceCheckSampleBar { // Compliant
  abstract String name();
}

@org.immutables.value.Value.Immutable
abstract class AbstractClassNoFieldShouldBeInterfaceCheckSampleWithFullAnnotation { // Compliant
  abstract String name();
}
