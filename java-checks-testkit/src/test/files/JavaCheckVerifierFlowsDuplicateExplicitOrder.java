class A {

  // same explicit order is provided
  void f() {
    b.toString(); // Noncompliant [[flows=f]] {{error}}  flow@f [[order=3]] {{msg3}}
    Object a = null; // flow@f [[order=1]] {{msg1}}
    Object b = new Object();  // flow@f [[order=1]] {{msg2}}
  }
}
