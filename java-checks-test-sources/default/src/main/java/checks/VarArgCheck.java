package checks;

class VarArgCheck {
  void nok(int... p) { // Noncompliant {{Do not use varargs.}}
//            ^^^
  }
  void nok(String foo, int... p) { // Noncompliant {{Do not use varargs.}}
//                        ^^^
  }
  void ok() {
  }
  void ok(int[] p) {
  }
  void ok(String foo, int p) {
  }
}
