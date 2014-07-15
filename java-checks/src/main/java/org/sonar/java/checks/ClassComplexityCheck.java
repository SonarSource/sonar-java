/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.sonar.sslr.api.AstNode;
import org.sonar.squidbridge.checks.ChecksHelper;
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.ast.api.JavaMetric;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.squidbridge.api.SourceClass;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(key = "ClassCyclomaticComplexity", priority = Priority.MAJOR,
  tags={"brain-overload"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class ClassComplexityCheck extends SquidCheck<LexerlessGrammar> {

  private static final int DEFAULT_MAX = 200;

  @RuleProperty(defaultValue = "" + DEFAULT_MAX)
  private int max = DEFAULT_MAX;

  @Override
  public void init() {
    subscribeTo(JavaGrammar.CLASS_DECLARATION);
  }

  @Override
  public void leaveNode(AstNode node) {
    SourceClass sourceClass = (SourceClass) getContext().peekSourceCode();
    int complexity = ChecksHelper.getRecursiveMeasureInt(sourceClass, JavaMetric.COMPLEXITY);
    if (complexity > max) {
      getContext().createLineViolation(this,
        "The Cyclomatic Complexity of this class is {0,number,integer} which is greater than {1,number,integer} authorized.",
        node,
        complexity,
        max);
    }
  }

  public void setMax(int max) {
    this.max = max;
  }

}
