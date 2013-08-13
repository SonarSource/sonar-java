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
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

import javax.annotation.Nullable;

@Rule(
  key = "S1173",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class NamesTooShortCheck extends SquidCheck<LexerlessGrammar> {

  private boolean isExcluded;

  @Override
  public void init() {
    subscribeTo(JavaGrammar.FOR_INIT, JavaGrammar.CATCH_FORMAL_PARAMETER);

    subscribeTo(JavaGrammar.CLASS_DECLARATION);
    subscribeTo(JavaGrammar.INTERFACE_DECLARATION);
    subscribeTo(JavaGrammar.ENUM_DECLARATION);
    subscribeTo(JavaGrammar.ENUM_CONSTANT);
    subscribeTo(JavaGrammar.INTERFACE_METHOD_OR_FIELD_DECL);
    subscribeTo(JavaGrammar.INTERFACE_MEMBER_DECL);
    subscribeTo(JavaGrammar.MEMBER_DECL);
    subscribeTo(JavaGrammar.VARIABLE_DECLARATOR_ID);
  }

  @Override
  public void visitFile(@Nullable AstNode node) {
    isExcluded = false;
  }

  @Override
  public void visitNode(AstNode node) {
    if (node.is(JavaGrammar.FOR_INIT, JavaGrammar.CATCH_FORMAL_PARAMETER)) {
      isExcluded = true;
    } else if (node.hasDirectChildren(JavaTokenType.IDENTIFIER)) {
      String value = node.getFirstChild(JavaTokenType.IDENTIFIER).getTokenOriginalValue();

      if (value.length() < 3 && !isExcluded) {
        getContext().createLineViolation(this, "Rename '" + value + "' to a meaningful name of at least 3 characters.", node);
      }
    }
  }

  @Override
  public void leaveNode(AstNode node) {
    if (node.is(JavaGrammar.FOR_INIT, JavaGrammar.CATCH_FORMAL_PARAMETER)) {
      isExcluded = false;
    }
  }

}
