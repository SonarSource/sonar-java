package checks.tests;

class CallSuperInTestCaseCheck extends junit.framework.TestCase {
}

class CallSuperInTestCaseCheckB extends junit.framework.TestCase {
  public void setUp() {} //Compliant, direct child
  public void tearDown() {} //Compliant, direct child
}

class CallSuperInTestCaseCheckD extends CallSuperInTestCaseCheckB {
  public void setUp() {
    super.setUp();
  }

  public void tearDown() {
    super.tearDown();
  }
}

class CallSuperInTestCaseCheckE extends CallSuperInTestCaseCheckB {
  public void setUp() { // Noncompliant {{Add a "super.setUp()" call to this method.}}
//            ^^^^^
  }
  public void tearDown() { // Noncompliant {{Add a "super.tearDown()" call to this method.}}
  }
}

class CallSuperInTestCaseCheckC extends CallSuperInTestCaseCheck {
  public void setUp() {} //Compliant, no override
  public void tearDown() {} //Compliant, no override
}
