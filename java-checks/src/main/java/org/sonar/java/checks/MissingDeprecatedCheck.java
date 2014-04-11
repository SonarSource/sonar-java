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
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.parser.JavaGrammar;

@Rule(
  key = "MissingDeprecatedCheck",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class MissingDeprecatedCheck extends AbstractDeprecatedChecker {

  private boolean classOrInterfaceIsDeprecated = false;

  @Override
  public void init() {
    super.init();
    subscribeTo(JavaGrammar.CLASS_DECLARATION, JavaGrammar.INTERFACE_DECLARATION);
  }

  @Override
  public void visitNode(AstNode node) {
    boolean hasDeprecatedAnnotation = hasDeprecatedAnnotationExcludingLocalVariables(node);
    boolean hasJavadocDeprecatedTag = hasJavadocDeprecatedTag(node);
    if(node.is(JavaGrammar.CLASS_DECLARATION, JavaGrammar.INTERFACE_DECLARATION)) {
      classOrInterfaceIsDeprecated = hasDeprecatedAnnotation || hasJavadocDeprecatedTag;
      return;
    }
    if(!classOrInterfaceIsDeprecated) {
      if (hasDeprecatedAnnotation && !hasJavadocDeprecatedTag) {
        getContext().createLineViolation(this, "Add the missing @deprecated Javadoc tag.", node);
      } else if (hasJavadocDeprecatedTag && !hasDeprecatedAnnotation) {
        getContext().createLineViolation(this, "Add the missing @Deprecated annotation.", node);
      }
    }
  }

}
