/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.checks.helpers.ConstantUtils;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4719")
public class StandardCharsetsConstantsCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {

  private static final String JAVA_IO_INPUTSTREAM = "java.io.InputStream";
  private static final String JAVA_IO_OUTPUTSTREAM = "java.io.OutputStream";
  private static final String JAVA_IO_OUTPUTSTREAMWRITER = "java.io.OutputStreamWriter";
  private static final String JAVA_IO_INPUTSTREAMREADER = "java.io.InputStreamReader";
  private static final String JAVA_NIO_CHARSET = "java.nio.charset.Charset";
  private static final String JAVA_LANG_STRING = "java.lang.String";

  private static final List<Charset> STANDARD_CHARSETS = Arrays.asList(
          StandardCharsets.ISO_8859_1,
          StandardCharsets.US_ASCII,
          StandardCharsets.UTF_16,
          StandardCharsets.UTF_16BE,
          StandardCharsets.UTF_16LE,
          StandardCharsets.UTF_8);

  private static final Map<String, String> ALIAS_TO_CONSTANT = createAliasToConstantNameMap();

  private static Map<String, String> createAliasToConstantNameMap() {
    ImmutableMap.Builder<String, String> aliases = ImmutableMap.builder();
    for (Charset charset : STANDARD_CHARSETS) {
      aliases.put(charset.name(), charset.name());

      for (String alias : charset.aliases()) {
        aliases.put(alias, charset.name());
      }
    }

    return aliases.build();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS, Tree.Kind.IDENTIFIER);
  }

  @Override
  public void visitNode(Tree tree) {
    super.visitNode(tree);
    if (hasSemantic() && tree.is(Tree.Kind.IDENTIFIER)) {
      onMemberSelectExpressionFound((IdentifierTree) tree);
    }
  }

  private void onMemberSelectExpressionFound(IdentifierTree identifierTree) {
    Symbol symbol = identifierTree.symbol();
    if (symbol.isVariableSymbol() && symbol.owner().type().is("com.google.common.base.Charsets")) {
      String identifier = identifierTree.name();
      if (STANDARD_CHARSETS.stream().anyMatch(c -> c.name().equals(identifier.replace("_", "-")))) {
        reportIssue(identifierTree, "Replace \"com.google.common.base.Charsets." + identifier + "\" with \"StandardCharsets." + identifier + "\".");
      }
    }
  }

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
      MethodMatcher.create().typeDefinition(JAVA_NIO_CHARSET).name("forName").parameters(JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(JAVA_LANG_STRING).name("getBytes").parameters(JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(JAVA_LANG_STRING).name("getBytes").parameters(JAVA_NIO_CHARSET),
      constructor(JAVA_LANG_STRING).parameters("byte[]", JAVA_LANG_STRING),
      constructor(JAVA_LANG_STRING).parameters("byte[]", "int", "int", JAVA_LANG_STRING),
      constructor(JAVA_IO_INPUTSTREAMREADER).parameters(JAVA_IO_INPUTSTREAM, JAVA_LANG_STRING),
      constructor(JAVA_IO_OUTPUTSTREAMWRITER).parameters(JAVA_IO_OUTPUTSTREAM, JAVA_LANG_STRING)
    );
  }

  private static MethodMatcher constructor(String type) {
    return MethodMatcher.create().typeDefinition(type).name("<init>");
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (mit.symbol().name().equals("forName")) {
      checkCharsetForNameCall(mit);
    } else {
      checkArguments(mit.arguments());
    }
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    checkArguments(newClassTree.arguments());
  }

  private void checkCharsetForNameCall(MethodInvocationTree mit) {
    ExpressionTree argument = mit.arguments().get(0);
    String constantName = getConstantName(argument);
    if (constantName != null) {
      reportIssue(mit, "Replace Charset.forName() call with StandardCharsets." + constantName);
    }
  }

  private void checkArguments(Arguments arguments) {
    ExpressionTree lastArgument = arguments.get(arguments.size() - 1);
    String constantName = getConstantName(lastArgument);
    if (constantName != null) {
      reportIssue(lastArgument, "Replace charset name argument with StandardCharsets." + constantName);
    }
  }

  private static String getConstantName(ExpressionTree argument) {
    String constantValue = ConstantUtils.resolveAsStringConstant(argument);
    return ALIAS_TO_CONSTANT.get(constantValue);
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava7Compatible();
  }
}
