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
package org.sonar.java.checks;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.WildcardPattern;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.ast.visitors.PublicApiChecker;
import org.sonar.java.model.PackageUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.Deque;
import java.util.List;
import java.util.regex.Pattern;

@Rule(
  key = "UndocumentedApi",
  name = "Public types, methods and fields (API) should be documented with Javadoc",
  tags = {"convention"},
  priority = Priority.MINOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("10min")
public class UndocumentedApiCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final Kind[] CLASS_KINDS = PublicApiChecker.classKinds();
  private static final Kind[] METHOD_KINDS = PublicApiChecker.methodKinds();

  private static final String DEFAULT_FOR_CLASSES = "**";

  @RuleProperty(
    key = "forClasses",
    description = "Pattern of classes which should adhere to this constraint. Ex : **.api.**",
    defaultValue = DEFAULT_FOR_CLASSES)
  public String forClasses = DEFAULT_FOR_CLASSES;

  private WildcardPattern[] patterns;
  private final Deque<ClassTree> classTrees = Lists.newLinkedList();
  private final Deque<Tree> currentParents = Lists.newLinkedList();

  private PublicApiChecker publicApiChecker;
  private String packageName;
  private final Pattern setterPattern = Pattern.compile("set[A-Z].*");
  private final Pattern getterPattern = Pattern.compile("(get|is)[A-Z].*");
  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    classTrees.clear();
    currentParents.clear();
    publicApiChecker = PublicApiChecker.newInstanceWithAccessorsHandledAsMethods();
    packageName = "";
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitCompilationUnit(CompilationUnitTree tree) {
    packageName = PackageUtils.packageName(tree.packageDeclaration(), "/");
    super.visitCompilationUnit(tree);
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    // don't visit anonymous classes, nothing in an anonymous class is part of public api.
  }

  @Override
  public void visitClass(ClassTree tree) {
    visitNode(tree);
    super.visitClass(tree);
    classTrees.pop();
    currentParents.pop();
  }

  @Override
  public void visitVariable(VariableTree tree) {
    visitNode(tree);
    super.visitVariable(tree);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    visitNode(tree);
    super.visitMethod(tree);
    currentParents.pop();
  }

  private void visitNode(Tree tree) {
    if (!isExcluded(tree)) {
      String javadoc = PublicApiChecker.getApiJavadoc(tree);
      if (javadoc == null) {
        context.addIssue(tree, this, "Document this public " + getType(tree) + ".");
      } else if (!javadoc.contains("{@inheritDoc}")) {
        List<String> undocumentedParameters = getUndocumentedParameters(javadoc, getParameters(tree));
        if (!undocumentedParameters.isEmpty()) {
          context.addIssue(tree, this, "Document the parameter(s): " + Joiner.on(", ").join(undocumentedParameters));
        }
        if (hasNonVoidReturnType(tree) && !hasReturnJavadoc(javadoc)) {
          context.addIssue(tree, this, "Document this method return value.");
        }
      }
    }
  }

  private static String getType(Tree tree) {
    String result = "";
    if (tree.is(Tree.Kind.CLASS)) {
      result = "class";
    } else if (tree.is(Tree.Kind.INTERFACE)) {
      result = "interface";
    } else if (tree.is(Tree.Kind.ENUM)) {
      result = "enum";
    } else if (tree.is(Tree.Kind.ANNOTATION_TYPE)) {
      result = "annotation";
    } else if (tree.is(Tree.Kind.CONSTRUCTOR)) {
      result = "constructor";
    } else if (tree.is(Tree.Kind.METHOD)) {
      result = "method";
    } else if (tree.is(Tree.Kind.VARIABLE)) {
      result = "field";
    }
    return result;
  }

  private boolean isExcluded(Tree tree) {
    return !isPublicApi(tree) || isAccessor(tree) || !isMatchingPattern();
  }

  private boolean isAccessor(Tree tree) {
    if (!classTrees.isEmpty() && !classTrees.peek().is(Tree.Kind.INTERFACE) && tree.is(Tree.Kind.METHOD)) {
      MethodTree methodTree = (MethodTree) tree;
      String name = methodTree.simpleName().name();
      return (setterPattern.matcher(name).matches() && methodTree.parameters().size() == 1) ||
        (getterPattern.matcher(name).matches() && methodTree.parameters().isEmpty());
    }
    return false;
  }

  private boolean isPublicApi(Tree tree) {
    Tree currentParent = currentParents.peek();
    if (tree.is(CLASS_KINDS)) {
      classTrees.push((ClassTree) tree);
      currentParents.push(tree);
    } else if (tree.is(METHOD_KINDS)) {
      currentParents.push(tree);
    }

    return publicApiChecker.isPublicApi(currentParent, tree);
  }

  private boolean isMatchingPattern() {
    return WildcardPattern.match(getPatterns(), className());
  }

  private String className() {
    String className = packageName;
    IdentifierTree identifierTree = classTrees.peek().simpleName();
    if (identifierTree != null) {
      className += "/" + identifierTree.name();
    }
    return className;
  }

  private WildcardPattern[] getPatterns() {
    if (patterns == null) {
      patterns = PatternUtils.createPatterns(forClasses);
    }
    return patterns;
  }

  private static List<String> getUndocumentedParameters(String javadoc, List<String> parameters) {
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    for (String parameter : parameters) {
      if (!hasParamJavadoc(javadoc, parameter)) {
        builder.add(parameter);
      }
    }
    return builder.build();
  }

  private static List<String> getParameters(Tree tree) {
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    if (tree.is(METHOD_KINDS)) {
      MethodTree methodTree = (MethodTree) tree;
      for (VariableTree variableTree : methodTree.parameters()) {
        builder.add(variableTree.simpleName().name());
      }
      // don't check type paramters documentation for methods
    } else if (tree.is(CLASS_KINDS)) {
      for (TypeParameterTree typeParam : ((ClassTree) tree).typeParameters()) {
        builder.add("<" + typeParam.identifier().name() + ">");
      }
    }
    return builder.build();
  }

  private static boolean hasParamJavadoc(String comment, String parameter) {
    return comment.matches("(?s).*@param\\s++" + parameter + ".*");
  }

  private boolean hasNonVoidReturnType(Tree tree) {
    // Backward compatibility : ignore methods from annotations.
    if (tree.is(Tree.Kind.METHOD) && !classTrees.peek().is(Tree.Kind.ANNOTATION_TYPE)) {
      Tree returnType = ((MethodTree) tree).returnType();
      return returnType == null || !(returnType.is(Tree.Kind.PRIMITIVE_TYPE) && "void".equals(((PrimitiveTypeTree) returnType).keyword().text()));
    }
    return false;
  }

  private static boolean hasReturnJavadoc(String comment) {
    return comment.contains("@return");
  }

}
