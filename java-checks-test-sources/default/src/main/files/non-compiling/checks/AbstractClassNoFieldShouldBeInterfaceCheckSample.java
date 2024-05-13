package checks;

import com.google.auto.value.AutoOneOf;
import com.google.auto.value.AutoValue;
import org.immutables.value.Value;

abstract class AbstractClassNoFieldShouldBeInterfaceCheckA {
  private int b;

  abstract void method();
}
abstract class AbstractClassNoFieldShouldBeInterfaceCheckB { // Noncompliant {{Convert the abstract class "AbstractClassNoFieldShouldBeInterfaceCheckB" into an interface.}}
//             ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  int method(){
    return 1;
  }
  class AbstractClassNoFieldShouldBeInterfaceCheckF {}
}
class AbstractClassNoFieldShouldBeInterfaceCheckC {
  int method(){
    return 1;
  }
}

abstract class AbstractClassNoFieldShouldBeInterfaceCheckD {
  protected void method() {

  }
}

abstract class AbstractClassNoFieldShouldBeInterfaceCheckE extends AbstractClassNoFieldShouldBeInterfaceCheckA {
}

abstract class AbstractClassNoFieldShouldBeInterfaceCheckF {
  public abstract double v();

  @Override
  public String toString() {
    return ":";
  }
}

abstract class AbstractClassNoFieldShouldBeInterfaceCheckG {
  public abstract double v();

  public String toString() {
    return ":";
  }
}

abstract class AbstractClassNoFieldShouldBeInterfaceCheckCar { // Compliant - has private methods
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
abstract class AbstractClassNoFieldShouldBeInterfaceCheckFoo { // Compliant - FN due to unknown annotation
  static AbstractClassNoFieldShouldBeInterfaceCheckFoo create(String name) {
    return new AbstractClassNoFieldShouldBeInterfaceCheckFooImplem();
  }
  abstract String name();
  @AutoValue.Builder
  abstract static class Builder { // Compliant - FN due to unknown annotation
    abstract Builder namer(String name);
  }
}

class AbstractClassNoFieldShouldBeInterfaceCheckFooImplem extends AbstractClassNoFieldShouldBeInterfaceCheckFoo {
  @Override
  String name() {
    return null;
  }
}

// Issue will be filtered by GoogleAutoFilter
@AutoOneOf(AbstractClassNoFieldShouldBeInterfaceCheckStringOrInteger.Kind.class)
abstract class AbstractClassNoFieldShouldBeInterfaceCheckStringOrInteger { // Compliant - FN due to unknown annotation
  public enum Kind {
    STRING, INTEGER
  }
}

@Value.Immutable
abstract class AbstractClassNoFieldShouldBeInterfaceCheckBar { // Compliant
  abstract String name();
}

@org.immutables.value.Value.Immutable
abstract class AbstractClassNoFieldShouldBeInterfaceCheckWithFullAnnotation { // Compliant
  abstract String name();
}

@creedthoughts.org.immutables.value.Value.Immutable
abstract class AbstractClassNoFieldShouldBeInterfaceCheckWithFullAnnotation2 { // Compliant
  abstract String name();
}
