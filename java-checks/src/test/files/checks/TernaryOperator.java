class A {
  String s = condition ? "" : null; // Noncompliant [[sc=24;ec=25]] {{Convert this usage of the ternary operator to an "if"/"else" structure.}}
}
