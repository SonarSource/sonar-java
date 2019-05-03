@SomeAnnotation(name = value)
package javax.annotation;

import java.text.ParseException;
import java.util.List;

@interface CheckForNull {}

@interface Nonnull {}

@interface Nullable {}

class NullPointerTest {

  // tests constructs that can generate an issue. namely, syntax constructs that performs a potential null dereference.
  public void testIssues() {
    //Not tested right away : those are invalid construction anyway.
//    null[0]; //  {{null is dereferenced.}}
//    null.field; //  {{null is dereferenced.}}
//    null.method(); //  {{null is dereferenced.}}
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
    return nullableField;
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

    Object[] array2 = checkForNullMethod(); // flow@array {{'checkForNullMethod()' can return null.}} flow@array {{Implies 'array2' can be null.}}
    i = array2.length; // Noncompliant [[flows=array]] {{A "NullPointerException" could be thrown; "array2" is nullable here.}} flow@array
  }
  public void testCheckNotNull(@CheckForNull Object parameter) {
    int i;
    i = checkForNullMethod().length; // Noncompliant {{A "NullPointerException" could be thrown; "checkForNullMethod()" can return null.}}
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
    a2.hashCode(); // Noncompliant {{A "NullPointerException" could be thrown; "a2" is nullable here.}}
    a3.hashCode(); // Noncompliant {{A "NullPointerException" could be thrown; "a3" is nullable here.}}

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
      argument1.hashCode(); // Noncompliant {{A "NullPointerException" could be thrown; "argument1" is nullable here.}}
      argument1 = argument3;
      argument1.hashCode(); // Compliant
    } else {
      argument1.hashCode(); // Compliant
      argument1 = null;
      argument1.hashCode(); // Noncompliant {{A "NullPointerException" could be thrown; "argument1" is nullable here.}}
    }
    argument1.hashCode(); // Compliant
    argument2.hashCode(); // Compliant
  }
  public void testIf2(Object argument1, Object argument2, Object argument3) {
    if (null != argument2) {
      argument2.hashCode(); // Compliant
      argument2 = null;
      argument2.hashCode(); // Noncompliant {{A "NullPointerException" could be thrown; "argument2" is nullable here.}}
    } else {
      argument2.hashCode(); // Noncompliant {{A "NullPointerException" could be thrown; "argument2" is nullable here.}}
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
    argument4.hashCode(); // Noncompliant {{A "NullPointerException" could be thrown; "argument4" is nullable here.}}
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
    int result2 = argument2 == null ? argument2.hashCode() : 0; // Noncompliant {{A "NullPointerException" could be thrown; "argument2" is nullable here.}}
    argument2.hashCode(); // Compliant
    int result3 = argument3 != null ? 0 : argument3.hashCode(); // Noncompliant {{A "NullPointerException" could be thrown; "argument3" is nullable here.}}
    argument3.hashCode(); // Compliant
    int result4 = argument4 != null ? argument4.hashCode() : 0; // Compliant
    argument4.hashCode(); // Noncompliant
  }

  public void testCondition() {
    String var1 = null;
    if (var1.equals("")) { } // Noncompliant {{A "NullPointerException" could be thrown; "var1" is nullable here.}}
    String var2 = nullableMethod();
    if (var2.equals("")) { } // Compliant
  }

  public void testTry() {
    Object object = null;
    try {
      object = new Object();
    } catch (Exception e) {
      object.hashCode(); // Noncompliant
    } finally {
      object.hashCode();// Noncompliant
    }
    Object object2;
    try{
      object2 = potentiallyRaiseException();
    } finally {
      System.out.println("foo");
    }
    object2.toString(); // not accessible with null value

  }

  public void testLogicalAnd(String str, Object object) {
    if (object != null && object.hashCode() == 0) ; // Compliant
    if (object != null && object.hashCode() != 0 && object.hashCode() != 0) ; // Compliant
    if (object == null && object.hashCode() == 0) ; // Noncompliant {{A "NullPointerException" could be thrown; "object" is nullable here.}}
  }
  public void testLogicalAnd2(String str, Object object) {
    if (object == null && object.hashCode() == 0 && object.hashCode() == 0); // Noncompliant {{A "NullPointerException" could be thrown; "object" is nullable here.}}
  }
  public void testLogicalAnd3(String str) {
    boolean b2 = str != null && str.length() == 0; // Compliant
    boolean b1 = str == null && str.length() == 0; // Noncompliant {{A "NullPointerException" could be thrown; "str" is nullable here.}}
  }

  public void testLogicalOr(String str, Object object) {
    if (object == null || object.hashCode() == 0) ; // Compliant
    if (object == null || object.hashCode() != 0 || object.hashCode() != 0) ; // Compliant
    if (object != null || object.hashCode() == 0) ; // Noncompliant {{A "NullPointerException" could be thrown; "object" is nullable here.}}
  }
  public void testLogicalOr2(String str, Object object) {
    if (object != null || object.hashCode() == 0 || object.hashCode() == 0) ; // Noncompliant {{A "NullPointerException" could be thrown; "object" is nullable here.}}
    boolean b1 = str == null || str.length() == 0; // Compliant
  }
  public void testLogicalOr3(String str) {
    boolean b2 = str != null || str.length() == 0; // Noncompliant {{A "NullPointerException" could be thrown; "str" is nullable here.}}
  }

  public void testDoWhileLoop(boolean condition) {
    Object object1 = null, object2 = null, object3 = null;
    do {
      object1.hashCode(); // Noncompliant {{A "NullPointerException" could be thrown; "object1" is nullable here.}}
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
    } while (object1.hashCode() < 0); // Noncompliant {{A "NullPointerException" could be thrown; "object1" is nullable here.}}
    object1.hashCode(); // issue already raised
    object2.hashCode(); // Compliant
    object3.hashCode(); // Compliant
  }

  public void testForLoop() {
    Object object = null;
    for (; object.hashCode() != 0; object.hashCode()) { // Noncompliant {{A "NullPointerException" could be thrown; "object" is nullable here.}}
      object.hashCode();
      object = null;
    }
  }
  public void testForLoop2() {
    for(Object object1 = null, object2 = null; true; object2.hashCode()) {
      object1.hashCode(); // Noncompliant {{A "NullPointerException" could be thrown; "object1" is nullable here.}}
    }
  }

  public void testForEachLoop() {
    Object value;
    Set<Object> set = null;
    Entry head = null;
    for(Object entry : set.values()) { // Noncompliant {{A "NullPointerException" could be thrown; "set" is nullable here.}}
      //all potential npe after this one are not raised as symbolic execution is cut.
      head.hashCode();
      value = null;
      value.hashCode();
    }
    head.hashCode();
    value.hashCode();
  }

  public void testForEachLoopGood() {
    List<String> foos = collectFoos();
    for (String foo : foos) {
      foo.toString();
    }
  }

  public void testPrimitiveForEachLoopGood() {
    boolean[] foos = collectFoos();
    for (boolean foo : foos) {
      if (foo) {
        println("true");
      }
    }
  }

  public static int testPrimitiveForEachLoopGuava(boolean... values) {
    int count = 0;
    for (boolean value : values) {
      if (value) {
        count++;
      }
    }
    return count;
  }

  private  Object a = null;

  public void newInstance() {
    if (a == null) {
      testPrimitiveForEachLoopGood();
    }
    a.toString();
  }
  protected Constructor constructor = null;
  public void newInstanceBis() throws IllegalAccessException, InstantiationException {
    if (constructor == null) {
      this.setDynaBeanClass();
    }
    return ((DynaBean) constructor.newInstance(constructorValues));
  }

  public void testWhileLoop() {
    Object object1 = null, object2 = new Object();
    while(object1.hashCode() > 0) { // Noncompliant {{A "NullPointerException" could be thrown; "object1" is nullable here.}}
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
    switch(str1) { // Noncompliant {{A "NullPointerException" could be thrown; "str1" is nullable here.}}
      case "ONE":
        str2.length();
    }
    str3.length();
  }
  public void testSwitch2() {
    String str1 = "", str2 = null, str3 = "";
    switch(str1) {
      case "ONE":
        str2.length(); // Noncompliant {{A "NullPointerException" could be thrown; "str2" is nullable here.}}
    }
    str3.length();
  }
  public void testSwitch3() {
    String str1 = "", str2 = "", str3 = null;
    switch(str1) {
      case "ONE":
        str2.length();
    }
    str3.length(); // Noncompliant {{A "NullPointerException" could be thrown; "str3" is nullable here.}}
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
    if(object.hashCode()) { } // Noncompliant {{A "NullPointerException" could be thrown; "object" is nullable here.}}
  }

  public void testComplexLoop(@Nullable Object nullableObject) {
    Object object1 = null, object11 = null, object12 = null;
    for (int i = 0; object11 == null; i += 1) {
      object11.hashCode(); // Noncompliant {{A "NullPointerException" could be thrown; "object11" is nullable here.}}
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
      object21.hashCode(); // Noncompliant {{A "NullPointerException" could be thrown; "object21" is nullable here.}}
      object22.hashCode(); // no issue, as npe is thrown on previous line
      nullableObject.hashCode();
      if(i == 1) {
        object2.hashCode(); // Compliant
      } else if(i == 0) {
        object2 = new Object();
      }
      object21 = null;
    }
    object2.hashCode();

    Object object3 = null;
    int j = 0;
    do {
      if(j == 1) {
        object3.hashCode(); // False negative
      } else if(j == 0) {
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
        object1.hashCode(); // Noncompliant
        break;
      case "THREE":
        object2 = new Object();
      case "FOUR":
        object2.hashCode(); // Noncompliant
        break;
      case "FIVE":
        object3.hashCode(); // Noncompliant {{A "NullPointerException" could be thrown; "object3" is nullable here.}}
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
    LinkedListEntry entry2 = null;
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
    if (a == null) { } // Coverage
    if (a != null) { } // Coverage
    a[0] = undefined.field; // Coverage
    a = 1 + 2; // Coverage
  }

  static int a1;
  static {
    a1 = 0;
  }

  public void hasNullableParameters(String a, @Nullable String b) {
    a.length();
    b.length(); // Noncompliant {{A "NullPointerException" could be thrown; "b" is nullable here.}}
  }

  public void test(@CheckForNull A a, @CheckForNull A b) {
    a.hashCode(); // Noncompliant
    b.hashCode(); // Noncompliant
  }

  public boolean indirectNull(CharSequence s1, CharSequence s2) {
    int length = s1.length();
    if (s1 == s2) {
      return true;
    }
    if (length != s2.length()) {
      return false;
    }
    return true;
  }

  void foo(Object qix, @Nullable Object bar) {
    foo(bar, NullPointerTest.class);
  }


  public void maybePropagateCancellation(@Nullable Object related) {
    if (related != null & isCancelled()) {
      related.toString();
    }
  }

    void while_loop() {
    Object currentParent = new Object();
    while (currentParent != null) {
      printState();
      currentParent.toString();
      currentParent = null;
    }
  }

  @Override
  public boolean equals(Object obj) {
    return getValue() == ((MyClass) obj).getValue(); // Noncompliant {{A "NullPointerException" could be thrown; "obj" is nullable here.}}
  }

  public boolean equals(MyClass obj) {
    return getValue() == ((MyClass) obj).getValue(); // Compliant : not the equals method
  }

  private void equalsToCheckForNull(Integer a) {
    Objeect b = checkForNullMethod();
    if (a.equals(b)) {
      System.out.println("Found!");
    }
    a.toString(); // Compliant: a cannot be null hereafter
    this.checkForNullMethod().toString(); // Noncompliant {{A "NullPointerException" could be thrown; "checkForNullMethod()" can return null.}}
  }

  @Nonnull
  public static String getNonNullString() {
    return "Rachmaninov";
  }

  public static void useNonNullString() {
    String nonNullString = getNonNullString();
    if (nonNullString != null) {
      System.out.println(nonNullString);
    }
    int n = nonNullString.length();  // Compliant!
  }

  Object parseNullException(String object) throws Exception {
    Object result = null;
    Exception ex = null;
    try {
      result = parseObject(object);
    } catch (ParseException e) {
      ex = e;
    }
    if (result == null) {
      throw ex; // Noncompliant {{A "NullPointerException" could be thrown; "ex" is nullable here.}}
    }
    return result;
  }

  private void bar(@Nullable Object v) {
    NullPointerTest.qix(v, v != null ? "A" : null); // Compliant
    NullPointerTest.qix(v, (v != null && "B".equals(v.toString())) ? "B" : null); // Compliant
    NullPointerTest.qix(v, (v == null || "B".equals(v.toString())) ? "B" : null); // Compliant
  }

  static void qix(Object o1, Object o2) {  }

  private static boolean testSomething1(List<String> p1, @Nullable String p2) {
    return p1.isEmpty() && p2 != null && p2.length() > 0 && p2.charAt(0) == '?';
  }

  private static boolean testSomething2(List<String> p1, @Nullable String p2) {
    return p1.isEmpty() || p2 == null || p2.length() > 0 || p2.charAt(0) == '?';
  }

  public boolean checkThatStuff(@Nullable Object c) {
    return checkSomething("HELLO") && c instanceof MyClass && ((MyClass) c).isLike("hello");
  }

  private static boolean checkSomething(String s) {
    return s.isEmpty();
  }
}

class MyClass {
  private MyClass(ThreadGroup tg) {
    java.security.AccessController.doPrivileged(
      (java.security.PrivilegedAction<Void>) () -> {
        final Thread hook = new Thread(
          tg,
          new Runnable() {
            @Override
            public void run() {
              unknownMethod(); // owner of the corresponding unknown symbol is PackageSymbol, with type null
            }
          },
          "threadName");
        return null;
      });
  }

  boolean isLike(String s) {
    return s.isEmpty();
  }
}
class RaisedExceptionCannotBeNull {
  void foo() {
    try {
      Thread.sleep(0);
    } catch (Exception ex) {
      for (Throwable cause = ex; cause != null; cause = cause.getCause()) { // assigned to cause then tested agains null...
        // do something
      }
      ex.getCause(); // FP on squid:S2259 as it might have been null or not because of the for loop condition
    }
  }
}

class FooBar {
  private static void foo(Object o, boolean b) {
    FooBar.foo(null, true); // Compliant
  }

  private static void bar(Object o, byte[] b) {
    FooBar.bar(null, new byte[10]); // Compliant
  }
}

class SimpleAssignments {
  Object myField;

  void foo() {
    this.myField = null;
    myField.toString(); // Noncompliant
  }

  void bar() {
    myField = null;
    this.myField.toString(); // Noncompliant
  }

  void gul() {
    this.myField = null;
    this.myField.toString(); // Noncompliant
  }

  void qix() {
    myField = null;
    myField.toString(); // Noncompliant
  }
}

class ResetFieldWhenThisUsedAsParameter {
  static class A {
    Object value;
    B b;

    void foo() {
      this.value = null;
      b.bar(this);
      value.toString(); // Compliant
    }
  }

  static class B {
    void bar(A a) {
      // do something to 'a'
    }
  }
}

class finalFieldSetToNull {
  final Object field = (null); // flow@fieldNull {{Implies 'field' is null.}}

  void foo() {
    // Noncompliant@+1 [[flows=fieldNull]] {{A "NullPointerException" could be thrown; "field" is nullable here.}}
    this.field.toString(); // flow@fieldNull {{'field' is dereferenced.}}
  }
}

class OptionalOrElseNull {
  void fun(java.util.Optional<String> opt) {
     String s = opt.orElse(null);
     s.toString(); // Noncompliant
  }
}

abstract class NonNullAnnotationOnForEach {
  void foo(List<Object> items) {
    for (@Nonnull Object item : items) {
      Object o = qix(bar(item)); // 'bar' can only be called with non-null constraint
      if ("foo".equals(o.toString())) {
        item.toString(); // Compliant
      }
    }
  }

  private static String bar(Object item) {
    if (item != null) { // defensive programming
      return item.toString();
    }
    return null;
  }

  abstract Object qix(String s);
}

class SpringAnnotations {
  void foo(@org.springframework.lang.Nullable Object o) {
    o.toString(); // Noncompliant
  }

  void bar(@org.springframework.lang.NonNull Object o) {
    if (o == null) {
      o.toString(); // Compliant - unreachable
    }
  }
}

class SynchronizedStatementTest {
  void test(Object obj) {
    if (obj == null) {
      synchronized (obj) { // Noncompliant {{A "NullPointerException" could be thrown; "obj" is nullable here.}}
      }
    }
  }
}

class OptionalOfNullableOrElseGet {
  void test() {
    java.util.function.Supplier<String> supplier = null;
    java.util.Optional<String> value = java.util.Optional.empty();
    java.util.function.Supplier<String> supplier2 = () -> "someString";
    System.out.println(value.orElseGet(supplier2)); // compliant
    System.out.println(value.orElseGet(supplier)); // Noncompliant NPE will be raised at runtime
  }
  void test2() {
    java.util.Optional<String> value = java.util.Optional.empty();
    System.out.println(value.orElseGet(null)); // Noncompliant NPE will be raised at runtime
  }
}
