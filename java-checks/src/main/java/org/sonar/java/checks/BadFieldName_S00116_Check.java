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
  key = "S00116",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class BadFieldName_S00116_Check extends SquidCheck<LexerlessGrammar> {

  private static final String DEFAULT_FORMAT = "^[a-z][a-zA-Z0-9]*$";

  @RuleProperty(
    key = "format",
    defaultValue = "" + DEFAULT_FORMAT)
  public String format = DEFAULT_FORMAT;

  private Pattern pattern = null;

  @Override
  public void init() {
    subscribeTo(JavaGrammar.FIELD_DECLARATION);
    pattern = Pattern.compile(format, Pattern.DOTALL);
  }

  @Override
  public void visitNode(AstNode astNode) {
    if (isNotStatic(astNode)) {
      for (AstNode variableDeclarator : astNode.getFirstChild(JavaGrammar.VARIABLE_DECLARATORS).getChildren(JavaGrammar.VARIABLE_DECLARATOR)) {
        check(variableDeclarator.getFirstChild(JavaTokenType.IDENTIFIER));
      }
    }
  }

  private void check(AstNode identifier) {
    String name = identifier.getTokenValue();
    if (!pattern.matcher(name).matches()) {
      getContext().createLineViolation(this, "Rename this field name to match the regular expression '" + format + "'.", identifier);
    }
  }

  private boolean isNotStatic(AstNode astNode) {
    for (AstNode modifier : astNode.getFirstAncestor(JavaGrammar.CLASS_BODY_DECLARATION).getChildren(JavaGrammar.MODIFIER)) {
      if (modifier.getFirstChild().is(JavaKeyword.STATIC)) {
        return false;
      }
    }
    return true;
  }

}
