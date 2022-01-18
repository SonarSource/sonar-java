/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S6373")
public class AllowXMLInclusionCheck  extends AbstractMethodDetection {

  private static final String X_INCLUDE_FEATURE = "http://apache.org/xml/features/xinclude";

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      MethodMatchers.create()
        .ofSubTypes(
          "javax.xml.parsers.DocumentBuilderFactory",
          "javax.xml.parsers.SAXParserFactory")
        .names("setXIncludeAware")
        .addParametersMatcher("boolean")
        .build(),
      MethodMatchers.create()
        .ofSubTypes("javax.xml.stream.XMLInputFactory")
        .names("setProperty")
        .addParametersMatcher("java.lang.String", "java.lang.Object")
        .build(),
      MethodMatchers.create()
        .ofSubTypes(
          "javax.xml.transform.TransformerFactory",
          "javax.xml.validation.SchemaFactory",
          "org.dom4j.io.SAXReader",
          "org.jdom2.input.SAXBuilder")
        .names("setFeature")
        .addParametersMatcher("java.lang.String", "boolean")
        .build()
    );
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (mit.arguments().size() == 2) {
      if(mit.arguments().get(0).asConstant(String.class).filter(X_INCLUDE_FEATURE::equals).isPresent()) {
        checkIncludeFeatureSetter(mit.arguments().get(1));
      }
    } else {
      checkIncludeFeatureSetter(mit.arguments().get(0));
    }
  }

  private void checkIncludeFeatureSetter(ExpressionTree expressionValue) {
    expressionValue
      .asConstant(Boolean.class)
      .filter(Boolean.TRUE::equals)
      .ifPresent(value -> reportIssue(expressionValue, "Disable the inclusion of files in XML processing."));
  }

}
