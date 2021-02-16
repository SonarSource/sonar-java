class A {
  B b;
  boolean booleanField;
  private void fun1(Object o1, Object o2) {
    if(b.method() > 0) {
      fun2(o1, o2);
    }
    if(b.method() > 0) {
      fun2(o1, o2);
    }
    // no flow should be computed for fun2 or fun3 yields.
    fun4(null, null); // Noncompliant
  }

  private boolean fun2(Object o1, Object o2) {
    Object o3 = new Object();
    Object o4 = new Object();
    if(booleanField) {
      fun3(o3, o4);
      return true;
    } else {
      fun3(o3, o4);
      return false;
    }
  }

  private void fun3(Object o1, Object o2) {
    Object o4 = new Object();
    Object o5 = new Object();
    if(booleanField) {
      fun2(o4, o5);
    } else {
      fun2(o4, o5);
    }
  }

  private void fun4(Object o1, Object o2) {
    o1.toString();
  }
}

abstract class B{
  int field = 12;
  abstract int method();
}
