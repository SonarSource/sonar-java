/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4435")
public class SecureXmlTransformerCheck extends AbstractMethodDetection {

  private static final String TRANSFORMER_FACTORY_CLASS_NAME = "javax.xml.transform.TransformerFactory";

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
        .ofSubTypes(TRANSFORMER_FACTORY_CLASS_NAME)
        .names("newInstance")
        .withAnyParameters()
        .build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree methodInvocation) {
    Tree enclosingMethod = ExpressionUtils.getEnclosingMethod(methodInvocation);
    if (enclosingMethod == null) {
      return;
    }
    MethodBodyVisitor visitor = new MethodBodyVisitor();
    enclosingMethod.accept(visitor);
    if (!visitor.foundCallsToSecuringMethods()) {
      reportIssue(methodInvocation.methodSelect(), "Secure this \"Transformer\" by either disabling external DTDs or enabling secure processing.");
    }
  }

  private static class MethodBodyVisitor extends BaseTreeVisitor {

    private static final String FEATURE_SECURE_PROCESSING_PROPERTY = "http://javax.xml.XMLConstants/feature/secure-processing";
    private static final String ACCESS_EXTERNAL_DTD_PROPERTY = "http://javax.xml.XMLConstants/property/accessExternalDTD";
    private static final String ACCESS_EXTERNAL_STYLESHEET_PROPERTY = "http://javax.xml.XMLConstants/property/accessExternalStylesheet";

    private static final MethodMatchers SET_FEATURE =
      MethodMatchers.create()
        .ofSubTypes(TRANSFORMER_FACTORY_CLASS_NAME)
        .names("setFeature")
        .addParametersMatcher("java.lang.String", "boolean")
        .build();

    private static final MethodMatchers SET_ATTRIBUTE =
      MethodMatchers.create()
        .ofSubTypes(TRANSFORMER_FACTORY_CLASS_NAME)
        .names("setAttribute")
        .addParametersMatcher("java.lang.String", "java.lang.Object")
        .build();

    private boolean hasSecureProcessingFeature = false;
    private boolean hasSecuredExternalDtd = false;
    private boolean hasSecuredExternalStylesheet = false;

    private boolean foundCallsToSecuringMethods() {
      return hasSecureProcessingFeature || (hasSecuredExternalDtd && hasSecuredExternalStylesheet);
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree methodInvocation) {
      Arguments arguments = methodInvocation.arguments();

      if (SET_FEATURE.matches(methodInvocation)
        && FEATURE_SECURE_PROCESSING_PROPERTY.equals(ExpressionsHelper.getConstantValueAsString(arguments.get(0)).value())
        && LiteralUtils.isTrue(arguments.get(1))) {

        hasSecureProcessingFeature = true;
      }

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

      super.visitMethodInvocation(methodInvocation);
    }

  }
}
