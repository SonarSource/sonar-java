enum MyEnum { // Compliant
}

// should be rejected during compilation
static enum MyStaticEnum {// Noncompliant {{Remove this redundant "static" qualifier.}}
}

class MyClass {
  enum MyNestedEnum { // Compliant
  }

  static enum MyNestedStaticNum { // Noncompliant {{Remove this redundant "static" qualifier.}}
  }
}
