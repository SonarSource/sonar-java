package checks;

import java.text.ParseException;

public class RedundantThrowsDeclarationCheck {
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
