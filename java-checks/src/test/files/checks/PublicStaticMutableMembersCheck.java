import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class A {
  public static String [] strings1 = {"first","second"};  // Noncompliant {{Make this member "protected".}}
  public static String [] strings2 = {"first","second"};  // Noncompliant

  protected static final String [] strings3 = {"first","second"};
  private static String [] strings4 = {"first","second"};
  private String [] strings5 = {"first","second"};

  public static Date beginning; // Noncompliant
  public static java.awt.Point point; // Noncompliant

  public static final Integer integer = 5;

  public static final Hashtable h = new Hashtable(); // Noncompliant

  public static final Map h2 = new Hashtable(); // Noncompliant

  public static final int[] data = new int[5]; // Noncompliant

  public static int[] data2 = new int[5]; // Noncompliant

  public static Point p = new Point(); // Noncompliant

  public static final List EMPTY_LIST = Arrays.asList();

  public static final List LIST = Arrays.asList("a"); // Noncompliant

  public static final List PROPER_LIST = Collections.unmodifiableList(Arrays.asList("a"));

  public static final List EMPTY_ARRAY_LIST = new ArrayList(Arrays.asList()); // Noncompliant

  public static final List ARRAY_LIST = new ArrayList(Arrays.asList("a")); // Noncompliant

  public static final Set SET = new HashSet(Arrays.asList("a")); // Noncompliant

  public static final Set PROPER_SET = Collections.unmodifiableSet(new HashSet(Arrays.asList("a")));

  public static final Map MAP = new HashMap(); // Noncompliant

  static {
    MAP.put("a", "b");
    MAP.put("c", "d");
  }

  public static final Map MAP_ANONYMOUS = new HashMap() {{ // Noncompliant
    put("a", "b");
    put("c", "d");
  }};

  public static final Map PROPER_MAP_ANONYMOUS = Collections.unmodifiableMap(new HashMap() {{
    put("a", "b");
    put("c", "d");
  }});

  // guava
  public static final List<String> immutableList = ImmutableList.of("a");
  public static final Set<String> immutableSet = ImmutableSet.of("a");
  public static final Map<String, String> immutableMap = ImmutableMap.of("a", "a");

  // apache commons collections 4.x
  public static final Map<String, String> immutableListApacheNew = new org.apache.commons.collections4.list.UnmodifiableList(new ArrayList<>());
  public static final List<String> immutableListApache = org.apache.commons.collections4.list.UnmodifiableList.unmodifiableList(new ArrayList<String>());
  public static final Set<String> immutableSetApache = org.apache.commons.collections4.set.UnmodifiableSet.unmodifiableSet(new HashSet<String>());
  public static final Map<String, String> immutableMapApache = org.apache.commons.collections4.map.UnmodifiableMap.unmodifiableMap(new HashMap<String, String>());

  public static final List noInitializer;
  public static final List unknown = foo(); // Noncompliant

  public static List emptyList = Arrays.asList();
  public void changeEmptyList() {
    emptyList = new ArrayList(); // Noncompliant {{Make member "emptyList" "protected".}}
  }
}
