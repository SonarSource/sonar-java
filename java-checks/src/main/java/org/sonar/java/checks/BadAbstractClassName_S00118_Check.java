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
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.regex.Pattern;

@Rule(
  key = "S00118",
  priority = Priority.MAJOR)
public class BadAbstractClassName_S00118_Check extends SquidCheck<LexerlessGrammar> {

  private static final String DEFAULT_FORMAT = "^Abstract[A-Z][a-zA-Z0-9]*$";

  @RuleProperty(
    key = "format",
    defaultValue = "" + DEFAULT_FORMAT)
  public String format = DEFAULT_FORMAT;

  private Pattern pattern = null;

  @Override
  public void init() {
    subscribeTo(JavaGrammar.CLASS_DECLARATION);
    pattern = Pattern.compile(format, Pattern.DOTALL);
  }

  @Override
  public void visitNode(AstNode astNode) {
    String name = astNode.getFirstChild(JavaTokenType.IDENTIFIER).getTokenValue();
    if (pattern.matcher(name).matches()) {
      if (!isAbstract(astNode)) {
        getContext().createLineViolation(this, "Make this class abstract or rename it, since it matches the regular expression '" + format + "'.", astNode);
      }
    } else {
      if (isAbstract(astNode)) {
        getContext().createLineViolation(this, "Rename this abstract class name to match the regular expression '" + format + "'.", astNode);
      }
    }
  }

  private boolean isAbstract(AstNode astNode) {
    AstNode modifier = astNode.getPreviousAstNode();
    while (modifier != null && modifier.is(JavaGrammar.MODIFIER)) {
      if (modifier.getFirstChild().is(JavaKeyword.ABSTRACT)) {
        return true;
      }
      modifier = modifier.getPreviousAstNode();
    }
    return false;
  }

}
