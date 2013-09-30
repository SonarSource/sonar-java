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
import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.java.ast.visitors.MethodHelper;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "S1210",
  priority = Priority.CRITICAL)
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class EqualsNotOverridenWithCompareToCheck extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribeTo(JavaGrammar.CLASS_BODY);
    subscribeTo(JavaGrammar.ENUM_DECLARATION);
  }

  @Override
  public void visitNode(AstNode node) {
    MethodHelper compareToMethod = null;
    boolean hasEquals = false;

    for (MethodHelper method : MethodHelper.getMethods(node)) {
      System.out.println("handling: " + method);
      if (method.getParameters().size() == 1) {
        if ("compareTo".equals(method.getName().getTokenOriginalValue())) {
          compareToMethod = method;
        } else if ("equals".equals(method.getName().getTokenOriginalValue())) {
          hasEquals = true;
        }
      }
    }

    if (compareToMethod != null && !hasEquals) {
      getContext().createLineViolation(this, "Override \"equals(Object obj)\" to comply with the contract of the \"compareTo(T o)\" method", compareToMethod.getName());
    }
  }

}
