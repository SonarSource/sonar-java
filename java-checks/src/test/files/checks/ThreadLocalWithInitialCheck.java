import java.util.ArrayList;
import java.util.List;

class A {
  ThreadLocal<List<String>> myThreadLocal =
    new ThreadLocal<List<String>>() { // Noncompliant [[sc=9;ec=34]] {{Replace this anonymous class with a call to "ThreadLocal.withInitial". (sonar.java.source not set. Assuming 8 or greater.)}}
      @Override
      protected List<String> initialValue() {
        return new ArrayList<String>();
      }
    };

  ThreadLocal<List<String>> myThreadLocal2 = ThreadLocal.withInitial(ArrayList::new);
  ThreadLocal<List<String>> myThreadLocal3 =
    new ThreadLocal<List<String>>() { // compliant : more than one method
      @Override
      protected List<String> initialValue() {
        return new ArrayList<String>();
      }

      List<String> get() {
        return null;
      }
    };
  ThreadLocal<List<String>> myThreadLocal4 = new ThreadLocal<List<String>>(); // compliant
  ThreadLocal<List<String>> myThreadLocal5 =
    new ThreadLocal<List<String>>() { // compliant : only overriden method is not initialValue
      List<String> get() {
        return null;
      }
    };
  ThreadLocal<List<String>> myThreadLocal6 =
    new ThreadLocal<List<String>>() { // compliant
      final String myVar = "";
      final String myVar2 = "";
    };
  ThreadLocal<List<String>> myThreadLocal7 =
    new ThreadLocal<List<String>>() { // compliant : overload of initial value
      protected List<String> initialValue(String param) {
        return new ArrayList<String>();
      }
    };
}
