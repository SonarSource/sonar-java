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
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.java.ast.visitors.MethodHelper;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.List;

@Rule(
  key = "S1201",
  priority = Priority.CRITICAL,
  tags={"pitfall"})
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class MethodNamedEqualsCheck extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    MethodHelper.subscribe(this);
  }

  @Override
  public void visitNode(AstNode node) {
    MethodHelper methodHelper = new MethodHelper(node);

    if ("equals".equalsIgnoreCase(methodHelper.getName().getTokenOriginalValue()) && !hasSingleObjectParameter(methodHelper)) {
      getContext().createLineViolation(this, "Either override Object.equals(Object), or totally rename the method to prevent any confusion.", methodHelper.getName());
    }
  }

  private static boolean hasSingleObjectParameter(MethodHelper methodHelper) {
    List<AstNode> parameters = methodHelper.getParameters();
    if (parameters.size() != 1) {
      return false;
    }

    return isObjectType(parameters.get(0).getFirstChild(JavaGrammar.TYPE));
  }

  private static boolean isObjectType(AstNode node) {
    return AstNodeTokensMatcher.matches(node, "Object") ||
      AstNodeTokensMatcher.matches(node, "java.lang.Object");
  }

}
