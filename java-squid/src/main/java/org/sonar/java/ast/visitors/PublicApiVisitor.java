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
package org.sonar.java.ast.visitors;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Trivia;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.measures.Metric;

public class PublicApiVisitor extends JavaAstVisitor {

  @Override
  public void init() {
    subscribeTo(
        getContext().getGrammar().typeDeclaration,
        getContext().getGrammar().classBodyDeclaration,
        getContext().getGrammar().interfaceBodyDeclaration,
        getContext().getGrammar().annotationTypeElementDeclaration);
  }

  @Override
  public void visitNode(AstNode astNode) {
    SourceCode currentResource = getContext().peekSourceCode();
    if (isPublicApi(astNode)) {
      currentResource.add(Metric.PUBLIC_API, 1);
      if (isDocumentedApi(astNode)) {
        currentResource.add(Metric.PUBLIC_DOC_API, 1);
      }
    }
  }

  private boolean isPublicApi(AstNode astNode) {
    // TODO
    return isPublic(astNode);
  }

  private boolean isDocumentedApi(AstNode astNode) {
    // TODO
    for (Trivia trivia : astNode.getToken().getTrivia()) {
      if (trivia.isComment()) {
        return true;
      }
    }
    return false;
  }

  private boolean isPublic(AstNode astNode) {
    if (astNode.is(getContext().getGrammar().annotationTypeElementDeclaration)) {
      return true;
    }
    for (AstNode modifierNode : astNode.findDirectChildren(getContext().getGrammar().modifier)) {
      if (modifierNode.getChild(0).is(JavaKeyword.PUBLIC)) {
        return true;
      }
    }
    return false;
  }

}
