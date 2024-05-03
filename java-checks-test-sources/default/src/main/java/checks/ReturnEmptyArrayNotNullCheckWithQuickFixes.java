package checks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

class ReturnEmptyArrayNotNullCheckWithQuickFixes {

  Object[] array1() {
    Object[] result = new Object[0];
    return null; // Noncompliant {{Return an empty array instead of null.}} [[quickfixes=array]]
//         ^^^^
    // fix@array {{Replace "null" with an empty array}}
    // edit@array [[sc=12;ec=16]] {{new Object[0]}}
  }

  String[][] array2() {
    String[][] result = new String[0][0];
    return null; // Noncompliant {{Return an empty array instead of null.}} [[quickfixes=multi_dim_array]]
//         ^^^^
    // fix@multi_dim_array {{Replace "null" with an empty array}}
    // edit@multi_dim_array [[sc=12;ec=16]] {{new String[0][0]}}
  }

  List<String>[] array_generics() {
    List<String>[] result = new List[0];
    return null; // Noncompliant {{Return an empty array instead of null.}} [[quickfixes=array_generics]]
//         ^^^^
    // fix@array_generics {{Replace "null" with an empty array}}
    // edit@array_generics [[sc=12;ec=16]] {{new List[0]}}
  }

  List<Map<String[], Set<Object>>[]>[][] strange_array() {
    List<Map<String[], Set<Object>>[]>[][] result = new List[0][0];
    return null; // Noncompliant {{Return an empty array instead of null.}} [[quickfixes=strange_array]]
//         ^^^^
    // fix@strange_array {{Replace "null" with an empty array}}
    // edit@strange_array [[sc=12;ec=16]] {{new List[0][0]}}
  }

  List<Object> list() {
    List<Object> result = Collections.emptyList();
    return null; // Noncompliant {{Return an empty collection instead of null.}} [[quickfixes=list]]
//         ^^^^
    // fix@list {{Replace "null" with an empty List}}
    // edit@list [[sc=12;ec=16]] {{Collections.emptyList()}}
  }

  ArrayList<Object> arraylist() {
    ArrayList<Object> result = new ArrayList<>();
    return null; // Noncompliant {{Return an empty collection instead of null.}} [[quickfixes=arraylist]]
//         ^^^^
    // fix@arraylist {{Replace "null" with an empty ArrayList}}
    // edit@arraylist [[sc=12;ec=16]] {{new ArrayList<>()}}
  }

  LinkedList<Object> linkedlist() {
    LinkedList<Object> result = new LinkedList<>();
    return null; // Noncompliant {{Return an empty collection instead of null.}} [[quickfixes=linkedlist]]
//         ^^^^
    // fix@linkedlist {{Replace "null" with an empty LinkedList}}
    // edit@linkedlist [[sc=12;ec=16]] {{new LinkedList<>()}}
  }

  // we don't know if there is a constructor
  MyCustomList<Object> customList() {
    return null; // Noncompliant {{Return an empty collection instead of null.}} [[quickfixes=!]]
//         ^^^^
  }

  Set<Object> set() {
    Set<Object> result = Collections.emptySet();
    return null; // Noncompliant {{Return an empty collection instead of null.}} [[quickfixes=set]]
//         ^^^^
    // fix@set {{Replace "null" with an empty Set}}
    // edit@set [[sc=12;ec=16]] {{Collections.emptySet()}}
  }

  HashSet<Object> hashset() {
    HashSet<Object> result = new HashSet<>();
    return null; // Noncompliant {{Return an empty collection instead of null.}} [[quickfixes=hashset]]
//         ^^^^
    // fix@hashset {{Replace "null" with an empty HashSet}}
    // edit@hashset [[sc=12;ec=16]] {{new HashSet<>()}}
  }

  TreeSet<Object> treeset() {
    TreeSet<Object> result = new TreeSet<>();
    return null; // Noncompliant {{Return an empty collection instead of null.}} [[quickfixes=treeset]]
//         ^^^^
    // fix@treeset {{Replace "null" with an empty TreeSet}}
    // edit@treeset [[sc=12;ec=16]] {{new TreeSet<>()}}
  }

  SortedSet<Object> sortedset() {
    SortedSet<Object> result = Collections.emptySortedSet();
    return null; // Noncompliant {{Return an empty collection instead of null.}} [[quickfixes=sortedset]]
//         ^^^^
    // fix@sortedset {{Replace "null" with an empty SortedSet}}
    // edit@sortedset [[sc=12;ec=16]] {{Collections.emptySortedSet()}}
  }

  NavigableSet<Object> navigableset() {
    NavigableSet<Object> result = Collections.emptyNavigableSet();
    return null; // Noncompliant {{Return an empty collection instead of null.}} [[quickfixes=navigableset]]
//         ^^^^
    // fix@navigableset {{Replace "null" with an empty NavigableSet}}
    // edit@navigableset [[sc=12;ec=16]] {{Collections.emptyNavigableSet()}}
  }

  Map<Object, Object> map() {
    Map<Object, Object> result = Collections.emptyMap();
    return null; // Noncompliant {{Return an empty map instead of null.}} [[quickfixes=map]]
//         ^^^^
    // fix@map {{Replace "null" with an empty Map}}
    // edit@map [[sc=12;ec=16]] {{Collections.emptyMap()}}
  }

  HashMap<Object, Object> hashmap() {
    HashMap<Object, Object> result = new HashMap<>();
    return null; // Noncompliant {{Return an empty map instead of null.}} [[quickfixes=hashmap]]
//         ^^^^
    // fix@hashmap {{Replace "null" with an empty HashMap}}
    // edit@hashmap [[sc=12;ec=16]] {{new HashMap<>()}}
  }

  SortedMap<Object,Object> sortedmap() {
    SortedMap<Object, Object> result = Collections.emptySortedMap();
    return null; // Noncompliant {{Return an empty map instead of null.}} [[quickfixes=sortedmap]]
//         ^^^^
    // fix@sortedmap {{Replace "null" with an empty SortedMap}}
    // edit@sortedmap [[sc=12;ec=16]] {{Collections.emptySortedMap()}}
  }

  NavigableMap<Object, Object> navigableMap() {
    NavigableMap<Object, Object> result = Collections.emptyNavigableMap();
    return null; // Noncompliant {{Return an empty map instead of null.}} [[quickfixes=navigableMap]]
//         ^^^^
    // fix@navigableMap {{Replace "null" with an empty NavigableMap}}
    // edit@navigableMap [[sc=12;ec=16]] {{Collections.emptyNavigableMap()}}
  }

  Collection<Object> collection() {
    Collection<Object> result = Collections.emptyList();
    return null; // Noncompliant {{Return an empty collection instead of null.}} [[quickfixes=collection]]
//         ^^^^
    // fix@collection {{Replace "null" with an empty Collection}}
    // edit@collection [[sc=12;ec=16]] {{Collections.emptyList()}}
  }

  // using vectors triggers java:S1149 - so no quick-fix
  Vector<Object> vector() {
    Vector<Object> result = new Vector<>();
    return null; // Noncompliant {{Return an empty collection instead of null.}} [[quickfixes=!]]
//         ^^^^
  }

  abstract static class MyCustomList<T> implements List<T> { }

}
