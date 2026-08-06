package checks.regex;

public class ReluctantQuantifierCheckSampleWithoutSemantic {

  String url;

  void noncompliant(String str) {
    str.matches("<.+?>"); // Noncompliant

    str.matches("<\\S+?>"); // Noncompliant

    str.matches("<\\D+?>"); // Noncompliant

    str.matches("<\\W+?>"); // Noncompliant

    str.matches("<.{2,5}?>"); // Noncompliant

    str.matches("<\\S{2,5}?>"); // Noncompliant

    str.matches("<\\D{2,5}?>"); // Noncompliant

    str.matches("<\\W{2,5}?>"); // Noncompliant

    str.matches("<.{2,}?>"); // Noncompliant

    str.matches("\".*?\""); // Noncompliant

    str.matches(".*?\\w"); // Noncompliant

    str.matches(".*?\\W"); // Noncompliant

    str.matches(".*?\\p{L}"); // Noncompliant

    str.matches(".*?\\P{L}"); // Noncompliant

    str.matches("\\[.*?\\]"); // Noncompliant

    str.matches(".+?[abc]"); // Noncompliant

    str.matches("(?-U:\\s)*?\\S");
    str.matches("(?U:\\s)*?\\S"); // Noncompliant

    str.matches("(?U:a|\\s)*?\\S");
    str.matches("\\S*?\\s");
    str.matches("\\S*?(?-U:\\s)");
    str.matches("\\S*?(?U:\\s)"); // Noncompliant

    str.matches("\\S*?(?U)\\s"); // Noncompliant

    str.matches("(?:(?m))*?a");
    str.matches("(?:(?m:.))*?(?:(?m))");

    // This replacement might not be equivalent in case of full match, but is equivalent in case of split
    str.matches(".+?[^abc]"); // Noncompliant

    str.matches(".+?\\x{1F4A9}"); // Noncompliant

    str.matches("<abc.*?>"); // Noncompliant

    str.matches("<.+?>|otherstuff"); // Noncompliant

    str.matches("(<.+?>)*"); // Noncompliant

    str.matches("\\S+?[abc]"); // Noncompliant

    str.matches("\\D+?[abc]"); // Noncompliant

    str.matches("\\w+?[abc]"); // Noncompliant

    str.matches("\\S*?[abc]"); // Noncompliant

    str.matches("\\D*?[abc]"); // Noncompliant

    str.matches("\\w*?[abc]"); // Noncompliant

    str.matches("\\S+?[^abc]"); // Noncompliant

    str.matches("\\s+?[^abc]"); // Noncompliant

  }

  void compliant(String str) {
    str.matches("<[^>]++>");
    str.matches("<[^>]+>");
    str.matches("<[^>]+?>");
    str.matches("<.{42}?>"); // Adding a ? to a fixed quantifier is pointless, but also doesn't cause any backtracking issues
    str.matches("<.+>");
    str.matches("<.++>");
    str.matches("<--.?-->");
    str.matches("<--.+?-->");
    str.matches("<--.*?-->");
    str.matches("/\\*.?\\*/");
    str.matches("<[^>]+>?");
    str.matches("");
    str.matches(".*?(?:a|b|c)"); // Alternatives are currently not covered even if they contain only single characters
  }
  
  void no_intersection(String str) {
    str.matches("<\\d+?>");
    str.matches("<\\s+?>");
    str.matches("<\\w+?>");

    str.matches("<\\s{2,5}?>");
    str.matches("<\\d{2,5}?>");
    str.matches("<\\w{2,5}?>");

    str.matches("\\d+?[abc]");
    str.matches("\\s+?[abc]");
    str.matches("\\W+?[abc]");

    str.matches("\\W*?[abc]");
    str.matches("\\s*?[abc]");
    str.matches("\\d*?[abc]");

    str.matches("\\d*?\\p{L}");
    str.matches("\\d*?\\P{L}"); // There is an intersection but we currently do not support p{.} and P{.}

    str.matches("\\p{L}*?\\D"); // There is an intersection but we currently do not support p{.} and P{.}
    str.matches("\\P{L}*?\\d"); // There is an intersection but we currently do not support p{.} and P{.}
  }
}
