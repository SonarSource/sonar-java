package checks;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

class TryWithResourcesCheck {
  String foo(String fileName) {
    FileReader fr = null;
    BufferedReader br = null;
    try { // Noncompliant [[sc=5;ec=8;secondary=12,13]] {{Change this "try" to a try-with-resources.}}
      fr = new FileReader(fileName);
      br = new BufferedReader(fr);
      return br.readLine();
    } catch (Exception e) {
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
        }
      }
      if (fr != null) {
        try {
          br.close();
        } catch (IOException e) {
        }
      }
    }
    try { // compliant, no finally block so let's rely on unclosed resource rule
      fr = new FileReader(fileName);
    } catch (Exception e){

    }
    try (
      FileReader fr2 = new FileReader(fileName);
      BufferedReader br2 = new BufferedReader(fr)) { // compliant
      return br.readLine();
    } catch (Exception e) {
    }
    return null;
  }

  void newJustBeforeTryStatement() {
    Auto a1 = new Auto();
    Auto a2 = new Auto();
    try { // Noncompliant [[sc=5;ec=8;secondary=45,46]] {{Change this "try" to a try-with-resources.}}
      a1.doSomething();
    }  finally {
      a1.close();
      a2.close();
    }
  }

  void newJustBeforeAndAfterTryStatement() {
    Auto a1 = null;
    Auto a2 = new Auto();
    try { // Noncompliant [[sc=5;ec=8;secondary=57,59]] {{Change this "try" to a try-with-resources.}}
      a1 = new Auto();
      a1.doSomething();
    }  finally {
      a1.close();
      a2.close();
    }
  }

  void methodBetweenNewAndTry() {
    Auto a = new Auto();
    a.doSomething();
    try {
      a.doSomething();
    }  finally {
      a.close();
    }
  }

  class B {}

  void unknownNewBetweenNewAndTry() {
    Auto a = new Auto();
    B b = new B();
    try {
      a.doSomething();
    }  finally {
      a.close();
    }
  }

  Auto passThrough(Auto a) { return a; }

  void newInsideMethodInvocation() {
    Auto a = passThrough(new Auto()); // Compliant, we do not know what happens in the method
    try {
      a.doSomething();
    }  finally {
      a.close();
    }
  }

  void newJustBeforeTryWithResource() {
    Auto a1 = new Auto();
    try (Auto a2 = new Auto()) {
      a1.doSomething();
    }  finally {
      a1.close();
    }
  }

  void enclosedTryWithFinallyStatements() {
    Auto a1 = new Auto();
    try { // Noncompliant [[sc=5;ec=8;secondary=110,113]] {{Change this "try" to a try-with-resources.}}
      a1.doSomething();
      Auto a2 = new Auto();
      try {
        a2.doSomething();
      } finally {
        a2.close();
      }
    }  finally {
      a1.close();
    }
  }

  void enclosedTryStatements() {
    Auto a1 = new Auto();
    try { // Noncompliant [[sc=5;ec=8;secondary=125,128]] {{Change this "try" to a try-with-resources.}}
      a1.doSomething();
      Auto a2 = new Auto();
      try {
        a2.doSomething();
        a2.close();
      } catch (Exception e) {}
    }  finally {
      a1.close();
    }
  }

  void method_with_while_continue(boolean a) {
    while (a) {
      new java.io.BufferedInputStream(null, 4096);
      try { // Noncompliant
      } finally {
        continue;
      }
    }
  }

  static class Auto implements AutoCloseable {
    public void doSomething() {}

    @Override
    public void close() {}
  }

}
