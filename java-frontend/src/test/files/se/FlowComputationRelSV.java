
class A {

  // test that we compute flow not only on relational SV both also on SVs from which relational SV was computed (recursively)
  void rel() {
    int c = 0;  // flow@unary_rel,rel {{zero}}
    int a = c;  // FIXME SONARJAVA-2049 _flow@unary_rel,rel missing reassignment message
    int b = 0;  // flow@unary_rel,rel {{zero}}
    boolean cond = (b == a) == true; // flow@rel  FIXME SONARJAVA-2049 unary_rel should have a message here too, symbol is not tracked
    if (cond) { // Noncompliant [[flows=rel]] flow@rel {{Condition is always true.}} flow@unary_rel {{Implies 'cond' is true.}}

    }

    if (!cond) { // Noncompliant [[flows=unary_rel]] flow@unary_rel {{Condition is always false.}}

    }
  }

}
