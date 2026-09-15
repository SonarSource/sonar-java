package checks;

class S9399CheckSample {

  private static final Object STATIC_LOCK = new Object();
  private static Object staticLock = new Object();
  private final Object instanceLock = new Object();
  private int count;
  private String name;
  private static int staticCount;

  void basicStaticLockOnInstanceField() {
    synchronized (STATIC_LOCK) { // Noncompliant {{Use an instance-level lock to guard instance fields.}}
//                ^^^^^^^^^^^
      count++;
    }
  }

  void staticLockWithThisQualifiedAccess() {
    synchronized (STATIC_LOCK) { // Noncompliant
      this.name = "value";
    }
  }

  void staticLockGuardingMultipleInstanceFields() {
    synchronized (STATIC_LOCK) { // Noncompliant
      int x = count + name.length();
    }
  }

  void nonFinalStaticLock() {
    synchronized (staticLock) { // Noncompliant
      count = 42;
    }
  }

  void classQualifiedStaticLock() {
    synchronized (S9399CheckSample.STATIC_LOCK) { // Noncompliant
      count++;
    }
  }

  void staticLockOnStaticField() {
    synchronized (STATIC_LOCK) { // Compliant - static lock guards static field
      staticCount++;
    }
  }

  void instanceLockOnInstanceField() {
    synchronized (instanceLock) { // Compliant - instance lock guards instance field
      count++;
    }
  }

  void synchronizedOnThis() {
    synchronized (this) { // Compliant - this is an instance lock
      count++;
    }
  }

  void staticLockWithLocalVariablesOnly() {
    synchronized (STATIC_LOCK) { // Compliant - no instance field access
      int local = 0;
      local++;
    }
  }

  static void staticMethodWithStaticLock() {
    synchronized (STATIC_LOCK) { // Compliant - static method, no instance fields accessible
      staticCount++;
    }
  }

  void staticLockAccessingOtherObjectField(S9399CheckSample other) {
    synchronized (STATIC_LOCK) { // Compliant - accessing field on a different object
      other.count = 10;
    }
  }

  void staticLockMixedStaticAndInstance() {
    synchronized (STATIC_LOCK) { // Noncompliant
      staticCount++;
      count++;
    }
  }

  void staticLockWithMethodCallOnly() {
    synchronized (STATIC_LOCK) { // Compliant - method call, not direct field access
      toString();
    }
  }

  void staticLockWithFieldReadInCondition() {
    synchronized (STATIC_LOCK) { // Noncompliant
      if (count > 0) {
        staticCount++;
      }
    }
  }

  class InnerClass {
    private int innerField;

    void innerStaticLockOnInnerField() {
      synchronized (STATIC_LOCK) { // Noncompliant
        innerField++;
      }
    }
  }
}

class S9399_SeparateClass {
  private static final Object LOCK = new Object();
  private int value;

  void compliantWithLocalVariable() {
    synchronized (LOCK) { // Compliant - parameter used, not instance field
      int temp = 5;
      System.out.println(temp);
    }
  }

  void noncompliantBasic() {
    synchronized (LOCK) { // Noncompliant
      value = 10;
    }
  }
}
