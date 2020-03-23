package checks;

import checks.Foo;
import com.github.jknack.handlebars.internal.Files;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import java.io.IOException;
import java.net.MalformedURLException;
import org.slf4j.Logger;

import static java.util.logging.Level.WARNING;

class CatchUsesExceptionWithContextCheck {
  private static final Logger LOGGER = null;
  private static final org.slf4j.Marker MARKER = null;
  private static final java.util.logging.Logger JAVA_LOGGER = null;

  private void f(Exception x) {
    try {
    } catch (Exception e) {                     // Noncompliant {{Either log or rethrow this exception.}} [[sc=14;ec=25]]
    } catch (Exception e) {                     // Compliant
      System.out.println(e);
    } catch (Exception e) {                     // Noncompliant
      System.out.println("foo: " + e.getMessage());
    } catch (Exception e) {                     // Compliant
      System.out.println("" + e);
    } catch (Exception f) {                     // Noncompliant
      System.out.println("" + x);
    } catch (Exception f) {                     // Compliant
      System.out.println("" + f);
    } catch (Exception e) {                     // Compliant
      System.out.println("" + e);
      try {
      } catch (Exception f) {                   // Noncompliant
      }
    } catch (Exception e) {
      try {
      } catch (Exception f) {                   // Noncompliant {{Either log or rethrow this exception.}}
        System.out.println("" + e);
      }
    } catch (RuntimeException e) {
      try {
      } catch (Exception f) {                   // Compliant
        System.out.println("" + f);
      }
      System.out.println("" + e);
    }
  }

  private void g() {
    System.out.println();
  }

  private void h() {
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
      if (true) {
        throw new java.text.ParseException("", 0);
      } else {
        throw new MalformedURLException();
      }
      Thread.currentThread().join();
    } catch (NumberFormatException e) {          // Compliant
      return;
    } catch (InterruptedException e) {           // Compliant
      /* do nothing */
    } catch (java.text.ParseException e) {                 // Compliant
    } catch (MalformedURLException e) {          // Compliant
    } catch (java.time.format.DateTimeParseException e) {          // Compliant
    }

    try {
    } catch (Exception e) {                      // Compliant
       foo(someContextVariable, e);
    } catch (Exception e) {                      // Compliant
      throw (Exception)new Foo("bar").initCause(e);
    } catch (Exception e) {                      // Compliant
      foo(null, e).bar();
    } catch (Exception e) {                      // Compliant
      throw foo(e).bar();
    } catch (Exception e) {                      // Noncompliant
      throw e.getCause();
    } catch (Exception e) {                      // Compliant
      throw (Exception)e;
    } catch (Exception e) {                      // Compliant
      throw (e);
    } catch (Exception e) {                      // Noncompliant
      throw (e).getClause();
    } catch (Exception e) {                      // Compliant
      Exception e2 = e;
      throw e2;
    } catch (Exception e) {                      // Compliant
      Exception foo = new RuntimeException(e);
    } catch (Exception e) {
      Exception foo = (e);
    } catch (Exception e) {                      // Compliant
      Exception foo;
      foo = e;
    } catch (java.lang.NumberFormatException e) { // Compliant
    } catch (java.net.MalformedURLException e) {  // Compliant
    } catch (java.time.format.DateTimeParseException e) {          // Compliant
    } catch (java.text.ParseException e) {        // Compliant
    } catch (java.text.foo e) {                   // Noncompliant
    } catch (java.foo.ParseException e) {         // Noncompliant [[sc=14;ec=39]]
    } catch (foo.text.ParseException e) {         // Noncompliant
    } catch (text.ParseException e) {             // Noncompliant
    } catch (foo.java.text.ParseException e) {    // Noncompliant
    } catch (Exception e) {                       // Compliant
      Exception foo = false ? e : null;
    } catch (Exception e) {                       // Compliant
      Exception foo = false ? null : e;
    } catch (Exception e) {                       // Compliant
      Exception e2;
      foo = (e2 = e) ? null : null;
    } catch (Exception e) {                       // Compliant
      throw wrapHttpException ? handleHttpException(e) : null;
    } catch (Exception e) {                       // Compliant
      throw wrapHttpException ? null : e;
    }
    catch (Exception e) {                     // Noncompliant
      try {
      } catch (Exception f) {                   // Noncompliant
       System.out.println("", e.getCause());
      }
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
      MyClass m = new MyClass() {
        public String doSomething(Exception innerException) {
          return innerException.getMessage();
        }
      };
      LOGGER.warn("Not a context for exception ", m.toString());
    }
    try {
      /* ... */
    } catch (Exception e) { // Compliant
      MyClass m = new MyClass() {
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
      JAVA_LOGGER.logp(WARNING, "", "", "Some context for exception", unknown);
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

  interface MyClass {
    void doSomething();
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
