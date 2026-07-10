package checks;

class HardCodedSecretCheckCustom {
  private void test() {
    String variable2 = "login=a&secret=sk_live_xf2fh0Hu3LqXlqqUg2DEWhEz"; // Compliant
    String variable5 = "login=a&marmalade=aaaaaaaaaaaaaabcdefg"; // Compliant, fake value filter
    String variable5_2 = "login=a&marmalade=sk_live_xf2fh0Hu3LqXlqqUg2DEWhEz"; // Noncompliant {{'marmalade' detected in this expression, review this potentially hard-coded secret.}}
//                       ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  }
}
