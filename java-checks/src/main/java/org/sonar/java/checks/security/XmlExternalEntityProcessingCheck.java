/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
import java.util.function.Predicate;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import static org.sonar.java.matcher.TypeCriteria.subtypeOf;

@Rule(key = "S2755")
public class XmlExternalEntityProcessingCheck extends IssuableSubscriptionVisitor {

  private static final String XML_INPUT_FACTORY_CLASS_NAME = XMLInputFactory.class.getName();
  private static final String SAX_PARSER_FACTORY_CLASS_NAME = SAXParserFactory.class.getName();
  private static final String XML_READER_FACTORY_CLASS_NAME = XMLReaderFactory.class.getName();
  private static final String XML_READER_CLASS_NAME = XMLReader.class.getName();
  private static final String DOCUMENT_BUILDER_FACTORY_CLASS_NAME = DocumentBuilderFactory.class.getName();
  private static final String VALIDATOR_CLASS_NAME = Validator.class.getName();
  private static final String SCHEMA_CLASS_NAME = Schema.class.getName();


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

  private final List<XxeCheck> xxeChecks = Arrays.asList(
    new XxeCheck(newInstanceMethod(XML_INPUT_FACTORY_CLASS_NAME), new XMLInputFactorySecuringPredicate()),
    new XxeCheck(MethodMatcher.create().typeDefinition(XML_INPUT_FACTORY_CLASS_NAME).name("newFactory").withAnyParameters(), new XMLInputFactorySecuringPredicate()),
    new XxeCheck(newInstanceMethod(SAX_PARSER_FACTORY_CLASS_NAME), new SecureProcessingFeaturePredicate(SAX_PARSER_FACTORY_CLASS_NAME)),
    new XxeCheck(newInstanceMethod(DOCUMENT_BUILDER_FACTORY_CLASS_NAME), new SecureProcessingFeaturePredicate(DOCUMENT_BUILDER_FACTORY_CLASS_NAME)),
    new XxeCheck(CREATE_XML_READER_MATCHER, new SecureProcessingFeaturePredicate(XML_READER_CLASS_NAME)),
    new XxeCheck(CREATE_VALIDATOR, new AccessExternalDTDOrSchemaPredicate(VALIDATOR_CLASS_NAME))
  );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    xxeChecks.forEach(check -> check.checkMethodInvocation((MethodInvocationTree) tree));
  }

  private class XxeCheck {

    private final MethodMatcher triggeringInvocationMatcher;
    private final Predicate<MethodInvocationTree> securingInvocationPredicate;

    private XxeCheck(MethodMatcher triggeringInvocationMatcher, Predicate<MethodInvocationTree> securingInvocationPredicate) {
      this.triggeringInvocationMatcher = triggeringInvocationMatcher;
      this.securingInvocationPredicate = securingInvocationPredicate;
    }

    private void checkMethodInvocation(MethodInvocationTree methodInvocation) {
      if (triggeringInvocationMatcher.matches(methodInvocation)) {
        MethodTree enclosingMethod = ExpressionUtils.getEnclosingMethod(methodInvocation);
        if (enclosingMethod != null) {
          if (securingInvocationPredicate instanceof AccessExternalDTDOrSchemaPredicate) {
            ((AccessExternalDTDOrSchemaPredicate) securingInvocationPredicate).externalDTDDisabled = false;
            ((AccessExternalDTDOrSchemaPredicate) securingInvocationPredicate).externalSchemaDisabled = false;
          }
          MethodVisitor methodVisitor = new MethodVisitor(securingInvocationPredicate);
          enclosingMethod.accept(methodVisitor);
          if (!methodVisitor.isExternalEntityProcessingDisabled) {
            reportIssue(methodInvocation.methodSelect(), "Disable XML external entity (XXE) processing.");
          }
        }
      }
    }
  }

  private static class MethodVisitor extends BaseTreeVisitor {

    private final Predicate<MethodInvocationTree> securingInvocationPredicate;
    private boolean isExternalEntityProcessingDisabled = false;

    private MethodVisitor(Predicate<MethodInvocationTree> securingInvocationPredicate) {
      this.securingInvocationPredicate = securingInvocationPredicate;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree methodInvocation) {
      if (securingInvocationPredicate.test(methodInvocation)) {
        isExternalEntityProcessingDisabled = true;
      }
      super.visitMethodInvocation(methodInvocation);
    }
  }

  private static class XMLInputFactorySecuringPredicate implements Predicate<MethodInvocationTree> {

    private static final MethodMatcher SET_PROPERTY =
      MethodMatcher.create()
        .typeDefinition(subtypeOf(XML_INPUT_FACTORY_CLASS_NAME))
        .name("setProperty")
        .parameters(JAVA_LANG_STRING, "java.lang.Object");

    @Override
    public boolean test(MethodInvocationTree methodInvocation) {
      Arguments arguments = methodInvocation.arguments();
      if (SET_PROPERTY.matches(methodInvocation)) {
        String propertyName = ExpressionsHelper.getConstantValueAsString(arguments.get(0)).value();
        if (XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES.equals(propertyName) || XMLInputFactory.SUPPORT_DTD.equals(propertyName)) {
          ExpressionTree propertyValue = arguments.get(1);
          return Boolean.FALSE.equals(ExpressionsHelper.getConstantValueAsBoolean(propertyValue).value()) || "false".equalsIgnoreCase(ExpressionsHelper.getConstantValueAsString(propertyValue).value());
        }
      }
      return false;
    }
  }

  private static class SecureProcessingFeaturePredicate implements Predicate<MethodInvocationTree> {

    private final MethodMatcher methodMatcher;

    private SecureProcessingFeaturePredicate(String className) {
      this.methodMatcher = setFeatureMethodMatcher(className);
    }

    @Override
    public boolean test(MethodInvocationTree methodInvocation) {
      if (methodMatcher.matches(methodInvocation)) {
        Arguments arguments = methodInvocation.arguments();
        String featureName = ExpressionsHelper.getConstantValueAsString(arguments.get(0)).value();
        return Boolean.TRUE.equals(ExpressionsHelper.getConstantValueAsBoolean(arguments.get(1)).value())
          && (XMLConstants.FEATURE_SECURE_PROCESSING.equals(featureName)
          || "http://apache.org/xml/features/disallow-doctype-decl".equals(featureName));
      }
      return false;
    }

    private static MethodMatcher setFeatureMethodMatcher(String className) {
      return MethodMatcher.create()
        .typeDefinition(subtypeOf(className))
        .name("setFeature")
        .parameters(JAVA_LANG_STRING, "boolean");
    }
  }

  private static class AccessExternalDTDOrSchemaPredicate implements Predicate<MethodInvocationTree> {

    private final MethodMatcher methodMatcher;
    private boolean externalDTDDisabled = false;
    private boolean externalSchemaDisabled = false;

    private AccessExternalDTDOrSchemaPredicate(String className) {
      this.methodMatcher = setPropertyMethodMatcher(className);
    }

    @Override
    public boolean test(MethodInvocationTree methodInvocation) {
      if (methodMatcher.matches(methodInvocation)) {
        Arguments arguments = methodInvocation.arguments();
        String propertyName = ExpressionsHelper.getConstantValueAsString(arguments.get(0)).value();
        String propertyValue = ExpressionsHelper.getConstantValueAsString(arguments.get(1)).value();
        if ("".equals(propertyValue) && XMLConstants.ACCESS_EXTERNAL_DTD.equals(propertyName)) {
          externalDTDDisabled = true;
        }
        if ("".equals(propertyValue) && XMLConstants.ACCESS_EXTERNAL_SCHEMA.equals(propertyName)) {
          externalSchemaDisabled = true;
        }
        return externalDTDDisabled && externalSchemaDisabled;
      }
      return false;
    }

    private static MethodMatcher setPropertyMethodMatcher(String className) {
      return MethodMatcher.create()
        .typeDefinition(subtypeOf(className))
        .name("setProperty")
        .parameters(JAVA_LANG_STRING, "java.lang.Object");
    }
  }

}
