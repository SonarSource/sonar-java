package checks;

import java.io.IOException;
import java.text.ParseException;

public class RedundantThrowsDeclarationCheckSample {
}

abstract class MySuperClass {
  abstract void foo() throws MyException;
}

abstract class ThrownCheckedExceptions extends MySuperClass {

  void foo1() throws MyException { // Compliant - unknown method is called
    unknownMethod();
    bar();
  }

  void foo2() throws UnknownException { // Compliant - unknown exception
    qix();
  }

  void foo3(java.io.File file) throws ParseException { // Compliant if we can't resolve types
    try (UnknownClass mac = getUnknownClass(file)) {
      // do something
    }
  }

  abstract void bar();
  abstract void qix() throws UnknownException;
  abstract AutoCloseable getAutoCloseableWithoutExceptionPlease(java.io.File file);

  void foo15(java.io.File file) throws Exception { // Compliant - AutoCloseable.close() throws Exception
      // java9+
    try (ac) {
      // do something
    }
  }
}

class MyException extends Exception {}

abstract class Test {
  abstract void foo() throws Unknown, Unknown;
}

class C extends UnknownParent {
  public C() throws IllegalAccessException {
  }
}

record MyRecord(int a) {
  // [Compilation ERROR] invalid compact constructor in record MyRecord, throws clause not allowed for compact constructor
  private MyRecord throws IOException { // Noncompliant {{Remove the declaration of thrown exception 'java.io.IOException', as it cannot be thrown from constructor's body.}} [[quickfixes=qf_all_in_record]]
//                        ^^^^^^^^^^^
    // fix@qf_all_in_record {{Remove "IOException"}}
    // edit@qf_all_in_record [[sc=19;ec=38]] {{}}
  }
}
