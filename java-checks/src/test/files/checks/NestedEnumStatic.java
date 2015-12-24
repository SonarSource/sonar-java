enum MyEnum { // Compliant
}

// should be rejected during compilation
static enum MyStaticEnum {// Noncompliant [[sc=8;ec=12]] {{Remove this redundant "static" qualifier.}}
}

class MyClass {
  enum MyNestedEnum { // Compliant
  }

  static enum MyNestedStaticNum { // Noncompliant {{Remove this redundant "static" qualifier.}}
  }
}
