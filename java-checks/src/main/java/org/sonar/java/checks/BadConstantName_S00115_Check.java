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

import com.google.common.base.Preconditions;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.regex.Pattern;

@Rule(
  key = "S00115",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class BadConstantName_S00115_Check extends SquidCheck<LexerlessGrammar> {

  private static final String DEFAULT_FORMAT = "^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$";

  @RuleProperty(
    key = "format",
    defaultValue = "" + DEFAULT_FORMAT)
  public String format = DEFAULT_FORMAT;

  private Pattern pattern = null;

  @Override
  public void init() {
    subscribeTo(JavaGrammar.FIELD_DECLARATION, JavaGrammar.ENUM_CONSTANT, JavaGrammar.CONSTANT_DECLARATOR_REST);
    pattern = Pattern.compile(format, Pattern.DOTALL);
  }

  @Override
  public void visitNode(AstNode astNode) {
    if (astNode.is(JavaGrammar.CONSTANT_DECLARATOR_REST)) {
      check(astNode.getPreviousAstNode());
    } else if (astNode.is(JavaGrammar.ENUM_CONSTANT)) {
      check(astNode.getFirstChild(JavaTokenType.IDENTIFIER));
    } else {
      // FIELD_DECLARATION
      if (isConstant(astNode)) {
        for (AstNode variableDeclarator : astNode.getFirstChild(JavaGrammar.VARIABLE_DECLARATORS).getChildren(JavaGrammar.VARIABLE_DECLARATOR)) {
          AstNode identifierNode = variableDeclarator.getFirstChild(JavaTokenType.IDENTIFIER);
          if (!identifierNode.getTokenValue().equals(SerializableContract.SERIAL_VERSION_UID_FIELD)) {
            check(identifierNode);
          }
        }
      }
    }
  }

  private void check(AstNode identifier) {
    Preconditions.checkArgument(identifier.is(JavaTokenType.IDENTIFIER));
    String name = identifier.getTokenValue();
    if (!pattern.matcher(name).matches()) {
      getContext().createLineViolation(this, "Rename this constant name to match the regular expression '" + format + "'.", identifier);
    }
  }

  private boolean isConstant(AstNode astNode) {
    boolean isStatic = false;
    boolean isFinal = false;
    for (AstNode modifier : astNode.getFirstAncestor(JavaGrammar.CLASS_BODY_DECLARATION).getChildren(JavaGrammar.MODIFIER)) {
      if (modifier.getFirstChild().is(JavaKeyword.STATIC)) {
        isStatic = true;
      } else if (modifier.getFirstChild().is(JavaKeyword.FINAL)) {
        isFinal = true;
      }
    }
    return isStatic && isFinal;
  }

}
