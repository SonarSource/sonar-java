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

import java.text.MessageFormat;

@Rule(
  key = "S1206",
  priority = Priority.CRITICAL)
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class EqualsOverridenWithHashCodeCheck extends SquidCheck<LexerlessGrammar> {

  private static final String HASHCODE = "hashCode";
  private static final String EQUALS = "equals";

  @Override
  public void init() {
    subscribeTo(JavaGrammar.CLASS_BODY);
    subscribeTo(JavaGrammar.ENUM_BODY_DECLARATIONS);
  }

  @Override
  public void visitNode(AstNode node) {
    boolean hasHashCode = false;
    boolean hasEquals = false;

    for (MethodHelper method : MethodHelper.getMethods(node)) {
      if (method.getParameters().size() == 0 && HASHCODE.equals(method.getName().getTokenOriginalValue())) {
        hasHashCode = true;
      } else if (method.getParameters().size() == 1 && EQUALS.equals(method.getName().getTokenOriginalValue())) {
        hasEquals = true;
      }
    }

    if (hasHashCode ^ hasEquals) {
      getContext().createLineViolation(
        this,
        getMessage(node.is(JavaGrammar.CLASS_BODY), hasHashCode),
        node.is(JavaGrammar.CLASS_BODY) ? node : node.getParent());
    }
  }

  private static String getMessage(boolean isClass, boolean hasHashCode) {
    return MessageFormat.format(
      "This {0} overrides \"{1}()\" and should therefore also override \"{2}()\".",
      isClass ? "class" : "enum",
      hasHashCode ? HASHCODE : EQUALS,
      hasHashCode ? EQUALS : HASHCODE);
  }

}
