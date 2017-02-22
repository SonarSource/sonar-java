import org.sonar.java.checks.TooManyStatementsPerLine_S00122_CheckTest;
import org.sonar.java.checks.TooManyStatementsPerLine_S00122_CheckTest.E;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

abstract class TooManyStatementsPerLine {

  enum MyEnum {
    A, B
  }

  static private final String CLASS_PREFIX;
  static {
    if (Math.random() > 0.5) throw new InternalError(); // Noncompliant {{At most one statement is allowed per line, but 2 statements were found on this line.}}
  }

  int a;
  int b; // Compliant - not a statement

  String method(MyEnum enumValue, List myList, boolean test) {
    
    class MyClass {
      void bar() {
        int delta; long beta, gamma; // Noncompliant {{At most one statement is allowed per line, but 2 statements were found on this line.}}
      }
    }
    
    int myVar, my2ndVar = 4; // Compliant
    int ich; int ni; int san; // Noncompliant {{At most one statement is allowed per line, but 3 statements were found on this line.}}
    
    doSomething(); doSomethingElse(); // Noncompliant {{At most one statement is allowed per line, but 2 statements were found on this line.}}
    
    int a = 0; a++; // Noncompliant {{At most one statement is allowed per line, but 2 statements were found on this line.}}
    
    new Object().getClass();
    (new A()).getClass();
    TooManyStatementsPerLine tmspl = new A();
    tmspl.new InnerClass();
    
    doSomething(
    ); doSomethingElse(); // Noncompliant {{At most one statement is allowed per line, but 2 statements were found on this line.}}
    
    Object[] elementData;
    Object[] data = elementData = new Object[10]; // Compliant
    
    if (test) {} // Compliant

    if (test) {} if (test) {} // Noncompliant {{At most one statement is allowed per line, but 2 statements were found on this line.}}
    
    if (test)
      System.out.println("plop"); // Compliant
    
    if (test) {
      return doSomething(
      );} // Compliant
    if (test)
      return "";
    else if (test)
      return "";
    else
    {
      return "";
    } // Compliant
    
    if (test) {
      // comment
    } if (test) { // Noncompliant {{At most one statement is allowed per line, but 2 statements were found on this line.}}
      a++;
    }
    if(test) return false + ""; // Noncompliant {{At most one statement is allowed per line, but 2 statements were found on this line.}}
    if (test) System.out.println( // Noncompliant {{At most one statement is allowed per line, but 2 statements were found on this line.}}
        "ServerTableEntry constructed with activation command " +
            test);
    if(test){
      throw new Exception();
    }else if(test) return true + ""; // Noncompliant {{At most one statement is allowed per line, but 2 statements were found on this line.}}
    
    Object root;
    if (test) root = has(MyEnum.A) // Noncompliant {{At most one statement is allowed per line, but 2 statements were found on this line.}}
                           ? new Object() 
                           : new Object();
                           
    int foo = has(MyEnum.A) ? 0 : 1 ; // Compliant

    try {
      System.out.println("plop"); // Compliant
    } catch (Exception e){
      return ""; } // Compliant
    finally {
      a++; foo(); // Noncompliant {{At most one statement is allowed per line, but 2 statements were found on this line.}}
    }
    
    try {
      System.out.println("plop"); // Compliant
    } catch (Exception e) {
      System.out.println("plop"); // Compliant
    }
    
    try (FileInputStream s = new FileInputStream("")) {
      System.out.println("plop"); // Compliant
    } catch (Exception e) {
      System.out.println("plop"); // Compliant
    }
    
    for (int i = 0; i < 42; i++)
    {
      continue;
    }

    for (int i = 0; i < 42; i++) {}
    
    for (Object object : myList) { object.getClass(); } // Noncompliant {{At most one statement is allowed per line, but 2 statements were found on this line.}}
    
    while (test); // Compliant

    label: while (test) { // Compliant
      break label; // Compliant
    }
    
    do { a++; } while (a < 10); // Noncompliant {{At most one statement is allowed per line, but 2 statements were found on this line.}}
    
    do a++; while (a < 20); // Noncompliant {{At most one statement is allowed per line, but 2 statements were found on this line.}}
    
    do {} while(a < 20); // Compliant
    
    do ++a; // Noncompliant {{At most one statement is allowed per line, but 2 statements were found on this line.}}
    while (a < 20); // Compliant
    
    synchronized (myList) { System.out.println(""); } // Noncompliant {{At most one statement is allowed per line, but 2 statements were found on this line.}}
    
    switch(enumValue) {
      case A: 
        a++; a--; break; // Noncompliant {{At most one statement is allowed per line, but 3 statements were found on this line.}}
      default:
        a++;
        break; 
    }
    
    elementData[2] = 4;
    assert true;
    "1".length(); // literals does not have associated syntax token...
    assert true; "1" // Noncompliant {{At most one statement is allowed per line, but 2 statements were found on this line.}}
      .length();
  }

  private void doSomethingElse() {
  }

  private String doSomething() {
    return "";
  }

  public class InnerClass {
    void bar() {
      int b; b++; // Noncompliant {{At most one statement is allowed per line, but 2 statements were found on this line.}}
    }
  }
  
  abstract void foo();

  boolean has(Object o) {
    return true;
  }
  
  void e_method() {
    TooManyStatementsPerLine.<A>methodStatic(new A()); // Compliant
  }

  static <T> T methodStatic(T param) {
    return param;
  };
}

class A extends TooManyStatementsPerLine {

  @Override
  void foo() {
  }

}
  class Test {
    {
    int x = 0; int y = 0; // Noncompliant
    if (x == y) System.out.println(""); // Noncompliant
    }
    }


