package checks.regex;

public class ReluctantQuantifierCheck {

  void noncompliant(String str) {
    str.matches("<.+?>"); // Noncompliant [[sc=19;ec=22]] {{Replace this use of a reluctant quantifier with "[^>]++".}}
    str.matches("<.{2,5}?>"); // Noncompliant [[sc=19;ec=26]] {{Replace this use of a reluctant quantifier with "[^>]{2,5}+".}}
    str.matches("<.{2,}?>"); // Noncompliant [[sc=19;ec=25]] {{Replace this use of a reluctant quantifier with "[^>]{2,}+".}}
    str.matches("\".*?\""); // Noncompliant [[sc=20;ec=23]] {{Replace this use of a reluctant quantifier with "[^\"]*+".}}
    str.matches(".*?\\w"); // Noncompliant [[sc=18;ec=21]] {{Replace this use of a reluctant quantifier with "\\W*+".}}
    str.matches(".*?\\W"); // Noncompliant [[sc=18;ec=21]] {{Replace this use of a reluctant quantifier with "\\w*+".}}
    str.matches("\\[.*?\\]"); // Noncompliant [[sc=21;ec=24]] {{Replace this use of a reluctant quantifier with "[^\\]]*+".}}
    str.matches(".+?[abc]"); // Noncompliant [[sc=18;ec=21]] {{Replace this use of a reluctant quantifier with "[^abc]++".}}
    str.matches(".+?[^abc]"); // Noncompliant [[sc=18;ec=21]] {{Replace this use of a reluctant quantifier with "[abc]++".}}
    str.matches("<abc.*?>"); // Noncompliant [[sc=22;ec=25]] {{Replace this use of a reluctant quantifier with "[^>]*+".}}
    str.matches("<.+?>|otherstuff"); // Noncompliant [[sc=19;ec=22]] {{Replace this use of a reluctant quantifier with "[^>]++".}}
    str.matches("(<.+?>)*"); // Noncompliant [[sc=20;ec=23]] {{Replace this use of a reluctant quantifier with "[^>]++".}}
  }

  void compliant(String str) {
    str.matches("<[^>]++>");
    str.matches("<[^>]+>");
    str.matches("<[^>]+?>");
    str.matches("<\\w+?>");
    str.matches("<.{42}?>"); // Adding a ? to a fixed quantifier is pointless, but also doesn't cause any backtracking issues
    str.matches("<.+>");
    str.matches("<.++>");
    str.matches("<--.?-->");
    str.matches("/\\*.?\\*/");
    str.matches("<[^>]+>?");
  }
}
