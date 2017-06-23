package test;

class Base {
  static final long serialVersionUID = 1L;

  static String myStaticField;

  String baseField; // Compliant

  private String privateBaseField; // Compliant

  String baseMethod() {
    return null;
  }
}

class Derived11 extends Base {
  boolean baseField; // Noncompliant [[sc=11;ec=20]] {{"baseField" is the name of a field in "Base".}}

  String myStaticField; // Compliant - ignore static fields in parent class

  int BaseField; // Compliant - same name but different case handled by S4025

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

  int BaseField; // Compliant - same name but different case handled by S4025

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
