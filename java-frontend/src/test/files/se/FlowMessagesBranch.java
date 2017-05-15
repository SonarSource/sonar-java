abstract class A {

  void foo(Object a, Object b) {
    if (a == null       // flow@npe1 {{Implies 'a' can be null.}}
      || null == b) {   // flow@npe2 {{Implies 'b' can be null.}}
      // Noncompliant@+1 [[flows=npe1]] {{A "NullPointerException" could be thrown; "a" is nullable here.}}
      a.toString();     // flow@npe1 {{'a' is dereferenced.}}
      // Noncompliant@+1 [[flows=npe2]] {{A "NullPointerException" could be thrown; "b" is nullable here.}}
      b.toString();     // flow@npe2 {{'b' is dereferenced.}}
    }
  }

  void bar(boolean a) {
    if (a == true) { // flow@cot {{Implies 'a' is true.}} flow@cot {{Implies 'a' is true.}} flow@cot {{Implies 'a' is non-null.}}
      // Noncompliant@+1 [[flows=cot]] {{Remove this expression which always evaluates to "true"}}
      if (a == true) { }  // flow@cot {{Expression is always true.}}
    }
  }

}
