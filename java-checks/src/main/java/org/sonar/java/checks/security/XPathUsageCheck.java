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
package org.sonar.java.checks.security;

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.NameCriteria;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S4817")
public class XPathUsageCheck extends AbstractMethodDetection {

  private static final String MESSAGE = "Make sure that executing this XPATH expression is safe here.";
  private static final String APACHE_JXPATH_CONTEXT = "org.apache.commons.jxpath.JXPathContext";
  private static final String APACHE_XALAN_XPATH_API = "org.apache.xpath.XPathAPI";
  private static final String JAVA_LANG_STRING = "java.lang.String";

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
      // === javax.xml.xpath.XPath ===
      MethodMatcher.create().typeDefinition("javax.xml.xpath.XPath").name("compile").parameters(JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition("javax.xml.xpath.XPath").name("evaluate").withAnyParameters(),

      // === Apache XML Security ===
      MethodMatcher.create().typeDefinition("org.apache.xml.security.utils.XPathAPI").name("evaluate").withAnyParameters(),
      MethodMatcher.create().typeDefinition("org.apache.xml.security.utils.XPathAPI").name("selectNodeList").withAnyParameters(),

      // === Apache Xalan ===
      MethodMatcher.create().typeDefinition(APACHE_XALAN_XPATH_API).name("eval").withAnyParameters(),
      MethodMatcher.create().typeDefinition(APACHE_XALAN_XPATH_API).name("selectNodeIterator").withAnyParameters(),
      MethodMatcher.create().typeDefinition(APACHE_XALAN_XPATH_API).name("selectNodeList").withAnyParameters(),
      MethodMatcher.create().typeDefinition(APACHE_XALAN_XPATH_API).name("selectSingleNode").withAnyParameters(),

      // === org.apache.commons.jxpath ===
      MethodMatcher.create().typeDefinition(APACHE_JXPATH_CONTEXT).name(NameCriteria.startsWith("compile")).parameters(JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(APACHE_JXPATH_CONTEXT).name(NameCriteria.startsWith("createPath")).withAnyParameters(),
      MethodMatcher.create().typeDefinition(APACHE_JXPATH_CONTEXT).name("getPointer").parameters(JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(APACHE_JXPATH_CONTEXT).name("getValue").withAnyParameters(),
      MethodMatcher.create().typeDefinition(APACHE_JXPATH_CONTEXT).name("iterate").parameters(JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(APACHE_JXPATH_CONTEXT).name("iteratePointers").parameters(JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(APACHE_JXPATH_CONTEXT).name("removeAll").parameters(JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(APACHE_JXPATH_CONTEXT).name("removePath").parameters(JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(APACHE_JXPATH_CONTEXT).name("selectNodes").parameters(JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(APACHE_JXPATH_CONTEXT).name("selectSingleNode").parameters(JAVA_LANG_STRING),
      MethodMatcher.create().typeDefinition(APACHE_JXPATH_CONTEXT).name("setValue").parameters(JAVA_LANG_STRING, "java.lang.Object"));
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    reportIssue(ExpressionUtils.methodName(mit), MESSAGE);
  }

}
