package checks;

import java.util.ArrayList;
import java.util.List;

public class PrivateFieldUsedLocallyCheck {

  // fix@qf1 {{Remove the "privateField" field and declare it as a local variable in the relevant method}}
  // edit@qf1 [[sl=+1;el=+1;sc=37;ec=37]] {{\nint privateField = 1;\n}}
  // edit@qf1 [[sc=3;ec=32]] {{}}
  private int privateField = 1; // Noncompliant [[sc=15;ec=27;quickfixes=qf1]]
  public int useLocally(int value) {
    this.privateField = 42;
    return privateField * value;
  }

  class InitializedInMethod {

    // fix@qf2 {{Remove the "privateList" field and declare it as a local variable in the relevant method}}
    // edit@qf2 [[sl=+2;el=+2;sc=25;ec=25]] {{\nList<Integer> privateList;\n}}
    // edit@qf2 [[sc=5;ec=39]] {{}}
    private List<Integer> privateList; // Noncompliant [[sc=27;ec=38;quickfixes=qf2]]

    int onlyUsedHere() {
      privateList = new ArrayList<>(42);
      return privateList.size();
    }
  }

}
