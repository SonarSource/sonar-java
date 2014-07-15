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
import org.sonar.java.ast.visitors.MethodHelper;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "S1175",
  priority = Priority.MAJOR,
  tags={"pitfall"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class ObjectFinalizeOverloadedCheck extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    MethodHelper.subscribe(this);
  }

  @Override
  public void visitNode(AstNode node) {
    MethodHelper method = new MethodHelper(node);

    if ("finalize".equals(method.getName().getTokenOriginalValue()) && !method.getParameters().isEmpty()) {
      getContext().createLineViolation(this, "Rename this method to avoid any possible confusion with Object.finalize().", node);
    }
  }

}
