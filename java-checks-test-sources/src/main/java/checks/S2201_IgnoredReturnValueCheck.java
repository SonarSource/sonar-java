package checks;

import java.math.BigDecimal;
import java.math.BigInteger;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;

class S2201_IgnoredReturnValueCheck {
  List<String> list;
  HashSet<String> set;
  Map<Object, Object> map;
  HashMap<Object, Object> hashmap;
  void voidMethod() {}
  int intMethod() { return 0; }
  void foo() {
    int a = intMethod(); //Compliant
    intMethod(); //Compliant
    voidMethod(); //Compliant
    new S2201_IgnoredReturnValueCheck().intMethod();// Compliant
    new S2201_IgnoredReturnValueCheck().voidMethod();// Compliant
    Integer.valueOf("1").byteValue(); // Noncompliant {{The return value of "byteValue" must be used.}}
    "plop".replace('p', 'b'); // Noncompliant [[sc=12;ec=19]] {{The return value of "replace" must be used.}}
    new RuntimeException("plop").getStackTrace()[0].getClassName(); // Noncompliant {{The return value of "getClassName" must be used.}}
    a++;
    list.stream().filter(s -> s.length() > 4).map(String::length).forEach(i -> {System.out.println(i);});

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
    o.map(String::toString); // Noncompliant
    com.google.common.base.Optional<String> o2 = com.google.common.base.Optional.absent();
    o2.transform(@Nullable String::toString); // Noncompliant

    String s = "s";
    s.intern(); // Compliant

    Character c = Character.valueOf('c');
    c.toChars(0, new char[42], 21); // Compliant
    s.getBytes(java.nio.charset.Charset.forName("UTF-8")); // Noncompliant not within a try/catch

    list.size(); // Noncompliant
    list.iterator(); // Noncompliant
    list.contains(new Object()); // Noncompliant
    list.toArray(); // Noncompliant

    set.containsAll(Collections.singletonList(new Object())); // Noncompliant
    set.isEmpty(); // Noncompliant

    map.get("yolo"); // Noncompliant
    map.getOrDefault("yolo", "yes"); // Noncompliant
    map.size(); // Noncompliant
    map.isEmpty(); // Noncompliant
    hashmap.values(); // Noncompliant
    hashmap.keySet(); // Noncompliant
    hashmap.entrySet(); // Noncompliant
    hashmap.containsValue(new Object()); // Noncompliant
    hashmap.containsKey(new Object()); // Noncompliant

    Object[] arr = new Object[42];

    set.add("hello"); // Compliant
    list.toArray(arr);
    map.put(new Object(), null);
  }

  private boolean textIsInteger(String textToCheck) {

    try {
      Integer.parseInt(textToCheck, 10); // OK
      textToCheck.getBytes(java.nio.charset.Charset.forName("UTF-8"));
      return true;
    } catch (NumberFormatException ignored) {
      return false;
    } catch (Exception e) {
      // do nothing
    }
    try {
      Integer.parseInt(textToCheck, 10); // Noncompliant
      textToCheck.getBytes(java.nio.charset.Charset.forName("UTF-8")); // Noncompliant
      return true;
    } finally {
      // do something
    }
  }

  private void putIfAbsentTest() {
    final java.util.concurrent.ConcurrentMap<String, String> map1 = new java.util.concurrent.ConcurrentHashMap<>();
    map1.putIfAbsent("val", "val"); // Compliant, 'putIfAbsent' does have a side-effect
  }

  void streamTerminals() {
    Stream.of("a", "b", "c").toArray(); // Noncompliant
    Stream.of("a", "b", "c").reduce((a,b) -> a+b); // Noncompliant
    Stream.of("a", "b", "c").collect(Collectors.joining()); // Noncompliant
    Stream.of("a", "b", "c").collect(Collectors.toList()); // Noncompliant
    Stream.of("a", "b", "c").min(Comparator.naturalOrder()); // Noncompliant
    Stream.of("a", "b", "c").max(Comparator.naturalOrder()); // Noncompliant
    Stream.of("a", "b", "c").count(); // Noncompliant
    Stream.of("a", "b", "c").anyMatch(s -> s.length() > 1); // Noncompliant
    Stream.of("a", "b", "c").allMatch(String::isEmpty); // Noncompliant
    Stream.of("a", "b", "c").noneMatch(String::isBlank); // Noncompliant
    Stream.of("a", "b", "c").findFirst(); // Noncompliant
    Stream.of("a", "b", "c").findAny(); // Noncompliant
    Stream.of("a", "b", "c").toList(); // Noncompliant
  }
}
