package checks;


import java.time.ZoneId;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

public class SynchronizationOnStringOrBoxedCheck {
  
  private final Boolean bLock = Boolean.FALSE;
  private final Integer iLock = Integer.valueOf(0);
  private final String sLock = "LOCK";
  private final Optional<String> opLock = Optional.ofNullable(sLock);
  private final OptionalInt opIntLock = OptionalInt.of(1);
  private final OptionalLong opLongLock = OptionalLong.empty();
  private final OptionalDouble opDoubleLock = OptionalDouble.of(1.2e-5);
  private final ZoneId zoneId = ZoneId.systemDefault();

  private final Object oLock = new Object();
  
  void method1() {
    
    synchronized(bLock) {  // Noncompliant [[sc=18;ec=23]] {{Synchronize on a new "Object" instead.}}
      // ...
    }
    synchronized(iLock) {  // Noncompliant
      // ...
    }
    synchronized(sLock) {  // Noncompliant
      // ...
    }
    synchronized(opLock) {  // Noncompliant
      // ...
    }
    synchronized(opIntLock) {  // Noncompliant
      // ...
    }
    synchronized(opLongLock) {  // Noncompliant
      // ...
    }
    synchronized(opDoubleLock) {  // Noncompliant
      // ...
    }
    synchronized(zoneId) {  // Noncompliant
      // ...
    }
    synchronized(oLock) {
      // ...
    }
  }
  
}
