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
package org.sonar.plugins.java.api;

import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.java.annotations.Beta;
import org.sonarsource.api.sonarlint.SonarLintSide;

/**
 * This batch extension should be extended to provide the classes to be used to instantiate checks.
 * The register method has to be implemented and the registrarContext should register the repository keys.
 *
 * <pre>
 *   {@code
 *   public void register(RegistrarContext registrarContext) {
 *     registrarContext.registerClassesForRepository("RepositoryKey", listOfCheckClasses);
 *   }
 *   }
 * </pre>
 */
@Beta
@SonarLintSide
@ScannerSide
public interface CheckRegistrar {

  /**
   * This method is called during an analysis to get the classes to use to instantiate checks.
   * @param registrarContext the context that will be used by the java-plugin to retrieve the classes for checks.
   */
  void register(RegistrarContext registrarContext);

  /**
   * This method is called during the definition of the SonarJava rules, implementing it allows to register
   * rules in the same rule repository as SonarJava while its being defined.
   * @param context the context that will be used by the java-plugin to define the rules in its repository.
   * @param javaRepository the repository currently being defined
   */
  default void customRulesDefinition(RulesDefinition.Context context, RulesDefinition.NewRepository javaRepository) {

  }

  /**
   * This method is called during an analysis to register instantiated checks using the CheckFactory
   * @param registrarContext the context that will be used by the java-plugin to retrieve the classes for checks.
   * @param checkFactory the factory to be used to instantiate checks
   */
  default void register(RegistrarContext registrarContext, CheckFactory checkFactory) {
    register(registrarContext);
  }

}
