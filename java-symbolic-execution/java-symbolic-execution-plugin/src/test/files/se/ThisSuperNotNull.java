class Super {}

class A extends Super {

  void f() {
    if (this == null) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}

    }
    super.toString(); // Compliant, as super is never null
  }

  void jdk9_zippath_fp(A o) {
    if (o.equals(this)) {
      return;
    }
    this.toString(); // no FP here
  }

  final boolean equals(Object o) {
    return o != null && o instanceof A;
  }

}
