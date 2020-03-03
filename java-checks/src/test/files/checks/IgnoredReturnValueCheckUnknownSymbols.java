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
  UnknownType unknownTypeMethod() {}
  void foo() {
    unknownTypeMethod();//Compliant type is unknown
    unresolvedMethod();//Compliant method is not resolved so type is unknown
    fluentMethod(""); //Compliant
  }
}
