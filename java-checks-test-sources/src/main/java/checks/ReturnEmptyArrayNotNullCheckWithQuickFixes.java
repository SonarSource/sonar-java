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
    return null; // Noncompliant [[sc=12;ec=16;quickfixes=array]] {{Return an empty array instead of null.}}
    // fix@array {{Replace "null" with an empty array}}
    // edit@array [[sc=12;ec=16]] {{new Object[0]}}
  }

  String[][] array2() {
    String[][] result = new String[0][0];
    return null; // Noncompliant [[sc=12;ec=16;quickfixes=multi_dim_array]] {{Return an empty array instead of null.}}
    // fix@multi_dim_array {{Replace "null" with an empty array}}
    // edit@multi_dim_array [[sc=12;ec=16]] {{new String[0][0]}}
  }

  List<Object> list() {
    List<Object> result = Collections.emptyList();
    return null; // Noncompliant [[sc=12;ec=16;quickfixes=list]] {{Return an empty collection instead of null.}}
    // fix@list {{Replace "null" with an empty List}}
    // edit@list [[sc=12;ec=16]] {{Collections.emptyList()}}
  }

  ArrayList<Object> arraylist() {
    ArrayList<Object> result = new ArrayList<>();
    return null; // Noncompliant [[sc=12;ec=16;quickfixes=arraylist]] {{Return an empty collection instead of null.}}
    // fix@arraylist {{Replace "null" with an empty ArrayList}}
    // edit@arraylist [[sc=12;ec=16]] {{new ArrayList<>()}}
  }

  LinkedList<Object> linkedlist() {
    LinkedList<Object> result = new LinkedList<>();
    return null; // Noncompliant [[sc=12;ec=16;quickfixes=linkedlist]] {{Return an empty collection instead of null.}}
    // fix@linkedlist {{Replace "null" with an empty LinkedList}}
    // edit@linkedlist [[sc=12;ec=16]] {{new LinkedList<>()}}
  }

  // we don't know if there is a constructor
  MyCustomList<Object> customList() {
    return null; // Noncompliant [[sc=12;ec=16;quickfixes=!]] {{Return an empty collection instead of null.}}
  }

  Set<Object> set() {
    Set<Object> result = Collections.emptySet();
    return null; // Noncompliant [[sc=12;ec=16;quickfixes=set]] {{Return an empty collection instead of null.}}
    // fix@set {{Replace "null" with an empty Set}}
    // edit@set [[sc=12;ec=16]] {{Collections.emptySet()}}
  }

  HashSet<Object> hashset() {
    HashSet<Object> result = new HashSet<>();
    return null; // Noncompliant [[sc=12;ec=16;quickfixes=hashset]] {{Return an empty collection instead of null.}}
    // fix@hashset {{Replace "null" with an empty HashSet}}
    // edit@hashset [[sc=12;ec=16]] {{new HashSet<>()}}
  }

  TreeSet<Object> treeset() {
    TreeSet<Object> result = new TreeSet<>();
    return null; // Noncompliant [[sc=12;ec=16;quickfixes=treeset]] {{Return an empty collection instead of null.}}
    // fix@treeset {{Replace "null" with an empty TreeSet}}
    // edit@treeset [[sc=12;ec=16]] {{new TreeSet<>()}}
  }

  SortedSet<Object> sortedset() {
    SortedSet<Object> result = Collections.emptySortedSet();
    return null; // Noncompliant [[sc=12;ec=16;quickfixes=sortedset]] {{Return an empty collection instead of null.}}
    // fix@sortedset {{Replace "null" with an empty SortedSet}}
    // edit@sortedset [[sc=12;ec=16]] {{Collections.emptySortedSet()}}
  }

  NavigableSet<Object> navigableset() {
    NavigableSet<Object> result = Collections.emptyNavigableSet();
    return null; // Noncompliant [[sc=12;ec=16;quickfixes=navigableset]] {{Return an empty collection instead of null.}}
    // fix@navigableset {{Replace "null" with an empty NavigableSet}}
    // edit@navigableset [[sc=12;ec=16]] {{Collections.emptyNavigableSet()}}
  }

  Map<Object, Object> map() {
    Map<Object, Object> result = Collections.emptyMap();
    return null; // Compliant - maps are not covered
  }

  HashMap<Object, Object> hashmap() {
    HashMap<Object, Object> result = new HashMap<>();
    return null; // Compliant - maps are not covered
  }

  SortedMap<Object,Object> sortedmap() {
    SortedMap<Object, Object> result = Collections.emptySortedMap();
    return null; // Compliant - maps are not covered
  }

  NavigableMap<Object, Object> navigaleMap() {
    NavigableMap<Object, Object> result = Collections.emptyNavigableMap();
    return null; // Compliant - maps are not covered
  }

  Collection<Object> collection() {
    Collection<Object> result = Collections.emptyList();
    return null; // Noncompliant [[sc=12;ec=16;quickfixes=collection]] {{Return an empty collection instead of null.}}
    // fix@collection {{Replace "null" with an empty Collection}}
    // edit@collection [[sc=12;ec=16]] {{Collections.emptyList()}}
  }

  // using vectors triggers java:S1149 - so no quick-fix
  Vector<Object> vector() {
    Vector<Object> result = new Vector<>();
    return null; // Noncompliant [[sc=12;ec=16;quickfixes=!]] {{Return an empty collection instead of null.}}
  }

  abstract static class MyCustomList<T> implements List<T> { }

}
