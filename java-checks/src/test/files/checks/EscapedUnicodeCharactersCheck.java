
class A {
  void method() {
    String prefix = "n\uuuu00E9e"; // Noncompliant {{Remove this Unicode escape sequence and use the character instead.}}
    prefix = "n\u00E9e\u0001v"; // Noncompliant
    // compliant, only escaped
    prefix = "\u00E9\u00E9\u00E9\u00E9";
    // compliant, only unprintable
    prefix = "a\u0001b\u0002c";
    prefix = "n\\u00E9e";
    prefix = "née";
    prefix = "";
  }
}
