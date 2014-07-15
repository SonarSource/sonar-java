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
import com.sonar.sslr.api.Token;
import org.sonar.squidbridge.CommentAnalyser;
import org.sonar.squidbridge.SquidAstVisitorContext;
import org.sonar.squidbridge.api.CheckMessage;
import org.sonar.squidbridge.api.CodeCheck;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.api.SourceProject;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.io.File;
import java.util.Stack;

/**
 * Replacement for {@link com.sonar.sslr.squid.SquidAstVisitorContextImpl<JavaGrammar>}.
 */
public class VisitorContext extends SquidAstVisitorContext<LexerlessGrammar> {

  private final Stack<SourceCode> sourceCodeStack = new Stack<SourceCode>();
  private final SourceProject project;
  private File file;
  private CommentAnalyser commentAnalyser;

  public VisitorContext(SourceProject project) {
    if (project == null) {
      throw new IllegalArgumentException("project cannot be null.");
    }
    this.project = project;
    sourceCodeStack.add(project);
  }

  public void setCommentAnalyser(CommentAnalyser commentAnalyser) {
    this.commentAnalyser = commentAnalyser;
  }

  /** {@inheritDoc} */
  @Override
  public CommentAnalyser getCommentAnalyser() {
    return commentAnalyser;
  }

  /** {@inheritDoc} */
  @Override
  public void addSourceCode(SourceCode child) {
    peekSourceCode().addChild(child);
    sourceCodeStack.add(child);
  }

  /** {@inheritDoc} */
  @Override
  public void popSourceCode() {
    sourceCodeStack.pop();
  }

  /** {@inheritDoc} */
  @Override
  public SourceCode peekSourceCode() {
    return sourceCodeStack.peek();
  }

  public void setFile(File file) {
    popTillSourceProject();
    this.file = file;
  }

  private void popTillSourceProject() {
    while (!(peekSourceCode() instanceof SourceProject)) {
      popSourceCode();
    }
  }

  /** {@inheritDoc} */
  @Override
  public File getFile() {
    return file;
  }

  public SourceProject getProject() {
    return project;
  }

  /**
   * @deprecated
   */
  @Override
  @Deprecated
  public LexerlessGrammar getGrammar() {
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public void createFileViolation(CodeCheck check, String message, Object... messageParameters) {
    createLineViolation(check, message, -1, messageParameters);
  }

  /** {@inheritDoc} */
  @Override
  public void createLineViolation(CodeCheck check, String message, AstNode node, Object... messageParameters) {
    createLineViolation(check, message, node.getToken(), messageParameters);
  }

  /** {@inheritDoc} */
  @Override
  public void createLineViolation(CodeCheck check, String message, Token token, Object... messageParameters) {
    createLineViolation(check, message, token.getLine(), messageParameters);
  }

  /** {@inheritDoc} */
  @Override
  public void createLineViolation(CodeCheck check, String message, int line, Object... messageParameters) {
    CheckMessage checkMessage = new CheckMessage(check, message, messageParameters);
    if (line > 0) {
      checkMessage.setLine(line);
    }
    log(checkMessage);
  }

  public void log(CheckMessage message) {
    if (peekSourceCode() instanceof SourceFile) {
      peekSourceCode().log(message);
    } else if (peekSourceCode().getParent(SourceFile.class) != null) {
      peekSourceCode().getParent(SourceFile.class).log(message);
    } else {
      throw new IllegalStateException("Unable to log a check message on source code '" + (peekSourceCode() == null ? "[NULL]" : peekSourceCode().getKey()) + "'");
    }
  }

}
