package checks;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class NullReturnedOnComputeIfPresentOrAbsent {

  public static String lambda(String k, String v) {
    return null;
  }

  public void badComputeIfPresent() {
    Map<String, String> map = new HashMap<>();
    map.computeIfPresent("myKey", (key, value) -> null); // Noncompliant {{Use "Map.containsKey(key)" followed by "Map.put(key, null)" to add null values.}}

    map.computeIfPresent("myKey", NullReturnedOnComputeIfPresentOrAbsent::lambda); // Compliant Uninteresting corner case

    String nullValue = null;
    map.computeIfPresent("myKey", (key, value) -> nullValue); // Compliant Uninteresting corner case

    map.computeIfPresent("myKey", (key, value) -> value + " modified"); // Compliant

    TreeMap<String, String> second = new TreeMap<>();
    second.computeIfPresent("myKey", (key, value) -> null); // Compliant Corner case subtype of java.util.Map may break specifications

    Map<String, String> third = new TreeMap<>();
    third.computeIfPresent("myKey", (key, value) -> null); // Noncompliant {{Use "Map.containsKey(key)" followed by "Map.put(key, null)" to add null values.}}
  }
}
