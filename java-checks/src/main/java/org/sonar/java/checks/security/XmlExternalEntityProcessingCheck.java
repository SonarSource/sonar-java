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
import java.util.function.Predicate;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import static org.sonar.java.checks.helpers.ExpressionsHelper.getConstantValueAsBoolean;
import static org.sonar.java.checks.helpers.ExpressionsHelper.getConstantValueAsString;
import static org.sonar.java.matcher.TypeCriteria.subtypeOf;

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
  private static final String JAVA_LANG_OBJECT = "java.lang.Object";

  private static MethodMatcher newInstanceMethod(String className) {
    return MethodMatcher.create()
      .typeDefinition(className)
      .name("newInstance")
      .withAnyParameters();
  }

  private final List<Predicate<MethodInvocationTree>> triggeringSecuringHelpers = Arrays.asList(
    new XMLInputFactorySecuringPredicate(newInstanceMethod(XML_INPUT_FACTORY_CLASS_NAME)),
    new XMLInputFactorySecuringPredicate(MethodMatcher.create().typeDefinition(XML_INPUT_FACTORY_CLASS_NAME).name("newFactory").withAnyParameters()),
    new SecureProcessingFeaturePredicate(newInstanceMethod(SAX_PARSER_FACTORY_CLASS_NAME), SAX_PARSER_FACTORY_CLASS_NAME),
    new SecureProcessingFeaturePredicate(newInstanceMethod(DOCUMENT_BUILDER_FACTORY_CLASS_NAME), DOCUMENT_BUILDER_FACTORY_CLASS_NAME),
    new SecureProcessingFeaturePredicate(CREATE_XML_READER_MATCHER, XML_READER_CLASS_NAME),
    new AccessExternalDTDOrSchemaPredicate(CREATE_VALIDATOR, VALIDATOR_CLASS_NAME),
    new SecureXmlTransformerPredicate()
  );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree methodInvocationTree = (MethodInvocationTree) tree;
    triggeringSecuringHelpers.forEach(check -> {
      if (check.test(methodInvocationTree)) {
        reportIssue(methodInvocationTree.methodSelect(), "Disable XML external entity (XXE) processing.");
      }
    });
  }

  private static class XMLInputFactorySecuringPredicate extends TriggeringSecuringHelper {

    private static final String IS_SUPPORTING_EXTERNAL_ENTITIES_PROPERTY = "javax.xml.stream.isSupportingExternalEntities";
    private static final String SUPPORT_DTD_PROPERTY = "javax.xml.stream.supportDTD";

    private static final MethodMatcher SET_PROPERTY =
      MethodMatcher.create()
        .typeDefinition(subtypeOf(XML_INPUT_FACTORY_CLASS_NAME))
        .name("setProperty")
        .parameters(JAVA_LANG_STRING, JAVA_LANG_OBJECT);

    private boolean foundSecuringCall = false;

    private XMLInputFactorySecuringPredicate(MethodMatcher triggeringInvocationMatcher) {
      super(triggeringInvocationMatcher);
    }

    @Override
    public void processSecuringMethodInvocation(MethodInvocationTree methodInvocation) {
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
    public boolean isSecured() {
      return foundSecuringCall;
    }
  }

  private static class SecureProcessingFeaturePredicate extends TriggeringSecuringHelper {

    private static final String FEATURE_SECURE_PROCESSING_PROPERTY = "http://javax.xml.XMLConstants/feature/secure-processing";
    private static final String FEATURE_DISALLOW_DOCTYPE_DECL = "http://apache.org/xml/features/disallow-doctype-decl";

    private final MethodMatcher methodMatcher;

    private boolean foundSecuringCall = false;

    private SecureProcessingFeaturePredicate(MethodMatcher triggeringInvocationMatcher, String securingClassName) {
      super(triggeringInvocationMatcher);
      this.methodMatcher = setFeatureMethodMatcher(securingClassName);
    }

    @Override
    public void processSecuringMethodInvocation(MethodInvocationTree methodInvocation) {
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
    public boolean isSecured() {
      return foundSecuringCall;
    }

    private static MethodMatcher setFeatureMethodMatcher(String className) {
      return MethodMatcher.create()
        .typeDefinition(subtypeOf(className))
        .name("setFeature")
        .parameters(JAVA_LANG_STRING, "boolean");
    }
  }

  private static class AccessExternalDTDOrSchemaPredicate extends TriggeringSecuringHelper {

    private static final String ACCESS_EXTERNAL_DTD_PROPERTY = "http://javax.xml.XMLConstants/property/accessExternalDTD";
    private static final String ACCESS_EXTERNAL_SCHEMA_PROPERTY = "http://javax.xml.XMLConstants/property/accessExternalSchema";

    private final MethodMatcher methodMatcher;
    private boolean externalDTDDisabled = false;
    private boolean externalSchemaDisabled = false;

    private AccessExternalDTDOrSchemaPredicate(MethodMatcher triggeringInvocationMatcher, String securingClassName) {
      super(triggeringInvocationMatcher);
      this.methodMatcher = setPropertyMethodMatcher(securingClassName);
    }

    @Override
    public void processSecuringMethodInvocation(MethodInvocationTree methodInvocation) {
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
    public boolean isSecured() {
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
        .parameters(JAVA_LANG_STRING, JAVA_LANG_OBJECT);
    }
  }

  private static class SecureXmlTransformerPredicate extends TriggeringSecuringHelper {

    private static final String TRANSFORMER_FACTORY_CLASS_NAME = "javax.xml.transform.TransformerFactory";

    private static final String ACCESS_EXTERNAL_DTD_PROPERTY = "http://javax.xml.XMLConstants/property/accessExternalDTD";
    private static final String ACCESS_EXTERNAL_STYLESHEET_PROPERTY = "http://javax.xml.XMLConstants/property/accessExternalStylesheet";

    private static final MethodMatcher SET_ATTRIBUTE =
      MethodMatcher.create()
        .typeDefinition(subtypeOf(TRANSFORMER_FACTORY_CLASS_NAME))
        .name("setAttribute")
        .parameters(JAVA_LANG_STRING, JAVA_LANG_OBJECT);

    private boolean hasSecuredExternalDtd = false;
    private boolean hasSecuredExternalStylesheet = false;

    SecureXmlTransformerPredicate() {
      super(MethodMatcher.create()
        .typeDefinition(subtypeOf(TRANSFORMER_FACTORY_CLASS_NAME))
        .name("newInstance")
        .withAnyParameters());
    }

    @Override
    public void processSecuringMethodInvocation(MethodInvocationTree methodInvocation) {
      Arguments arguments = methodInvocation.arguments();

      if (SET_ATTRIBUTE.matches(methodInvocation)) {
        String attributeName = ExpressionsHelper.getConstantValueAsString(arguments.get(0)).value();
        String attributeValue = ExpressionsHelper.getConstantValueAsString(arguments.get(1)).value();
        if ("".equals(attributeValue)) {
          if (ACCESS_EXTERNAL_DTD_PROPERTY.equals(attributeName)) {
            hasSecuredExternalDtd = true;
          } else if (ACCESS_EXTERNAL_STYLESHEET_PROPERTY.equals(attributeName)) {
            hasSecuredExternalStylesheet = true;
          }
        }
      }
    }

    @Override
    public boolean isSecured() {
      return hasSecuredExternalDtd && hasSecuredExternalStylesheet;
    }

    @Override
    public void resetState() {
      hasSecuredExternalDtd = false;
      hasSecuredExternalStylesheet = false;
    }
  }

}
