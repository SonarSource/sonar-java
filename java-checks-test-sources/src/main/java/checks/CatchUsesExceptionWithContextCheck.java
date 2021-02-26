package checks;

import com.github.jknack.handlebars.internal.Files;
import java.io.IOException;
import java.net.MalformedURLException;
import org.apache.xerces.xni.XNIException;
import org.slf4j.Logger;

import static java.util.logging.Level.WARNING;

class CatchUsesExceptionWithContextCheck {
  private static final Logger LOGGER = null;
  private static final org.slf4j.Marker MARKER = null;
  private static final java.util.logging.Logger JAVA_LOGGER = null;

  private void f(Exception x) {
    try {
    } catch (Exception e) {                     // Noncompliant {{Either log or rethrow this exception.}} [[sc=14;ec=25]]
    }
    try {
    } catch (Exception e) {                     // Compliant
      System.out.println(e);
    }
    try {
    }catch (Exception e) {                     // Noncompliant
      System.out.println("foo: " + e.getMessage());
    }
    try {
    } catch (Exception e) {                     // Compliant
      System.out.println("" + e);
    }
    try {
    } catch (Exception f) {                     // Noncompliant
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
      } catch (Exception f) {                   // Noncompliant
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
      } catch (Exception f) {                   // Noncompliant {{Either log or rethrow this exception.}}
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
    } catch (Exception e) {                     // Noncompliant
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
    } catch (Exception e) {                      // Noncompliant
      int a;
    } catch (Throwable e) {                      // Noncompliant
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
    } catch (Exception e) {                      // Noncompliant
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

  private void doSomething(Object e) {}
  private void doSomethingElse(String a, String b, String c, String d) {}

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
}
