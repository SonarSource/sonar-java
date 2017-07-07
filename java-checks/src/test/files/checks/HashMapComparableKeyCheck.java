import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;

abstract class A<T> {
  public static class Key {}
  public static class ComparableKey implements Comparable<ComparableKey> {
    @Override public int compareTo(ComparableKey o) { return 0; }
  }

  void foo() {
    Map<Key,String> m1 = new HashMap<>(); // Noncompliant [[sc=30;ec=39]] {{Implement "Comparable" in "Key" or switch key type.}}
    Map<Key,String> m2 = new HashMap<Key,String>(); // Noncompliant [[sc=38;ec=41]] {{Implement "Comparable" in "Key" or switch key type.}}
    Map<Key,String> m3;
    m3 = new HashMap<>(); // Noncompliant [[sc=14;ec=23]] {{Implement "Comparable" in "Key" or switch key type.}}
    new HashMap<Key,String>(); // Noncompliant [[sc=17;ec=20]] {{Implement "Comparable" in "Key" or switch key type.}}
    Map<Key, String> m4 = new HashMap(); // Noncompliant [[sc=31;ec=38]] {{Implement "Comparable" in "Key" or switch key type.}}

    Map<ComparableKey,String> m5 = new HashMap<>();
    new TreeMap<Key,String>();
    Map m6 = new HashMap(); // raw type
    Map<Key, String> m7 = getMap();
    Map<Key, String> m8 = new TreeMap<>();
    new HashMap<T, String>();

    A a;
    a = new A();
  }

  abstract Map<Key,String> getMap();

}
