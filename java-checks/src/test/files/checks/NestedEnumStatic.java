enum MyEnum { // Compliant
}

// should be rejected during compilation
static enum MyStaticEnum {// Noncompliant [[sc=1;ec=7;secondary=5]] {{Remove this redundant "static" qualifier; nested enum types are implicitly static.}}
}

class MyClass {
  enum MyNestedEnum { // Compliant
  }

  static enum MyNestedStaticNum { // Noncompliant {{Remove this redundant "static" qualifier; nested enum types are implicitly static.}}
  }
}
