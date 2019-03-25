import java.util.List;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.math.BigInteger;
import java.math.BigDecimal;
import java.util.Optional;

class A {
  List<String> list;
  void voidMethod() {}
  int intMethod() {}
  UnknownType unknownTypeMethod() {}
  void foo() {
    int a = intMethod(); //Compliant
    intMethod(); //Compliant
    voidMethod(); //Compliant
    new A().intMethod();//Compliant
    new A().voidMethod();//Compliant
    unknownTypeMethod();//Compliant type is unknown
    unresolvedMethod();//Compliant method is not resolved so type is unknown
    fluentMethod(""); //Compliant
    Integer.valueOf("1").byteValue(); // Noncompliant {{The return value of "byteValue" must be used.}}
    "plop".replace('p', 'b'); // Noncompliant [[sc=12;ec=19]] {{The return value of "replace" must be used.}}
    new RuntimeException("plop").getStackTrace()[0].getClassName(); // Noncompliant {{The return value of "getClassName" must be used.}}
    a++;
    list.stream().filter(s -> s.length() > 4).map(s -> s.length()).forEach(i -> {System.out.println(i);});

    DayOfWeek.of(5); // Noncompliant
    Duration.ofDays(5); // Noncompliant
    Instant.now(); // Noncompliant
    LocalDate.now(); // Noncompliant
    LocalDateTime.now(); // Noncompliant
    LocalTime.now(); // Noncompliant
    Month.of(11).minus(12); // Noncompliant
    MonthDay.now().withMonth(11); // Noncompliant
    OffsetDateTime.now().minusDays(2); // Noncompliant
    OffsetTime.now(); // Noncompliant
    Period.ofDays(2); // Noncompliant
    Year.now(); // Noncompliant
    YearMonth.now(); // Noncompliant
    ZonedDateTime.now(); // Noncompliant
    BigInteger.valueOf(12L).add(BigInteger.valueOf(12563159)); // Noncompliant
    BigDecimal.valueOf(12L).add(BigDecimal.valueOf(12563159)); // Noncompliant
    Optional<String> o = Optional.empty();
    o.map(o -> o.toString()); // Noncompliant

    String s = "s";
    s.intern(); // Compliant

    Character c = Character.valueOf('c');
    c.toChars(0, new char[42], 21); // Compliant
    s.getBytes(java.nio.charset.Charset.forName("UTF-8")); // Noncompliant not within a try/catch
  }

  private boolean textIsInteger(String textToCheck) {

    try {
      Integer.parseInt(textToCheck, 10); // OK
      textToCheck.getBytes(java.nio.charset.Charset.forName("UTF-8"));
      return true;
    } catch (NumberFormatException ignored) {
      return false;
    }
    try {
      Integer.parseInt(textToCheck, 10); // Noncompliant
      textToCheck.getBytes(java.nio.charset.Charset.forName("UTF-8")); // Noncompliant
      return true;
    }finally {
      // do something
    }
  }

  private void putIfAbsentTest() {
    private final java.util.concurrent.ConcurrentMap<String, String> map1 = new java.util.concurrent.ConcurrentHashMap<>();
    map1.putIfAbsent("val", "val"); // Compliant, 'putIfAbsent' does have a side-effect
  }
}
