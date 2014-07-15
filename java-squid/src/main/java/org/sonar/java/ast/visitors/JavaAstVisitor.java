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

import org.sonar.squidbridge.SquidAstVisitor;
import org.sonar.squidbridge.api.SourceClass;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.api.SourcePackage;
import org.sonar.sslr.parser.LexerlessGrammar;

/**
 * Shortcut for {@code SquidAstVisitor<JavaGrammar>}.
 */
public abstract class JavaAstVisitor extends SquidAstVisitor<LexerlessGrammar> {

  protected final SourceFile peekSourceFile() {
    SourceCode sourceCode = getContext().peekSourceCode();
    if (sourceCode.isType(SourceFile.class)) {
      return (SourceFile) getContext().peekSourceCode();
    }
    return sourceCode.getParent(SourceFile.class);
  }

  protected final SourcePackage peekParentPackage() {
    SourceCode sourceCode = getContext().peekSourceCode();
    if (sourceCode.isType(SourcePackage.class)) {
      return (SourcePackage) getContext().peekSourceCode();
    }
    return sourceCode.getParent(SourcePackage.class);
  }

  protected final SourceClass peekSourceClass() {
    SourceCode sourceCode = getContext().peekSourceCode();
    if (sourceCode.isType(SourceClass.class)) {
      return (SourceClass) sourceCode;
    }
    return sourceCode.getParent(SourceClass.class);
  }

}
