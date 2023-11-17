package checks;

enum NestedEnumStaticEnum { // Compliant
}


class NestedEnumStaticClass {
  enum MyNestedEnum { // Compliant
  }

  static enum MyNestedStaticEnum { // Noncompliant [[sc=3;ec=9;secondary=11]] {{Remove this redundant "static" qualifier; nested enum types are implicitly static.}}
  }
}
