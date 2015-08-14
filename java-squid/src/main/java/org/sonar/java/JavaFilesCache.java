/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.JavaTree.PackageDeclarationTreeImpl;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.signature.MethodSignaturePrinter;
import org.sonar.java.signature.MethodSignatureScanner;
import org.sonar.java.syntaxtoken.LastSyntaxTokenFinder;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.io.File;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class JavaFilesCache extends BaseTreeVisitor implements JavaFileScanner {

  @VisibleForTesting
  Map<String, File> resourcesCache = Maps.newHashMap();

  @VisibleForTesting
  Map<String, Integer> methodStartLines = Maps.newHashMap();

  @VisibleForTesting
  Multimap<Integer, String> suppressWarningLines = HashMultimap.create();

  private File currentFile;
  private Deque<String> currentClassKey = new LinkedList<>();
  private Deque<Tree> parent = new LinkedList<>();
  private Deque<Integer> anonymousInnerClassCounter = new LinkedList<>();
  private String currentPackage;

  public Map<String, File> getResourcesCache() {
    return resourcesCache;
  }

  public Map<String, Integer> getMethodStartLines() {
    return methodStartLines;
  }

  public Multimap<Integer, String> getSuppressWarningLines() {
    return suppressWarningLines;
  }

  public boolean hasSuppressWarningLines() {
    return !suppressWarningLines.isEmpty();
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    JavaTree.CompilationUnitTreeImpl tree = (JavaTree.CompilationUnitTreeImpl) context.getTree();
    currentPackage = PackageDeclarationTreeImpl.packageNameAsString(tree.packageDeclaration()).replace('.', '/');
    currentFile = context.getFile();
    currentClassKey.clear();
    parent.clear();
    anonymousInnerClassCounter.clear();
    suppressWarningLines.clear();
    scan(tree);
  }

  @Override
  public void visitClass(ClassTree tree) {
    String className = "";
    IdentifierTree simpleName = tree.simpleName();
    if (simpleName != null) {
      className = simpleName.name();
    }
    String key = getClassKey(className);
    currentClassKey.push(key);
    parent.push(tree);
    anonymousInnerClassCounter.push(0);
    resourcesCache.put(key, currentFile);
    handleSuppressWarning(tree);
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
      // inner class declared within method
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
    methodStartLines.put(methodKey, tree.simpleName().identifierToken().line());
    handleSuppressWarning(tree);
    super.visitMethod(tree);
    parent.pop();
  }

  @Override
  public void visitVariable(VariableTree tree) {
    handleSuppressWarning(tree);
    super.visitVariable(tree);
  }

  private void handleSuppressWarning(ClassTree tree) {
    int endLine = tree.closeBraceToken().line();
    handleSuppressWarning(tree.modifiers().annotations(), endLine);
  }

  private void handleSuppressWarning(VariableTree variable) {
    int variableEndLine = variable.simpleName().identifierToken().line();
    if (variable.initializer() != null) {
      variableEndLine = LastSyntaxTokenFinder.lastSyntaxToken(variable).line();
    }
    handleSuppressWarning(variable.modifiers().annotations(), variableEndLine);
  }

  private void handleSuppressWarning(MethodTree tree) {
    int endLine = tree.simpleName().identifierToken().line();
    // if we have no block, then we assume method is on one line on the method name line.
    if (tree.block() != null) {
      endLine = tree.block().closeBraceToken().line();
    }
    handleSuppressWarning(tree.modifiers().annotations(), endLine);
  }

  private void handleSuppressWarning(List<AnnotationTree> annotationTrees, int endLine) {
    int startLine = 0;
    List<String> warnings = Lists.newArrayList();
    for (AnnotationTree annotationTree : annotationTrees) {
      if (isSuppressWarningsAnnotation(annotationTree)) {
        startLine = ((JavaTree) annotationTree).getLine();
        warnings.addAll(getSuppressWarningArgs(annotationTree));
        break;
      }
    }
    for (int i = startLine; i <= endLine; i++) {
      suppressWarningLines.putAll(i, warnings);
    }
  }

  private static boolean isSuppressWarningsAnnotation(AnnotationTree annotationTree) {
    return annotationTree.annotationType().symbolType().is("java.lang.SuppressWarnings");
  }

  private static List<String> getSuppressWarningArgs(AnnotationTree annotationTree) {
    return getValueFromExpression(annotationTree.arguments().get(0));
  }

  private static List<String> getValueFromExpression(ExpressionTree expression) {
    List<String> args = Lists.newArrayList();
    if (expression.is(Tree.Kind.STRING_LITERAL)) {
      args.add(LiteralUtils.trimQuotes(((LiteralTree) expression).value()));
    } else if (expression.is(Tree.Kind.NEW_ARRAY)) {
      for (ExpressionTree initializer : ((NewArrayTree) expression).initializers()) {
        args.addAll(getValueFromExpression(initializer));
      }
    }
    return args;
  }
}
