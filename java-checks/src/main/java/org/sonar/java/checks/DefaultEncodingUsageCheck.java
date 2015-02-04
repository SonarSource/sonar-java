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
package org.sonar.java.checks;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.expression.NewClassTreeImpl;
import org.sonar.java.resolve.Symbol.MethodSymbol;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;
import java.util.Set;

@Rule(
  key = "S1943",
  name = "Classes and methods that rely on the default system encoding should not be used",
  tags = {"bug"},
  priority = Priority.MAJOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.ARCHITECTURE_RELIABILITY)
@SqaleConstantRemediation("15min")
public class DefaultEncodingUsageCheck extends AbstractMethodDetection {

  private static final String INT = "int";
  private static final String BOOLEAN = "boolean";
  private static final String BYTE_ARRAY = "byte[]";
  private static final String JAVA_IO_FILE = "java.io.File";
  private static final String JAVA_IO_FILEWRITER = "java.io.FileWriter";
  private static final String JAVA_IO_FILEREADER = "java.io.FileReader";
  private static final String JAVA_IO_PRINTWRITER = "java.io.PrintWriter";
  private static final String JAVA_IO_PRINTSTREAM = "java.io.PrintStream";
  private static final String JAVA_IO_INPUTSTREAM = "java.io.InputStream";
  private static final String JAVA_IO_OUTPUTSTREAM = "java.io.OutputStream";
  private static final String JAVA_IO_BYTEARRAYOUTPUTSTREAM = "java.io.ByteArrayOutputStream";
  private static final String JAVA_IO_OUTPUTSTREAMWRITER = "java.io.OutputStreamWriter";
  private static final String JAVA_IO_INPUTSTREAMREADER = "java.io.InputStreamReader";
  private static final String JAVA_NIO_FILE_PATH = "java.nio.file.Path";
  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String JAVA_UTIL_SCANNER = "java.util.Scanner";
  private static final String JAVA_UTIL_FORMATTER = "java.util.Formatter";

  private static final String[] FORBIDDEN_TYPES = {JAVA_IO_FILEREADER, JAVA_IO_FILEWRITER};

  private Set<Tree> excluded = Sets.newHashSet();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    super.scanFile(context);
    excluded.clear();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS, Tree.Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!excluded.contains(tree)) {
      super.visitNode(tree);
      if (tree.is(Tree.Kind.VARIABLE)) {
        VariableTree variableTree = (VariableTree) tree;
        boolean foundIssue = checkForbiddenTypes(tree, (AbstractTypedTree) variableTree.type());
        if (foundIssue) {
          excluded.add(variableTree.initializer());
        }
      } else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
        checkForbiddenTypes(tree, (MethodInvocationTreeImpl) tree);
      }
    }
  }

  private boolean checkForbiddenTypes(Tree tree, AbstractTypedTree typedTree) {
    boolean foundIssue = false;
    Type symbolType = typedTree.getSymbolType();
    for (String forbiddenType : FORBIDDEN_TYPES) {
      if (symbolType.is(forbiddenType)) {
        addIssue(tree, "Remove this use of \"" + forbiddenType + "\"");
        foundIssue = true;
      }
    }
    return foundIssue;
  }

  @Override
  protected List<MethodInvocationMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(
      method(JAVA_LANG_STRING, "getBytes"),
      method(JAVA_LANG_STRING, "getBytes", INT, INT, BYTE_ARRAY, INT),
      constructor(JAVA_LANG_STRING, BYTE_ARRAY),
      constructor(JAVA_LANG_STRING, BYTE_ARRAY, INT, INT),
      method(JAVA_IO_BYTEARRAYOUTPUTSTREAM, "toString"),
      constructor(JAVA_IO_FILEREADER).withNoParameterConstraint(),
      constructor(JAVA_IO_FILEWRITER).withNoParameterConstraint(),
      constructor(JAVA_IO_INPUTSTREAMREADER, JAVA_IO_INPUTSTREAM),
      constructor(JAVA_IO_OUTPUTSTREAMWRITER, JAVA_IO_OUTPUTSTREAM),
      constructor(JAVA_IO_PRINTSTREAM, JAVA_IO_FILE),
      constructor(JAVA_IO_PRINTSTREAM, JAVA_IO_OUTPUTSTREAM),
      constructor(JAVA_IO_PRINTSTREAM, JAVA_IO_OUTPUTSTREAM, BOOLEAN),
      constructor(JAVA_IO_PRINTSTREAM, JAVA_LANG_STRING),
      constructor(JAVA_IO_PRINTWRITER, JAVA_IO_FILE),
      constructor(JAVA_IO_PRINTWRITER, JAVA_IO_OUTPUTSTREAM),
      constructor(JAVA_IO_PRINTWRITER, JAVA_IO_OUTPUTSTREAM, BOOLEAN),
      constructor(JAVA_IO_PRINTWRITER, JAVA_LANG_STRING),
      constructor(JAVA_UTIL_FORMATTER, JAVA_LANG_STRING),
      constructor(JAVA_UTIL_FORMATTER, JAVA_IO_FILE),
      constructor(JAVA_UTIL_FORMATTER, JAVA_IO_OUTPUTSTREAM),
      constructor(JAVA_UTIL_SCANNER, JAVA_IO_FILE),
      constructor(JAVA_UTIL_SCANNER, JAVA_NIO_FILE_PATH),
      constructor(JAVA_UTIL_SCANNER, JAVA_IO_INPUTSTREAM));
  }

  private MethodInvocationMatcher method(String type, String methodName, String... argTypes) {
    MethodInvocationMatcher matcher = MethodInvocationMatcher.create().typeDefinition(type).name(methodName);
    for (String argType : argTypes) {
      matcher = matcher.addParameter(argType);
    }
    return matcher;
  }

  private MethodInvocationMatcher constructor(String type, String... argTypes) {
    return method(type, "<init>", argTypes);
  }

  @Override
  protected void onMethodFound(MethodInvocationTree mit) {
    MethodInvocationTreeImpl methodInvocationTreeImpl = (MethodInvocationTreeImpl) mit;
    String methodName = methodInvocationTreeImpl.getSymbol().getName();
    addIssue(mit, "Remove this use of \"" + methodName + "\"");
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    NewClassTreeImpl newClassTreeImpl = (NewClassTreeImpl) newClassTree;
    IdentifierTree constructorIdentifier = newClassTreeImpl.getConstructorIdentifier();
    MethodSymbol constructor = (MethodSymbol) getSemanticModel().getReference(constructorIdentifier);
    List<Type> parametersTypes = constructor.getParametersTypes();
    String signature = constructor.owner().getName() + "(" + Joiner.on(',').join(parametersTypes) + ")";
    addIssue(newClassTree, "Remove this use of constructor \"" + signature + "\"");
  }

}
