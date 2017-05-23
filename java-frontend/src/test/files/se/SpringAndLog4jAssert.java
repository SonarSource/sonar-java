import org.springframework.util.Assert;

import java.util.List;

class SpringAndLog4jAssert {
  void fun1() {
    Object o = null;
    org.apache.logging.log4j.core.util.Assert.requireNonNull(o, "");
    org.apache.logging.log4j.core.util.Assert.foo(o);
    o.toString();
  }
  void fun2() {
    Object o = null;
    Assert.notNull(o);
    Assert.foo();
    o.toString();
  }
  void fun3() {
    Object o = null;
    Assert.notNull(o, "");
    o.toString();
  }
  void fun4() {
    List<String> o = null;
    Assert.notEmpty(o);
    o.toString();
  }
  void fun4() {
    Object o = null;
    Object b = new Object();
    Assert.isNull(b);
    o.toString();
  }
  void foo() {
    Assert.state( 1 < 2, "foo");
  }
}
