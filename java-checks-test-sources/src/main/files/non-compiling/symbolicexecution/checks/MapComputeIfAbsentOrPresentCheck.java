package symbolicexecution.checks;

import com.google.common.base.Preconditions;

import javax.annotation.CheckForNull;

import java.util.Map;
import java.util.Objects;

abstract class ExceptionThrown {
  void foo(Map<String, Object> items, String key) throws MyException {
    Object value = items.get(key);
    if (value == null) {
      items.put(key, bar()); // Compliant, bar() can throw a checked exception, so it can not be extracted to a lambda
    }
  }
  abstract String bar() throws MyException;
  abstract String bar2() throws MyRuntimeException;
  abstract String bar4() throws UnknownException;
  static class MyException extends Exception { }
  static class MyRuntimeException extends RuntimeException { }
  void foo2(Map<String, Object> items, String key) throws MyException {
    Object value = items.get(key);
    if (value == null) {
      items.put(key, unknown_method()); // Compliant, unknown method so put is not resolved
    }
  }
  void foo3(Map<String, Object> items, String key) {
    Object value = items.get(key); // Noncompliant (bar2 is throwing a runtime exception)
    if (value == null) {
      items.put(key, bar2());
    }
  }
  void foo4(Map<String, Object> items, String key) {
    Object value = items.get(key);
    if (value == null) {
      items.put(key, bar4()); // compliant : exception thrown is unknown
    }
  }

  void foo5(Map<String, UnknownObject> items, String key) throws MyException {
    Object value = items.get(key);
    if (value == null) {
      items.put(key, unknown_method()); // Compliant, unknown method so put is not resolved
    }
  }
}
