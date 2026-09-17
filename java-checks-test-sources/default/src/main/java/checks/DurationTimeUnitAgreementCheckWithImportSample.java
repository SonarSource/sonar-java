/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package checks;

import java.time.Duration;
import java.util.concurrent.Future;
import static java.util.concurrent.TimeUnit.SECONDS;

class DurationTimeUnitAgreementCheckWithImportSample {

  void test(Future<String> f, Duration d) throws Exception {
    f.get(d.toMillis(), SECONDS); // Noncompliant [[quickfixes=qf1]]
//                      ^^^^^^^
    // fix@qf1 {{Change TimeUnit to "MILLISECONDS"}}
    // edit@qf1 [[sc=25;ec=32]] {{TimeUnit.MILLISECONDS}}
    // edit@qf1 [[sl=20;sc=36;el=20;ec=36]] {{\nimport java.util.concurrent.TimeUnit;}}
  }
}
