import java.util.concurrent.atomic.AtomicIntegerArray;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.URL;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.Duration;
import java.time.Period;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

class A {
  private volatile int vInts0;
  private volatile int [] vInts;  // Noncompliant [[sc=11;ec=26]] {{Use an "AtomicIntegerArray" instead.}}
  private volatile long [] vLongs;  // Noncompliant [[sc=11;ec=27]] {{Use an "AtomicLongArray" instead.}}
  private volatile Object [] vObjects;  // Noncompliant [[sc=11;ec=29]] {{Use an "AtomicReferenceArray" instead.}}
  private volatile MyObj myObj;  // Noncompliant [[sc=11;ec=25]] {{Remove the "volatile" keyword from this field.}}
  private volatile Date myDate;  // Noncompliant [[sc=11;ec=24]] {{Remove the "volatile" keyword from this field.}}
  private AtomicIntegerArray vInts2;
  private MyObj myObj2;
  // Following variable declarations are compliant: standard immutable types
  // (One can use ([^\s]+\.)([A-Z][a-z]+)(")(,)? to match names from the list defined in the check)
  private volatile Color myColor;
  private volatile Cursor myCursor;
  private volatile Font myFont;
  private volatile File myFile;
  private volatile Boolean myBoolean;
  private volatile Byte myByte;
  private volatile Character myCharacter;
  private volatile Double myDouble;
  private volatile Float myFloat;
  private volatile Integer myInteger;
  private volatile Long myLong;
  private volatile Short myShort;
  private volatile String myString;
  private volatile BigDecimal myBigDecimal;
  private volatile BigInteger myBigInteger;
  private volatile Inet4Address myInet4Address;
  private volatile Inet6Address myInet6Address;
  private volatile URL myURL;
  private volatile Clock myClock;
  private volatile DayOfWeek myDayOfWeek;
  private volatile Instant myInstant;
  private volatile LocalDate myLocalDate;
  private volatile LocalDateTime myLocalDateTime;
  private volatile LocalTime myLocalTime;
  private volatile Month myMonth;
  private volatile MonthDay myMonthDay;
  private volatile OffsetDateTime myOffsetDateTime;
  private volatile OffsetTime myOffsetTime;
  private volatile Year myYear;
  private volatile YearMonth myYearMonth;
  private volatile ZoneId myZoneId;
  private volatile ZoneOffset myZoneOffset;
  private volatile ZonedDateTime myZonedDateTime;
  private volatile Duration myDuration;
  private volatile Period myPeriod;
  private volatile Locale myLocale;
  private volatile UUID myUUID;

  void foo(){}
}
enum MyEnum {
  FOO;
  private volatile int vInts0;
  private volatile int [] vInts;  // Noncompliant [[sc=11;ec=26]] {{Use an "AtomicIntegerArray" instead.}}
  private volatile MyObj myObj;  // Noncompliant [[sc=11;ec=25]] {{Remove the "volatile" keyword from this field.}}
  private AtomicIntegerArray vInts2;
  private MyObj myObj2;

  void foo(){}
}
