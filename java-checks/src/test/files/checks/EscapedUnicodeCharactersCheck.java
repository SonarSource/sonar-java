
class A {
  void method() {
    String prefix = "n\uuuu00E9e"; // Noncompliant [[sc=21;ec=34]] {{Remove this Unicode escape sequence and use the character instead.}}
    String prefix2= "n\\\uuuu00E9e"; // Noncompliant [[sc=21;ec=36]] {{Remove this Unicode escape sequence and use the character instead.}}
    prefix = "n\u00E9e\u0001v"; // Noncompliant
    // compliant, only escaped
    prefix = "\u00E9\u00E9\u00E9\u00E9";
    // compliant, only unprintable
    prefix = "a\u0001b\u0002c\u00A0";
    prefix = "\u007f";
    prefix = "a\u0080b\u0002c\u00A0\u0083\u0164"; // Noncompliant [[sc=14;ec=49]]
    prefix = "n\\u00E9e";
    prefix = "née";
    prefix = "";
  }
}
