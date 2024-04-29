package checks.regex;

import org.hibernate.validator.constraints.URL;

public class ReluctantQuantifierCheckSample {

  @URL(regexp = "<.+?>") // Noncompliant {{Replace this use of a reluctant quantifier with "[^>]++".}}
//                ^^^
  String url;

  void noncompliant(String str) {
    str.matches("<.+?>"); // Noncompliant {{Replace this use of a reluctant quantifier with "[^>]++".}}
//                ^^^
    str.matches("<\\S+?>"); // Noncompliant {{Replace this use of a reluctant quantifier with "[^>\\s]++".}}
//                ^^^^^
    str.matches("<\\D+?>"); // Noncompliant {{Replace this use of a reluctant quantifier with "[^>\\d]++".}}
//                ^^^^^
    str.matches("<\\W+?>"); // Noncompliant {{Replace this use of a reluctant quantifier with "[^>\\w]++".}}
//                ^^^^^
    str.matches("<.{2,5}?>"); // Noncompliant {{Replace this use of a reluctant quantifier with "[^>]{2,5}+".}}
//                ^^^^^^^
    str.matches("<\\S{2,5}?>"); // Noncompliant {{Replace this use of a reluctant quantifier with "[^>\\s]{2,5}+".}}
//                ^^^^^^^^^
    str.matches("<\\D{2,5}?>"); // Noncompliant {{Replace this use of a reluctant quantifier with "[^>\\d]{2,5}+".}}
//                ^^^^^^^^^
    str.matches("<\\W{2,5}?>"); // Noncompliant {{Replace this use of a reluctant quantifier with "[^>\\w]{2,5}+".}}
//                ^^^^^^^^^

    str.matches("<.{2,}?>"); // Noncompliant {{Replace this use of a reluctant quantifier with "[^>]{2,}+".}}
//                ^^^^^^
    str.matches("\".*?\""); // Noncompliant {{Replace this use of a reluctant quantifier with "[^\"]*+".}}
//                 ^^^
    str.matches(".*?\\w"); // Noncompliant {{Replace this use of a reluctant quantifier with "\\W*+".}}
//               ^^^
    str.matches(".*?\\W"); // Noncompliant {{Replace this use of a reluctant quantifier with "\\w*+".}}
//               ^^^
    str.matches(".*?\\p{L}"); // Noncompliant {{Replace this use of a reluctant quantifier with "\\P{L}*+".}}
//               ^^^
    str.matches(".*?\\P{L}"); // Noncompliant {{Replace this use of a reluctant quantifier with "\\p{L}*+".}}
//               ^^^
    str.matches("\\[.*?\\]"); // Noncompliant {{Replace this use of a reluctant quantifier with "[^\\]]*+".}}
//                  ^^^
    str.matches(".+?[abc]"); // Noncompliant {{Replace this use of a reluctant quantifier with "[^abc]++".}}
//               ^^^
    str.matches("(?-U:\\s)*?\\S");
    str.matches("(?U:\\s)*?\\S"); // Noncompliant {{Replace this use of a reluctant quantifier with "[\\s\\S]*+".}}
//               ^^^^^^^^^^
    str.matches("(?U:a|\\s)*?\\S");
    str.matches("\\S*?\\s");
    str.matches("\\S*?(?-U:\\s)");
    str.matches("\\S*?(?U:\\s)"); // Noncompliant {{Replace this use of a reluctant quantifier with "[\\S\\s]*+".}}
//               ^^^^^
    str.matches("\\S*?(?U)\\s"); // Noncompliant {{Replace this use of a reluctant quantifier with "[\\S\\s]*+".}}
//               ^^^^^

    // coverage
    str.matches("(?:(?m))*?a");
    str.matches("(?:(?m:.))*?(?:(?m))");

    // This replacement might not be equivalent in case of full match, but is equivalent in case of split
    str.matches(".+?[^abc]"); // Noncompliant {{Replace this use of a reluctant quantifier with "[abc]++".}}
//               ^^^
    str.matches(".+?\\x{1F4A9}"); // Noncompliant {{Replace this use of a reluctant quantifier with "[^\\x{1F4A9}]++".}}
//               ^^^
    str.matches("<abc.*?>"); // Noncompliant {{Replace this use of a reluctant quantifier with "[^>]*+".}}
//                   ^^^
    str.matches("<.+?>|otherstuff"); // Noncompliant {{Replace this use of a reluctant quantifier with "[^>]++".}}
//                ^^^
    str.matches("(<.+?>)*"); // Noncompliant {{Replace this use of a reluctant quantifier with "[^>]++".}}
//                 ^^^
    str.matches("\\S+?[abc]"); // Noncompliant {{Replace this use of a reluctant quantifier with "[^abc\\s]++".}}
//               ^^^^^
    str.matches("\\D+?[abc]"); // Noncompliant {{Replace this use of a reluctant quantifier with "[^abc\\d]++".}}
//               ^^^^^
    str.matches("\\w+?[abc]"); // Noncompliant {{Replace this use of a reluctant quantifier with "[^abc\\W]++".}}
//               ^^^^^
    str.matches("\\S*?[abc]"); // Noncompliant {{Replace this use of a reluctant quantifier with "[^abc\\s]*+".}}
//               ^^^^^
    str.matches("\\D*?[abc]"); // Noncompliant {{Replace this use of a reluctant quantifier with "[^abc\\d]*+".}}
//               ^^^^^
    str.matches("\\w*?[abc]"); // Noncompliant {{Replace this use of a reluctant quantifier with "[^abc\\W]*+".}}
//               ^^^^^
    str.matches("\\S+?[^abc]"); // Noncompliant {{Replace this use of a reluctant quantifier with "[abc\\S]++".}}
//               ^^^^^
    str.matches("\\s+?[^abc]"); // Noncompliant {{Replace this use of a reluctant quantifier with "[abc\\s]++".}}
//               ^^^^^
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
