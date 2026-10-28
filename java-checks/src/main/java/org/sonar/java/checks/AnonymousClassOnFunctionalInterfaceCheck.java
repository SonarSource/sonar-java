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
package org.sonar.java.checks;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.AnonymousClassToLambdaUtils;
import org.sonar.plugins.java.api.DependencyVersionAware;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.Version;

@Rule(key = "S9357")
public class AnonymousClassOnFunctionalInterfaceCheck implements JavaFileScanner, JavaVersionAwareVisitor, DependencyVersionAware {

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  /**
   * GWT supports lambda expressions only since version 2.8: the rule is disabled for projects depending on an older GWT version.
   */
  @Override
  public boolean isCompatibleWithDependencies(Function<String, Optional<Version>> dependencyFinder) {
    return Stream.of("gwt-user", "gwt-dev", "gwt-servlet")
      .map(dependencyFinder)
      .flatMap(Optional::stream)
      .allMatch(version -> version.isGreaterThanOrEqualTo("2.8"));
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    AnonymousClassToLambdaUtils.scanForAnonymousClassConvertibleToLambda(this, context);
  }

}
