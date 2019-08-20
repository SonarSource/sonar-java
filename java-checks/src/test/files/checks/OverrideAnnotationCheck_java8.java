interface InterfaceWithDefault {
  default boolean qix() { return true; }
  default boolean gul() { return false; }
  boolean bar();
}

abstract class AbstractMethodFromInterface implements InterfaceWithDefault {
  abstract boolean bar(); // Compliant
}

interface InterfaceExtendsDefault extends InterfaceWithDefault {
  boolean qix(); // Noncompliant [[sc=11;ec=14]] {{Add the "@Override" annotation above this method signature}}
  @Override
  boolean gul(); // Compliant - hide default behavior
}

class ImplemInterfaceWithDefault implements InterfaceWithDefault {
  public boolean gul() { return false; } // Noncompliant [[sc=18;ec=21]] {{Add the "@Override" annotation above this method signature}}
}
