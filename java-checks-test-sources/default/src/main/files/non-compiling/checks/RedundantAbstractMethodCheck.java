package checks;

abstract class RedundantAbstractMethodCheck implements UnknownInterface {
  @Override public abstract void foo(); // Compliant - we don't actually know
}

interface MyInterface {
  public void foo();
}

abstract class RedundantAbstractMethodCheck2 implements MyInterface {
  @Override public abstract void foo(); // Noncompliant
}
