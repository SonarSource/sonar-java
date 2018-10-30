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
import java.util.List;
import java.util.Map;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ConstantUtils;
import org.sonar.java.checks.helpers.IdentifierUtils;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4719")
public class StandardCharsetsConstantsCheck extends AbstractMethodDetection {

  private static final String INT = "int";
  private static final String BYTE_ARRAY = "byte[]";
  private static final String JAVA_IO_INPUTSTREAM = "java.io.InputStream";
  private static final String JAVA_IO_OUTPUTSTREAM = "java.io.OutputStream";
  private static final String JAVA_IO_OUTPUTSTREAMWRITER = "java.io.OutputStreamWriter";
  private static final String JAVA_IO_INPUTSTREAMREADER = "java.io.InputStreamReader";
  private static final String JAVA_NIO_CHARSET = "java.nio.charset.Charset";
  private static final String JAVA_LANG_STRING = "java.lang.String";

  private static final ImmutableMap<String, Charset> STANDARD_CHARSETS = new ImmutableMap.Builder<String, Charset>()
          .put("ISO_8859_1",  StandardCharsets.ISO_8859_1)
          .put("US_ASCII", StandardCharsets.US_ASCII)
          .put("UTF_16", StandardCharsets.UTF_16)
          .put("UTF_16BE", StandardCharsets.UTF_16BE)
          .put("UTF_16LE", StandardCharsets.UTF_16LE)
          .put("UTF_8", StandardCharsets.UTF_8)
          .build();

  private static final ImmutableMap<String, String> ALIAS_TO_CONSTANT = createAliasToConstantNameMap(STANDARD_CHARSETS);

  private static ImmutableMap<String, String> createAliasToConstantNameMap(ImmutableMap<String, Charset> charsets) {
    ImmutableMap.Builder<String, String> aliases = ImmutableMap.builder();

    for (Map.Entry<String, Charset> entry : charsets.entrySet()) {
      Charset charset = entry.getValue();
      aliases.put(charset.name(), entry.getKey());

      for (String alias : charset.aliases()) {
        aliases.put(alias, entry.getKey());
      }
    }

    return aliases.build();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS, Tree.Kind.MEMBER_SELECT);
  }

  @Override
  public void visitNode(Tree tree) {
    super.visitNode(tree);
    if (hasSemantic()) {
      checkMemberSelect(tree);
    }
  }

  private void checkMemberSelect(Tree tree) {
    if (tree.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mset = (MemberSelectExpressionTree) tree;
      onMemberSelectExpressionFound(mset);
    }
  }

  private void onMemberSelectExpressionFound(MemberSelectExpressionTree mset) {
    boolean isCharsetsCall = mset.expression().is(Tree.Kind.IDENTIFIER) && "Charsets".equals(((IdentifierTree) mset.expression()).name());
    if (isCharsetsCall) {
      String identifier = mset.identifier().name();
      if (STANDARD_CHARSETS.containsKey(identifier)) {
        reportIssue(mset, "Replace \"Charsets." + identifier + "\" with \"StandardCharsets." + identifier + "\".");
      }
    }
  }

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    ImmutableList.Builder<MethodMatcher> matchers = ImmutableList.<MethodMatcher>builder().add(
      method(JAVA_NIO_CHARSET, "forName").parameters(JAVA_LANG_STRING),
      method(JAVA_LANG_STRING, "getBytes").parameters(JAVA_LANG_STRING),
      method(JAVA_LANG_STRING, "getBytes").parameters(JAVA_NIO_CHARSET),
      constructor(JAVA_LANG_STRING).parameters(BYTE_ARRAY, JAVA_LANG_STRING),
      constructor(JAVA_LANG_STRING).parameters(BYTE_ARRAY, INT, INT, JAVA_LANG_STRING),
      constructor(JAVA_IO_INPUTSTREAMREADER).parameters(JAVA_IO_INPUTSTREAM, JAVA_LANG_STRING),
      constructor(JAVA_IO_OUTPUTSTREAMWRITER).parameters(JAVA_IO_OUTPUTSTREAM, JAVA_LANG_STRING)
    );

    return matchers.build();
  }

  private static MethodMatcher method(String type, String methodName) {
    return MethodMatcher.create().typeDefinition(type).name(methodName);
  }

  private static MethodMatcher constructor(String type) {
    return method(type, "<init>");
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (mit.symbol().name().equals("forName")) {
      checkCharsetForNameCall(mit);
    } else {
      Arguments arguments = mit.arguments();
      checkArguments(arguments);
    }
  }

  private void checkCharsetForNameCall(MethodInvocationTree mit) {
    ExpressionTree argument = mit.arguments().get(0);
    String constantName = getConstantName(argument);
    if (constantName != null) {
      reportIssue(mit, "Replace Charset.forName() call with StandardCharsets." + constantName);
    }
  }

  private String getConstantName(ExpressionTree argument) {
    Object constantValue = IdentifierUtils.getValue(argument, ConstantUtils::resolveAsStringConstant);
    if (constantValue != null) {
      return ALIAS_TO_CONSTANT.get(constantValue);
    }
    return null;
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    Arguments arguments = newClassTree.arguments();
    checkArguments(arguments);
  }

  private void checkArguments(Arguments arguments) {
    ExpressionTree lastArgument = arguments.get(arguments.size() - 1);
    String constantName = getConstantName(lastArgument);
    if (constantName != null) {
      reportIssue(lastArgument, "Replace charset name argument with StandardCharsets." + constantName);
    }
  }
}
