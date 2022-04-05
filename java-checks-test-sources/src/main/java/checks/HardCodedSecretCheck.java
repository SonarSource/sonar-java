package checks;

class HardCodedSecretCheck {
  String field = "login=a&secret=abcdefghijklmnopqrs"; // Noncompliant [[sc=18;ec=54]] {{'secret' detected in this expression, review this potentially hard-coded secret.}}
}
