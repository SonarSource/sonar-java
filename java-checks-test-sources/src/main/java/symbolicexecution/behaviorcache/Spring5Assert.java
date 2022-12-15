package symbolicexecution.behaviorcache;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

class Spring5Assert {

  private static final Supplier<String> FUNC = () -> "message";

  void testNotNull(@Nullable Object o) {
    Assert.notNull(o, () -> "message");
    o.toString();
  }

  void testNotNullNc(@Nullable Object o) {
    o.toString(); // Noncompliant
  }

  void testNotNull2(@Nullable Object o) {
    Assert.notNull(o, FUNC);
    o.toString();
  }
  
  void testNotNull3(@Nullable Object o) {
    Assert.notNull(o, FUNC);
    if (o != null) { // Noncompliant
      System.out.println();
    }
  }

  void testHasLength() {
    String s = null;
    Assert.hasLength(s, FUNC);
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
    Assert.hasText(s, FUNC);
    if (s.length() > 1) {
      System.out.println();
    }
  }

  void testNotEmpty() {
    Object[] arr = null;
    Assert.notEmpty(arr, FUNC);
    Object first = arr[0];
  }

  void testNotEmptyNc() {
    Object[] arr = null;
    Object first = arr[0]; // Noncompliant
  }

  void testNotEmptyColl() {
    Collection<String> coll = null;
    Assert.notEmpty(coll, FUNC);
    coll.toString();
  }

  void testNotEmptyMap() {
    Map<Integer, Integer> map = null;
    Assert.notEmpty(map, FUNC);
    map.toString();
  }

  void testIsInstanceOf() {
    Class<?> type = null;
    Assert.isInstanceOf(type, new Object(), FUNC);
    type.toString();
  }

  void testIsAssignable() {
    Class<?> superType = null;
    Assert.isAssignable(superType, Object.class, FUNC);
    superType.toString();
  }

  void testIsNull() {
    Object o = new Object();
    Assert.isNull(o, FUNC);
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
    Assert.isTrue(o != null, FUNC);
    o.toString();
  }

  void testIsTrue2(boolean b) {
    Assert.isTrue(b, FUNC);
    if (!b) { // Noncompliant
      System.out.println("dead code");
    }
  }

  void testState() {
    Object o = null;
    Assert.state(o != null, FUNC);
    o.toString();
  }


}
