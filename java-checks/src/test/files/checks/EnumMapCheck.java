import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;

class MyClass {

  public enum COLOR {
    RED, GREEN, BLUE, ORANGE;
  }
  Map<COLOR, String> moodMapWithNullKey = new HashMap<COLOR, String>();  // compliant because using null literal as a key.
  public void noncompliant() {
    Map<COLOR, String> moodMap = new HashMap<COLOR, String>(); // Noncompliant [[sc=34;ec=62]]
    new HashMap<COLOR, String>(); // Noncompliant
    Map<COLOR, String> moodMap2 = new HashMap<>(); // Noncompliant
    Map<MyClass.COLOR, String> moodMap3 = new HashMap(); // Noncompliant
    Map moodMap4 = (new HashMap<COLOR, String>()); // Noncompliant

    Map<COLOR, String> map;
    map = new HashMap<>(); // Noncompliant [[sc=11;ec=26]]
  }

  public void compliant() {
    Map<COLOR, String> enummap = new EnumMap<COLOR, String>(COLOR.class);
    Map<String, String> otherMap = new HashMap<String, String>();
    Map<COLOR, Integer> myMap = new LinkedHashMap<>(); // compliant, preserve insertion order
    Map<java.lang.String, String> otherMap2 = new HashMap<>();
    Map otherMap3 = new HashMap();
    Map otherMap4;
    Map otherMap5 = otherMap3;
    otherMap4 = new HashMap<>();
    new HashMap<String, String>();
    int a = 5;
    a = 3;
    foo(moodMapWithNullKey);
    Object o = this.moodMapWithNullKey;
    moodMapWithNullKey.put(COLOR.BLUE, "blue");
    moodMapWithNullKey.get(COLOR.BLUE);
    moodMapWithNullKey.put(null, "null");
  }
}
