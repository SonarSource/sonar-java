/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.checks.verifier.internal.InternalCheckVerifier;
import org.sonar.java.checks.verifier.internal.QuickFixExpectation;

import static org.sonar.java.checks.verifier.TestUtils.testSourcesPath;

class DateFormatWeekYearCheckTest {

  private final String prefix = "import java.text.SimpleDateFormat;\n"
                              + "class A {\n";
  private final String suffix = "\n}";

  List<QuickFixExpectation> quickFixes = Arrays.asList(
    new QuickFixExpectation(prefix, suffix)
      .setBefore("String result = new SimpleDateFormat(\"YYYY/MM/dd\").format(\"42\");")
       .setAfter("String result = new SimpleDateFormat(\"yyyy/MM/dd\").format(\"42\");"),
    new QuickFixExpectation(prefix, suffix)
      .setBefore("String result = new SimpleDateFormat(\"YYYYYYY/MM/dd\").format(\"42\");")
       .setAfter("String result = new SimpleDateFormat(\"yyyyyyy/MM/dd\").format(\"42\");")
  );

  @Test
  void test() {
    ((InternalCheckVerifier) CheckVerifier.newVerifier())
      .onFile(testSourcesPath("/checks/DateFormatWeekYearCheck.java"))
      .withCheck(new DateFormatWeekYearCheck())
      .withQuickFixes(quickFixes, QuickFixHelper::quickFixApplicator)
      .verifyIssues();
  }
}
