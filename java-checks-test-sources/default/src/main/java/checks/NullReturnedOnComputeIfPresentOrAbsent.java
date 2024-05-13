package checks;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class NullReturnedOnComputeIfPresentOrAbsent {

  public void badComputeIfPresent() {
    Map<String, String> map = new HashMap<>();
    map.computeIfPresent("myKey", (key, value) -> // Noncompliant {{Use "Map.containsKey(key)" followed by "Map.put(key, null)" to add null values.}}
//      ^^^^^^^^^^^^^^^^
      null);
//    ^^^^<

    map.computeIfPresent("myKey", NullReturnedOnComputeIfPresentOrAbsent::presentLambda); // Compliant uninteresting corner case

    String nullValue = null;
    map.computeIfPresent("myKey", (key, value) -> nullValue); // Compliant uninteresting corner case

    map.computeIfPresent("myKey", (key, value) -> value + " modified"); // Compliant

    TreeMap<String, String> second = new TreeMap<>();
    second.computeIfPresent("myKey", (key, value) -> null); // Compliant corner case where subtype of java.util.Map may break specification

    Map<String, String> third = new TreeMap<>();
    third.computeIfPresent("myKey", (key, value) -> null); // Noncompliant
  }

  public void badComputeIfAbsent() {
    Map<String, String> map = new HashMap<>();
    map.computeIfAbsent("myKey", key -> // Noncompliant {{Use "Map.containsKey(key)" followed by "Map.put(key, null)" to add null values.}}
//      ^^^^^^^^^^^^^^^
      null);
//    ^^^^<
    map.computeIfAbsent("myKey", NullReturnedOnComputeIfPresentOrAbsent::absentLambda); // Compliant uninteresting corner case

    String nullValue = null;
    map.computeIfAbsent("myKey", key -> nullValue); // Compliant uninteresting corner case

    map.computeIfAbsent("myKey", key -> "brand new"); // Compliant

    TreeMap<String, String> second = new TreeMap<>();
    second.computeIfAbsent("myKey", key -> null); // Compliant corner case where subtype of java.util.Map may break specification

    Map<String, String> third = new TreeMap<>();
    third.computeIfAbsent("myKey", key -> null); // Noncompliant
  }

  public static String presentLambda(String k, String v) {
    return null;
  }

  public static String absentLambda(String k) {
    return null;
  }
}
