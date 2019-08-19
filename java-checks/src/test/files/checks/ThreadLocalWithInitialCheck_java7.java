import java.util.ArrayList;
import java.util.List;

class A {
  ThreadLocal<List<String>> myThreadLocal =
    new ThreadLocal<List<String>>() { // Compliant
      @Override
      protected List<String> initialValue() {
        return new ArrayList<String>();
      }
    };
}
