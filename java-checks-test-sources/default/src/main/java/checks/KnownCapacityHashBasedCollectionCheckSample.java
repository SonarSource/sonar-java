package checks;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.WeakHashMap;

class KnownCapacityHashBasedCollectionCheckSample {

  private static final int cap = 1;

  void nonCompliant() {
    HashMap<String, String> hashMap = new HashMap<>(100); // Noncompliant[[sc=39;ec=57;]]
    HashMap<String, String> hashMap2 = new HashMap<>(100); // Noncompliant {{Replace this call to the constructor with the better suited static method HashMap.newHashMap(int numMappings)}}
    HashSet<String> hashSet2 = new HashSet<>(100); // Noncompliant {{Replace this call to the constructor with the better suited static method HashSet.newHashSet(int numMappings)}}
    Integer capacity = 10;
    HashMap<String, String> hm = new HashMap<>(capacity); // Noncompliant
    HashSet<String> hs = new HashSet<>(cap); // Noncompliant
    WeakHashMap<String, String> whm = new WeakHashMap<>(2); // Noncompliant {{Replace this call to the constructor with the better suited static method WeakHashMap.newWeakHashMap(int numMappings)}}
    LinkedHashMap<String, String> lhm = new LinkedHashMap<>(3); // Noncompliant {{Replace this call to the constructor with the better suited static method LinkedHashMap.newLinkedHashMap(int numMappings)}}
    LinkedHashSet<String> lhs = new LinkedHashSet<>(3); // Noncompliant {{Replace this call to the constructor with the better suited static method LinkedHashSet.newLinkedHashSet(int numMappings)}}
  }

  void compliant() {
    HashMap<String, String> hashMap = new HashMap<>();
    HashSet<String> hashSet = new HashSet<>();
    HashMap<String, String> hashMap2 = new HashMap<>(Map.of("key", "val"));
    HashSet<String> hashSet2 = new HashSet<>(Collections.singleton(""));
    WeakHashMap<String, String> whm2 = new WeakHashMap<>(2, 2f); 
  }

}
