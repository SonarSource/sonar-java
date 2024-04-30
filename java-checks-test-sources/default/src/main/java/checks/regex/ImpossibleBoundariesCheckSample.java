package checks.regex;

import javax.validation.constraints.Email;
import java.util.regex.Pattern;

public class ImpossibleBoundariesCheckSample {

  @Email(regexp = "$USER") // Noncompliant {{Remove or replace this boundary that will never match because it appears before mandatory input.}}
//                 ^
  String email;

  void noncompliant(String str) {

    str.matches("$[a-z]^"); // Noncompliant {{Remove or replace this boundary that will never match because it appears before mandatory input.}} {{Remove or replace this boundary that will never match because it appears after mandatory input.}}
    str.matches("$[a-z]"); // Noncompliant {{Remove or replace this boundary that will never match because it appears before mandatory input.}}
//               ^
    str.matches("$(abc)"); // Noncompliant
//               ^
    str.matches("[a-z]^"); // Noncompliant
//                    ^
    str.matches("\\Z[a-z]"); // Noncompliant
//               ^^^
    str.matches("\\z[a-z]"); // Noncompliant
//               ^^^
    str.matches("[a-z]\\A"); // Noncompliant
//                    ^^^
    str.matches("($)a"); // Noncompliant
//                ^
    str.matches("a$|$a"); // Noncompliant
//                  ^
    str.matches("^a|a^"); // Noncompliant
//                   ^
    str.matches("a(b|^)"); // Noncompliant
//                   ^
    str.matches("(?=abc^)"); // Noncompliant
//                     ^
    str.matches("(?!abc^)"); // Noncompliant
//                     ^
    str.matches("abc(?=^abc)"); // Noncompliant
//                     ^
    str.matches("abc(?<=$abc)"); // Noncompliant
//                      ^
    str.matches("abc(?<=abc$)def"); // Noncompliant
//                         ^
    str.matches("abc(?<!abc$)def"); // Noncompliant
//                         ^
    str.matches("(?:abc(X|^))*Y?"); // Noncompliant
//                        ^
  }

  void probablyNonCompliant(String str) {
    str.matches("$.*"); // Noncompliant {{Remove or replace this boundary that can only match if the previous part matched the empty string because it appears before mandatory input.}}
    str.matches("$.?"); // Noncompliant {{Remove or replace this boundary that can only match if the previous part matched the empty string because it appears before mandatory input.}}

    str.matches("$a*"); // Noncompliant {{Remove or replace this boundary that can only match if the previous part matched the empty string because it appears before mandatory input.}}
    str.matches("$a?"); // Noncompliant {{Remove or replace this boundary that can only match if the previous part matched the empty string because it appears before mandatory input.}}
    str.matches("$[abc]*"); // Noncompliant {{Remove or replace this boundary that can only match if the previous part matched the empty string because it appears before mandatory input.}}
    str.matches("$[abc]?"); // Noncompliant {{Remove or replace this boundary that can only match if the previous part matched the empty string because it appears before mandatory input.}}

    str.matches(".*^"); // Noncompliant {{Remove or replace this boundary that can only match if the previous part matched the empty string because it appears after mandatory input.}}
    str.matches(".?^"); // Noncompliant {{Remove or replace this boundary that can only match if the previous part matched the empty string because it appears after mandatory input.}}

    str.matches("a*^"); // Noncompliant {{Remove or replace this boundary that can only match if the previous part matched the empty string because it appears after mandatory input.}}
    str.matches("a?^"); // Noncompliant {{Remove or replace this boundary that can only match if the previous part matched the empty string because it appears after mandatory input.}}
    str.matches("[abc]*^"); // Noncompliant {{Remove or replace this boundary that can only match if the previous part matched the empty string because it appears after mandatory input.}}
    str.matches("[abc]?^"); // Noncompliant {{Remove or replace this boundary that can only match if the previous part matched the empty string because it appears after mandatory input.}}

 // Noncompliant@+1
    str.matches("$.*^"); // Noncompliant
 // Noncompliant@+1
    str.matches("$.?^"); // Noncompliant
 // Noncompliant@+1
    str.matches("$a*^"); // Noncompliant
 // Noncompliant@+1
    str.matches("$a?^"); // Noncompliant
 // Noncompliant@+1
    str.matches("$[abc]*^"); // Noncompliant
 // Noncompliant@+1
    str.matches("$[abc]?^"); // Noncompliant
  }

  void compliant(String str) {
    str.matches("^[a-z]$");
    str.matches("^$");
    str.matches("^(?i)$");
    str.matches("^$(?i)");
    str.matches("^abc$|^def$");
    str.matches("(?i)^abc$");
    str.matches("()^abc$");
    str.matches("^abc$()");
    str.matches("^abc$\\b");
    str.matches("(?=abc)^abc$");
    str.matches("(?=^abc$)abc");
    str.matches("(?!^abc$)abc");
    str.matches("abc(?<=^abc$)");
    str.matches("^\\d$(?<!3)");
    str.matches("(?=$)");
    str.matches("(?i)(true)(?=(?:[^']|'[^']*')*$)");
    str.matches("(?:abc(X|$))*Y?");
    str.matches("(?:x*(Xab|^)abc)*Y?");
    Pattern.compile("^(0\\d{2}-\\d{8}(-\\d{1,4})?)|(0\\d{3}-\\d{7,8}(-\\d{1,4})?)$");
    Pattern.compile("^(((13[0-9])|(15([0-3]|[5-9]))|(18[0,5-9]))\\d{8})|(0\\d{2}-\\d{8})|(0\\d{3}-\\d{7})$");
    Pattern.compile("^((?<major>[0-9]{1,9})?(\\.(?<minor>[0-9]{1,9})(\\.$|[.-](?<micro>[0-9]{1,9}))?)?)([.-]?(?<qualifier>.+?))??([.-]redhat-(?<suffixversion>[0-9]{1,9}))?$");
  }

  void consideringNewLines() {
    Pattern.compile("a\\n^b$", Pattern.MULTILINE); // Compliant, matches "a\nb"
    Pattern.compile("(?m)a\\n^b$"); // Compliant
    Pattern.compile("a\\n^b$"); // Noncompliant

    // Even if the usage of "$" is suspicious and redundant, we do not raise an issue
    Pattern.compile("a$\nb", Pattern.MULTILINE); // Compliant, matches "a\nb"
    Pattern.compile("(?m)a$\nb"); // Compliant
    Pattern.compile("(?m)^1$\n2"); // Compliant
    Pattern.compile("a$\nb"); // Noncompliant

    Pattern.compile("^a$\r?\n?");
    Pattern.compile("^a$\r?\n?b"); // Noncompliant
    Pattern.compile("^a$.*", Pattern.DOTALL);
    Pattern.compile("^a$.*"); // Noncompliant
  }

}
