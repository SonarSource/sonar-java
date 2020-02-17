package checks;

import java.util.ArrayList;
import java.util.List;

class ThreadLocalWithInitialCheck_java7 {
  ThreadLocal<List<String>> myThreadLocal =
    new ThreadLocal<List<String>>() { // Compliant
      @Override
      protected List<String> initialValue() {
        return new ArrayList<String>();
      }
    };
}
