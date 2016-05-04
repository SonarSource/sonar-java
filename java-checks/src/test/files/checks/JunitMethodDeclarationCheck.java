import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class A extends TestCase{
  Test suite() {  }  // Noncompliant {{Make this method "public".}}
  public static boolean suite() {  }  // Noncompliant {{This method should return either a "junit.framework.Test" or a "junit.framework.TestSuite".}}
  public static Test suit() {  }  // Noncompliant {{This method should be named "suite" not "suit".}}
  public static void suit() {  }  // Noncompliant [[sc=22;ec=26]] {{This method should be named "suite" not "suit".}}
  public static TestSuite suit() {  }  // Noncompliant {{This method should be named "suite" not "suit".}}
  public static Test suite(int count) {  } // Noncompliant {{This method does not accept parameters.}}
  public Test suite() {  } // Noncompliant {{Make this method "static".}}

  public void setup() {  } // Noncompliant {{This method should be named "setUp" not "setup".}}
  public void setUp(int par) {  } // Noncompliant {{This method does not accept parameters.}}
  public int setUp() {  } // Noncompliant {{Make this method return "void".}}
  void setUp() {  } // Noncompliant {{Make this method "public".}}
  public void tearDwon() {  }  // Noncompliant {{This method should be named "tearDown" not "tearDwon".}}
  public void tearDown(int pat) {  }  // Noncompliant {{This method does not accept parameters.}}
  public int tearDown() {  }  // Noncompliant {{Make this method return "void".}}
  void tearDown() {  }  // Noncompliant {{Make this method "public".}}

  public static Test suite() {  }
  public static TestSuite suite() {  }
  public void setUp() {  }
  public void tearDown() {  }

  Foo coverage1() {}
}

public class B {
  void tearDown() {  }
}


public class FpS2391 extends TestCase {
  @Override
  protected void setUp() {   // <--- Make this method public
    System.out.println("setUp");
  }

  @Override
  protected void tearDown() {// <--- Make this method public
  }

  public void testMe() {
    System.out.println("testMe");
  }
  public void init() {}   // <--- This method should be named "suite" not "init"
  public void get() {}    // <--- This method should be named "setUp" not "get"
  public void twice() {}  // <--- This method should be named "suite" not "twice"
  public void sleep() {}  // <--- This method should be named "setUp" not "sleep".
  public void purge() {}  // <--- This method should be named "suite" not "purge"
  public void set() {}    // Noncompliant {{This method should be named "setUp" not "set".}} might be a false positive
  public void split() {}  // <--- This method should be named "suite" not "split"
}