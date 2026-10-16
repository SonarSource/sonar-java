package checks;

public class StringIsBlankCheckSample_java10 {
  public boolean sample(String s) {
    boolean b;

    // `String.isBlank()` does not exist before Java 11, so these stay emptiness checks on the trimmed string

    b = s.strip().length() == 0; // Noncompliant {{Use "isEmpty()" to check whether a "String" is empty or not.}} [[quickfixes=qf1]]
    // fix@qf1 {{Replace with "isEmpty()"}}
    // edit@qf1 [[sc=19;ec=32]]{{isEmpty()}}

    b = s.strip().length() > 0; // Noncompliant {{Use "isEmpty()" to check whether a "String" is empty or not.}} [[quickfixes=qf2]]
    // fix@qf2 {{Replace with "isEmpty()"}}
    // edit@qf2 [[sc=9;ec=9]]{{!}}
    // edit@qf2 [[sc=19;ec=31]]{{isEmpty()}}

    b = s.trim().length() == 0; // Noncompliant {{Use "isEmpty()" to check whether a "String" is empty or not.}} [[quickfixes=qf3]]
    // fix@qf3 {{Replace with "isEmpty()"}}
    // edit@qf3 [[sc=18;ec=31]]{{isEmpty()}}

    b = 0 == s.trim().length(); // Noncompliant {{Use "isEmpty()" to check whether a "String" is empty or not.}} [[quickfixes=qf4]]
    // fix@qf4 {{Replace with "isEmpty()"}}
    // edit@qf4 [[sc=9;ec=14]]{{}}
    // edit@qf4 [[sc=23;ec=31]]{{isEmpty()}}

    return b;
  }
}