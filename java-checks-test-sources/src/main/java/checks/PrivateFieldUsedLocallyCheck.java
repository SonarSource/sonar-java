package checks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PrivateFieldUsedLocallyCheck {

  // fix@qf1 {{Move declaration to the relevant method}}
  // edit@qf1 [[sl=+2;el=+2;sc=37;ec=37]] {{\n    int privateField = 1;}}
  // edit@qf1 [[sc=3;ec=32]] {{}}
  private int privateField = 1; // Noncompliant [[sc=15;ec=27;quickfixes=qf1]]

  public int useLocally(int value) {
    this.privateField = 42;
    return privateField * value;
  }

  class InitializedInMethod {

    // fix@qf2 {{Move declaration to the relevant method}}
    // edit@qf2 [[sl=+2;el=+2;sc=25;ec=25]] {{\n      List<Integer> privateList;}}
    // edit@qf2 [[sc=5;ec=39]] {{}}
    private List<Integer> privateList; // Noncompliant [[sc=27;ec=38;quickfixes=qf2]]

    int onlyUsedHere() {
      privateList = new ArrayList<>(42);
      return privateList.size() + 1;
    }

  }

  class MultiLineDeclaration {
    // fix@qf3 {{Move declaration to the relevant method}};
    // edit@qf3 [[sl=+3;el=+3;sc=38;ec=38]] {{\n      String lowerCasedInput = "Hello" +\n      "World!";}}
    // edit@qf3 [[sl=+0;el=+1;sc=5;ec=16]] {{}}
    // Noncompliant@+1 [[sc=20;ec=35;quickfixes=qf3]]
    private String lowerCasedInput = "Hello" +
      "World!";

    String useLocally(String input) {
      lowerCasedInput = input.toLowerCase(Locale.ROOT);
      return input + " -> " + lowerCasedInput;
    }
  }

}
