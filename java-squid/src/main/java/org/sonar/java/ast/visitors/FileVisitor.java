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
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.api.SourcePackage;

import java.io.File;

public class FileVisitor extends JavaAstVisitor {

  @Override
  public void visitFile(AstNode astNode) {
    SourceFile sourceFile = createSourceFile(peekParentPackage(), getContext().getFile());
    getContext().addSourceCode(sourceFile);
  }

  @Override
  public void leaveFile(AstNode astNode) {
    Preconditions.checkState(getContext().peekSourceCode().isType(SourceFile.class));
    getContext().popSourceCode();
  }

  private static SourceFile createSourceFile(SourcePackage parentPackage, File file) {
    StringBuilder key = new StringBuilder();
    if (parentPackage != null && !"".equals(parentPackage.getKey())) {
      key.append(parentPackage.getKey());
      key.append("/");
    }
    key.append(file.getName());
    return new SourceFile(key.toString(), file.getPath());
  }

}
