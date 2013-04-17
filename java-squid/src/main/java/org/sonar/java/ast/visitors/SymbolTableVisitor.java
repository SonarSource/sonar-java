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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.SquidUtils;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.scan.source.SymbolPerspective;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.resolve.Symbol;

import java.util.Map;

public class SymbolTableVisitor extends JavaAstVisitor {

  private static final Logger LOG = LoggerFactory.getLogger(SymbolTableVisitor.class);

  private final ResourcePerspectives perspectives;

  public SymbolTableVisitor(ResourcePerspectives perspectives) {
    this.perspectives = perspectives;
  }

  @Override
  public void visitFile(AstNode astNode) {
    if (astNode == null) {
      // parse error
      return;
    }

    SemanticModel semanticModel;
    try {
      semanticModel = SemanticModel.createFor(astNode);
    } catch (Exception e) {
      LOG.error("Unable to create symbol table for " + getContext().getFile(), e);
      return;
    }

    JavaFile sonarFile = SquidUtils.convertJavaFileKeyFromSquidFormat(peekSourceFile().getKey());
    SymbolPerspective symbolPerspective = perspectives.as(SymbolPerspective.class, sonarFile).begin();

    for (Map.Entry<AstNode, Symbol> entry : semanticModel.getSymbols().entrySet()) {
      AstNode declaration = entry.getKey();
      org.sonar.api.scan.source.Symbol sonarSymbol = symbolPerspective
        .newSymbol()
        .setDeclaration(startOffsetFor(declaration), endOffsetFor(declaration))
        .build();

      SymbolPerspective.ReferencesBuilder referencesBuilder = symbolPerspective.declareReferences(sonarSymbol);
      for (AstNode usage : semanticModel.getUsages(entry.getValue())) {
        referencesBuilder.addReference(usage.getFromIndex());
      }
    }
    symbolPerspective.end();
  }

  private static int startOffsetFor(AstNode astNode) {
    return astNode.getFromIndex();
  }

  private static int endOffsetFor(AstNode astNode) {
    return astNode.getFromIndex() + astNode.getTokenValue().length();
  }

}
