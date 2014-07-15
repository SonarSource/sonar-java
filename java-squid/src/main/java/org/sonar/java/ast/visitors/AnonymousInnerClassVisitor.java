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
package org.sonar.java.ast.visitors;

import com.sonar.sslr.api.AstNode;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.squidbridge.api.SourceClass;

public class AnonymousInnerClassVisitor extends JavaAstVisitor {

  @Override
  public void init() {
    subscribeTo(JavaGrammar.CLASS_CREATOR_REST);
  }

  @Override
  public void visitNode(AstNode astNode) {
    if (isInnerClass(astNode)) {
      SourceClass parentClass = peekSourceClass();
      int anonymousInnerClassNo = determineAnonymousInnerClassNo(parentClass);
      String anonymousInnerClassName = parentClass.getName() + "$" + anonymousInnerClassNo;
      String anonymousInnerClassKey = parentClass.getKey() + "$" + anonymousInnerClassNo;
      SourceClass anonymousInnerClass = new SourceClass(anonymousInnerClassKey, anonymousInnerClassName);
      anonymousInnerClass.setStartAtLine(astNode.getTokenLine());
      getContext().addSourceCode(anonymousInnerClass);
    }
  }

  @Override
  public void leaveNode(AstNode astNode) {
    if (isInnerClass(astNode)) {
      getContext().popSourceCode();
    }
  }

  private int determineAnonymousInnerClassNo(SourceClass parentClass) {
    int anonymousInnerClassNo = 1;
    while (true) {
      SourceClass anonymousInnerClass = new SourceClass(parentClass.getKey() + "$" + anonymousInnerClassNo);
      if (!parentClass.hasChild(anonymousInnerClass)) {
        return anonymousInnerClassNo;
      }
      anonymousInnerClassNo++;
    }
  }

  private boolean isInnerClass(AstNode astNode) {
    return astNode.hasDirectChildren(JavaGrammar.CLASS_BODY);
  }

}
