import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

class NonPublic extends TestCase {
  Test suite() {  }  // Noncompliant {{Make this method "public".}}
  void setUp() {  } // Noncompliant {{Make this method "public".}}
  void tearDown() {  }  // Noncompliant {{Make this method "public".}}
}

class WrongName extends TestCase {
  public static Test a() {  }  // Noncompliant {{This method should be named "suite" not "a".}}
  public static TestSuite b() {  }  // Noncompliant {{This method should be named "suite" not "b".}}
  public static void suit() {  }  // Noncompliant [[sc=22;ec=26]] {{This method should be named "suite" not "suit".}}
  public void setup() {  } // Noncompliant {{This method should be named "setUp" not "setup".}}
  public void tearDwon() {  }  // Noncompliant {{This method should be named "tearDown" not "tearDwon".}}
  public static boolean suite() {  }  // Noncompliant {{This method should return either a "junit.framework.Test" or a "junit.framework.TestSuite".}}
}

public class Wrong extends TestCase {
  public static Test suite(int count) {  } // Noncompliant {{This method does not accept parameters.}}
  public Test suite() {  } // Noncompliant {{Make this method "static".}}

  public void setUp(int par) {  } // Noncompliant {{This method does not accept parameters.}}
  public int setUp() {  } // Noncompliant {{Make this method return "void".}}

  public void tearDown(int pat) {  }  // Noncompliant {{This method does not accept parameters.}}
  public int tearDown() {  }  // Noncompliant {{Make this method return "void".}}
}

public class Compliant extends TestCase {
  public static Test suite() { }
  public void setUp() { }
  public void tearDown() { }
}

public class B {
  void tearDown() {  } // Compliant - class B does not extend TestCase
}


public class FpS2391 extends TestCase {
  @Override
  protected void setUp() {   // Compliant - protected
    System.out.println("setUp");
  }

  @Override
  protected void tearDown() {// Compliant - protected
  }

  public static TestSuite suite() { // Compliant - return type is subtype of Test
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
