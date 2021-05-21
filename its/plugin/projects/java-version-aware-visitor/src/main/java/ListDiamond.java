import java.util.ArrayList;
import java.util.List;

public class ListDiamond {
  private void foo(Object o) {
    List<Object> os = new ArrayList<Object>(); // java:S2293 when sonar.java.source is set to java7+
  }
}

