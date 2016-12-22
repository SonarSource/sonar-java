import java.util.Map;
class ISuiteListener {
  private final Map<Class<? extends ISuiteListener>, ISuiteListener> m_suiteListeners;

  private static <E> void maybeAddListener(Map<Class<? extends E>, E> map, Class<? extends E> type, E value) {
    // do something
  }

  void foo(ISuiteListener suite) {
    maybeAddListener(m_suiteListeners, suite.getClass(),  suite);
  }

}