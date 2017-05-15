
class A {

  void test() {
    boolean b = true; // flow@unary {{'b' is assigned true.}} flow@unary {{'b' is assigned non-null.}}
    if (!b) { // Noncompliant [[flows=unary]] flow@unary {{Expression is always false.}}

    }
  }

  void test2() {
    boolean b = true; // flow@unary2 {{'b' is assigned true.}} flow@unary2 {{'b' is assigned non-null.}}
    boolean c = !b; // no message, because no constraint on unary sv see SONARJAVA-1911
    if (c) { // Noncompliant [[flows=unary2]] flow@unary2 {{Expression is always false.}}

    }
  }

}
