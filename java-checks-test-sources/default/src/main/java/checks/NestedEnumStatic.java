package checks;

enum NestedEnumStaticEnum { // Compliant
}


class NestedEnumStaticClass {
  enum MyNestedEnum { // Compliant
  }

  static enum MyNestedStaticEnum { // Noncompliant {{Remove this redundant "static" qualifier; nested enum types are implicitly static.}}
//^^^^^^
//  ^^^<
  }
}
