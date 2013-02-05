/*
 * Sonar Java
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
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.visitors.JavaAstCheck;

@Rule(key = "AvoidBreakOutsideSwitch", priority = Priority.MAJOR)
public class BreakCheck extends JavaAstCheck {

  @Override
  public void init() {
    subscribeTo(JavaKeyword.BREAK);
  }

  @Override
  public void visitNode(AstNode node) {
    if (node.getFirstAncestor(getContext().getGrammar().switchStatement) == null) {
      getContext().createLineViolation(this, "The 'break' branching statement prevents refactoring the source code to reduce the complexity.", node);
    }
  }

}
