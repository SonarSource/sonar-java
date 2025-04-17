/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.verifier;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleScope;
import org.sonar.api.rules.RuleAnnotationUtils;
import org.sonar.plugins.java.api.CheckRegistrar;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScanner;

public class TestCheckRegistrarContext extends CheckRegistrar.RegistrarContext {

  public final List<Class<? extends JavaCheck>> mainCheckClasses = new ArrayList<>();
  public final List<JavaCheck> mainCheckInstances = new ArrayList<>();
  public final List<RuleKey> mainRuleKeys = new ArrayList<>();

  public final List<Class<? extends JavaCheck>> testCheckClasses = new ArrayList<>();
  public final List<JavaCheck> testCheckInstances = new ArrayList<>();
  public final List<RuleKey> testRuleKeys = new ArrayList<>();

  public final Set<RuleKey> autoScanCompatibleRules = new HashSet<>();

  @Override
  public void registerMainChecks(String repositoryKey, Collection<?> javaCheckClassesAndInstances) {
    validateAndRegisterChecks(repositoryKey, javaCheckClassesAndInstances, mainCheckClasses, mainCheckInstances, mainRuleKeys);
  }

  @Override
  public void registerTestChecks(String repositoryKey, Collection<?> javaCheckClassesAndInstances) {
    validateAndRegisterChecks(repositoryKey, javaCheckClassesAndInstances, testCheckClasses, testCheckInstances, testRuleKeys);
  }

  @Override
  public void registerMainSharedCheck(JavaCheck check, Collection<RuleKey> ruleKeys) {
    registerMainHook(check);
    mainRuleKeys.addAll(ruleKeys);
  }

  private void registerMainHook(JavaCheck check) {
    mainCheckClasses.add(check.getClass());
    mainCheckInstances.add(check);
  }

  @Override
  public void registerTestSharedCheck(JavaCheck check, Collection<RuleKey> ruleKeys) {
    registerTestHook(check);
    testRuleKeys.addAll(ruleKeys);
  }

  private void registerTestHook(JavaCheck check) {
    testCheckClasses.add(check.getClass());
    testCheckInstances.add(check);
  }

  @Override
  public void registerCustomFileScanner(RuleScope ruleScope, JavaFileScanner scanner) {
    if (ruleScope == RuleScope.MAIN || ruleScope == RuleScope.ALL) {
      registerMainHook(scanner);
    }
    if (ruleScope == RuleScope.TEST || ruleScope == RuleScope.ALL) {
      registerTestHook(scanner);
    }
  }

  @Override
  public void registerAutoScanCompatibleRules(Collection<RuleKey> ruleKeys) {
    autoScanCompatibleRules.addAll(ruleKeys);
  }

  private static void validateAndRegisterChecks(String repositoryKey,
                                                Collection<?> javaCheckClassesAndInstances,
                                                List<Class<? extends JavaCheck>> destCheckClasses,
                                                List<JavaCheck> destCheckInstances,
                                                List<RuleKey> destRuleKeys) {
    if (StringUtils.isBlank(repositoryKey)) {
      throw new IllegalArgumentException("Please specify a non blank repository key");
    }
    for (Object javaCheckClassOrInstance : javaCheckClassesAndInstances) {
      Class<? extends JavaCheck> checkClass;
      JavaCheck check;
      try {
        if (javaCheckClassOrInstance instanceof Class) {
          checkClass = (Class<? extends JavaCheck>) javaCheckClassOrInstance;
          check = checkClass.getDeclaredConstructor().newInstance();
        } else {
          check = (JavaCheck) javaCheckClassOrInstance;
          checkClass = check.getClass();
        }
      } catch (ClassCastException | NoSuchMethodException | InstantiationException | IllegalAccessException |
               InvocationTargetException e) {
        throw new IllegalStateException(String.format("Fail to instantiate %s", javaCheckClassOrInstance), e);
      }
      RuleKey ruleKey = RuleKey.of(repositoryKey, RuleAnnotationUtils.getRuleKey(checkClass));
      destCheckClasses.add(checkClass);
      destCheckInstances.add(check);
      destRuleKeys.add(ruleKey);
    }
  }
}
