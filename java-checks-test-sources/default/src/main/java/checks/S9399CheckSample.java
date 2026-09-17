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

  void staticLockWithInstanceMethodCall() {
    synchronized (STATIC_LOCK) { // Compliant - instance method call, not direct field access
      helper();
    }
  }

  private void helper() {
    count++;
  }

  void staticLockWithNonIdentifierQualifier(S9399CheckSample other) {
    synchronized (STATIC_LOCK) { // Compliant - field access on result of method call, not on this instance
      getOther().count++;
    }
  }

  private S9399CheckSample getOther() {
    return new S9399CheckSample();
  }

  void staticLockWithParenthesizedExpression() {
    synchronized ((STATIC_LOCK)) { // Noncompliant
      count++;
    }
  }

  void staticLockWithCastQualifier(Object o) {
    synchronized (STATIC_LOCK) { // Compliant - field access on cast expression, not on this instance
      ((S9399CheckSample) o).count++;
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

  void staticLockWithInnerClassInstantiation() {
    synchronized (STATIC_LOCK) { // Compliant - no instance field access
      InnerClass inner = new InnerClass();
    }
  }

  class InnerClassAccessingOuterField {
    void accessOuterField() {
      synchronized (STATIC_LOCK) { // Noncompliant
        count++;
      }
    }
  }
}

class S9399_SeparateClass {
  private static final Object LOCK = new Object();
  private int value;

  void compliantWithLocalVariable() {
    synchronized (LOCK) { // Compliant - only a local variable is used, no instance field
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

class S9399_ClassLiteralLock {
  private int count;
  private static int staticCount;

  void classLiteralLockOnInstanceField() {
    synchronized (S9399_ClassLiteralLock.class) { // Noncompliant {{Use an instance-level lock to guard instance fields.}}
//                ^^^^^^^^^^^^^^^^^^^^^^^^^^^^
      count++;
    }
  }

  void classLiteralLockOnStaticField() {
    synchronized (S9399_ClassLiteralLock.class) { // Compliant - static lock guards static field
      staticCount++;
    }
  }
}

class S9399_ExternalLockHolder {
  static final Object SHARED_LOCK = new Object();
}

class S9399_ExternalLockUser {
  private int value;

  void externalStaticLockOnInstanceField() {
    synchronized (S9399_ExternalLockHolder.SHARED_LOCK) { // Noncompliant
      value++;
    }
  }
}

class S9399_BaseClass {
  protected int baseField;
}

class S9399_SubClass extends S9399_BaseClass {
  private static final Object LOCK = new Object();

  void accessInheritedField() {
    synchronized (LOCK) { // Noncompliant
      baseField++;
    }
  }
}

class S9399_MethodCallLock {
  private int count;

  void synchronizedOnMethodCall() {
    synchronized (getObject()) { // Compliant - not a static lock
      count++;
    }
  }

  private Object getObject() {
    return new Object();
  }
}

enum S9399_EnumWithLock {
  INSTANCE;

  private static final Object LOCK = new Object();
  private int count;

  void lockedMethod() {
    synchronized (LOCK) { // Noncompliant
      count++;
    }
  }
}

class S9399_OuterWithInnerLock {
  static class Inner {
    static final Object INNER_LOCK = new Object();
  }

  private int value;

  void nestedMemberSelectLock() {
    synchronized (S9399_OuterWithInnerLock.Inner.INNER_LOCK) { // Noncompliant
      value++;
    }
  }

  void nestedMemberSelectLockOnStaticField() {
    synchronized (Inner.INNER_LOCK) { // Compliant - static lock guards static field
      staticField++;
    }
  }

  private static int staticField;
}

class S9399_DeeplyNestedClass {
  private static final Object LOCK = new Object();

  class Level1 {
    class Level2 {
      private int deepField;

      void deepNesting() {
        synchronized (LOCK) { // Noncompliant
          deepField++;
        }
      }

      void accessOuterField() {
        synchronized (LOCK) { // Noncompliant
          outerField++;
        }
      }
    }

    private int level1Field;

    void level1Access() {
      synchronized (LOCK) { // Noncompliant
        level1Field++;
      }
    }
  }

  private int outerField;
}

interface S9399_InterfaceWithDefault {
  Object LOCK = new Object();

  default void defaultMethod() {
    synchronized (LOCK) { // Compliant - inside interface, no instance fields
      System.out.println("locked");
    }
  }
}
