class A {

  void test(Object o) {
    Boolean b = o != null;
    if (b == null) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}

    }

    String s1 = null;
    String s2 = "test";
    String s = s1 + s2; // s == "nulltest"
    if (s == null) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}

    }

    Integer i = 1;
    Integer j = null;
    Integer mul = i * j;  // FN NPE should be detected here - see MMF-859
    if (mul == null) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}

    }
  }
}
