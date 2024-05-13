package checks.regex;

public class RegexComplexityCheckWithThreshold1 {

  // Since this check is sensitive to comments, all comments that mark a regex as non-compliant or that explain
  // why it is (non-)compliant should not be on the same line as the regex or on the line before it.

  void compliant(String str) {
    str.matches("x*");

    String part1 = "x*";
    String part2 = "y+";
    str.matches(part1 + part2);
  }

  void nonCompliant(String str) {
 // Noncompliant@+2 {{Simplify this regular expression to reduce its complexity from 2 to the 1 allowed.}}
    str.matches(
      "x*y+"
    );
 // Noncompliant@+2 {{Simplify this regular expression to reduce its complexity from 2 to the 1 allowed.}}
    String pattern1 =
      "x*" +
        "y+";
 // Noncompliant@+2 {{Simplify this regular expression to reduce its complexity from 2 to the 1 allowed.}}
    String pattern2 =
      "a*" +
        "b+";
    str.matches(pattern1 + pattern2);
  }

  void compliantBecauseOfComments(String str) {
    str.matches("x*" + // lots of xs
      "y+"); // lots of ys

    String pattern1 =
      "x*" + // lots of xs
        "y+"; // lots of ys
    String pattern2 =
      "a*" + // lots of as
        "b+"; // lots of bs
    str.matches(pattern1 + pattern2);

    String pattern3 =
      /*
       * lots of xs
       */
      "x*" +
        /*
         * lots of ys
         */
        "y+";
    String pattern4 =
      // lots of as
      "a*" +
        // lots of bs
        "b+";
    str.matches(pattern3 + pattern4);

    String pattern5 =
      "x* # lots of xs" +
        "y+ # lots of ys";
    String pattern6 =
      "a* # lots of as" +
        "b+ # lots of bs";
    str.matches("(?x)" + pattern5 + pattern6);
  }

  void noncompliantDespiteComments(String str) {
 // Noncompliant@+3 {{Simplify this regular expression to reduce its complexity from 2 to the 1 allowed.}}
 // Noncompliant@+3 {{Simplify this regular expression to reduce its complexity from 2 to the 1 allowed.}}
    str.matches(
      "x*y+" + // lots of xs and ys
        "a*b+" // lots of as and bs
    );

 // Noncompliant@+3 {{Simplify this regular expression to reduce its complexity from 2 to the 1 allowed.}}
 // Noncompliant@+3 {{Simplify this regular expression to reduce its complexity from 2 to the 1 allowed.}}
    String pattern =
      "x*y+ # lots of xs and ys" +
        "a*b+ # lots of as and bs";
    str.matches("(?x)" + pattern);
  }

}
