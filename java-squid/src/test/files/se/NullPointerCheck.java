@SomeAnnotation(name = value)
package javax.annotation;

import java.lang.Object;

@interface CheckForNull {}

@interface Nonnull {}

@interface Nullable {}

class NullPointerTest {

  // tests constructs that can generate an issue. namely, syntax constructs that performs a potential null dereference.
  public void testIssues() {
    //Not tested right away : those are invalid construction anyway.
//    null[0]; //  {{null is dereferenced}}
//    null.field; //  {{null is dereferenced}}
//    null.method(); //  {{null is dereferenced}}
  }

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

    i = notnullableField.length; // No issue

    Object[] array2 = notnullableMethod();
    i = array2.length; // No issue

    i = notnullableMethod().length; // No issue

    parameter.hashCode();
  }

  public void testCheckNotNull(@CheckForNull Object parameter) {
    int i;
    Object o;

    Object[] array1 = checkForNullField;
    i = array1.length; // False negative

    i = checkForNullField.length; // False negative, instance and static fields are not checked

    Object[] array2 = checkForNullMethod();
    i = array2.length; // Noncompliant 2 {{NullPointerException might be thrown as 'array2' is nullable here}}
  }
  public void testCheckNotNull(@CheckForNull Object parameter) {
    int i;
    i = checkForNullMethod().length; // Noncompliant 2 {{NullPointerException might be thrown as 'checkForNullMethod' is nullable here}}
  }

  public void testNullable(@Nullable Object parameter) {
    int i;
    Object o;

    Object[] array1 = nullableField;
    if (array1.length != 0) { } // False negative

    i = nullableField.length; // False negative, instance and static fields are not checked

    Object[] array2 = nullableMethod();
    i = array2.length; // Compliant

    i = nullableMethod().length; // Compliant
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
    a2.hashCode(); // Noncompliant {{NullPointerException might be thrown as 'a2' is nullable here}}
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
    method2(checkForNullField, // False negative, instance and static fields are not checked
      checkForNullField, // False negative, instance and static fields are not checked
      checkForNullField); // False negative, instance and static fields are not checked

    method1(notnullableMethod(), // No issue
      notnullableMethod(), // No issue
      notnullableMethod()); // No issue
    method2(notnullableMethod(), // No issue
      notnullableMethod(), // No issue
      notnullableMethod()); // No issue
    method1(checkForNullMethod(), // No issue
      checkForNullMethod(), // No issue
      checkForNullMethod()); // No issue
//    method2(checkForNullMethod(), // false negative Noncompliant {{'checkForNullMethod' is nullable here and method 'method2' does not accept nullable argument}}
//      checkForNullMethod(), // false negative Noncompliant {{'checkForNullMethod' is nullable here and method 'method2' does not accept nullable argument}}
//      checkForNullMethod()); // false negative Noncompliant {{'checkForNullMethod' is nullable here and method 'method2' does not accept nullable argument}}

    method1(null, // No issue
      null, // No issue
      null); // No issue
//    method2(null, // false negative  Noncompliant {{method 'method2' does not accept nullable argument}}
//      null, // false negative Noncompliant {{method 'method2' does not accept nullable argument}}
//      null); // false negative Noncompliant {{method 'method2' does not accept nullable argument}}
  }
  public void testIf(Object argument1) {
    argument1.hashCode(); // Compliant
  }
  public void testIf(Object argument1, Object argument2, Object argument3) {
    if (argument1 == null) {
      argument1.hashCode(); // Noncompliant {{NullPointerException might be thrown as 'argument1' is nullable here}}
      argument1 = argument3;
      argument1.hashCode(); // Compliant
    } else {
      argument1.hashCode(); // Compliant
      argument1 = null;
      argument1.hashCode(); // Noncompliant {{NullPointerException might be thrown as 'argument1' is nullable here}}
    }
    argument1.hashCode(); // Compliant
    argument2.hashCode(); // Compliant
  }
  public void testIf2(Object argument1, Object argument2, Object argument3) {
    if (null != argument2) {
      argument2.hashCode(); // Compliant
      argument2 = null;
      argument2.hashCode(); // Noncompliant {{NullPointerException might be thrown as 'argument2' is nullable here}}
    } else {
      argument2.hashCode(); // Noncompliant {{NullPointerException might be thrown as 'argument2' is nullable here}}
      argument2 = argument3;
      argument2.hashCode(); // Compliant
    }
    argument2.hashCode(); // Compliant
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

    if (condition) {
      argument4 = null;
    } else {
      argument4 = null;
    }
    argument4.hashCode(); // Noncompliant 8 {{NullPointerException might be thrown as 'argument4' is nullable here}}
  }

  public void testIfMerge2(Object argument1, Object argument2, Object argument3, Object argument4, boolean condition) {
    if (argument3 == null) {
      if (condition) {
        argument3 = new Object();
      } else {
        argument3 = new Object();
      }
      argument3.hashCode(); // Compliant
    }
    argument3.hashCode(); // Compliant
  }

  public void testConditional(Object argument1, Object argument2, Object argument3, Object argument4) {
    int result1 = argument1 == null ? 0 : argument1.hashCode(); // Compliant
    argument1.hashCode(); // Noncompliant
    int result2 = argument2 == null ? argument2.hashCode() : 0; // Noncompliant {{NullPointerException might be thrown as 'argument2' is nullable here}}
    argument2.hashCode(); // Compliant
    int result3 = argument3 != null ? 0 : argument3.hashCode(); // Noncompliant {{NullPointerException might be thrown as 'argument3' is nullable here}}
    argument3.hashCode(); // Compliant
    int result4 = argument4 != null ? argument4.hashCode() : 0; // Compliant
    argument4.hashCode(); // Noncompliant
  }

  public void testCondition() {
    String var1 = null;
    if (var1.equals("")) { } // Noncompliant {{NullPointerException might be thrown as 'var1' is nullable here}}
    String var2 = nullableMethod();
    if (var2.equals("")) { } // Compliant
  }

  public void testTry() {
    Object object = null;
    try {
      object = new Object();
    } catch (Exception e) {
      object.hashCode(); // false negative : not explored in CFG
    } finally {
      object.hashCode();
    }
  }

  public void testLogicalAnd(String str) {
    if (object != null && object.hashCode() == 0) ; // Compliant
    if (object != null && object.hashCode() != 0 && object.hashCode() != 0) ; // Compliant
    if (object == null && object.hashCode() == 0) ; // Noncompliant {{NullPointerException might be thrown as 'object' is nullable here}}
  }
  public void testLogicalAnd2(String str) {
    if (object == null && object.hashCode() == 0 && object.hashCode() == 0); // Noncompliant {{NullPointerException might be thrown as 'object' is nullable here}}
  }
  public void testLogicalAnd3(String str) {
    boolean b2 = str != null && str.length() == 0; // Compliant
    boolean b1 = str == null && str.length() == 0; // Noncompliant {{NullPointerException might be thrown as 'str' is nullable here}}
  }

  public void testLogicalOr(String str) {
    if (object == null || object.hashCode() == 0) ; // Compliant
    if (object == null || object.hashCode() != 0 || object.hashCode() != 0) ; // Compliant
    if (object != null || object.hashCode() == 0) ; // Noncompliant {{NullPointerException might be thrown as 'object' is nullable here}}
  }
  public void testLogicalOr2(String str) {
    if (object != null || object.hashCode() == 0 || object.hashCode() == 0) ; // Noncompliant {{NullPointerException might be thrown as 'object' is nullable here}}
    boolean b1 = str == null || str.length() == 0; // Compliant
  }
  public void testLogicalOr3(String str) {
    boolean b2 = str != null || str.length() == 0; // Noncompliant {{NullPointerException might be thrown as 'str' is nullable here}}
  }

  public void testDoWhileLoop(boolean condition) {
    Object object1 = null, object2 = null, object3 = null;
    do {
      object1.hashCode(); // Noncompliant {{NullPointerException might be thrown as 'object1' is nullable here}}
      if (condition) {
        object2 = new Object();
      }
      object1 = null;
      object3 = new Object();
    } while (object1.hashCode()); //issue already raised
  }
  public void testDoWhileLoop2(boolean condition) {
    Object object1 = new Object(), object2 = null, object3 = null;
    do {
      object1.hashCode();
      if (condition) {
        object2 = new Object();
      }
      object1 = null;
      object3 = new Object();
    } while (object1.hashCode() < 0); // Noncompliant 2 {{NullPointerException might be thrown as 'object1' is nullable here}}
    object1.hashCode(); // issue already raised
    object2.hashCode(); // Compliant
    object3.hashCode(); // Compliant
  }

  public void testForLoop() {
    Object object = null;
    for (; object.hashCode() != 0; object.hashCode()) { // Noncompliant {{NullPointerException might be thrown as 'object' is nullable here}}
      object.hashCode();
      object = null;
    }
   }
  public void testForLoop2() {
    for(Object object1 = null, object2 = null; true; object2.hashCode()) {
      object1.hashCode(); // Noncompliant {{NullPointerException might be thrown as 'object1' is nullable here}}
    }
  }

  public void testForEachLoop() {
    Object value;
    Set<Object> set = null;
    Entry head = null;
    for(Object entry : set.values()) { // enhanced for loop not handled
      head.hashCode(); // enhanced for loop not handled
      value = null;
      value.hashCode(); // enhanced for loop not handled
    }
    head.hashCode(); // Noncompliant {{NullPointerException might be thrown as 'head' is nullable here}}
    value.hashCode(); // False negative
  }

  public void testWhileLoop() {
    Object object1 = null, object2 = new Object();
    while(object1.hashCode() > 0) { // Noncompliant {{NullPointerException might be thrown as 'object1' is nullable here}}
      object2.hashCode(); // Compliant, issue already raised
     }
    object1.hashCode(); // Compliant, issue already raised
    object2.hashCode(); // Compliant
  }
  public void testWhileLoop() {
    Object object1 = new Object(), object2 = null, object3 = null;
    while(object1.hashCode() > 0) { // compliant
      object2.hashCode(); // Noncompliant
    }
    object1.hashCode(); // Compliant, issue already raised
    //(if condition of while is false, we might end up here with object2 null)
    object2.hashCode(); // Noncompliant
  }



  public void testHoistedLoop(boolean condition) {
    Object a = null;
    while(condition) {
      a.hashCode(); // Noncompliant
      a = null;
    }
    a.hashCode(); // Noncompliant
  }

  public void testInstanceField() {
    nullableField = null;
    nullableField.hashCode(); // Noncompliant
  }

  public void testSwitch() {
    String str1 = null, str2 = "", str3 = "";
    switch(str1) { // Noncompliant {{NullPointerException might be thrown as 'str1' is nullable here}}
    case "ONE":
      str2.length();
    }
    str3.length();
  }
  public void testSwitch2() {
    String str1 = "", str2 = null, str3 = "";
    switch(str1) {
    case "ONE":
      str2.length(); // Noncompliant {{NullPointerException might be thrown as 'str2' is nullable here}}
    }
    str3.length();
  }
  public void testSwitch3() {
    String str1 = "", str2 = "", str3 = null;
    switch(str1) {
      case "ONE":
        str2.length();
    }
    str3.length(); // Noncompliant {{NullPointerException might be thrown as 'str3' is nullable here}}
  }

  public void testMergeOnParameter(@CheckForNull Object o) {
    if(o != null) {
      return;
    }
    o.toString(); // Noncompliant
  }

  public void testMergeOnParameter(@Nullable Object o) {
    if(o == null) {
      return;
    }
    o.hashCode(); // Compliant, constraint is lost
    Object a = o;
    a.hashCode(); // Compliant
  }

  public void testAssignNullableMethod() {
    Object object;
    object = nullableMethod();
    if(object.hashCode()) { } // Compliant
    object = null;
    if(object.hashCode()) { } // Noncompliant {{NullPointerException might be thrown as 'object' is nullable here}}
  }

  public void testComplexLoop(@Nullable Object nullableObject) {
    Object object1 = null, object11 = null, object12 = null;
    for (int i = 0; object11 == null; i += 1) {
      object11.hashCode(); // Noncompliant {{NullPointerException might be thrown as 'object11' is nullable here}}
      object12.hashCode();
      nullableObject.hashCode();
      if (i == 1) {
        object1.hashCode(); // Compliant
      } else if (i == 0) {
        object1 = new Object();
      }
      object11 = null;
    }
    object1.hashCode(); // Compliant not executed because loop is always executed at least once and raising NPE
  }
  public void testComplexLoop2(@Nullable Object nullableObject) {
    Object object2 = null, object21 = null, object22 = null;
    int i = 0;
    while(object21 == null) {
      object21.hashCode(); // Noncompliant {{NullPointerException might be thrown as 'object21' is nullable here}}
      object22.hashCode(); // no issue, as npe is thrown on previous line
      nullableObject.hashCode();
      if(i == 1) {
        object2.hashCode(); // Compliant
      } else if(i == 0) {
        object2 = new Object();
      }
      object21 = null;
    }
    //(false positive ?)
    object2.hashCode(); // Noncompliant

    Object object3 = null;
    int i = 0;
    do {
      if(i == 1) {
        object3.hashCode(); // False negative
      } else if(i == 0) {
        object3 = new Object();
      }
    } while (condition);
    object3.hashCode(); // False negative
  }

  void testComplexSwitch(String str) {
    Object object1 = null, object2 = null, object3 = null, object4 = new Object();
    switch(str) {
    case "ONE":
      object1 = new Object();
      break;
    case "TWO":
      object1.hashCode(); // Noncompliant 2
      break;
    case "THREE":
      object2 = new Object();
    case "FOUR":
      object2.hashCode(); // Noncompliant
      break;
    case "FIVE":
      object3.hashCode(); // Noncompliant 2 {{NullPointerException might be thrown as 'object3' is nullable here}}
      object4 = null;
    case "SIX":
      object4.hashCode(); // False negative
    }
  }

  public static class LinkedListEntry {
    @Nullable
    LinkedList parent() {
      return null;
    }
  }

  public void testAssignSelfMember() {
    LinkedListEntry entry2;
    entry2 = entry2.parent(); // Noncompliant
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
