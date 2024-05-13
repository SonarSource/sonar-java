package checks;

class HardCodedSecretCheckCustom {
  private void test() {
    String variable2 = "login=a&secret=aaaaaaaaaaaaaabcdefg"; // Compliant
    String variable5 = "login=a&marmalade=aaaaaaaaaaaaaabcdefg"; // Noncompliant {{'marmalade' detected in this expression, review this potentially hard-coded secret.}}
//                     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  }
}
