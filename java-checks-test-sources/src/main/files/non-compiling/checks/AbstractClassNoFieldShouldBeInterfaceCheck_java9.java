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

public abstract class Car { // Noncompliant
  public void start() {
      turnOnLights();
      startEngine();
  }

  public abstract void stop();

  private void turnOnLights() {}
  private void startEngine() {}
}
