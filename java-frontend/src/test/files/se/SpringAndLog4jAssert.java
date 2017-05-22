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
}
