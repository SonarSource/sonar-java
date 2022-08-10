package symbolicexecution.behaviorcache;

import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;

class Optionable {
  private static String getHeader(Map<String, String> headers) {
    return ofNullable(headers)
      .orElse(new HashMap<>())
      .get("Some header");
  }

  void main() {
    getHeader(null); // Compliant
  }
}
