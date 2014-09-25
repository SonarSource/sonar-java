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
package org.sonar.java;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.java.signature.MethodSignaturePrinter;
import org.sonar.java.signature.MethodSignatureScanner;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.File;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JavaFilesCache extends BaseTreeVisitor implements JavaFileScanner {


  @VisibleForTesting
  Map<String, File> resourcesCache = Maps.newHashMap();

  @VisibleForTesting
  Map<String, Integer> methodStartLines = Maps.newHashMap();

  @VisibleForTesting
  Set<Integer> ignoredLines = Sets.newHashSet();

  private File currentFile;
  private Deque<String> currentClassKey = new LinkedList<String>();
  private Deque<Tree> parent = new LinkedList<Tree>();
  private Deque<Integer> anonymousInnerClassCounter = new LinkedList<Integer>();
  private String currentPackage;

  public Map<String, File> getResourcesCache() {
    return resourcesCache;
  }

  public Map<String, Integer> getMethodStartLines() {
    return methodStartLines;
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    JavaTree.CompilationUnitTreeImpl tree = (JavaTree.CompilationUnitTreeImpl) context.getTree();
    currentPackage = tree.packageNameAsString().replace('.', '/');
    currentFile = context.getFile();
    currentClassKey.clear();
    parent.clear();
    anonymousInnerClassCounter.clear();
    ignoredLines.clear();
    scan(tree);
  }

  @Override
  public void visitClass(ClassTree tree) {
    String className = "";
    if (tree.simpleName() != null) {
      className = tree.simpleName().name();
    }
    String key = getClassKey(className);
    currentClassKey.push(key);
    parent.push(tree);
    anonymousInnerClassCounter.push(0);
    resourcesCache.put(key, currentFile);
    isSuppressWarning(tree);
    super.visitClass(tree);
    currentClassKey.pop();
    parent.pop();
    anonymousInnerClassCounter.pop();
  }

  private String getClassKey(String className) {
    String key = className;
    if (StringUtils.isNotEmpty(currentPackage)) {
      key = currentPackage + "/" + className;
    }
    if ("".equals(className) || (parent.peek() != null && parent.peek().is(Tree.Kind.METHOD))) {
      //inner class declared within method
      int count = anonymousInnerClassCounter.pop() + 1;
      key = currentClassKey.peek() + "$" + count + className;
      anonymousInnerClassCounter.push(count);
    } else if (currentClassKey.peek() != null) {
      key = currentClassKey.peek() + "$" + className;
    }
    return key;
  }

  @Override
  public void visitMethod(MethodTree tree) {
    parent.push(tree);
    String methodKey = currentClassKey.peek() + "#" + MethodSignaturePrinter.print(MethodSignatureScanner.scan(tree));
    methodStartLines.put(methodKey, ((JavaTree) tree.simpleName()).getLine());
    isSuppressWarning(tree);
    super.visitMethod(tree);
    parent.pop();
  }

  private void isSuppressWarning(ClassTree tree) {
    int endLine = ((InternalSyntaxToken) tree.closeBraceToken()).getLine();
    isSuppressWarning(tree.modifiers().annotations(), endLine);
  }

  private void isSuppressWarning(MethodTree tree) {
    int endLine = ((JavaTree) tree.simpleName()).getLine();
    //if we have no block, then we assume method is on one line on the method name line.
    if (tree.block() != null) {
      endLine = ((InternalSyntaxToken) tree.block().closeBraceToken()).getLine();
    }
    isSuppressWarning(tree.modifiers().annotations(), endLine);
  }

  private void isSuppressWarning(List<AnnotationTree> annotationTrees, int endLine) {
    boolean hasSuppressAllWarnings = false;
    int startLine = 0;
    for (AnnotationTree annotationTree : annotationTrees) {
      if (isSuppressAllWarnings(annotationTree)) {
        startLine = ((JavaTree) annotationTree).getLine();
        hasSuppressAllWarnings = true;
      }
    }

    if (hasSuppressAllWarnings) {
      for (int i = startLine; i <= endLine; i++) {
        ignoredLines.add(i);
      }
    }
  }

  private boolean isSuppressAllWarnings(AnnotationTree annotationTree) {
    boolean suppressWarningsType = false;
    Tree type = annotationTree.annotationType();
    if (type.is(Tree.Kind.IDENTIFIER)) {
      suppressWarningsType = "SuppressWarnings".equals(((IdentifierTree) type).name());
    } else if (type.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mset = (MemberSelectExpressionTree) type;
      suppressWarningsType = "SuppressWarnings".equals(mset.identifier().name()) &&
          mset.expression().is(Tree.Kind.MEMBER_SELECT) && "lang".equals(((MemberSelectExpressionTree) mset.expression()).identifier().name());
    }
    if (suppressWarningsType) {
      for (ExpressionTree expression : annotationTree.arguments()) {
        if (expression.is(Tree.Kind.STRING_LITERAL) && "\"all\"".equals(((LiteralTree) expression).value())) {
          return true;
        }
      }
    }
    return false;
  }

  public Set<Integer> ignoredLines() {
    return ignoredLines;
  }
}
