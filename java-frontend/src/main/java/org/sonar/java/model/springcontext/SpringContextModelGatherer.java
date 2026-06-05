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

import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.model.DefaultModuleScannerContext;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.internal.EndOfAnalysis;

/**
 * Base class for visitors that need to gather data in the SpringContextModel at the end of the analysis.
 * Extending classes will have to implement the AST visitor pattern, gather relevant spring-related data, and store it
 * in the SpringContextModel at the of a module analysis.
 */
public abstract class SpringContextModelGatherer extends SubscriptionVisitor implements EndOfAnalysis {

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
