/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.checks.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.SecuringInvocationPredicate;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.checks.helpers.ExpressionsHelper.getConstantValueAsBoolean;
import static org.sonar.java.checks.helpers.ExpressionsHelper.getConstantValueAsString;
import static org.sonar.java.matcher.TypeCriteria.subtypeOf;
import static org.sonar.java.model.ExpressionUtils.isSelectOnThisOrSuper;

@Rule(key = "S2755")
public class XmlExternalEntityProcessingCheck extends IssuableSubscriptionVisitor {

  private static final String XML_INPUT_FACTORY_CLASS_NAME = "javax.xml.stream.XMLInputFactory";
  private static final String SAX_PARSER_FACTORY_CLASS_NAME = "javax.xml.parsers.SAXParserFactory";
  private static final String XML_READER_FACTORY_CLASS_NAME = "org.xml.sax.helpers.XMLReaderFactory";
  private static final String XML_READER_CLASS_NAME = "org.xml.sax.XMLReader";
  private static final String DOCUMENT_BUILDER_FACTORY_CLASS_NAME = "javax.xml.parsers.DocumentBuilderFactory";
  private static final String VALIDATOR_CLASS_NAME = "javax.xml.validation.Validator";
  private static final String SCHEMA_CLASS_NAME = "javax.xml.validation.Schema";

  private static final MethodMatcher CREATE_XML_READER_MATCHER = MethodMatcher.create()
    .typeDefinition(XML_READER_FACTORY_CLASS_NAME)
    .name("createXMLReader")
    .withAnyParameters();

  private static final MethodMatcher CREATE_VALIDATOR = MethodMatcher.create()
    .typeDefinition(SCHEMA_CLASS_NAME)
    .name("newValidator")
    .withAnyParameters();
  private static final String JAVA_LANG_STRING = "java.lang.String";

  private static MethodMatcher newInstanceMethod(String className) {
    return MethodMatcher.create()
      .typeDefinition(className)
      .name("newInstance")
      .withAnyParameters();
  }

  private final List<TriggeringSecuringCheck> triggeringSecuringChecks = Arrays.asList(
    new TriggeringSecuringCheck(newInstanceMethod(XML_INPUT_FACTORY_CLASS_NAME), new XMLInputFactorySecuringPredicate()),
    new TriggeringSecuringCheck(MethodMatcher.create().typeDefinition(XML_INPUT_FACTORY_CLASS_NAME).name("newFactory").withAnyParameters(), new XMLInputFactorySecuringPredicate()),
    new TriggeringSecuringCheck(newInstanceMethod(SAX_PARSER_FACTORY_CLASS_NAME), new SecureProcessingFeaturePredicate(SAX_PARSER_FACTORY_CLASS_NAME)),
    new TriggeringSecuringCheck(newInstanceMethod(DOCUMENT_BUILDER_FACTORY_CLASS_NAME), new SecureProcessingFeaturePredicate(DOCUMENT_BUILDER_FACTORY_CLASS_NAME)),
    new TriggeringSecuringCheck(CREATE_XML_READER_MATCHER, new SecureProcessingFeaturePredicate(XML_READER_CLASS_NAME)),
    new TriggeringSecuringCheck(CREATE_VALIDATOR, new AccessExternalDTDOrSchemaPredicate(VALIDATOR_CLASS_NAME))
  );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree methodInvocationTree = (MethodInvocationTree) tree;
    triggeringSecuringChecks.forEach(check -> {
      if (check.shouldReportMethodInvocation(methodInvocationTree)) {
        reportIssue(methodInvocationTree.methodSelect(), "Disable XML external entity (XXE) processing.");
      }
    });
  }

  private static class TriggeringSecuringCheck {

    private final MethodMatcher triggeringInvocationMatcher;
    private final SecuringInvocationPredicate securingInvocationPredicate;

    private TriggeringSecuringCheck(MethodMatcher triggeringInvocationMatcher, SecuringInvocationPredicate securingInvocationPredicate) {
      this.triggeringInvocationMatcher = triggeringInvocationMatcher;
      this.securingInvocationPredicate = securingInvocationPredicate;
    }

    private boolean shouldReportMethodInvocation(MethodInvocationTree methodInvocation) {
      if (triggeringInvocationMatcher.matches(methodInvocation)) {
        MethodTree enclosingMethod = ExpressionUtils.getEnclosingMethod(methodInvocation);
        if (enclosingMethod != null) {
          securingInvocationPredicate.resetState();
          Optional<Symbol> assignedSymbol = getAssignedSymbol(methodInvocation);
          if (assignedSymbol.isPresent()) {
            MethodVisitor methodVisitor = new MethodVisitor(securingInvocationPredicate, assignedSymbol.get());
            enclosingMethod.accept(methodVisitor);
            return !methodVisitor.isExternalEntityProcessingDisabled();
          }
        }
      }
      return false;
    }

    private static Optional<Symbol> getAssignedSymbol(MethodInvocationTree mit) {
      Tree parent = mit.parent();
      if (parent != null) {
        if (parent.is(Tree.Kind.ASSIGNMENT)) {
          return extractIdentifierSymbol(((AssignmentExpressionTree) parent).variable());
        } else if (parent.is(Tree.Kind.VARIABLE)) {
          return Optional.of(((VariableTree) parent).simpleName().symbol());
        }
      }
      return Optional.empty();
    }
  }

  private static Optional<Symbol> extractIdentifierSymbol(ExpressionTree tree) {
    ExpressionTree cleanedExpression = ExpressionUtils.skipParentheses(tree);
    if (cleanedExpression.is(Tree.Kind.IDENTIFIER)) {
      return Optional.of(((IdentifierTree) cleanedExpression).symbol());
    } else if (cleanedExpression.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree selectTree = (MemberSelectExpressionTree) cleanedExpression;
      if (isSelectOnThisOrSuper(selectTree)) {
        return Optional.of(selectTree.identifier().symbol());
      }
    }
    return Optional.empty();
  }

  private static class MethodVisitor extends BaseTreeVisitor {

    private final SecuringInvocationPredicate securingInvocationPredicate;
    private Symbol variable;

    private MethodVisitor(SecuringInvocationPredicate securingInvocationPredicate, Symbol variable) {
      this.securingInvocationPredicate = securingInvocationPredicate;
      this.variable = variable;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree methodInvocation) {
      if (isSameVariableSymbol(methodInvocation)) {
        securingInvocationPredicate.processInvocation(methodInvocation);
      }
      super.visitMethodInvocation(methodInvocation);
    }

    boolean isExternalEntityProcessingDisabled() {
      return securingInvocationPredicate.satisfy();
    }

    private boolean isSameVariableSymbol(MethodInvocationTree mit) {
      ExpressionTree methodSelect = mit.methodSelect();
      if (methodSelect.is(Kind.MEMBER_SELECT)) {
        return extractIdentifierSymbol(((MemberSelectExpressionTree)methodSelect).expression()).filter(s -> s.equals(variable)).isPresent();
      }
      return false;
    }
  }


  private static class XMLInputFactorySecuringPredicate implements SecuringInvocationPredicate {

    private static final String IS_SUPPORTING_EXTERNAL_ENTITIES_PROPERTY = "javax.xml.stream.isSupportingExternalEntities";
    private static final String SUPPORT_DTD_PROPERTY = "javax.xml.stream.supportDTD";

    private static final MethodMatcher SET_PROPERTY =
      MethodMatcher.create()
        .typeDefinition(subtypeOf(XML_INPUT_FACTORY_CLASS_NAME))
        .name("setProperty")
        .parameters(JAVA_LANG_STRING, "java.lang.Object");

    private boolean foundSecuringCall = false;

    @Override
    public void processInvocation(MethodInvocationTree methodInvocation) {
      Arguments arguments = methodInvocation.arguments();
      if (SET_PROPERTY.matches(methodInvocation)) {
        String propertyName = getConstantValueAsString(arguments.get(0)).value();
        if (IS_SUPPORTING_EXTERNAL_ENTITIES_PROPERTY.equals(propertyName) || SUPPORT_DTD_PROPERTY.equals(propertyName)) {
          ExpressionTree propertyValue = arguments.get(1);
          if (Boolean.FALSE.equals(getConstantValueAsBoolean(propertyValue).value())
            || "false".equalsIgnoreCase(getConstantValueAsString(propertyValue).value())) {
            foundSecuringCall = true;
          }
        }
      }
    }

    @Override
    public void resetState() {
      foundSecuringCall = false;
    }

    @Override
    public boolean satisfy() {
      return foundSecuringCall;
    }
  }

  private static class SecureProcessingFeaturePredicate implements SecuringInvocationPredicate {

    private static final String FEATURE_SECURE_PROCESSING_PROPERTY = "http://javax.xml.XMLConstants/feature/secure-processing";
    private static final String FEATURE_DISALLOW_DOCTYPE_DECL = "http://apache.org/xml/features/disallow-doctype-decl";

    private final MethodMatcher methodMatcher;

    private boolean foundSecuringCall = false;

    private SecureProcessingFeaturePredicate(String className) {
      this.methodMatcher = setFeatureMethodMatcher(className);
    }

    @Override
    public void processInvocation(MethodInvocationTree methodInvocation) {
      if (methodMatcher.matches(methodInvocation)) {
        Arguments arguments = methodInvocation.arguments();
        String featureName = getConstantValueAsString(arguments.get(0)).value();
        if (Boolean.TRUE.equals(getConstantValueAsBoolean(arguments.get(1)).value())
          && (FEATURE_SECURE_PROCESSING_PROPERTY.equals(featureName)
          || FEATURE_DISALLOW_DOCTYPE_DECL.equals(featureName))) {
          foundSecuringCall = true;
        }
      }
    }

    @Override
    public void resetState() {
      foundSecuringCall = false;
    }

    @Override
    public boolean satisfy() {
      return foundSecuringCall;
    }

    private static MethodMatcher setFeatureMethodMatcher(String className) {
      return MethodMatcher.create()
        .typeDefinition(subtypeOf(className))
        .name("setFeature")
        .parameters(JAVA_LANG_STRING, "boolean");
    }
  }

  private static class AccessExternalDTDOrSchemaPredicate implements SecuringInvocationPredicate {

    private static final String ACCESS_EXTERNAL_DTD_PROPERTY = "http://javax.xml.XMLConstants/property/accessExternalDTD";
    private static final String ACCESS_EXTERNAL_SCHEMA_PROPERTY = "http://javax.xml.XMLConstants/property/accessExternalSchema";

    private final MethodMatcher methodMatcher;
    private boolean externalDTDDisabled = false;
    private boolean externalSchemaDisabled = false;

    private AccessExternalDTDOrSchemaPredicate(String className) {
      this.methodMatcher = setPropertyMethodMatcher(className);
    }

    @Override
    public void processInvocation(MethodInvocationTree methodInvocation) {
      if (methodMatcher.matches(methodInvocation)) {
        Arguments arguments = methodInvocation.arguments();
        String propertyName = getConstantValueAsString(arguments.get(0)).value();
        String propertyValue = getConstantValueAsString(arguments.get(1)).value();
        if ("".equals(propertyValue) && ACCESS_EXTERNAL_DTD_PROPERTY.equals(propertyName)) {
          externalDTDDisabled = true;
        }
        if ("".equals(propertyValue) && ACCESS_EXTERNAL_SCHEMA_PROPERTY.equals(propertyName)) {
          externalSchemaDisabled = true;
        }
      }
    }

    @Override
    public boolean satisfy() {
      return externalDTDDisabled && externalSchemaDisabled;
    }

    @Override
    public void resetState() {
      externalDTDDisabled = false;
      externalSchemaDisabled = false;
    }

    private static MethodMatcher setPropertyMethodMatcher(String className) {
      return MethodMatcher.create()
        .typeDefinition(subtypeOf(className))
        .name("setProperty")
        .parameters(JAVA_LANG_STRING, "java.lang.Object");
    }
  }

}
