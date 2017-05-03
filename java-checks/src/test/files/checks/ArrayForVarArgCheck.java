class Bar{}
class Foo extends Bar {}
class ArrayForVarArgCheck {
  ArrayForVarArgCheck(String ... params) {
  }
  public void callTheThing() {
    doTheThing(new String[] { "s1", "s2"});  // Noncompliant {{Remove this array creation and simply pass the elements.}} [[sc=16;ec=42]]
    doTheThing(new String[12]);
    doTheThing(new String[0]);  // Noncompliant {{Remove this array creation.}}
    doTheThing(new String[] {});  // Noncompliant {{Remove this array creation.}}
    doTheThing("s1", "s2");
    doTheThing2(new Foo[] { "s1", "s2"});  // Noncompliant {{Disambiguate this call by either casting as "Bar" or "Bar[]"}}
    doTheThing2(new Foo[12]);  // Noncompliant {{Disambiguate this call by either casting as "Bar" or "Bar[]"}}
    doTheThing2(new Foo[0]);  // Noncompliant {{Disambiguate this call by either casting as "Bar" or "Bar[]"}}
    doTheThing2(new Foo(), new Bar());
    new ArrayForVarArgCheck();
    new ArrayForVarArgCheck(new String[0]); // Noncompliant {{Remove this array creation.}}
    new ArrayForVarArgCheck(new String[12]);
    new ArrayForVarArgCheck(new String[] { "s1", "s2"});  // Noncompliant {{Remove this array creation and simply pass the elements.}}

  }

  public void doTheThing (String ... args) {
  }
  public void doTheThing2 (Bar ... args) {
  }
}
