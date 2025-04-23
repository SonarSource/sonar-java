package checks.unused;

import java.util.stream.Stream;

public class UnusedLocalVariableCheck_java22 {
  private UnusedLocalVariableCheck_java22() {}

  public static int count(int[] elements) {
    int count = 0;
    for (int element : elements) { // Noncompliant[[quickfixes=qf_ulv]]
//           ^^^^^^^
      // fix@qf_ulv {{Replace unused local variable with _}}
      // edit@qf_ulv [[sc=10;ec=21]]{{var _}}
      count++;
    }

    for (int a : new int[]{0, 1, 2}) { // Noncompliant[[quickfixes=qf_f1]]
//           ^
      // fix@qf_f1 {{Replace unused local variable with _}}
      // edit@qf_f1 [[sc=10;ec=15]]{{var _}}
      count++;
    }

    return count;
  }

  public static void tryWithResources() {
    try (Stream foo = Stream.of()) { // Noncompliant {{Remove this unused "foo" local variable.}} [[quickfixes=qf_tr1]]
//              ^^^
//              fix@qf_tr1 {{Replace unused local variable with _}}
//              edit@qf_tr1 [[sc=10;ec=20]] {{var _}}
    } catch (Exception e) {
    }

    Stream bar = Stream.of();
    try(bar) {
    } catch (Exception e) {
    }
  }
}
