package test;

class Base {
  static final long serialVersionUID = 1L;

  String baseField; // Compliant

  private String privateBaseField; // Compliant

  String baseMethod() {
    return null;
  }
}

class Derived11 extends Base {
  boolean baseField; // Noncompliant {{"baseField" is the name of a field in "Base".}}

  int BaseField; // Noncompliant {{"BaseField" differs only by case from "baseField" in "Base".}}

  String privateBaseField; // Compliant, exception

  boolean derived11Field; // Compliant

  static final long serialVersionUID; // Compliant, exception

  @Override
  String baseMethod() { // Compliant
    return null;
  }
}

class Derived12 extends Base {
  boolean derived12Field; // Compliant
}

class Derived22 extends Derived12 {
  boolean baseField; // Noncompliant {{"baseField" is the name of a field in "Base".}}

  int BaseField; // Noncompliant {{"BaseField" differs only by case from "baseField" in "Base".}}

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
