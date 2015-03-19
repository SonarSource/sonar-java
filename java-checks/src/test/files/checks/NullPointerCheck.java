@SomeAnnotation(name = value)
package javax.annotation;

@interface CheckForNull {}

@interface Nonnull {}

@interface Nullable {}

class NullPointerTest {

  public Object[] field;

  public Object[] method() {
    return null;
  }

  @Nonnull
  public Object[] notnullableField;

  @Nonnull
  public Object[] notnullableMethod() {
    return null;
  }

  @CheckForNull
  public Object[] checkForNullField;

  @CheckForNull
  public Object[] checkForNullMethod() {
    return null;
  }

  @Nullable
  public Object[] nullableField;

  @Nullable
  public Object[] nullableMethod() {
    return null;
  }

  public void testNotnullable(Object[] parameter) {
    int i;
    Object o;

    Object[] array1 = notnullableField;
    i = array1.length; // No issue
    o = array1[0]; // No issue
    array1.hashCode(); // No issue

    i = notnullableField.length; // No issue
    o = notnullableField[0]; // No issue
    notnullableField.hashCode(); // No issue

    Object[] array2 = notnullableMethod();
    i = array2.length; // No issue
    o = array2[0]; // No issue
    array2.hashCode(); // No issue

    i = notnullableMethod().length; // No issue
    o = notnullableMethod()[0]; // No issue
    notnullableMethod().hashCode(); // No issue

    parameter.hashCode();
  }

  public void testCheckNotNull(@CheckForNull Object parameter) {
    int i;
    Object o;

    Object[] array1 = checkForNullField;
    i = array1.length; // Noncompliant
    o = array1[0]; // Noncompliant
    array1.hashCode(); // Noncompliant

    i = checkForNullField.length; // False negative
    o = checkForNullField[0]; // False negative
    checkForNullField.hashCode(); // False negative

    Object[] array2 = checkForNullMethod();
    i = array2.length; // Noncompliant
    o = array2[0]; // Noncompliant
    array2.hashCode(); // Noncompliant

    i = checkForNullMethod().length; // Noncompliant
    o = checkForNullMethod()[0]; // Noncompliant
    checkForNullMethod().hashCode(); // Noncompliant
  }

  public void testNullable(@Nullable Object parameter) {
    int i;
    Object o;

    Object[] array1 = nullableField;
    if (array1.length != 0) { } // Noncompliant
    if (array1[0] != 0) { } // Noncompliant
    if (array1.hashCode() != 0) { } // Noncompliant

    i = nullableField.length; // False negative
    o = nullableField[0]; // False negative
    nullableField.hashCode(); // False negative

    Object[] array2 = nullableMethod();
    i = array2.length; // False negative
    o = array2[0]; // False negative
    array2.hashCode(); // False negative

    i = nullableMethod().length; // Noncompliant
    o = nullableMethod()[0]; // Noncompliant
    nullableMethod().hashCode(); // Noncompliant
  }

  public class A {
    @DummyAnnotation
    Object a;
    @CheckForNull
    Object b;
    @Nullable
    Object c;
  }

  public void testMemberSelect(A a1, @CheckForNull A a2, @Nullable A a3) {
    a1.hashCode(); // No issue
    a2.hashCode(); // False negative
    a3.hashCode(); // False negative

    a1.a.hashCode(); // No issue
    a1.b.hashCode(); // False negative
    a1.c.hashCode(); // False negative
  }

  public void method1(@Nullable Object[] a1, @Nullable Object... variadic) {
  }

  public void method2(@Nonnull Object[] a1, @Nonnull Object... variadic) {
  }

  public void testMethodInvocation() {
    method1(notnullableField, // No issue
      notnullableField, // No issue
      notnullableField); // No issue
    method2(notnullableField, // No issue
      notnullableField, // No issue
      notnullableField); // No issue
    method1(checkForNullField, // No issue
      checkForNullField, // No issue
      checkForNullField); // No issue
    method2(checkForNullField, // False negative
      checkForNullField, // False negative
      checkForNullField); // False negative

    method1(notnullableMethod(), // No issue
      notnullableMethod(), // No issue
      notnullableMethod()); // No issue
    method2(notnullableMethod(), // No issue
      notnullableMethod(), // No issue
      notnullableMethod()); // No issue
    method1(checkForNullMethod(), // No issue
      checkForNullMethod(), // No issue
      checkForNullMethod()); // No issue
    method2(checkForNullMethod(), // Not compliant
      checkForNullMethod(), // Not compliant
      checkForNullMethod()); // Not compliant

    method1(null, // No issue
      null, // No issue
      null); // No issue
    method2(null, // Not compliant
      null, // Not compliant
      null); // Not compliant
  }

  public void testIf(Object argument1, Object argument2) {
    argument1.hashCode(); // Compliant
    if (argument1 == null) {
      argument1.hashCode(); // Noncompliant
      argument1 = argument2;
      argument1.hashCode(); // Compliant
    } else {
      argument1.hashCode(); // Compliant
      argument1 = null;
      argument1.hashCode(); // Noncompliant
    }
    if (null != argument1) {
      argument1.hashCode(); // Compliant
      argument1 = null;
      argument1.hashCode(); // Noncompliant
    } else {
      argument1.hashCode(); // Noncompliant
      argument1 = argument2;
      argument1.hashCode(); // Compliant
    }
    argument1.hashCode(); // Compliant
  }

  public void testConditional(Object argument) {
    int result1 = argument == null ? 0 : argument.hashCode(); // Compliant
    int result2 = argument == null ? argument.hashCode() : 0; // Noncompliant
    int result3 = argument != null ? 0 : argument.hashCode(); // Noncompliant
    int result4 = argument != null ? argument.hashCode() : 0; // Compliant
  }

  public void testIfMerge1(Object argument1, Object argument2, Object argument3, Object argument4, boolean condition) {
    if (argument1 == null) {
      argument1 = new Object();
    } else {
    }
    argument1.hashCode(); // Compliant

    if (null != argument2) {
    } else {
      argument2 = new Object();
    }
    argument2.hashCode(); // Compliant

    if (argument3 == null) {
      if (condition) {
        argument3 = new Object();
      } else {
        argument3 = new Object();
      }
      argument3.hashCode(); // Compliant
    }

    if (condition) {
      argument4 = null;
    } else {
      argument4 = null;
    }
    argument4.hashCode(); // Noncompliant
  }

  public void testCondition() {
    String var1 = null;
    if (var1.equals("")) { } // Noncompliant
    String var2 = nullableMethod();
    if (var2.equals("")) { } // Noncompliant
  }

  public void testTry() {
    Object object = null;
    try {
      object = new Object();
    } catch (Exception e) {
      object.hashCode(); // Noncompliant
    } finally {
      object.hashCode(); // Noncompliant
    }
  }

  public void testLogicalAnd(String str) {
    Object object = null;
    if (object != null && object.hashCode() == 0); // Compliant
    if (object != null && object.hashCode() != 0 && object.hashCode() != 0); // Compliant
    if (object == null && object.hashCode() == 0); // Noncompliant
    if (object == null && object.hashCode() == 0 && object.hashCode() == 0); // Noncompliant
    boolean b2 = str != null && str.length() == 0; // Compliant
    boolean b1 = str == null && str.length() == 0; // Noncompliant
  }

  public void testLogicalOr(String str) {
    Object object = null;
    if (object == null || object.hashCode() == 0); // Compliant
    if (object == null || object.hashCode() != 0 || object.hashCode() != 0); // Compliant
    if (object != null || object.hashCode() == 0); // Noncompliant
    if (object != null || object.hashCode() == 0 || object.hashCode() == 0); // Noncompliant
    boolean b1 = str == null || str.length() == 0; // Compliant
    boolean b2 = str != null || str.length() == 0; // Noncompliant
  }

  public void testForLoop() {
    for(Object object = null; object != null; object.hashCode()) { // Compliant
      object.hashCode(); // Compliant
    }
  }

  public void testWhileLoop() {
    Object object = null;
    while(object != null) {
      object.hashCode(); // Compliant
    }
    object.hashCode(); // Compliant
  }

  @interface CoverageAnnotation {
  }

  @CoverageAnnotation // Coverage
  public Object coverageMethod() { // Coverage
    return new Object();
  }

  public void testCoverage(Object[] a) {
    coverageMethod().hashCode(); // Coverage
    invalidMethod(); // Coverage
    if (0) { } // Coverage
    if (0 == 0) { } // Coverage
    a[0] = null; // Coverage
    if (null == coverageMethod()) { } // Coverage
    if (a == a) { } // Coverage
    if (a == null) { } // Coverage
    if (a != null) { } // Coverage
    undefined.field; // Coverage
    a = 1 + 2; // Coverage
  }

  static int a;
  static {
    a = 0;
  }

}
