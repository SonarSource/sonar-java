class A {
  void foo(boolean b) {
    String res;
    res = b ? "yo" : "lo";
    res = (b ? b : !b) ? "yo" : "lo"; // Noncompliant [[sc=11;ec=23]] {{Extract this nested ternary operation into an independent statement.}}
    res = b ? b ? "yo" : "lo" : "yolo"; // Noncompliant [[sc=15;ec=30]] {{Extract this nested ternary operation into an independent statement.}}
    res = b ? "yo" : b ? "lo" : "yolo"; // Noncompliant [[sc=22;ec=39]] {{Extract this nested ternary operation into an independent statement.}}
  }
}
