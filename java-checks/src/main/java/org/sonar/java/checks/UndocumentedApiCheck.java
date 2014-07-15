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
import com.sonar.sslr.api.AstNode;
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.api.utils.WildcardPattern;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.java.ast.visitors.MethodHelper;
import org.sonar.java.ast.visitors.PublicApiVisitor;
import org.sonar.squidbridge.api.SourceClass;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.List;
import java.util.regex.Pattern;

@Rule(key = "UndocumentedApi", priority = Priority.MAJOR,
    tags = {"convention"})
public class UndocumentedApiCheck extends SquidCheck<LexerlessGrammar> {

  private static final String DEFAULT_FOR_CLASSES = "**";
  private Pattern setterPattern = Pattern.compile("set[A-Z].*");
  private Pattern getterPattern = Pattern.compile("(get|is)[A-Z].*");

  @RuleProperty(
      key = "forClasses",
      defaultValue = DEFAULT_FOR_CLASSES)
  public String forClasses = DEFAULT_FOR_CLASSES;

  private WildcardPattern[] patterns;

  @Override
  public void init() {
    PublicApiVisitor.subscribe(this);
  }

  @Override
  public void visitNode(AstNode node) {
    if (!isExcluded(node)) {
      String javadoc = PublicApiVisitor.getApiJavadoc(node);

      if (javadoc == null) {
        getContext().createLineViolation(this, "Document this public " + PublicApiVisitor.getType(node) + ".", node);
      } else {
        List<String> undocumentedParameters = getUndocumentedParameters(javadoc, getParameters(node));
        if (!undocumentedParameters.isEmpty()) {
          getContext().createLineViolation(this, "Document the parameter(s): " + Joiner.on(", ").join(undocumentedParameters), node);
        }

        if (hasNonVoidReturnType(node) && !hasReturnJavadoc(javadoc)) {
          getContext().createLineViolation(this, "Document this method return value.", node);
        }
      }
    }
  }

  private boolean isExcluded(AstNode node) {
    return isAccessor(node) ||
        !isPublicApi(node) ||
        !isMatchingPattern();
  }

  private boolean isAccessor(AstNode node) {
    boolean result = false;
    //setter resolution  based solely on names and parameters number : generate false negative. But for undocumented API we tolerate it.
    if (node.is(JavaGrammar.METHOD_DECLARATOR_REST, JavaGrammar.VOID_METHOD_DECLARATOR_REST)) {
      MethodHelper methodHelper = new MethodHelper(node);
      String methodName = methodHelper.getName().getTokenOriginalValue();
      result = (setterPattern.matcher(methodName).matches() && methodHelper.getParameters().size() == 1)
          || (getterPattern.matcher(methodName).matches() && !methodHelper.hasParameters());
    }
    return result;
  }

  private boolean isPublicApi(AstNode node) {
    return PublicApiVisitor.isPublicApi(node);
  }

  private boolean isMatchingPattern() {
    return WildcardPattern.match(getPatterns(), peekSourceClass().getKey());
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

  private static List<String> getParameters(AstNode node) {
    ImmutableList.Builder<String> builder = ImmutableList.builder();

    AstNode formalParameters = node.getFirstChild(JavaGrammar.FORMAL_PARAMETERS);
    if (formalParameters != null) {
      for (AstNode parameter : formalParameters.getDescendants(JavaGrammar.VARIABLE_DECLARATOR_ID)) {
        builder.add(parameter.getTokenOriginalValue());
      }
    }

    AstNode typeParameters = node.getFirstChild(JavaGrammar.TYPE_PARAMETERS);
    if (typeParameters != null) {
      for (AstNode parameter : typeParameters.getChildren(JavaGrammar.TYPE_PARAMETER)) {
        builder.add("<" + parameter.getTokenOriginalValue() + ">");
      }
    }

    return builder.build();
  }

  private static boolean hasParamJavadoc(String comment, String parameter) {
    return comment.matches("(?s).*@param\\s++" + parameter + ".*");
  }

  private static boolean hasNonVoidReturnType(AstNode node) {
    return node.is(JavaGrammar.METHOD_DECLARATOR_REST, JavaGrammar.INTERFACE_METHOD_DECLARATOR_REST) &&
        !isGenericMethodReturningVoid(node);
  }

  private static boolean isGenericMethodReturningVoid(AstNode node) {
    return node.getParent().is(JavaGrammar.GENERIC_METHOD_OR_CONSTRUCTOR_REST) &&
        node.getParent().hasDirectChildren(JavaKeyword.VOID);
  }

  private static boolean hasReturnJavadoc(String comment) {
    return comment.contains("@return");
  }

  private final SourceClass peekSourceClass() {
    SourceCode sourceCode = getContext().peekSourceCode();
    if (sourceCode.isType(SourceClass.class)) {
      return (SourceClass) sourceCode;
    }
    return sourceCode.getParent(SourceClass.class);
  }

}
