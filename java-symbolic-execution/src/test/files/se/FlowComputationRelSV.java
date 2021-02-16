
class A {

  // test that we compute flow not only on relational SV both also on SVs from which relational SV was computed (recursively)
  void rel() {
    int c = 0;
    int a = c;  // flow@unary_rel,rel {{Implies 'a' has the same value as 'c'.}}
    int b = 0;
    boolean cond = (b == a) == true; // see SONARJAVA-1911
    if (cond) { // Noncompliant [[flows=rel]] flow@rel {{Expression is always true.}} flow@unary_rel {{Implies 'cond' is true.}}

    }

    if (!cond) { // Noncompliant [[flows=unary_rel]] flow@unary_rel {{Expression is always false.}}

    }
  }

  void catof1() {
    Object a = new Object(); // flow@catof1 {{Constructor implies 'not null'.}} flow@catof1 {{Implies 'a' is not null.}}
    if (a == null) { // Noncompliant [[flows=catof1]] {{Change this condition so that it does not always evaluate to "false"}} flow@catof1 {{Expression is always false.}}
    System.out.println();
  }
}

  void catof2() {
    Object a = new Object(); // flow@catof2 {{Constructor implies 'not null'.}} flow@catof2 {{Implies 'a' is not null.}}
    if ((a == null) == true) { // Noncompliant [[flows=catof2]] {{Change this condition so that it does not always evaluate to "false"}} flow@catof2 {{Expression is always false.}}
      System.out.println();
    }
  }

  void catof3() {
    Object a = new Object(); // flow@catof3 {{Constructor implies 'not null'.}} flow@catof3 {{Implies 'a' is not null.}}
    Object b = null; // flow@catof3 {{Implies 'b' is null.}}
    if ((a == b) == true) { // Noncompliant [[flows=catof3]] {{Change this condition so that it does not always evaluate to "false"}} flow@catof3 {{Expression is always false.}}
      System.out.println();
    }
  }

  void catof3b() {
    Object a = new Object(); // flow@catof3b {{Constructor implies 'not null'.}} flow@catof3b {{Implies 'a' is not null.}}
    Object b = null; // flow@catof3b {{Implies 'b' is null.}}
    boolean cond = a == b;  // no message here, because no constraint on 'a==b', it is not yet evaluated see SONARJAVA-1911
    b = new Object(); // b is not relevant here
    a = null; // a is not relevant here
    if (cond == true) { // Noncompliant [[flows=catof3b]] {{Change this condition so that it does not always evaluate to "false"}} flow@catof3b {{Expression is always false.}}
      System.out.println();
    }
  }

  void npe1(Object a) {
    Object b = null;
    if (a == b) { // flow@npe1 {{Implies 'a' is null.}}
      a.toString(); // Noncompliant [[flows=npe1]] flow@npe1 {{'a' is dereferenced.}}
    }
  }

  void npe2(Object a, Object b) {
    if (a == b) {
      // FIXME SONARJAVA-2272
      if (b == null) { // flow@npe2 {{Implies 'b' can be null.}}
        a.toString(); // Noncompliant [[flows=npe2]] flow@npe2 {{'a' is dereferenced.}}
      }
    }
  }

  void same_symbolic_value_referenced_with_different_symbols() {
    Object o = null; // flow@samesv {{Implies 'o' is null.}}
    Object a = o; // flow@samesv {{Implies 'a' has the same value as 'o'.}}
    Object b = o; // flow@samesv {{Implies 'b' has the same value as 'o'.}}
    if (a == b) { // Noncompliant [[flows=samesv]] flow@samesv {{Expression is always true.}}

    }
  }

  void equalsSV() {
    Object o = true; // flow@equals {{Implies 'o' is true.}}
    Object a = o; // flow@equals {{Implies 'a' has the same value as 'o'.}}
    Object b = o; // flow@equals {{Implies 'b' has the same value as 'o'.}}
    if (a.equals(b)) { // Noncompliant [[flows=equals]] flow@equals {{Expression is always true.}}

    }
  }

  void f() {
    boolean a = true;  // flow@nested {{Implies 'a' is true.}}
    boolean b = true;  // flow@nested {{Implies 'b' is true.}}
    if (a == b == true) { // Noncompliant [[flows=nested]] flow@nested {{Expression is always true.}}

    }
  }

}
