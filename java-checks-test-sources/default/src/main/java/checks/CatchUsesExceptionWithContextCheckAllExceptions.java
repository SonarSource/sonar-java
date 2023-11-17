package checks;

import java.net.MalformedURLException;
import java.text.ParseException;
import java.time.format.DateTimeParseException;

abstract class CatchUsesExceptionWithContextCheckAllExceptions {

  private void bar() {
    try {
      foo();
    } catch (NumberFormatException e) {  // Noncompliant {{Either log or rethrow this exception.}}
    } catch (InterruptedException e) {   // Noncompliant
    } catch (ParseException e) {         // Noncompliant
    } catch (MalformedURLException e) {  // Noncompliant
    } catch (DateTimeParseException e) { // Noncompliant
    }
  }

  abstract void foo() throws InterruptedException, ParseException, MalformedURLException;
}
