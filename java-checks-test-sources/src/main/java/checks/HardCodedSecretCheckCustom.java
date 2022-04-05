package checks;

class HardCodedSecretCheckCustom {
  private void test() {
    String variable2 = "login=a&secret=abcdefghijklmnopqrs"; // Compliant
    String variable5 = "login=a&marmalade=abcdefghijklmnopqrs"; // Noncompliant [[sc=24;ec=63]] {{'marmalade' detected in this expression, review this potentially hard-coded secret.}}
  }
}
