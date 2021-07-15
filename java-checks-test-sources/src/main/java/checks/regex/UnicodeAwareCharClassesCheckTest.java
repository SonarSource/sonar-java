/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package checks.regex;

import java.util.regex.Pattern;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.URL;
import javax.validation.constraints.Pattern.Flag;

public class UnicodeAwareCharClassesCheckTest {

  @Email(regexp = "[a-z]") // Noncompliant [[sc=21;ec=24]] {{Replace this character range with a Unicode-aware character class.}}
  String email;

  @URL(regexp = "\\p{Lower}", flags = jakarta.validation.constraints.Pattern.Flag.DOTALL) // Noncompliant [[sc=4;ec=7;secondary=32]] {{Enable the "(?U)" flag or use a Unicode-aware alternative.}}
  String url1;

  @URL(regexp = "(?U)\\p{Lower}") // Compliant
  String url2;

  @URL(regexp = "\\p{ASCII}") // Compliant
  String url3;

  void NoncompliantCharRanges() {
    Pattern.compile("[a-z]"); // Noncompliant [[sc=23;ec=26]] {{Replace this character range with a Unicode-aware character class.}}
    Pattern.compile("[A-Z]"); // Noncompliant
    Pattern.compile("[0-9a-z]"); // Noncompliant
    Pattern.compile("[abcA-Zdef]"); // Noncompliant
    Pattern.compile("[\\x{61}-\\x{7A}]"); // Noncompliant [[sc=23;ec=38]]
    Pattern.compile("[a-zA-Z]"); // Noncompliant [[sc=22;ec=30;secondary=47,47]] {{Replace these character ranges with Unicode-aware character classes.}}
    String regex = "[a-zA-Z]"; // Noncompliant
    Pattern.compile(regex + regex);
  }

  void NoncompliantPredefinedPosixClasses() {
    Pattern.compile("\\p{Lower}"); // Noncompliant [[sc=13;ec=20;secondary=53]] {{Enable the "UNICODE_CHARACTER_CLASS" flag or use a Unicode-aware alternative.}}
    Pattern.compile("\\p{Alnum}"); // Noncompliant
    Pattern.compile("\\p{Space}"); // Noncompliant
    Pattern.compile("\\s"); // Noncompliant
    Pattern.compile("\\S"); // Noncompliant
    Pattern.compile("\\w"); // Noncompliant
    Pattern.compile("\\W"); // Noncompliant
    Pattern.compile("\\s\\w\\p{Lower}"); // Noncompliant
    Pattern.compile("\\S\\p{Upper}\\w"); // Noncompliant
  }

  void compliantCharRanges() {
    Pattern.compile("[0-9]"); // Compliant: we do not consider digits
    Pattern.compile("[a-y]"); // Compliant: It appears a more restrictive range than simply 'all letters'
    Pattern.compile("[D-Z]");
    Pattern.compile("[\\x{1F600}-\\x{1F637}]");
  }

  void compliantPredefinedPosixClasses() {
    Pattern.compile("\\p{ASCII}");
    Pattern.compile("\\p{Cntrl}");
    Pattern.compile("\\p{Lower}", Pattern.UNICODE_CHARACTER_CLASS);
    Pattern.compile("(?U)\\p{Lower}");
    Pattern.compile("\\w", Pattern.UNICODE_CHARACTER_CLASS);
    Pattern.compile("(?U)\\w");
    Pattern.compile("(?U:\\w)");
    Pattern.compile("\\w", Pattern.CANON_EQ | Pattern.COMMENTS | Pattern.UNICODE_CHARACTER_CLASS | Pattern.UNIX_LINES);
    Pattern.compile("\\w((?U)\\w)\\w");
    Pattern.compile("\\w(?U:[a-y])\\w"); // Compliant. We assume the developer knows what they are doing if they are using unicode flags somewhere.
  }

}
