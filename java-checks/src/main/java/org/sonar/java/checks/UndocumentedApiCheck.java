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
import org.sonar.api.utils.WildcardPattern;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.ast.visitors.JavaAstCheck;
import org.sonar.java.ast.visitors.PublicApiVisitor;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceMethod;

@Rule(key = "UndocumentedApi", priority = Priority.MAJOR)
public class UndocumentedApiCheck extends JavaAstCheck {

  private static final String DEFAULT_FOR_CLASSES = "**";

  @RuleProperty(
    key = "forClasses",
    defaultValue = DEFAULT_FOR_CLASSES)
  public String forClasses = DEFAULT_FOR_CLASSES;

  private WildcardPattern[] patterns;

  @Override
  public void init() {
    PublicApiVisitor.subscribe(this);
  }

  @Override
  public void visitNode(AstNode astNode) {
    SourceCode currentResource = getContext().peekSourceCode();
    if (!WildcardPattern.match(getPatterns(), peekSourceClass().getKey())) {
      return;
    }
    if (currentResource instanceof SourceMethod && ((SourceMethod) currentResource).isAccessor()) {
      return;
    }
    if (PublicApiVisitor.isPublicApi(astNode) && !PublicApiVisitor.isDocumentedApi(astNode)) {
      getContext().createLineViolation(this, "Document this public " + PublicApiVisitor.getType(astNode) + ".", astNode);
    }
  }

  private WildcardPattern[] getPatterns() {
    if (patterns == null) {
      patterns = PatternUtils.createPatterns(forClasses);
    }
    return patterns;
  }

}
