package checks.tests.AssertionsInTestsCheck;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.approvej.ApprovalBuilder.approve;
import static org.approvej.print.MultiLineStringPrintFormat.multiLineString;

class ApproveJTest {

  @Test
  void contains_no_assertions() { // Noncompliant
  }

  public String hello() {return "hello";}

  @Test
  void approve_string() {
    String result = hello();
    approve(result)
      .byFile();
  }

  public record Person(String name, LocalDate birthDate) {}

  @Test
  void approve_person() {
    Person jane = new Person("Jane Doe", LocalDate.of(1990, 1, 1));
    approve(jane).named("jane").printedAs(multiLineString()).byFile();
  }

}
