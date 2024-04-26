class A {
  String s = condition ? "" : null; // Noncompliant {{Convert this usage of the ternary operator to an "if"/"else" structure.}}
//                     ^
}
