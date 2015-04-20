package test;

class Base {
  String baseField; // Compliant

  String baseMethod() {
    return null;
  }
}

class Derived11 extends Base {
  boolean baseField; // Noncompliant

  boolean derived11Field; // Compliant

  @Override
  String baseMethod() { // Compliant
    return null;
  }
}

class Derived12 extends Base {
  boolean derived12Field; // Compliant
}

class Derived22 extends Derived12 {
  boolean baseField; // Noncompliant

  boolean derived22Field; // Compliant

  @Override
  String baseMethod() { // Compliant
    return null;
  }
}

class Unrelated {
  boolean baseField; // Compliant

  String baseMethod() { // Compliant
    return null;
  }
}
