package checks;

import com.github.jknack.handlebars.internal.Files;
import java.io.IOException;
import java.net.MalformedURLException;
import org.apache.xerces.xni.XNIException;
import org.slf4j.Logger;

import static java.util.logging.Level.WARNING;
import static checks.CatchUsesExceptionWithContextCheck.MyCustomLogger.staticallyImportedMethod;
import static checks.CatchUsesExceptionWithContextCheck.Provider.staticallyImportedMethodFromProvider;

class CatchUsesExceptionWithContextCheck {

  private static class A {}

  private static final Logger LOGGER = null;
  private static final org.slf4j.Marker MARKER = null;
  private static final java.util.logging.Logger JAVA_LOGGER = null;
  private static final Provider PROVIDER = new Provider();
  private static final MyCustomLogger CUSTOM_LOGGER = new MyCustomLogger();

  private void f(Exception x) {
    try {
    } catch (Exception e) { // Noncompliant {{Either log or rethrow this exception.}}
//           ^^^^^^^^^^^
    }
    try {
    } catch (Exception e) {                     // Compliant
      System.out.println(e);
    }
    try {
    }catch (Exception e) { // Noncompliant
      System.out.println("foo: " + e.getMessage());
    }
    try {
    } catch (Exception e) {                     // Compliant
      System.out.println("" + e);
    }
    try {
    } catch (Exception f) { // Noncompliant
      System.out.println("" + x);
    }
    try {
    } catch (Exception f) {                     // Compliant
      System.out.println("" + f);
    }
    try {
    } catch (Exception e) {                     // Compliant
      System.out.println("" + e);
      try {
      } catch (Exception f) { // Noncompliant
      }
    }
    try {
    } catch (RuntimeException e) {
      try {
      } catch (Exception f) {                   // Compliant
        System.out.println("" + f);
      }
      System.out.println("" + e);
    } catch (Exception e) {
      try {
      } catch (Exception f) { // Noncompliant {{Either log or rethrow this exception.}}
        System.out.println("" + e);
      }
    }
  }

  private void g() {
    System.out.println();
  }

  private void h() throws Throwable {
    Object someContextVariable = null;
    try {
      /* ... */
    } catch (Error e) {                     // Compliant
      throw com.google.common.base.Throwables.propagate(e);
    } catch (RuntimeException e) {              // Compliant - propagation
      throw e;
    } catch (Exception e) { // Noncompliant
      throw new RuntimeException("context");
    }

    try {
      /* ... */
    } catch (Exception e) {                      // Compliant
      throw new RuntimeException("context", e);
    }

    try {
    } catch (Exception e) {                      // Compliant
      throw e;
    } finally {
    }

    try {
    } catch (Exception e) { // Noncompliant
      int a;
    } catch (Throwable e) { // Noncompliant
    }

    try {
      Files.read("");
    } catch (IOException e) {                    // Compliant
      throw com.google.common.base.Throwables.propagate(e);
    }

    try {
      Files.read("");
    } catch (IOException e) {                    // Compliant
      throw new RuntimeException(e);
    } catch (Exception e) { // Noncompliant
      throw new RuntimeException(e.getMessage());
    } catch (Error e) {                      // Compliant
      throw com.google.common.base.Throwables.propagate(e);
    }

    try {
    } catch (RuntimeException e) {                      // Compliant
      throw e;
    } catch (Exception ex) {
      throw new XNIException(ex);
    }


    try {
      Thread.currentThread().join();
      if (true) {
        throw new java.text.ParseException("", 0);
      } else {
        throw new MalformedURLException();
      }
    } catch (NumberFormatException e) {          // Compliant
      return;
    } catch (InterruptedException e) {           // Compliant
      /* do nothing */
    } catch (java.text.ParseException e) {                 // Compliant
    } catch (MalformedURLException e) {          // Compliant
    } catch (java.time.format.DateTimeParseException e) {          // Compliant
    }

    try {
    } catch (NumberFormatException | java.time.format.DateTimeParseException e) { // Compliant
    }
  }

  void bar(Class<?> clazz) {
    try {
      clazz.getMethod("bar", new Class[0]);
    } catch (NoSuchMethodException e) { // Compliant
      // do nothing
    } catch (Exception e) { // Noncompliant
      System.out.println("" + e.getCause());
    }
  }

  private void fooBar() {
    try {
    } catch (Exception e) { // Compliant
      LOGGER.warn("abc");
      doSomething(e);
    }
    try {
      /* ... */
    } catch (Exception e) { // Compliant
      String message = "Exception raised while authenticating user: " + e.getMessage();
      LOGGER.warn(message);
    }
    try {
      /* ... */
    } catch (Exception e) { // Compliant
      LOGGER.warn("Some context for exception: " + e.getMessage());
    }
    try {
      /* ... */
    } catch (Exception e) { // Compliant
      String message = e.getMessage();
      LOGGER.warn("Some context for exception: " + message);
    }
    try {
      /* ... */
    } catch (Exception e) { // Noncompliant
      MyClassForCatchUses m = new MyClassForCatchUses() {
        public String doSomething(Exception innerException) {
          return innerException.getMessage();
        }
      };
      LOGGER.warn("Not a context for exception ", m.toString());
    }
    try {
      /* ... */
    } catch (Exception e) { // Compliant
      MyClassForCatchUses m = new MyClassForCatchUses() {
        public String doSomething(Exception innerException) {
          return innerException.getMessage();
        }
      };
      LOGGER.warn(m.toString(), e.getMessage());
    }
    try {
      /* ... */
    } catch (Exception e) { // Compliant
      String message = e.getMessage();
      LOGGER.warn("Some context for exception: {}", message);
    }
    try {
      /* ... */
    } catch (Exception e) { // Noncompliant
      LOGGER.warn(e.getMessage());
    }
    try {
      /* ... */
    } catch (Exception e) { // Compliant
      LOGGER.warn(MARKER, "Some context for exception: {}", e.getMessage());
    }
    try {
      /* ... */
    } catch (Exception e) { // Compliant
      LOGGER.warn(MARKER, "Some context for exception", e);
    }
    try {
      /* ... */
    } catch (Exception e) { // Noncompliant
      LOGGER.warn(MARKER, e.getMessage());
    }
    try {
      /* ... */
    } catch (Exception e) { // Noncompliant
      LOGGER.warn("Something is broken");
    }
    try {
      /* ... */
    } catch (Exception e) { // Noncompliant
      String message = "Something is broken";
      String message2 = e.getMessage();
      LOGGER.warn(message);
    }
    try {
      /* ... */
    } catch (Exception e) { // Compliant
      String message = "Some context for exception" + e.getMessage();
      LOGGER.warn(message);
    }

    try {
      /* ... */
    } catch (Exception e) { // Compliant
      String message = "Some context for exception" + e.getMessage();
      JAVA_LOGGER.info(message);
    }
    try {
      /* ... */
    } catch (Exception e) { // Compliant
      JAVA_LOGGER.info("Some context for exception" + e.getMessage());
    }
    try {
      /* ... */
    } catch (Exception e) { // Noncompliant
      JAVA_LOGGER.info(e.getMessage());
    }

    try {
      /* ... */
    } catch (Exception e) { // Compliant
      String message = "Some context for exception" + e.getMessage();
      JAVA_LOGGER.log(WARNING, message);
    }
    try {
      /* ... */
    } catch (Exception e) { // Compliant
      String message = "Some context for exception";
      message += e.getMessage();
      JAVA_LOGGER.log(WARNING, message);
    }
    try {
      /* ... */
    } catch (Exception e) { // Noncompliant
      String message = e.getMessage();
      JAVA_LOGGER.log(WARNING, message);
    }
    try {
      /* ... */
    } catch (Exception e) { // Compliant
      String message = e.getMessage();
      JAVA_LOGGER.log(WARNING, "Some context for exception", message);
    }
    try {
      /* ... */
    } catch (Exception e) { // Noncompliant
      String message = e.getMessage();
      JAVA_LOGGER.log(WARNING, "Some context for exception", "notTheMessage");
    }
    try {
      /* ... */
    } catch (Exception e) { // Noncompliant
      String message = e.getMessage();
      JAVA_LOGGER.log(null);
    }

    try {
      /* ... */
    } catch (Exception e) { // Compliant
      String message = "Some context for exception" + e.getMessage();
      JAVA_LOGGER.logp(WARNING, "", "", message);
    }
    try {
      /* ... */
    } catch (Exception e) { // Noncompliant
      JAVA_LOGGER.logp(WARNING, "", "", e.getMessage());
    }
    try {
      /* ... */
    } catch (Exception e) { // Noncompliant
      String notAMessage = "";
      notAMessage = notAMessage;
      JAVA_LOGGER.logp(WARNING, "", "", notAMessage);
    }
    try {
      /* ... */
    } catch (Exception e) { // Noncompliant
      JAVA_LOGGER.logp(WARNING, "", e.getMessage(), e.getMessage());
      doSomethingElse("", "", "", e.getMessage());
    }
    try {
      /* ... */
    } catch (Exception e) { // Compliant
      String message = e.getMessage();
      JAVA_LOGGER.logp(WARNING, "", "", "Some context for exception", message);
    }
    try {
      /* ... */
    } catch (Exception e) { // Noncompliant
      String message = e.getMessage();
      JAVA_LOGGER.logp(WARNING, "", "", "Some context for exception", "notTheMessage");
    }

    try {
      /* ... */
    } catch (Exception e) { // Compliant
      String message = "Some context for exception" + e.getMessage();
      JAVA_LOGGER.logrb(WARNING, "", "", "", message);
    }
    try {
      /* ... */
    } catch (Exception e) { // Noncompliant
      JAVA_LOGGER.logrb(WARNING, "", "", "", e.getMessage());
    }
    try {
      /* ... */
    } catch (Exception e) { // Compliant
      String message = e.getMessage();
      JAVA_LOGGER.logrb(WARNING, "", "", "", "Some context for exception", message);
    }
    try {
      /* ... */
    } catch (Exception e) { // Noncompliant
      String message = e.getMessage();
      JAVA_LOGGER.logrb(WARNING, "", "", "", "Some context for exception", "notTheMessage");
    }
  }


  void customLogs() {
    try {
      /* ... */
    } catch (Exception e) { // Compliant
      CUSTOM_LOGGER.log(e);
    }
    try {
      /* ... */
    } catch (Exception e) { // Noncompliant
      CUSTOM_LOGGER.log(e.getMessage());
    }
    try {
      /* ... */
    } catch (Exception e) { // Compliant, heuristic detects "log", assume it will be correctly handled
      String message = "Some context for exception" + e.getMessage();
      CUSTOM_LOGGER.log(message);
    }
    try {
      /* ... */
    } catch (Exception e) { // Compliant
      CUSTOM_LOGGER.log("something", e);
    }
    try {
      /* ... */
    } catch (Exception e) { // Compliant
      String message = "Some context for exception" + e.getMessage();
      CUSTOM_LOGGER.log("something", message);
    }
    try {
      /* ... */
    } catch (Exception e) { // Compliant
      CUSTOM_LOGGER.doSomethingWithException(e);
    }
    try {
      /* ... */
    } catch (Exception e) { // Compliant, heuristic detects "log", assume it will be correctly handled
      String message = "Some context for exception" + e.getMessage();
      CUSTOM_LOGGER.doSomethingWithMessage(message);
    }
    try {
      /* ... */
    } catch (Exception e) { // Compliant
      PROVIDER.getLogger().doSomethingWithException(e);
    }
    try {
      /* ... */
    } catch (Exception e) { // Compliant, heuristic detects "log", assume it will be correctly handled
      String message = "Some context for exception" + e.getMessage();
      PROVIDER.getLogger().doSomethingWithMessage(message);
    }
    try {
      /* ... */
    } catch (Exception e) { // Compliant
      PROVIDER.doSomethingWithException(e);
    }
    try {
      /* ... */
    } catch (Exception e) { // Noncompliant
      String message = "Some context for exception" + e.getMessage();
      PROVIDER.doSomethingWithMessage(message); // No clear sign that this call will do something useful
    }
    try {
      /* ... */
    } catch (Exception e) { // Compliant, heuristic detects "log", assume it will be correctly handled
      String message = "Some context for exception" + e.getMessage();
      logSomething(message);
    }
    try {
      /* ... */
    } catch (Exception e) { // Noncompliant
      String message = "Some context for exception" + e.getMessage();
      doSomething(message); // No clear sign that this call will do something useful
    }
    try {
      /* ... */
    } catch (Exception e) { // Noncompliant
      String message = "Some context for exception" + e.getMessage();
      PROVIDER.getLogger();
    }
    try {
      /* ... */
    } catch (Exception e) { // Compliant, heuristic detects "log", assume it will be correctly handled
      String message = "Some context for exception" + e.getMessage();
      CUSTOM_LOGGER.log("a", message);
      PROVIDER.getLogger();
    }
    try {
      /* ... */
    } catch (Exception e) { // Compliant
      String message = "Some context for exception" + e.getMessage();
      CUSTOM_LOGGER.setSomething("something").log(message);
    }
    try {
      /* ... */
    } catch (Exception e) { // Compliant
      String message = "Some context for exception" + e.getMessage();
      CUSTOM_LOGGER.setSomething(message).doSomethingWithMessage("Other message");
    }
    try {
      /* ... */
    } catch (Exception e) { // Noncompliant
      CUSTOM_LOGGER.log(e.getMessage());
    }
    try {
      /* ... */
    } catch (Exception e) { // Compliant
      String message = "Some context for exception" + e.getMessage();
      MyCustomLogger.staticallyImportedMethod(message);
    }
    try {
      /* ... */
    } catch (Exception e) { // Compliant, type contains "log"
      String message = "Some context for exception" + e.getMessage();
      staticallyImportedMethod(message);
    }
    try {
      /* ... */
    } catch (Exception e) { // Noncompliant
      String message = "Some context for exception" + e.getMessage();
      staticallyImportedMethodFromProvider(message);
    }
  }

  private void doSomething(Object e) {}
  private void doSomethingElse(String a, String b, String c, String d) {}
  private void logSomething(Object e) {}

  interface MyClassForCatchUses {
    String doSomething(Exception innerException);
  }

  MyEnum foo(Object... args) {
    try {
      return Enum.valueOf(MyEnum.class, "C");
    } catch (IllegalArgumentException e) { // Compliant
      return null;
    }
  }

  MyEnum qix() {
    try {
      return MyEnum.valueOf("C");
    } catch (IllegalArgumentException e) { // Compliant
      return null;
    }
  }

  MyEnum gul() {
    try {
      new A() {
        void bul() throws Exception {
          MyEnum.valueOf("C");
        }
      };
      java.util.function.Function<String, MyEnum> getValue = (name) -> MyEnum.valueOf(name);
      return MyEnum.valueOf("C");
    } catch (IllegalArgumentException e) { // Compliant
      return null;
    }
  }

  private enum MyEnum {
    A, B;
  }

  static class MyCustomLogger {
    void log(Throwable t) {
    }
    void log(String t) {
    }
    void log(String s, Throwable t) {
    }
    void log(String s1, String s2) {
    }
    void doSomethingWithException(Throwable t) {
    }
    void doSomethingWithMessage(String t) {
    }
    public static void staticallyImportedMethod(String t) {
    }
    MyCustomLogger setSomething(String s) {
      return this;
    }
  }

  static class Provider {
    MyCustomLogger getLogger() {
      return new MyCustomLogger();
    }
    void doSomethingWithException(Throwable t) {
    }
    void doSomethingWithMessage(String t) {
    }
    public static void staticallyImportedMethodFromProvider(String t) {
    }
  }

  private void log_builder() {
    class Builder {
      Builder(String message) {
      }
      void log() {
      }
    }
    try {
      /* ... */
    } catch (Exception e) { // Compliant
      String message = "Exception raised while authenticating user: " + e.getMessage();
      new Builder(message).log();
    }
  }

}
