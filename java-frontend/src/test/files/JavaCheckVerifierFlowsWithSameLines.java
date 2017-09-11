class A {

  void test_same_lines() {
    b.toString(); // Noncompliant [[flows=f1,f2]] {{error}}  flow@f1,f2  {{line4}}
    Object a = null; // flow@f1 {{f1}} flow@f2 {{f2}}
    Object b = new Object();  // flow@f1,f2 {{line6}}
  }

  void test_same_lines_and_messages() {
    b.toString(); // Noncompliant [[flows=same1,same2]] {{error}}  flow@same1,same2  {{msg1}}
    Object a = null; // flow@same1,same2 {{msg2}}
    Object b = new Object();  // flow@same1,same2 {{msg3}}
  }

}
