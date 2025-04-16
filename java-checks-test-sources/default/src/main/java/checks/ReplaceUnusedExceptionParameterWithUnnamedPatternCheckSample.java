package checks;

import io.restassured.exception.PathException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ReplaceUnusedExceptionParameterWithUnnamedPatternCheckSample {
  public void simpleNonCompliant() {
    try {
    } catch (Exception e) {
      var x = e;
    }

    List<String> elements = List.of();
    int value = 0;
    try {
      var elem = elements.get(10);
      value = Integer.parseInt(elem);
    } catch (NumberFormatException nfe) { // Noncompliant {{Replace nfe with an unnamed pattern.}}
      //                           ^^^
      System.err.println("Wrong number format");
    } catch (IndexOutOfBoundsException ioob) { // Noncompliant {{Replace ioob with an unnamed pattern.}}
      System.err.println("No such element");
    }
  }

  public void simpleCompliant() {
    List<String> elements = List.of();
    int value = 0;
    try {
      var elem = elements.get(10);
      value = Integer.parseInt(elem);
    } catch (NumberFormatException _) { // compliant
      System.err.println("Wrong number format");
    } catch (IndexOutOfBoundsException _) { // compliant
      System.err.println("No such element");
    }
  }

  public void severalExceptionNonCompliant() {
    try {

    } catch (NumberFormatException | IndexOutOfBoundsException e) { // Noncompliant

    } catch (PathException | ClassCastException e) { // Noncompliant

    }
  }

  public void severalExceptionCompliant() {
    try {

    } catch (NumberFormatException | IndexOutOfBoundsException _) { // compliant

    } catch ( PathException | ClassCastException _) { // compliant

    }
  }

  public void nestedExceptionNonCompliant() {
    try {
    } catch (IndexOutOfBoundsException e) { // Noncompliant
      try {

      } catch (NumberFormatException k) { // Noncompliant
        try {

        } catch (PathException _) {

        }
      }
    }
  }

  public void codeInsideCatchBlockNonCompliant() {
    try {
    } catch (Exception e) { // Noncompliant
      int x = 0;
      foo(new RuntimeException());
      e();
    }
  }

  public void useExceptionCompliant() {
    try {
    } catch (Exception e) {
      var x = e;
    }

    try {
    } catch (Exception e) {
      foo(e);
    }

    try {
    } catch (Exception e) {
      throw e;
    }

    try {
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    try {
    } catch (Exception e) {
      var s = e.getStackTrace();
    }

    try {
    } catch (Exception e) {
      Supplier<Integer> s =() -> {
        var x = e;
        return 0;
      };
    }
  }

  public void foo(Exception e) {
  }

  public void e(){}
}
