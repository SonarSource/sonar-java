package checks;

import java.io.IOException;

class ArrayForVarArgCheckBar {}
class ArrayForVarArgCheckFoo extends ArrayForVarArgCheckBar {}
class ArrayForVarArgCheck {
  ArrayForVarArgCheckFoo foo = new ArrayForVarArgCheckFoo();

  ArrayForVarArgCheck(String ... params) { }
  <X> ArrayForVarArgCheck(int i, X ... xs) { }
  public void callTheThing(String s) throws IOException {
    doTrickyThing(new String[][]{new String[]{"hello", "world"}});  // Noncompliant {{Remove this array creation and simply pass the elements.}}
    doTrickyThing(new String[]{"hello", "world"});  // Compliant

    doTrickyThing(new ArrayForVarArgCheckFoo[][]{new ArrayForVarArgCheckFoo[]{foo, foo}});  // Noncompliant {{Remove this array creation and simply pass the elements.}}
    doTrickyThing(new ArrayForVarArgCheckFoo[]{foo, foo});  // Compliant

    doTheThing(new String[] { "s1", "s2"});  // Noncompliant {{Remove this array creation and simply pass the elements.}} [[sc=16;ec=42]]
    doTheThing(new String[12]);
    doTheThing(new String[0]);  // Noncompliant {{Remove this array creation.}}
    doTheThing(new String[] {});  // Noncompliant {{Remove this array creation.}}
    doTheThing("s1", "s2");
    doTheThing("s1");
    doTheThing2(new ArrayForVarArgCheckFoo[] {foo, foo});  // Noncompliant {{Remove this array creation and simply pass the elements.}}
    doTheThing2(new ArrayForVarArgCheckFoo[12]);
    doTheThing2(new ArrayForVarArgCheckFoo[0]);  // Noncompliant {{Remove this array creation.}}
    doTheThing2(new ArrayForVarArgCheckFoo(), new ArrayForVarArgCheckBar());
    callTheThing("");
    new ArrayForVarArgCheck();
    new ArrayForVarArgCheck(new String[0]); // Noncompliant {{Remove this array creation.}}
    new ArrayForVarArgCheck(new String[1 - 1]);
    new ArrayForVarArgCheck(new String[12]);
    new ArrayForVarArgCheck(new String[] { "s1", "s2"});  // Noncompliant {{Remove this array creation and simply pass the elements.}}

    java.util.List<String> items = java.util.Arrays.asList(new String[]{"hello", "world"}); // Noncompliant {{Remove this array creation and simply pass the elements.}}
    foo(new String[]{"hello", "world"}); // Noncompliant {{Remove this array creation and simply pass the elements.}}

    new ArrayForVarArgCheck(14, new String[]{"hello", "world"}); // Noncompliant {{Remove this array creation and simply pass the elements.}}

    arrayThenVarargs(new int[0]); // Compliant, not the varargs argument
    arrayThenVarargs(new int[]{1,2,3});
    arrayThenVarargs(new int[]{1,2,3}, "s1", "s2");
    arrayThenVarargs(new int[]{1,2,3}, new String[]{"hello", "world"}); // Noncompliant
    arrayThenVarargs(new int[]{1,2,3}, new String[0]); // Noncompliant

    java.nio.file.Files.write(java.nio.file.Paths.get("myPath"), new byte[0]); // Compliant, byte array is not a varargs
    java.nio.file.Files.write(java.nio.file.Paths.get("myPath"), new byte[] {' ', 'A', 'B', 'C'}); // Compliant, byte array is not a varargs


    ambiguous(new String[] {}); // Noncompliant {{Disambiguate this call by either casting as "Object" or "Object[]".}}
    ambiguous(new String[0]); // Noncompliant {{Disambiguate this call by either casting as "Object" or "Object[]".}}
    ambiguous(new String[12]); // Noncompliant {{Disambiguate this call by either casting as "Object" or "Object[]".}}
    ambiguous(new String[] {"A", "B"}); // Noncompliant {{Disambiguate this call by either casting as "Object" or "Object[]".}}
  }

  public void doTrickyThing(String[]... args) {
  }
  public void doTrickyThing(ArrayForVarArgCheckBar[]... args) {
  }
  public void doTheThing (String ... args) {
  }
  public void doTheThing2 (ArrayForVarArgCheckBar... args) {
  }
  public void arrayThenVarargs (int[] array, String... args) {
  }

  public static <T> void foo(T... ts) {
    return;
  }

  public void ambiguous(Object...obj) {
    return;
  }
}

class Overload{
  Overload(int i) {
    this(i, new String[0]);
  }
  Overload(int i, String ... params) {
  }

  void useFun() {
    fun(12, new String[0]); // Noncompliant
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
