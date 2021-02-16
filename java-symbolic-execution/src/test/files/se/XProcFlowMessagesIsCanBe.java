abstract class A {

  void foo(A a) {
    if (a == null) { // flow@npe_foo {{Implies 'a' is null.}}
      // Noncompliant@+1 [[flows=npe_foo]]
      a.toString(); // flow@npe_foo {{'a' is dereferenced.}}
    }
    doSomething();
  }

  void bar(A a) {
    if (a == null) { // flow@npe_bar {{Implies 'a' can be null.}}
      doSomething();
    }
    // Noncompliant@+1 [[flows=npe_bar]]
    a.toString(); // flow@npe_bar {{'a' is dereferenced.}}
  }

  void gul(A a) {
    if (a != null) { // flow@npe_gul {{Implies 'a' can be null.}}
      doSomething();
    }
    // Noncompliant@+1 [[flows=npe_gul]]
    a.toString(); // flow@npe_gul {{'a' is dereferenced.}}
  }

  void qix(A a) {
    if (a == null) { // flow@npe_qix1 {{Implies 'a' is null.}}
      b = 4;
    } else {
      a = null; // flow@npe_qix2 {{Implies 'a' is null.}}
    }
    // Noncompliant@+1 [[flows=npe_qix1,npe_qix2]]
    a.toString(); // flow@npe_qix1 {{'a' is dereferenced.}} flow@npe_qix2 {{'a' is dereferenced.}}
  }

  int b;
  abstract void doSomething();
}
