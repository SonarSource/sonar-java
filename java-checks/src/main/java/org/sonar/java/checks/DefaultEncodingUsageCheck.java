/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodsHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.List;
import java.util.Set;

@Rule(key = "S1943")
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
        boolean foundIssue = checkForbiddenTypes(variableTree.simpleName(), variableTree.type().symbolType());
        if (foundIssue) {
          excluded.add(variableTree.initializer());
        }
      } else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
        MethodInvocationTree mit = (MethodInvocationTree) tree;
        checkForbiddenTypes(MethodsHelper.methodName(mit), mit.symbolType());
      }
    }
  }

  private boolean checkForbiddenTypes(Tree reportTree, Type symbolType) {
    for (String forbiddenType : FORBIDDEN_TYPES) {
      if (symbolType.is(forbiddenType)) {
        reportIssue(reportTree, "Remove this use of \"" + forbiddenType + "\"");
        return true;
      }
    }
    return false;
  }

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(
      method(JAVA_LANG_STRING, "getBytes").withoutParameter(),
      method(JAVA_LANG_STRING, "getBytes").parameters(INT, INT, BYTE_ARRAY, INT),
      constructor(JAVA_LANG_STRING).parameters(BYTE_ARRAY),
      constructor(JAVA_LANG_STRING).parameters(BYTE_ARRAY, INT, INT),
      method(JAVA_IO_BYTEARRAYOUTPUTSTREAM, "toString").withoutParameter(),
      constructor(JAVA_IO_FILEREADER).withAnyParameters(),
      constructor(JAVA_IO_FILEWRITER).withAnyParameters(),
      constructor(JAVA_IO_INPUTSTREAMREADER).parameters(JAVA_IO_INPUTSTREAM),
      constructor(JAVA_IO_OUTPUTSTREAMWRITER).parameters(JAVA_IO_OUTPUTSTREAM),
      constructor(JAVA_IO_PRINTSTREAM).parameters(JAVA_IO_FILE),
      constructor(JAVA_IO_PRINTSTREAM).parameters(JAVA_IO_OUTPUTSTREAM),
      constructor(JAVA_IO_PRINTSTREAM).parameters(JAVA_IO_OUTPUTSTREAM, BOOLEAN),
      constructor(JAVA_IO_PRINTSTREAM).parameters(JAVA_LANG_STRING),
      constructor(JAVA_IO_PRINTWRITER).parameters(JAVA_IO_FILE),
      constructor(JAVA_IO_PRINTWRITER).parameters(JAVA_IO_OUTPUTSTREAM),
      constructor(JAVA_IO_PRINTWRITER).parameters(JAVA_IO_OUTPUTSTREAM, BOOLEAN),
      constructor(JAVA_IO_PRINTWRITER).parameters(JAVA_LANG_STRING),
      constructor(JAVA_UTIL_FORMATTER).parameters(JAVA_LANG_STRING),
      constructor(JAVA_UTIL_FORMATTER).parameters(JAVA_IO_FILE),
      constructor(JAVA_UTIL_FORMATTER).parameters(JAVA_IO_OUTPUTSTREAM),
      constructor(JAVA_UTIL_SCANNER).parameters(JAVA_IO_FILE),
      constructor(JAVA_UTIL_SCANNER).parameters(JAVA_NIO_FILE_PATH),
      constructor(JAVA_UTIL_SCANNER).parameters(JAVA_IO_INPUTSTREAM));
  }

  private static MethodMatcher method(String type, String methodName) {
    return MethodMatcher.create().typeDefinition(type).name(methodName);
  }

  private static MethodMatcher constructor(String type) {
    return method(type, "<init>");
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    reportIssue(MethodsHelper.methodName(mit), "Remove this use of \"" + mit.symbol().name() + "\"");
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    Symbol symbol = newClassTree.constructorSymbol();
    if (symbol.isMethodSymbol()) {
      Symbol.MethodSymbol constructor = (Symbol.MethodSymbol) symbol;
      String signature = constructor.owner().name() + "(" + Joiner.on(',').join(constructor.parameterTypes()) + ")";
      reportIssue(newClassTree.identifier(), "Remove this use of constructor \"" + signature + "\"");
    }
  }

}
