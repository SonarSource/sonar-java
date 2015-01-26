import java.util.Collection;
import java.util.List;
import java.util.Date;

interface A {
  public static String[] MY_ARRAY = null; // Noncompliant
  public static Collection<String> MY_COLLECTION = null; // Noncompliant
  public static Collection MY_2ND_COLLECTION = null; // Noncompliant
  public static List<String> MY_LIST = null; // Noncompliant
  public static List MY_2ND_LIST = null; // Noncompliant
  public static Date MY_DATE = null; // Noncompliant
  public static int MY_INT = 0; // Compliant
  public static B<String> MY_PARAMETRIC_TYPE = null; // Compliant
  public static C MY_FIELD = null; // Compliant

  public Collection<String> myCollection = null; // Compliant
  public Collection my2ndCollection = null; // Compliant
  public List<String> myList = null; // Compliant
  public List my2ndList = null; // Compliant
  public Date myDate = null; // Compliant
  public int myInt = 0; // Compliant
  public B<String> myParametricType = null; // Compliant
  public C myField = null; // Compliant

  public void doSomething(); // not a field
}

class B<T> {
}

class C {
}