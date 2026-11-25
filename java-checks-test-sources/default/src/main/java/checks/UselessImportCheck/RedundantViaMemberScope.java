package checks.UselessImportCheck;

import checks.UselessImportCheck.RedundantViaMemberScope.Nested; // Noncompliant {{Remove this unused import 'checks.UselessImportCheck.RedundantViaMemberScope.Nested'.}}
import java.util.Map;
import java.util.Map.Entry; // Noncompliant {{Remove this unused import 'java.util.Map.Entry'.}}
import java.util.Set;

abstract class RedundantViaMemberScope<K, V> implements Map<K, V> {

  Entry<K, V> inherited() {
    return null;
  }

  Set<Entry<K, V>> entries() {
    return null;
  }

  Nested nested;

  static class Nested {
  }
}
