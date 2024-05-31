import java.util.List;

class Log4jAssert {
  void fun1() {
    Object o = null;
    org.apache.logging.log4j.core.util.Assert.requireNonEmpty(o);
    org.apache.logging.log4j.core.util.Assert.requireNonEmpty(o, "");
    o.toString();
  }
}
