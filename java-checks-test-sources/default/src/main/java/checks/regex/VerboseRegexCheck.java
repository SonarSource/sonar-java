package checks.regex;

import java.util.regex.Pattern;

public class VerboseRegexCheck {
  void nonCompliant() {
    Pattern.compile("[0-9]"); // Noncompliant {{Use concise character class syntax '\\d' instead of '[0-9]'.}}
    Pattern.compile("[^0-9]"); // Noncompliant  {{Use concise character class syntax '\\D' instead of '[^0-9]'.}}
    Pattern.compile("something[0-9]somethingElse"); // Noncompliant [[sc=31;ec=36]] {{Use concise character class syntax '\\d' instead of '[0-9]'.}}

    Pattern.compile("[A-Za-z0-9_]");    // Noncompliant  {{Use concise character class syntax '\\w' instead of '[A-Za-z0-9_]'.}}
    Pattern.compile("[0-9_A-Za-z]");    // Noncompliant
    Pattern.compile("[^A-Za-z0-9_]");   // Noncompliant  {{Use concise character class syntax '\\W' instead of '[^A-Za-z0-9_]'.}}
    Pattern.compile("[^0-9_A-Za-z]");   // Noncompliant

    Pattern.compile("x{0,1}");          // Noncompliant {{Use concise quantifier syntax '?' instead of '{0,1}'.}}
    Pattern.compile("x{0,1}?");         // Noncompliant {{Use concise quantifier syntax '?' instead of '{0,1}'.}}
    Pattern.compile("x{0,}");           // Noncompliant {{Use concise quantifier syntax '*' instead of '{0,}'.}}
    Pattern.compile("x{0,}?");          // Noncompliant {{Use concise quantifier syntax '*' instead of '{0,}'.}}
    Pattern.compile("x{1,}");           // Noncompliant {{Use concise quantifier syntax '+' instead of '{1,}'.}}
    Pattern.compile("x{1,}?");          // Noncompliant {{Use concise quantifier syntax '+' instead of '{1,}'.}}
    Pattern.compile("x{2,2}");          // Noncompliant {{Use concise quantifier syntax '{2}' instead of '{2,2}'.}}
    Pattern.compile("x{2,2}?");         // Noncompliant {{Use concise quantifier syntax '{2}' instead of '{2,2}'.}}

    Pattern.compile("[\\W\\w]"); // Noncompliant {{Use concise character class syntax '.' instead of '[\\W\\w]'.}}
    Pattern.compile("[\\d\\D]"); // Noncompliant {{Use concise character class syntax '.' instead of '[\\d\\D]'.}}
    Pattern.compile("[\\s\\S]", Pattern.DOTALL); // Noncompliant {{Use concise character class syntax '.' instead of '[\\s\\S]'.}}
    Pattern.compile("(?s)[\\s\\S]"); // Noncompliant {{Use concise character class syntax '.' instead of '[\\s\\S]'.}}
  }

  void compliant() {
    Pattern.compile("\\d");
    Pattern.compile("\\D");
    Pattern.compile("\\w");
    Pattern.compile(".");
    Pattern.compile("a*");

    Pattern.compile("[x]");
    Pattern.compile("[12]");
    Pattern.compile("[1234]");
    Pattern.compile("[1-3]");
    Pattern.compile("[1-9abc]");
    Pattern.compile("[1-9a-bAB]");
    Pattern.compile("[1-9a-bA-Z!]");
    Pattern.compile("[1-2[a][b][c]]");
    Pattern.compile("[0-9[a][b][c]]");
    Pattern.compile("[0-9a-z[b][c]]");
    Pattern.compile("[0-9a-zA-Z[c]]");
    Pattern.compile("x?");
    Pattern.compile("x*");
    Pattern.compile("x+");
    Pattern.compile("x{2}");
    Pattern.compile("[\\s\\S]"); // Compliant without flag "Pattern.DOTALL"
    Pattern.compile("[\\w\\S]");
    Pattern.compile("[\\d\\S]");
    Pattern.compile("[\\s\\d]");
  }
}
