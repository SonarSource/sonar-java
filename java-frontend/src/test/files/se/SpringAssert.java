import java.util.Collection;
import java.util.Map;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.List;

class SpringAssert {

  void test_hasLength() {
    String s = null;
    Assert.hasLength(s);
    s.toString();
  }

  void test_hasText() {
    String s = null;
    Assert.hasText(s);
    s.toString();
  }

  void test_isAssignable() {
    Class<?> superType = null;
    Assert.isAssignable(superType, Object.class);
    superType.toString();
    Class<?> subtype = null;
    Assert.isAssignable(Object.class, subtype);
    subtype.toString();
  }

  void test_isInstanceOf() {
    Class<?> type = null;
    Assert.isInstanceOf(type, new Object());
    type.toString();
  }

  void test_isNull() {
    Object o = new Object();
    Assert.isNull(o);
    if (o == null) {
    }
  }

  void test_isTrue() {
    Object o = null;
    Assert.isTrue(o != null);
    o.toString();
  }

  void test_notEmpty1() {
    Collection<?> c = null;
    Assert.notEmpty(c);
    c.toString();
  }

  void test_notEmpty2() {
    Object[] arr = null;
    Assert.notEmpty(arr);
    arr.toString();
  }

  void test_notEmpty3() {
    Map<?, ?> m = null;
    Assert.notEmpty(m);
    m.toString();
  }

  void test_notNull() {
    Object o = null;
    Assert.notNull(o);
    o.toString();
  }

  void test_state() {
    Object o = null;
    Assert.state(o != null);
    o.toString();
  }

  void test_objectutils_isEmpty() {
    Object o = null;
    if (!ObjectUtils.isEmpty(o)) {
      o.toString();
    }
  }

  void test_stringutils_hasText() {
    String s = null;
    if (StringUtils.hasText(s)) {
      s.toString();
    }
  }

  void test_stringutils_hasLength() {
    String s = null;
    if (StringUtils.hasLength(s)) {
      s.toString();
    }
  }
}
