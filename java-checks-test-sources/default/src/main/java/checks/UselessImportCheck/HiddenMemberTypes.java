package checks.UselessImportCheck;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List; // Noncompliant {{Remove this unused import 'java.util.List'.}}
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set; // Noncompliant {{Remove this unused import 'java.util.Set'.}}
import java.util.TreeMap;
import java.util.WeakHashMap;

class HiddenMemberTypes {

  static class Linked<K, V> extends LinkedHashMap<K, V> {
    @Override
    protected boolean removeEldestEntry(Entry<K, V> eldest) {
      return size() > 10;
    }
  }

  static class Sorted extends Properties {
    private static final Comparator<Entry<Object, Object>> BY_KEY = Entry.comparingByKey((a, b) -> a.toString().compareTo(b.toString()));

    Entry<Object, Object> first() {
      return null;
    }
  }

  static class Tree<K, V> extends TreeMap<K, V> {
    Entry<K, V> any() {
      return firstEntry();
    }
  }

  static class Weak<K, V> extends WeakHashMap<K, V> {
    Entry<K, V> none() {
      return null;
    }
  }

  java.util.Set<String> referencedWithItsQualifiedName;
}
