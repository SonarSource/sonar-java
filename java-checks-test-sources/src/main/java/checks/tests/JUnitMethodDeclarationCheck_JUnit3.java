package checks.tests;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

class JUnitMethodDeclarationCheck_NonPublic extends TestCase {
  Test suite() { return null; }  // Noncompliant {{Make this method "public".}}
}

class JUnitMethodDeclarationCheck_WrongName extends TestCase {
  public static Test a() { return null; }  // Noncompliant {{This method should be named "suite" not "a".}}
  public static TestSuite b() { return null; }  // Noncompliant {{This method should be named "suite" not "b".}}
  public static void suit() {  }  // Noncompliant [[sc=22;ec=26]] {{This method should be named "suite" not "suit".}}
  public void setup() {  } // Noncompliant {{This method should be named "setUp" not "setup".}}
  public void tearDwon() {  }  // Noncompliant {{This method should be named "tearDown" not "tearDwon".}}
  public static boolean suite() { return false; }  // Noncompliant {{This method should return either a "junit.framework.Test" or a "junit.framework.TestSuite".}}
}

class JUnitMethodDeclarationCheck_Wrong extends TestCase {
  public static Test suite(int count) { return null; } // Noncompliant {{This method does not accept parameters.}}
  public Test suite() { return null; } // Noncompliant {{Make this method "static".}}

  public void setUp(int par) {  } // Noncompliant {{This method does not accept parameters.}}

  public void tearDown(int pat) {  }  // Noncompliant {{This method does not accept parameters.}}
}

class JUnitMethodDeclarationCheck_Compliant extends TestCase {
  Object field; // for coverage

  public static Test suite() { return null; }
  @Override
  public void setUp() { }
  @Override
  public void tearDown() { }
}

class JUnitMethodDeclarationCheck_NotTestCase {
  void tearDown() {  } // Compliant - class B does not extend TestCase
}

class JUnitMethodDeclarationCheck_FpS2391 extends TestCase {
  @Override
  protected void setUp() {   // Compliant - protected
    System.out.println("setUp");
  }

  @Override
  protected void tearDown() {// Compliant - protected
  }

  public static TestSuite suite() { // Compliant - return type is subtype of Test
    return null;
  }

  public void testMe() {
    System.out.println("testMe");
  }
  public void init() {}   // Compliant
  public void get() {}    // Compliant
  public void twice() {}  // Compliant
  public void sleep() {}  // Compliant
  public void purge() {}  // Compliant
  public void set() {}    // Noncompliant {{This method should be named "setUp" not "set".}} might be a false positive
  public void split() {}  // Compliant
}
