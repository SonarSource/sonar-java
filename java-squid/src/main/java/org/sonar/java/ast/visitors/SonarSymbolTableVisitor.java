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
import org.sonar.api.source.Symbolizable;
import org.sonar.java.SemanticModelProvider;
import org.sonar.java.SonarComponents;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.resolve.Symbol;

import java.util.Map;

public class SonarSymbolTableVisitor extends JavaAstVisitor {

  private final SemanticModelProvider semanticModelProvider;
  private final SonarComponents sonarComponents;

  public SonarSymbolTableVisitor(SonarComponents sonarComponents, SemanticModelProvider semanticModelProvider) {
    this.sonarComponents = sonarComponents;
    this.semanticModelProvider = semanticModelProvider;
  }

  @Override
  public void visitFile(AstNode astNode) {
    SemanticModel semanticModel = semanticModelProvider.semanticModel();
    if (semanticModel == null) {
      // parse or semantic error
      return;
    }

    Symbolizable symbolizable = sonarComponents.symbolizableFor(getContext().getFile());
    Symbolizable.SymbolTableBuilder symbolTableBuilder = symbolizable.newSymbolTableBuilder();

    for (Map.Entry<AstNode, Symbol> entry : semanticModel.getSymbols().entrySet()) {
      AstNode declaration = entry.getKey();
      org.sonar.api.source.Symbol symbol = symbolTableBuilder.newSymbol(startOffsetFor(declaration), endOffsetFor(declaration));

      for (AstNode usage : semanticModel.getUsages(entry.getValue())) {
        symbolTableBuilder.newReference(symbol, startOffsetFor(usage));
      }
    }

    symbolizable.setSymbolTable(symbolTableBuilder.build());
  }

  private static int startOffsetFor(AstNode astNode) {
    return astNode.getFromIndex();
  }

  private static int endOffsetFor(AstNode astNode) {
    return astNode.getFromIndex() + astNode.getTokenValue().length();
  }

}
