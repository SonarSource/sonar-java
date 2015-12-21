class A extends junit.framework.TestCase {
}

class B extends junit.framework.TestCase {
  public void setUp() {} //Compliant, direct child
  public void tearDown() {} //Compliant, direct child
}

class D extends B {
  public void setUp() {
    super.setUp();
  }

  public void tearDown() {
    super.tearDown();
  }
}

class E extends B {
  public void setUp() { // Noncompliant [[sc=15;ec=20]] {{Add a "super.setUp()" call to this method.}}
  }
  public void tearDown() { // Noncompliant {{Add a "super.tearDown()" call to this method.}}
  }
}

class C extends A {
  public void setUp() {} //Compliant, no override
  public void tearDown() {} //Compliant, no override
}
