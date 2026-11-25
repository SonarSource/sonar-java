import static java.lang.Math.*;
import static java.util.Map.Entry;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.Locale.Category.FORMAT;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class StaticImports {
  double area(double radius) {
    return PI * pow(radius, 2);
  }

  long timeout() {
    TimeUnit unit = SECONDS;
    return unit.toMillis(max(1, abs(-3)));
  }

  String key(Entry<String, Integer> entry) {
    return entry.getKey() + E;
  }

  Locale.Category category() {
    return FORMAT;
  }

  int sum(Map<String, Integer> map) {
    int total = 0;
    for (Entry<String, Integer> entry : map.entrySet()) {
      total += entry.getValue();
    }
    return total + SECONDS.ordinal();
  }
}
