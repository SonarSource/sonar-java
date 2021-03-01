package checks;

import checks.Foo;
import com.github.jknack.handlebars.internal.Files;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import java.io.IOException;
import java.net.MalformedURLException;
import org.slf4j.Logger;

import static java.util.logging.Level.WARNING;

class CatchUsesExceptionWithContextCheck {
  void tryCatchWithUnknown() {
    try {
      /* ... */
    } catch (Exception e) { // Noncompliant
      JAVA_LOGGER.logp(WARNING, "", "", "Some context for exception", unknown);
    }
  }

  void tryWithMultipleCatch() {
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
    } catch (NumberFormatException e) { // Compliant
    } catch (MalformedURLException e) {  // Compliant
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
}
