package checks.tests;

class CallSuperInTestCaseCheckSample extends junit.framework.TestCase {
}

class CallSuperInTestCaseCheckSampleB extends junit.framework.TestCase {
  public void setUp() {} //Compliant, direct child
  public void tearDown() {} //Compliant, direct child
}

class CallSuperInTestCaseCheckSampleD extends CallSuperInTestCaseCheckSampleB {
  public void setUp() {
    super.setUp();
  }

  public void tearDown() {
    super.tearDown();
  }
}

class CallSuperInTestCaseCheckSampleE extends CallSuperInTestCaseCheckSampleB {
  public void setUp() { // Noncompliant [[sc=15;ec=20]] {{Add a "super.setUp()" call to this method.}}
  }
  public void tearDown() { // Noncompliant {{Add a "super.tearDown()" call to this method.}}
  }
}

class CallSuperInTestCaseCheckSampleC extends CallSuperInTestCaseCheckSample {
  public void setUp() {} //Compliant, no override
  public void tearDown() {} //Compliant, no override
}
