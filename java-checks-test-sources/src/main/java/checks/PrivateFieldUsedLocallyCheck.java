package checks;

public class PrivateFieldUsedLocallyCheck {

  // fix@qf1 {{Remove the "privateField" field and declare it as a local variable in the relevant method.}}
  // edit@qf1 [[sl=+1;el=+1;sc=37;ec=37]] {{\nint privateField = 1;\n}}
  // edit@qf1 [[sc=3;ec=32]] {{}}
  private int privateField = 1; // Noncompliant [[sc=15;ec=27;quickfixes=qf1]]
  public int useLocally(int value) {
    this.privateField = 42;
    return privateField * value;
  }
}
