
class A {

  // test that we compute flow not only on relational SV both also on SVs from which relational SV was computed (recursively)
  void rel() {
    int c = 0;
    int a = c;  // FIXME SONARJAVA-2049 _flow@unary_rel,rel missing reassignment message
    int b = 0;
    boolean cond = (b == a) == true;
    if (cond) { // Noncompliant [[flows=rel]] flow@rel {{Expression is always true.}} flow@unary_rel {{Implies 'cond' is true.}} flow@unary_rel {{Implies 'cond' is non-null.}}

    }

    if (!cond) { // Noncompliant [[flows=unary_rel]] flow@unary_rel {{Expression is always false.}}

    }
  }

}
