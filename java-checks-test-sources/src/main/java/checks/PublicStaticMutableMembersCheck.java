package checks;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PublicStaticMutableMembersCheck {
  public static String [] strings1 = {"first","second"};  // Noncompliant [[sc=27;ec=35]] {{Make this member "protected".}}
  public static String [] strings2 = {"first","second"};  // Noncompliant

  protected static final String [] strings3 = {"first","second"};
  private static String [] strings4 = {"first","second"};
  private String [] strings5 = {"first","second"};

  public static Date beginning; // Noncompliant
  public static Point point; // Noncompliant

  public static final Integer integer = 5;
  public static final Integer integer2 = integer;

  public static final Hashtable h = new Hashtable(); // Noncompliant

  public static final Map h2 = new Hashtable(); // Noncompliant

  public static final int[] data = new int[5]; // Noncompliant
  public static final int[] data_unknown_size = new int[getDim()]; // Noncompliant

  public static int getDim() {
    return 42;
  }

  public static final int[] EMPTY_DATA_1 = new int[0]; // Compliant
  public static final int[] EMPTY_DATA_2 = {}; // Compliant
  public static final int[] EMPTY_DATA_3 = new int[]{}; // Compliant
  public static final int[] EMPTY_DATA_4 = EMPTY_DATA_3; // Compliant
  public static final int[] NON_EMPTY_DATA_1 = new int[]{ 0 }; // Noncompliant - dim 1 array
  public static final int[][] NON_EMPTY_DATA_2 = {new int[0], {}}; // Noncompliant - you can still modify sub array
  public static final int[] NON_EMPTY_DATA_3 = NON_EMPTY_DATA_1; // Noncompliant

  public static int[] data2 = new int[5]; // Noncompliant

  public static Point p = new Point(); // Noncompliant
  public static Point p2 = p; // Noncompliant

  public static final List EMPTY_LIST = Arrays.asList();

  public static final List LIST = Arrays.asList("a"); // Noncompliant

  public static final List PROPER_LIST = Collections.unmodifiableList(Arrays.asList("a"));

  public static final List EMPTY_ARRAY_LIST = new ArrayList(Arrays.asList()); // Noncompliant

  public static final List ARRAY_LIST = new ArrayList(Arrays.asList("a")); // Noncompliant

  public static final Set SET = new HashSet(Arrays.asList("a")); // Noncompliant

  public static final Set PROPER_SET = Collections.unmodifiableSet(new HashSet(Arrays.asList("a")));

  public static final Map MAP = new HashMap(); // Noncompliant
  public static final Map otherMap = MAP; // Noncompliant

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

  public static final Set<Object> SINGLETON_UNMODIFIABLE_SET = Collections.singleton(new Object());
  public static final List<Object> SINGLETON_UNMODIFIABLE_LIST = Collections.singletonList(new Object());
  public static final Map<Object, Object> SINGLETON_UNMODIFIABLE_MAP = Collections.singletonMap(new Object(), new Object());

  public static final Set<Object> EMTPY_SET = Collections.emptySet();
  public static final List<Object> EMPTY_LIST2 = Collections.emptyList();
  public static final Map<Object, Object> EMPTY_MAP = Collections.emptyMap();

  // apache commons collections 4.x
  public static final List<String> immutableListApacheNew = new org.apache.commons.collections4.list.UnmodifiableList(new ArrayList<>());
  public static final List<String> immutableListApache = org.apache.commons.collections4.list.UnmodifiableList.unmodifiableList(new ArrayList<String>());
  public static final Set<String> immutableSetApache = org.apache.commons.collections4.set.UnmodifiableSet.unmodifiableSet(new HashSet<String>());
  public static final Map<String, String> immutableMapApache = org.apache.commons.collections4.map.UnmodifiableMap.unmodifiableMap(new HashMap<String, String>());

  public static final List<String> otherImmutableListApache = immutableListApache;

  public static List emptyList = Arrays.asList();
  public static List otherEmptyList = Arrays.asList();
  public void changeEmptyList() {
    emptyList = new ArrayList(); // Noncompliant {{Make member "emptyList" "protected".}}
    PublicStaticMutableMembersCheck.otherEmptyList = new ArrayList(); // Noncompliant {{Make member "otherEmptyList" "protected".}}
  }

  enum E {
    A, B, C
  }
  // FALSE negative: no issues raised when initializer type is unknown
  // in this case Collections.unmodifiableSet(EnumSet.of is unknown because of generics
  public static final Set<E> set = Collections.unmodifiableSet(EnumSet.of(
    E.A,
    E.C
  ));

  public static final List<String> CUSTOM_MUTABLE_LIST = customMutableList(); // Noncompliant
  public static final List<String> CUSTOM_IMMUTABLE_LIST = customImmutableList();
  public static final List<String> CUSTOM_UNMODIFIABLE_LIST = customUnmodifiableList();

  private static List<String> customMutableList() {
    return Collections.emptyList();
  }

  private static List<String> customImmutableList() {
    return Collections.emptyList();
  }

  private static List<String> customUnmodifiableList() {
    return Collections.emptyList();
  }
}

interface PublicStaticMutableMembersCheck_I {
  public static String[] MY_ARRAY = null; // Noncompliant {{Move "MY_ARRAY" to a class and lower its visibility}}
  public static Collection<String> MY_COLLECTION = null; // Noncompliant {{Move "MY_COLLECTION" to a class and lower its visibility}}
  public static Collection MY_2ND_COLLECTION = null; // Noncompliant {{Move "MY_2ND_COLLECTION" to a class and lower its visibility}}
  public static List<String> MY_LIST = null; // Noncompliant {{Move "MY_LIST" to a class and lower its visibility}}
  public static List MY_2ND_LIST = null; // Noncompliant {{Move "MY_2ND_LIST" to a class and lower its visibility}}
  public static Date MY_DATE = null; // Noncompliant {{Move "MY_DATE" to a class and lower its visibility}}
  public static int MY_INT = 0; // Compliant
  public static PublicStaticMutableMembersCheckB<String> MY_PARAMETRIC_TYPE = null; // Compliant
  public static PublicStaticMutableMembersCheckC MY_FIELD = null; // Compliant

  public Collection<String> myCollection = null; // Noncompliant
  public Collection my2ndCollection = null; // Noncompliant
  public List<String> myList = null; // Noncompliant
  public List my2ndList = null; // Noncompliant
  public Date myDate = null; // Noncompliant
  public int myInt = 0; // Compliant
  public PublicStaticMutableMembersCheckB<String> myParametricType = null; // Compliant
  public PublicStaticMutableMembersCheckC myField = null; // Compliant

  public void doSomething(); // not a field

  public static Point p = new Point(); // Noncompliant
  public static Point otherP = p; // Noncompliant

  // apache commons collections 4.x
  public static final List<String> immutableListApacheNew = new org.apache.commons.collections4.list.UnmodifiableList(new ArrayList<>());
  public static final List<String> immutableListApache = org.apache.commons.collections4.list.UnmodifiableList.unmodifiableList(new ArrayList<String>());
  public static final Set<String> immutableSetApache = org.apache.commons.collections4.set.UnmodifiableSet.unmodifiableSet(new HashSet<String>());
  public static final Map<String, String> immutableMapApache = org.apache.commons.collections4.map.UnmodifiableMap.unmodifiableMap(new HashMap<String, String>());

  public static final Set PROPER_SET = Collections.unmodifiableSet(new HashSet(Arrays.asList("a")));
  public static final List EMPTY_LIST = Arrays.asList();
}

class PublicStaticMutableMembersCheckB<T> {
}

class PublicStaticMutableMembersCheckC {
}
