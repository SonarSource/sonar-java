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
package org.sonar.java.model.springcontext;

import java.util.Optional;
import java.util.function.Function;
import org.sonar.java.model.DefaultModuleScannerContext;
import org.sonar.plugins.java.api.DependencyVersionAware;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.Version;
import org.sonar.plugins.java.api.internal.EndOfAnalysis;

/**
 * Base class for visitors that need to gather data in the SpringContextModel at the end of the analysis.
 * Extending classes will have to implement the AST visitor pattern, gather relevant spring-related data, and store it
 * in the SpringContextModel at the of a module analysis.
 *
 * <p>All gatherers are skipped when none of {@code spring-context}, {@code spring-beans},
 * {@code spring-boot-starter}, or {@code spring-boot-starter-web} is present in the module classpath.
 *
 * <p>Extends {@link IssuableSubscriptionVisitor} so that {@link #leaveFile} is invoked by
 * {@code IssuableSubscriptionVisitorsRunner} after each file. This is required for per-file cache
 * writes: without it, cache entries are never stored and {@link #scanWithoutParsing} always misses,
 * preventing unchanged files from being skipped in incremental analyses.
 */
public abstract class SpringContextModelGatherer extends IssuableSubscriptionVisitor implements EndOfAnalysis, DependencyVersionAware {

  @Override
  public boolean isCompatibleWithDependencies(Function<String, Optional<Version>> dependencyFinder) {
    return dependencyFinder.apply("spring-context")
      .or(() -> dependencyFinder.apply("spring-beans"))
      .or(() -> dependencyFinder.apply("spring-boot-starter"))
      .or(() -> dependencyFinder.apply("spring-boot-starter-web"))
      .isPresent();
  }

  @Override
  public final void endOfAnalysis(ModuleScannerContext context) {
    var defaultModuleContext = (DefaultModuleScannerContext) context;
    gatherSpringContextData(context, defaultModuleContext.getSpringContextModel());
  }

  /**
   * Method called at the end of the analysis of a module, allowing to store gathered data in the SpringContextModel.
   */
  public abstract void gatherSpringContextData(ModuleScannerContext context, SpringContextModel springContextModel);

}
