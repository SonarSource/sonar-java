package symbolicexecution.behaviorcache;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

class Spring5Assert {

  Supplier<String> func = () -> "message";

  void testNotNull(@Nullable Object o) {
    Assert.notNull(o, () -> "message");
    o.toString();
  }

  void testNotNullNc(@Nullable Object o) {
    o.toString(); // Noncompliant
  }

  void testNotNull2(@Nullable Object o) {
    Assert.notNull(o, func);
    o.toString();
  }

  void testNotNull2Nc(@Nullable Object o) {
    o.toString(); // Noncompliant
  }

  void testHasLength() {
    String s = null;
    Assert.hasLength(s, () -> "message");
    if (s.length() > 1) {
      System.out.println();
    }
  }

  void testHasLengthNc() {
    String s = null;
    if (s.length() > 1) { // Noncompliant
      System.out.println();
    }
  }

  void testHasText() {
    String s = null;
    Assert.hasText(s, () -> "message");
    if (s.length() > 1) {
      System.out.println();
    }
  }

  void testHasTextNc() {
    String s = null;
    if (s.length() > 1) { // Noncompliant
      System.out.println();
    }
  }

  void testNotEmpty() {
    Object[] arr = null;
    Assert.notEmpty(arr, func);
    Object first = arr[0];
  }

  void testNotEmptyNc() {
    Object[] arr = null;
    Object first = arr[0]; // Noncompliant
  }

  void testNotEmptyColl() {
    Collection<String> coll = null;
    Assert.notEmpty(coll, func);
    coll.toString();
  }

  void testNotEmptyCollNc() {
    Collection<String> coll = null;
    coll.toString(); // Noncompliant
  }

  void testNotEmptyMap() {
    Map<Integer, Integer> map = null;
    Assert.notEmpty(map, func);
    map.toString();
  }

  void testNotEmptyMapNc() {
    Map<Integer, Integer> map = null;
    map.toString(); // Noncompliant
  }

  void testIsInstanceOf() {
    Class<?> type = null;
    Assert.isInstanceOf(type, new Object(), () -> "watch out!");
    type.toString();
  }

  void testIsInstanceOfNc() {
    Class<?> type = null;
    type.toString(); // Noncompliant
  }

  void testIsAssignable() {
    Class<?> superType = null;
    Assert.isAssignable(superType, Object.class, () -> "watch out!");
    superType.toString();
  }

  void testIsAssignableNc() {
    Class<?> superType = null;
    superType.toString(); // Noncompliant
  }

  void testIsNull() {
    Object o = new Object();
    Assert.isNull(o, func);
    if (o == null) {
      System.out.println();
    }
  }

  void testIsNullNc() {
    Object o = new Object();
    if (o == null) { // Noncompliant
      System.out.println();
    }
  }

  void testIsTrue(boolean b) {
    Object o = null;
    Assert.isTrue(o != null, func);
    o.toString();
  }

  void testIsTrue2(boolean b) {
    Assert.isTrue(b, func);
    if (!b) { // Noncompliant
      System.out.println("dead code");
    }
  }

  void testIsTrueNc() {
    Object o = null;
    o.toString(); // Noncompliant
  }

  void testState() {
    Object o = null;
    Assert.state(o != null, () -> "Error");
    o.toString();
  }

  void testStateNc() {
    Object o = null;
    o.toString(); // Noncompliant
  }

}
