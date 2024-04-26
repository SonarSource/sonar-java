import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

class Foo {
  private final static int VALUE;
  static { VALUE = 4; }
  
  public void myMethod() throws IOException {
    if(something) {
      executeTask();} // Noncompliant {{Move this closing curly brace to the next line.}}
//                  ^
    else if (somethingElse) {
      doSomethingElse();
    }

    if (0) {
    ; } else if (0) { // Noncompliant {{Move this closing curly brace to the next line.}}
    } else {
    }

    try {
      generateOrder();
    }
    finally { closeConnection();}
    
    try {
      executeTask();
    } catch (Exception e) { }
  }
  
  public int foo1() { return 0; }
  
  public void foo2() { }
  
  public void foo3(boolean test) {
    if (test) {
    }} // Noncompliant {{Move this closing curly brace to the next line.}}
  
  public void foo4(boolean test) {
    if (test) {
    } else {
    return; } // Noncompliant {{Move this closing curly brace to the next line.}}
  }
  
  public void foo5() {
    assert true;} // Noncompliant {{Move this closing curly brace to the next line.}}
  
  public void foo6() {
    try (InputStream is = new FileInputStream("")){  
    }} // Noncompliant {{Move this closing curly brace to the next line.}}
  
  public void foo7() {
    try (InputStream is = new FileInputStream("")){  
    } finally {
    }} // Noncompliant {{Move this closing curly brace to the next line.}}
  
  public void foo8(int test) {
    switch(test) {
    }   } // Noncompliant {{Move this closing curly brace to the next line.}}
  
  public void foo9(boolean test) {
    if(test) {
      do { 
      } while(true);    } // Noncompliant {{Move this closing curly brace to the next line.}}
    if(test) {
      while(true) { break; }
    }
  }
  
  public void foo10(boolean test, List<Object> list) {
    if (test) {
      for (int i = 0; i < array.length; i++) {
      }} // Noncompliant {{Move this closing curly brace to the next line.}}
    if (test) {
      for (Object object : list) {
      }} // Noncompliant {{Move this closing curly brace to the next line.}}
  }
  
  public void foo11() throws IOException {
    throw new IOException(); } // Noncompliant {{Move this closing curly brace to the next line.}}
  
  public void foo12() {
    synchronized (new Object()) {
    }} // Noncompliant {{Move this closing curly brace to the next line.}}
  
  public void foo13() {
    class InnerClass {
    }} // Noncompliant {{Move this closing curly brace to the next line.}}
  
  public void foo14() {
    int i;  } // Noncompliant {{Move this closing curly brace to the next line.}}
  
  public int foo15() {
    return 0;  } // Noncompliant {{Move this closing curly brace to the next line.}}
  
  public void foo16() {
    while (true) {
      break; } // Noncompliant {{Move this closing curly brace to the next line.}}
  }           
  
  public void fooLast() {
  }} // Noncompliant {{Move this closing curly brace to the next line.}}

@Properties({})
class Exceptions {
  int[] numbers = new int[] { 0, 1 };
}

class Bar {
  {
    switch(x) {
      default:
    }
  }
}

class EmptyClass {}

abstract class AbstractClass {
  abstract void foo();} // Noncompliant {{Move this closing curly brace to the next line.}}

@interface XmlIDREF {}

enum MethodType { GET, POST }

enum XmlNsForm {UNQUALIFIED, QUALIFIED, UNSET
}

enum MyEnum {A, B, C {
  }} // Noncompliant {{Move this closing curly brace to the next line.}}
