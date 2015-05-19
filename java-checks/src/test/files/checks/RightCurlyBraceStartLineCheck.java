import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

class Foo {
  private final static int VALUE;
  static { VALUE = 4; }                 // Noncompliant {{Move this closing curly brace to the next line.}}
  
  public void myMethod() throws IOException {
    if(something) {
      executeTask();}                   // Noncompliant {{Move this closing curly brace to the next line.}}
    else if (somethingElse) {
      doSomethingElse();
    }                                   // Compliant

    if (0) {
    ; } else if (0) {                   // Noncompliant {{Move this closing curly brace to the next line.}}
    } else {                            // Compliant
    }                                   // Compliant

    try {
      generateOrder();
    }                                   // Compliant
    finally { closeConnection();}       // Noncompliant {{Move this closing curly brace to the next line.}}
    
    try {
      executeTask();
    } catch (Exception e) { }           // Noncompliant {{Move this closing curly brace to the next line.}}
  }
  
  public int foo1() { return 0; }       // Noncompliant {{Move this closing curly brace to the next line.}}
  
  public void foo2() { }                // Noncompliant {{Move this closing curly brace to the next line.}}
  
  public void foo3(boolean test) {
    if (test) {
    }}                                  // Noncompliant {{Move this closing curly brace to the next line.}}
  
  public void foo4(boolean test) {
    if (test) {
    } else {
    continue; }                         // Noncompliant {{Move this closing curly brace to the next line.}}
  }
  
  public void foo5() {
    assert true;}                       // Noncompliant {{Move this closing curly brace to the next line.}}
  
  public void foo6() {
    try (InputStream is = new FileInputStream("")){  
    }}                                  // Noncompliant {{Move this closing curly brace to the next line.}}
  
  public void foo7() {
    try (InputStream is = new FileInputStream("")){  
    } finally {
    }}                                  // Noncompliant {{Move this closing curly brace to the next line.}}
  
  public void foo8(int test) {
    switch(test) {
    }   }                               // Noncompliant {{Move this closing curly brace to the next line.}}
  
  public void foo9(boolean test) {
    if(test) {
      do { 
      } while(true);    }               // Noncompliant {{Move this closing curly brace to the next line.}}
    if(test) {
      while(true) { break; }            // Noncompliant {{Move this closing curly brace to the next line.}}
    }
  }
  
  public void foo10(boolean test, List<Object> list) {
    if (test) {
      for (int i = 0; i < array.length; i++) {
      }}                                // Noncompliant {{Move this closing curly brace to the next line.}}
    if (test) {
      for (Object object : list) {
      }}                                // Noncompliant {{Move this closing curly brace to the next line.}}
  }
  
  public void foo11() throws IOException {
    throw new IOException(); }          // Noncompliant {{Move this closing curly brace to the next line.}}
  
  public void foo12() {
    synchronized (new Object()) {
    }}                                  // Noncompliant {{Move this closing curly brace to the next line.}}
  
  public void foo13() {
    class InnerClass {
    }}                                  // Noncompliant {{Move this closing curly brace to the next line.}}
  
  public void foo14() {
    int i;  }                           // Noncompliant {{Move this closing curly brace to the next line.}}
  
  public int foo15() {
    return 0;  }                        // Noncompliant {{Move this closing curly brace to the next line.}}
  
  public void foo16() {
    while (true) {
      break; }                          // Noncompliant {{Move this closing curly brace to the next line.}}
  }           
  
  public void fooLast() {
  }}                                    // Noncompliant {{Move this closing curly brace to the next line.}}

@Properties({}) // Compliant
class Exceptions {
  int[] numbers = new int[] { 0, 1 };   // Compliant
}

class Bar {
  {
    switch(x) {
      default:
    }
  }
}

class EmptyClass {}                     // Noncompliant {{Move this closing curly brace to the next line.}}

abstract class AbstractClass {
  abstract void foo();}                 // Noncompliant {{Move this closing curly brace to the next line.}}

@interface XmlIDREF {}                  // Noncompliant {{Move this closing curly brace to the next line.}}

enum MethodType { GET, POST }           // Noncompliant {{Move this closing curly brace to the next line.}}

enum XmlNsForm {UNQUALIFIED, QUALIFIED, UNSET
}                                       // Compliant

enum MyEnum {A, B, C {
  }}                                    // Noncompliant {{Move this closing curly brace to the next line.}}