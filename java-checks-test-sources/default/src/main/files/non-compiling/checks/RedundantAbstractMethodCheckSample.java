package checks;

abstract class RedundantAbstractMethodCheckSample implements UnknownInterface {
  @Override public abstract void foo(); // Compliant - we don't actually know
}

interface MyInterface {
  public void foo();
}

abstract class RedundantAbstractMethodCheckSample2 implements MyInterface {
  @Override public abstract void foo(); // Noncompliant
}
