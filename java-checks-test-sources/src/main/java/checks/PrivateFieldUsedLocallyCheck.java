package checks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PrivateFieldUsedLocallyCheck {

  class UseLocallyWithThis {

    // fix@qf1 {{Move declaration to the relevant method}}
    // edit@qf1 [[sl=+3;el=+3;sc=7;ec=12]] {{}}
    // edit@qf1 [[sl=+4;el=+4;sc=14;ec=19]] {{}}
    // edit@qf1 [[sl=+2;el=+2;sc=39;ec=39]] {{\n      int privateField = 1;}}
    // edit@qf1 [[sc=5;ec=34]] {{}}
    private int privateField = 1; // Noncompliant [[sc=17;ec=29;quickfixes=qf1]]

    public int useLocally(int value) {
      this.privateField = 42;
      return this.privateField * value;
    }
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

  class ParameterAmbiguity {
    private int parameter; // Noncompliant [[sc=17;ec=26;quickfixes=!]]
    int useLocally(int parameter) {
      this.parameter = parameter;
      return this.parameter * 2;
    }
  }

  class LocalVariableAmbiguity {
    private int localVariable; // Noncompliant [[sc=17;ec=30;quickfixes=!]]

    int useLocally(int parameter) {
      int localVariable = 12;
      this.localVariable = localVariable;
      return this.localVariable * parameter;
    }
  }

  class NoIssueRaisedWhenOnlyInConstructor {
    private int arg; // Compliant because we do not look into constructors

    NoIssueRaisedWhenOnlyInConstructor(int b) {
      arg = b;
      System.out.println(arg);
    }
  }

}
