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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.java.SemanticModelProvider;
import org.sonar.java.model.JavaTree;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;

import javax.annotation.Nullable;

public class SemanticModelVisitor extends BaseTreeVisitor implements SemanticModelProvider, JavaFileScanner {

  private static final Logger LOG = LoggerFactory.getLogger(SemanticModelVisitor.class);

  private SemanticModel semanticModel;

  @Override
  @Nullable
  public SemanticModel semanticModel() {
    return semanticModel;
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    try {
      AstNode astNode = ((JavaTree.CompilationUnitTreeImpl) context.getTree()).getAstNode();
      semanticModel = SemanticModel.createFor(astNode);
    } catch (Exception e) {
      LOG.error("Unable to create symbol table", e);
      semanticModel = null;
      return;
    }


  }
}
