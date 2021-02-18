class A {

  void foo(Object a, Object b) {
    if (a == null       // flow@npe1 {{Implies 'a' can be null.}}
      || null == b) {   // flow@npe2 {{Implies 'b' is null.}}
      // Noncompliant@+1 [[flows=npe1]] {{A "NullPointerException" could be thrown; "a" is nullable here.}}
      a.toString();     // flow@npe1 {{'a' is dereferenced.}}
      // Noncompliant@+1 [[flows=npe2]] {{A "NullPointerException" could be thrown; "b" is nullable here.}}
      b.toString();     // flow@npe2 {{'b' is dereferenced.}}
    }
  }

  void bar(boolean a) {
    if (a == true) { // flow@cot {{Implies 'a' is true.}}
      // Noncompliant@+1 [[flows=cot]] {{Remove this expression which always evaluates to "true"}}
      if (a == true) { }  // flow@cot {{Expression is always true.}}
    }
  }

  void qix(Object a) {
    Object o = a != null ? a.toString() : null; // flow@npe3 {{Implies 'o' can be null.}}
    // Noncompliant@+1 [[flows=npe3]] {{A "NullPointerException" could be thrown; "o" is nullable here.}}
    o.toString(); // flow@npe3 {{'o' is dereferenced.}}
  }

  void gul(Object a) {
    Object o = a != null ? null : null; // flow@npe4 {{Implies 'o' is null.}}
    // Noncompliant@+1 [[flows=npe4]] {{A "NullPointerException" could be thrown; "o" is nullable here.}}
    o.toString(); // flow@npe4 {{'o' is dereferenced.}}
  }

  void dag(Object a) {
    Object o = a == null ? null : a.toString(); // flow@npe5 {{Implies 'o' can be null.}}
    // Noncompliant@+1 [[flows=npe5]] {{A "NullPointerException" could be thrown; "o" is nullable here.}}
    o.toString(); // flow@npe5 {{'o' is dereferenced.}}
  }

  void cro(Object a) {
    Object o = a != null ? a.toString() : foo(); // flow@npe6 {{'foo()' returns null.}} flow@npe6 {{Implies 'o' is null.}}
    // Noncompliant@+1 [[flows=npe6]] {{A "NullPointerException" could be thrown; "o" is nullable here.}}
    o.toString(); // flow@npe6 {{'o' is dereferenced.}}
  }

  static Object foo() {
    return null;
  }

}
