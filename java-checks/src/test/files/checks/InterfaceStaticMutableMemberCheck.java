import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Date;

interface A {
  public static String[] MY_ARRAY = null; // Noncompliant {{Move "MY_ARRAY" to a class and lower its visibility}}
  public static Collection<String> MY_COLLECTION = null; // Noncompliant {{Move "MY_COLLECTION" to a class and lower its visibility}}
  public static Collection MY_2ND_COLLECTION = null; // Noncompliant {{Move "MY_2ND_COLLECTION" to a class and lower its visibility}}
  public static List<String> MY_LIST = null; // Noncompliant {{Move "MY_LIST" to a class and lower its visibility}}
  public static List MY_2ND_LIST = null; // Noncompliant {{Move "MY_2ND_LIST" to a class and lower its visibility}}
  public static Date MY_DATE = null; // Noncompliant {{Move "MY_DATE" to a class and lower its visibility}}
  public static int MY_INT = 0; // Compliant
  public static B<String> MY_PARAMETRIC_TYPE = null; // Compliant
  public static C MY_FIELD = null; // Compliant

  public Collection<String> myCollection = null; // Noncompliant
  public Collection my2ndCollection = null; // Noncompliant
  public List<String> myList = null; // Noncompliant
  public List my2ndList = null; // Noncompliant
  public Date myDate = null; // Noncompliant
  public int myInt = 0; // Compliant
  public B<String> myParametricType = null; // Compliant
  public C myField = null; // Compliant

  public void doSomething(); // not a field
  public static MyImmutableCollection<String> immutableList; //Compliant : immutable collection

  public static Point p = new Point(); // Noncompliant

  // guava
  public static final List<String> immutableList = ImmutableList.of("a");
  public static final Set<String> immutableSet = ImmutableSet.of("a");
  public static final Map<String, String> immutableMap = ImmutableMap.of("a", "a");

  // apache commons collections 4.x
  public static final Map<String, String> immutableListApacheNew = new org.apache.commons.collections4.list.UnmodifiableList(new ArrayList<>());
  public static final List<String> immutableListApache = org.apache.commons.collections4.list.UnmodifiableList.unmodifiableList(new ArrayList<String>());
  public static final Set<String> immutableSetApache = org.apache.commons.collections4.set.UnmodifiableSet.unmodifiableSet(new HashSet<String>());
  public static final Map<String, String> immutableMapApache = org.apache.commons.collections4.map.UnmodifiableMap.unmodifiableMap(new HashMap<String, String>());

  public static final Set PROPER_SET = Collections.unmodifiableSet(new HashSet(Arrays.asList("a")));
  public static final List EMPTY_LIST = Arrays.asList();
}

class B<T> {
}

class C {
}

class MyImmutableCollection extends com.google.common.collect.ImmutableCollection {}
