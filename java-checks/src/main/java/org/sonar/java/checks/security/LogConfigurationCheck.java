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
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

@Rule(key = "S4792")
public class LogConfigurationCheck extends AbstractMethodDetection {

  private static final String LOG4J_CONFIGURATOR = "org.apache.logging.log4j.core.config.Configurator";
  private static final String LOG4J_CONFIGURATION_SOURCE = "org.apache.logging.log4j.core.config.ConfigurationSource";
  private static final String MESSAGE = "Make sure that this logger's configuration is safe.";
  private static final String SET_LEVEL = "setLevel";
  private static final String ADD_APPENDER = "addAppender";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS, Tree.Kind.METHOD_REFERENCE, Tree.Kind.CLASS);
  }

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Arrays.asList(
      MethodMatcher.create().typeDefinition("org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory").name("newConfigurationBuilder").withoutParameter(),
      MethodMatcher.create().typeDefinition(LOG4J_CONFIGURATOR).name("setAllLevels").withAnyParameters(),
      MethodMatcher.create().typeDefinition(LOG4J_CONFIGURATOR).name(SET_LEVEL).withAnyParameters(),
      MethodMatcher.create().typeDefinition(LOG4J_CONFIGURATOR).name("setRootLevel").withAnyParameters(),
      MethodMatcher.create().typeDefinition("org.apache.logging.log4j.core.config.Configuration").name(ADD_APPENDER).withAnyParameters(),
      MethodMatcher.create().typeDefinition("org.apache.logging.log4j.core.config.LoggerConfig").name(ADD_APPENDER).withAnyParameters(),
      MethodMatcher.create().typeDefinition("org.apache.logging.log4j.core.config.LoggerConfig").name(SET_LEVEL).withAnyParameters(),
      MethodMatcher.create().typeDefinition("org.apache.logging.log4j.core.LoggerContext").name("setConfigLocation").withAnyParameters(),
      MethodMatcher.create().typeDefinition(LOG4J_CONFIGURATION_SOURCE).name("<init>").withAnyParameters(),
      MethodMatcher.create().typeDefinition(LOG4J_CONFIGURATION_SOURCE).name("fromResource").withAnyParameters(),
      MethodMatcher.create().typeDefinition(LOG4J_CONFIGURATION_SOURCE).name("fromUri").withAnyParameters(),
      MethodMatcher.create().typeDefinition("java.util.logging.LogManager").name("readConfiguration").withAnyParameters(),
      MethodMatcher.create().typeDefinition("java.util.logging.Logger").name(SET_LEVEL).withAnyParameters(),
      MethodMatcher.create().typeDefinition("java.util.logging.Logger").name("addHandler").withAnyParameters(),
      MethodMatcher.create().typeDefinition("ch.qos.logback.classic.Logger").name(ADD_APPENDER).withAnyParameters(),
      MethodMatcher.create().typeDefinition("ch.qos.logback.classic.Logger").name(SET_LEVEL).withAnyParameters(),
      MethodMatcher.create().typeDefinition("ch.qos.logback.classic.joran.JoranConfigurator").name("<init>").withAnyParameters(),
      MethodMatcher.create().typeDefinition("java.lang.System").name("setProperty").parameters("java.lang.String", "java.lang.String")
      );
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.CLASS)) {
      checkConfigurationFactoryExtension(((ClassTree) tree));
    } else {
      super.visitNode(tree);
    }
  }

  private void checkConfigurationFactoryExtension(ClassTree tree) {
    TypeTree superClass = tree.superClass();
    if (superClass != null && superClass.symbolType().is("org.apache.logging.log4j.core.config.ConfigurationFactory")) {
      reportIssue(superClass, MESSAGE);
    }
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (mit.symbol().name().equals("setProperty")) {
      String stringConstant = ExpressionsHelper.getConstantValueAsString(mit.arguments().get(0)).value();
      if ("logback.configurationFile".equals(stringConstant)) {
        reportIssue(mit, MESSAGE);
      }
    } else {
      reportIssue(mit, MESSAGE);
    }
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    reportIssue(newClassTree, MESSAGE);
  }
}
