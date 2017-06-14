class Bar{}
class Foo extends Bar {}
class ArrayForVarArgCheck {
  ArrayForVarArgCheck(String ... params) { }
  <X> ArrayForVarArgCheck(int i, X ... xs) { }
  public void callTheThing(String s) {
    doTheThing(new String[] { "s1", "s2"});  // Noncompliant {{Remove this array creation and simply pass the elements.}} [[sc=16;ec=42]]
    doTheThing(new String[12]);
    doTheThing(new String[0]);  // Noncompliant {{Remove this array creation.}}
    doTheThing(new String[] {});  // Noncompliant {{Remove this array creation.}}
    doTheThing("s1", "s2");
    doTheThing2(new Foo[] { "s1", "s2"});  // Noncompliant {{Disambiguate this call by either casting as "Bar" or "Bar[]".}}
    doTheThing2(new Foo[12]);  // Noncompliant {{Disambiguate this call by either casting as "Bar" or "Bar[]".}}
    doTheThing2(new Foo[0]);  // Noncompliant {{Disambiguate this call by either casting as "Bar" or "Bar[]".}}
    doTheThing2(new Foo(), new Bar());
    unknown(new Foo[0]);
    callTheThing("");
    new ArrayForVarArgCheck();
    new ArrayForVarArgCheck(new String[0]); // Noncompliant {{Remove this array creation.}}
    new ArrayForVarArgCheck(new String[1 - 1]);
    new ArrayForVarArgCheck(new String[12]);
    new ArrayForVarArgCheck(new String[] { "s1", "s2"});  // Noncompliant {{Remove this array creation and simply pass the elements.}}

    java.util.List<String> items = java.util.Arrays.asList(new String[]{"hello", "world"}); // Noncompliant {{Remove this array creation and simply pass the elements.}}
    foo(new String[]{"hello", "world"}); // Noncompliant {{Remove this array creation and simply pass the elements.}}

    new ArrayForVarArgCheck(14, new String[]{"hello", "world"}); // Noncompliant {{Remove this array creation and simply pass the elements.}}
  }

  public void doTheThing (String ... args) {
  }
  public void doTheThing2 (Bar ... args) {
  }
  public static <T> void foo(T... ts) {
    return;
  }
}

class Overload{
  Object o = fun(12, new String[0]); // Noncompliant
  Overload(int i) {
    this(i, new String[0]);
  }
  Overload(int i, String ... params) {
  }


  void fun(int i) {
    fun(i, new String[0]);
  }
  void fun(int i, String ... params) {
  }
}

class ParametrizedType<U> {
  public <T> ParametrizedType(U u, T ... t) { }

  static void foo(Object o) {
    new ParametrizedType<>(o, new String[] {"hello", "world"}); // Noncompliant {{Remove this array creation and simply pass the elements.}}
  }
}
