class A {

  void f() {
    if (this == null) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}

    }
    if (super == null) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}

    }
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
