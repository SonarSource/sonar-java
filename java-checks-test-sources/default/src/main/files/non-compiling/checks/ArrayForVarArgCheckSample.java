package checks;

class ArrayForVarArgCheckBar {}
class ArrayForVarArgCheckFoo extends ArrayForVarArgCheckBar {}
class ArrayForVarArgCheckSample {

  public void callTheThing(String s) {
    ArrayForVarArgCheckFoo foo = new ArrayForVarArgCheckFoo();

    doTheThing2(new ArrayForVarArgCheckFoo[] {foo, foo}); // Noncompliant {{Remove this array creation and simply pass the elements.}}

    unknown(new ArrayForVarArgCheckFoo[0]); // Compliant
    doTheThing2(new Unknown[0]); // Compliant
    takeUnknown(new Unknown[0]); // Compliant
    takeUnknown(new ArrayForVarArgCheckFoo[0]); // Compliant
  }

  public void doTheThing2 (ArrayForVarArgCheckBar... args) {
  }

  public void takeUnknown (Unknown... args) {
  }

}
