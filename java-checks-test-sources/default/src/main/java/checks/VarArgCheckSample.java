package checks;

class VarArgCheckSample {
  void nok(int... p) { // Noncompliant [[sc=15;ec=18]] {{Do not use varargs.}}
  }
  void nok(String foo, int... p) { // Noncompliant [[sc=27;ec=30]] {{Do not use varargs.}}
  }
  void ok() {
  }
  void ok(int[] p) {
  }
  void ok(String foo, int p) {
  }
}
