import org.joda.time.LocalDate;

class JodaTime {
  LocalDate currentDate = LocalDate.now();
}
// Noncompliant@0 {{Use the "java.time" API for date and time.}}
