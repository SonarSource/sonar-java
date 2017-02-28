class A {

  void f() {
    b.toString(); // Noncompliant [[flows=f]] {{error}}  flow@f {{msg1}}
    Object a = null; // flow@f {{msg2}} flow@f {{msg3}}
    Object b = new Object();  // flow@f {{msg4}} flow@f {{msg5}}
  }
}
