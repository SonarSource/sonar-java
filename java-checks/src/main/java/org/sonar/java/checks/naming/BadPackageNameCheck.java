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
package org.sonar.java.checks.naming;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.model.PackageUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.internal.EndOfAnalysis;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

@DeprecatedRuleKey(ruleKey = "S00120", repositoryKey = "squid")
@Rule(key = "S120")
public class BadPackageNameCheck implements JavaFileScanner, EndOfAnalysis {

  private static final String DEFAULT_FORMAT = "^[a-z_]+(\\.[a-z_][a-z0-9_]*)*$";

  @RuleProperty(
    key = "format",
    description = "Regular expression used to check the package names against.",
    defaultValue = DEFAULT_FORMAT)
  public String format = DEFAULT_FORMAT;

  private Pattern pattern = null;
  private final Set<String> badPackageNames = new HashSet<>();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    if (pattern == null) {
      pattern = Pattern.compile(format, Pattern.DOTALL);
    }
    var packageDeclaration = context.getTree().packageDeclaration();
    if (packageDeclaration != null) {
      String name = PackageUtils.packageName(packageDeclaration, ".");
      if (!pattern.matcher(name).matches()) {
        badPackageNames.add(name);
      }
    }
  }

  @Override
  public void endOfAnalysis(ModuleScannerContext context) {
    for (String badPackageName : badPackageNames) {
      context.addIssueOnProject(this, "Rename package \"" + badPackageName + "\" to match the regular expression '" + format + "'.");
    }
  }
}
