import static java.sql.Date.from;

class JodaTime {
  Date epochDate = from(Instant.EPOCH);
}
// Noncompliant@0 {{Use the "java.time" API for date and time.}}
