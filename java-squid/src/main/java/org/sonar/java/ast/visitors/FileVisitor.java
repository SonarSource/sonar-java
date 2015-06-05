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

import com.google.common.base.Preconditions;
import com.sonar.sslr.api.AstNode;
import org.sonar.squidbridge.SquidAstVisitor;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.io.File;

public class FileVisitor extends SquidAstVisitor<LexerlessGrammar> {

  @Override
  public void visitFile(AstNode astNode) {
    File file = getContext().getFile();
    getContext().addSourceCode(new SourceFile(file.getAbsolutePath(), file.getPath()));
  }

  @Override
  public void leaveFile(AstNode astNode) {
    Preconditions.checkState(getContext().peekSourceCode().isType(SourceFile.class));
    getContext().popSourceCode();
  }

}
