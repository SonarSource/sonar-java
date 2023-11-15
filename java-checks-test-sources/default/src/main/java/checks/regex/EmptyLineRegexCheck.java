package checks.regex;

import java.util.regex.Pattern;

public class EmptyLineRegexCheck {

  private static final int MY_FLAG = 0x10;

  private static final String MY_FIELD_STRING = "";

  void non_compliant_pattern_assigned(String str) {
    Pattern p1 = Pattern.compile("^$", Pattern.MULTILINE); // Noncompliant [[sc=34;ec=38;secondary=13,14]] {{Remove MULTILINE mode or change the regex.}}
    p1.matcher(str).find();
    p1.matcher((str)).find();

    Pattern p2 = Pattern.compile("(?m)^$"); // Noncompliant [[sc=34;ec=42]]
    p2.matcher(str).find();

    Pattern p3 = Pattern.compile("(?m)^$", Pattern.MULTILINE); // Noncompliant
    p3.matcher(str).find();
  }

  void non_compliant_pattern_directly_used(String str) {
    Pattern.compile("^$", Pattern.MULTILINE).matcher(str).find(); // Noncompliant [[sc=21;ec=25]]
    Pattern.compile("(^$)", Pattern.MULTILINE).matcher(str).find(); // Noncompliant
    Pattern.compile("(?:^$)", Pattern.MULTILINE).matcher(str).find(); // Noncompliant
    Pattern.compile("(?m)^$").matcher(str).find(); // Noncompliant
    Pattern.compile("(?m)(?x)^$").matcher(str).find(); // Noncompliant
    Pattern.compile("(?m)(^$)").matcher(str).find(); // Noncompliant
    Pattern.compile("(?m)^$", Pattern.MULTILINE).matcher(str).find(); // Noncompliant
    Pattern.compile("(?mx)^$").matcher(str).find(); // Noncompliant
    Pattern.compile("(?mi)^$").matcher(str).find(); // Noncompliant
    Pattern.compile("(?m:^$)").matcher(str).find(); // Noncompliant
    Pattern.compile("^$", Pattern.MULTILINE | Pattern.COMMENTS).matcher(str).find(); // Noncompliant
    Pattern.compile("^ $", Pattern.MULTILINE | Pattern.COMMENTS).matcher(str).find(); // Noncompliant
    Pattern.compile("^$|empty", Pattern.MULTILINE).matcher(str).find(); // Noncompliant
    Pattern.compile("(e)(^$)|(?m)^$").matcher(str).find(); // Noncompliant
  }

  void nonCompliantOnString(String str) {
    Pattern.compile("^$", Pattern.MULTILINE).matcher("").find(); // Noncompliant

    Pattern p1 = Pattern.compile("^$", Pattern.MULTILINE); // Noncompliant [[secondary=45]]
    boolean b1 = p1.matcher("notEmpty").find();
    boolean b2 = p1.matcher("").find();
  }

  void not_used_in_problematic_situations(String str) {
    Pattern p1 = Pattern.compile("^$"); // Compliant, not a multiline
    boolean b1 = p1.matcher(str).find();

    Pattern p2 = Pattern.compile("^$", Pattern.LITERAL); // Compliant, not a multiline
    boolean b2 = p2.matcher(str).matches();

    Pattern p2_2 = Pattern.compile("^$", 0x10); // Compliant, not a multiline
    boolean b2_2 = p2_2.matcher(str).matches();

    Pattern p2_3 = Pattern.compile("^$", this.MY_FLAG); // Compliant, not a multiline
    boolean b2_3 = p2_3.matcher(str).matches();

    Pattern.compile("^$").matcher(str).find(); // Compliant, no multiline flag

    Pattern p3 = Pattern.compile("^$", Pattern.MULTILINE); // Compliant, not used with find
    boolean b3 = p3.matcher(str).matches();

    Pattern p4 = Pattern.compile("(?m)^$"); // Compliant, not used with find
    boolean b4 = p4.matcher(str).matches();

    Pattern p5 = Pattern.compile("regex", Pattern.MULTILINE); // Compliant, not empty line regex
    boolean b5 = p5.matcher(str).find();

    Pattern.compile("^$", Pattern.MULTILINE).matcher(str).matches(); // Compliant, not used with find

    Pattern.compile("^|$", Pattern.MULTILINE).matcher(str).find(); // Compliant
    Pattern.compile("e^|$e", Pattern.MULTILINE).matcher(str).find(); // Compliant
    Pattern.compile("^$e", Pattern.MULTILINE).matcher(str).find(); // Compliant
    Pattern.compile("^e$", Pattern.MULTILINE).matcher(str).find(); // Compliant
    Pattern.compile("e^$", Pattern.MULTILINE).matcher(str).find(); // Compliant
    Pattern.compile("e(^$)", Pattern.MULTILINE).matcher(str).find(); // Compliant
    Pattern.compile("ee(^$)", Pattern.MULTILINE).matcher(str).find(); // Compliant
    Pattern.compile("(^$)e", Pattern.MULTILINE).matcher(str).find(); // Compliant
    Pattern.compile("(^$)ee", Pattern.MULTILINE).matcher(str).find(); // Compliant
    Pattern.compile("(e)(^$)", Pattern.MULTILINE).matcher(str).find(); // Compliant
    Pattern.compile("(e)(^)($)", Pattern.MULTILINE).matcher(str).find(); // Compliant
    Pattern.compile("[a-c]^$", Pattern.MULTILINE).matcher(str).find(); // Compliant
    Pattern.compile("[a-c](^$)", Pattern.MULTILINE).matcher(str).find(); // Compliant
    Pattern.compile("e(?m:^$)", Pattern.MULTILINE).matcher(str).find(); // Compliant
    Pattern.compile("ee(?m:^$)", Pattern.MULTILINE).matcher(str).find(); // Compliant
    Pattern.compile("(?m:e^)(?m:$e)", Pattern.MULTILINE).matcher(str).find(); // Compliant
    Pattern.compile("(?m)(e^$)").matcher(str).find(); // Compliant
    Pattern.compile("^^", Pattern.MULTILINE).matcher(str).find(); // Compliant
    Pattern.compile("$$", Pattern.MULTILINE).matcher(str).find(); // Compliant
    Pattern.compile("$\\B", Pattern.MULTILINE).matcher(str).find(); // Compliant
    Pattern.compile("(?m:(?-m:^$))").matcher(str).find(); // Compliant

    // 3 FN, we don't expect anyone writing this.
    Pattern.compile("(?:^)(?:$)", Pattern.MULTILINE).matcher(str).find(); // FN
    Pattern.compile("(?m:^)(?m:$)").matcher(str).find(); // FN
    Pattern.compile("(^)($)", Pattern.MULTILINE).matcher(str).find(); // FN
  }

  void tested_for_emptiness(String str) {
    Pattern p4 = Pattern.compile("(?m)^$"); // Compliant, tested for emptiness
    boolean b4 = p4.matcher(str).find() || str.isEmpty();
  }

  boolean tested_for_emptiness_2(String str) {
    Pattern p4 = Pattern.compile("(?m)^$"); // Compliant, tested for emptiness
    if (str.isEmpty()) {
      return true;
    }
    return p4.matcher(str).find();
  }

  boolean tested_for_emptiness_3(String str) {
    if (str.isEmpty()) {
      return true;
    }
    return Pattern.compile("(?m)^$").matcher(str).find();
  }

  boolean tested_for_emptiness_4(String str) {
    Pattern p4 = Pattern.compile("(?m)^$"); // FN, we consider any test for emptiness to be compliant
    if (str.isEmpty()) {
      System.out.println("str is empty!");
    }
    return p4.matcher(str).find();
  }

  boolean not_tested_for_emptiness(String str1, String str2) {
    Pattern p4 = Pattern.compile("(?m)^$"); // Noncompliant [[secondary=136]]
    if (str1.isEmpty()) {
      return false;
    }
    return p4.matcher(str1).find()
      && p4.matcher(str2).find();
  }

  void not_identifier(String str1) {
    // Compliant, don't report on fields to avoid FP.
    Pattern.compile("^$", Pattern.MULTILINE).matcher(MY_FIELD_STRING).find();
    Pattern.compile("^$", Pattern.MULTILINE).matcher(this.MY_FIELD_STRING).find();
  }

  void from_variable() {
    String str = getString();
    Pattern.compile("^$", Pattern.MULTILINE).matcher(str).find(); // Noncompliant
  }

  void from_variable_compliant() {
    String str = getString();
    if (str.isEmpty()) {
      return;
    }
    Pattern.compile("^$", Pattern.MULTILINE).matcher(str).find(); // Compliant
  }

  void in_replace(String str) {
    String s1 = str.replaceAll("(?m)^$", "Empty"); // Noncompliant [[sc=32;ec=40]]
    String s2 = str.replaceAll("^$", "Empty"); // Compliant
    String s3 = "".replaceAll("(?m)^$", "Empty"); // Noncompliant
    String s4 = (str).replaceAll("(?m)^$", "Empty"); // Noncompliant

    String s5 = str.replaceFirst("(?m)^$", "Empty"); // Noncompliant
    String s6 = str.replaceFirst("^$", "Empty"); // Compliant
    String s7 = "".replaceFirst("(?m)^$", "Empty"); // Noncompliant
    String s8 = (str).replaceFirst("(?m)^$", "Empty"); // Noncompliant
  }

  void in_replace_compliant(String str) {
    if (str.isEmpty()) {
      return;
    }
    String s1 = str.replaceAll("(?m)^$", "Empty"); // Compliant
    String s2 = str.replaceFirst("(?m)^$", "Empty"); // Compliant
    String s3 = (str).replaceAll("(?m)^$", "Empty"); // Compliant
  }

  void in_replace_all_compliant_2(String str) {
    String s1 = str.isEmpty() ? "Empty" : str.replaceAll("(?m)^$", "Empty"); // Compliant
    String s2 = str.isEmpty() || str.substring(1).equals("") ? "Empty" : str.replaceAll("(?m)^$", "Empty"); // Compliant
  }

  void in_matches(String str) {
    // When used in other context (with matches), mistakes are still possible, but we are not supporting it as it is really unlikely to happen.
    boolean b = str.matches("(?m).*^$.*"); // Compliant, FN
    Pattern.matches("(?m).*^$.*", str);
  }

  String getString() {
    return "";
  }

  @javax.validation.constraints.Pattern(regexp = "^$") // ignored
  String pattern;

}
