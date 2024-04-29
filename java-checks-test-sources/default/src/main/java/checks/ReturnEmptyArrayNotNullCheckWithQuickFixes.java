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
    return null; // Noncompliant [[quickfixes=array]] {{Return an empty array instead of null.}}
//         ^^^^
    // fix@array {{Replace "null" with an empty array}}
    // edit@array [[sc=12;ec=16]] {{new Object[0]}}
  }

  String[][] array2() {
    String[][] result = new String[0][0];
    return null; // Noncompliant [[quickfixes=multi_dim_array]] {{Return an empty array instead of null.}}
//         ^^^^
    // fix@multi_dim_array {{Replace "null" with an empty array}}
    // edit@multi_dim_array [[sc=12;ec=16]] {{new String[0][0]}}
  }

  List<String>[] array_generics() {
    List<String>[] result = new List[0];
    return null; // Noncompliant [[quickfixes=array_generics]] {{Return an empty array instead of null.}}
//         ^^^^
    // fix@array_generics {{Replace "null" with an empty array}}
    // edit@array_generics [[sc=12;ec=16]] {{new List[0]}}
  }

  List<Map<String[], Set<Object>>[]>[][] strange_array() {
    List<Map<String[], Set<Object>>[]>[][] result = new List[0][0];
    return null; // Noncompliant [[quickfixes=strange_array]] {{Return an empty array instead of null.}}
//         ^^^^
    // fix@strange_array {{Replace "null" with an empty array}}
    // edit@strange_array [[sc=12;ec=16]] {{new List[0][0]}}
  }

  List<Object> list() {
    List<Object> result = Collections.emptyList();
    return null; // Noncompliant [[quickfixes=list]] {{Return an empty collection instead of null.}}
//         ^^^^
    // fix@list {{Replace "null" with an empty List}}
    // edit@list [[sc=12;ec=16]] {{Collections.emptyList()}}
  }

  ArrayList<Object> arraylist() {
    ArrayList<Object> result = new ArrayList<>();
    return null; // Noncompliant [[quickfixes=arraylist]] {{Return an empty collection instead of null.}}
//         ^^^^
    // fix@arraylist {{Replace "null" with an empty ArrayList}}
    // edit@arraylist [[sc=12;ec=16]] {{new ArrayList<>()}}
  }

  LinkedList<Object> linkedlist() {
    LinkedList<Object> result = new LinkedList<>();
    return null; // Noncompliant [[quickfixes=linkedlist]] {{Return an empty collection instead of null.}}
//         ^^^^
    // fix@linkedlist {{Replace "null" with an empty LinkedList}}
    // edit@linkedlist [[sc=12;ec=16]] {{new LinkedList<>()}}
  }

  // we don't know if there is a constructor
  MyCustomList<Object> customList() {
    return null; // Noncompliant [[quickfixes=!]] {{Return an empty collection instead of null.}}
//         ^^^^
  }

  Set<Object> set() {
    Set<Object> result = Collections.emptySet();
    return null; // Noncompliant [[quickfixes=set]] {{Return an empty collection instead of null.}}
//         ^^^^
    // fix@set {{Replace "null" with an empty Set}}
    // edit@set [[sc=12;ec=16]] {{Collections.emptySet()}}
  }

  HashSet<Object> hashset() {
    HashSet<Object> result = new HashSet<>();
    return null; // Noncompliant [[quickfixes=hashset]] {{Return an empty collection instead of null.}}
//         ^^^^
    // fix@hashset {{Replace "null" with an empty HashSet}}
    // edit@hashset [[sc=12;ec=16]] {{new HashSet<>()}}
  }

  TreeSet<Object> treeset() {
    TreeSet<Object> result = new TreeSet<>();
    return null; // Noncompliant [[quickfixes=treeset]] {{Return an empty collection instead of null.}}
//         ^^^^
    // fix@treeset {{Replace "null" with an empty TreeSet}}
    // edit@treeset [[sc=12;ec=16]] {{new TreeSet<>()}}
  }

  SortedSet<Object> sortedset() {
    SortedSet<Object> result = Collections.emptySortedSet();
    return null; // Noncompliant [[quickfixes=sortedset]] {{Return an empty collection instead of null.}}
//         ^^^^
    // fix@sortedset {{Replace "null" with an empty SortedSet}}
    // edit@sortedset [[sc=12;ec=16]] {{Collections.emptySortedSet()}}
  }

  NavigableSet<Object> navigableset() {
    NavigableSet<Object> result = Collections.emptyNavigableSet();
    return null; // Noncompliant [[quickfixes=navigableset]] {{Return an empty collection instead of null.}}
//         ^^^^
    // fix@navigableset {{Replace "null" with an empty NavigableSet}}
    // edit@navigableset [[sc=12;ec=16]] {{Collections.emptyNavigableSet()}}
  }

  Map<Object, Object> map() {
    Map<Object, Object> result = Collections.emptyMap();
    return null; // Noncompliant [[quickfixes=map]] {{Return an empty map instead of null.}}
//         ^^^^
    // fix@map {{Replace "null" with an empty Map}}
    // edit@map [[sc=12;ec=16]] {{Collections.emptyMap()}}
  }

  HashMap<Object, Object> hashmap() {
    HashMap<Object, Object> result = new HashMap<>();
    return null; // Noncompliant [[quickfixes=hashmap]] {{Return an empty map instead of null.}}
//         ^^^^
    // fix@hashmap {{Replace "null" with an empty HashMap}}
    // edit@hashmap [[sc=12;ec=16]] {{new HashMap<>()}}
  }

  SortedMap<Object,Object> sortedmap() {
    SortedMap<Object, Object> result = Collections.emptySortedMap();
    return null; // Noncompliant [[quickfixes=sortedmap]] {{Return an empty map instead of null.}}
//         ^^^^
    // fix@sortedmap {{Replace "null" with an empty SortedMap}}
    // edit@sortedmap [[sc=12;ec=16]] {{Collections.emptySortedMap()}}
  }

  NavigableMap<Object, Object> navigableMap() {
    NavigableMap<Object, Object> result = Collections.emptyNavigableMap();
    return null; // Noncompliant [[quickfixes=navigableMap]] {{Return an empty map instead of null.}}
//         ^^^^
    // fix@navigableMap {{Replace "null" with an empty NavigableMap}}
    // edit@navigableMap [[sc=12;ec=16]] {{Collections.emptyNavigableMap()}}
  }

  Collection<Object> collection() {
    Collection<Object> result = Collections.emptyList();
    return null; // Noncompliant [[quickfixes=collection]] {{Return an empty collection instead of null.}}
//         ^^^^
    // fix@collection {{Replace "null" with an empty Collection}}
    // edit@collection [[sc=12;ec=16]] {{Collections.emptyList()}}
  }

  // using vectors triggers java:S1149 - so no quick-fix
  Vector<Object> vector() {
    Vector<Object> result = new Vector<>();
    return null; // Noncompliant [[quickfixes=!]] {{Return an empty collection instead of null.}}
//         ^^^^
  }

  abstract static class MyCustomList<T> implements List<T> { }

}
