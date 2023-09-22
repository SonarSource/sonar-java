/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.checks.verifier;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleAnnotationUtils;
import org.sonar.plugins.java.api.CheckRegistrar;
import org.sonar.plugins.java.api.JavaCheck;

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
    mainCheckClasses.add(check.getClass());
    mainCheckInstances.add(check);
    mainRuleKeys.addAll(ruleKeys);
  }

  @Override
  public void registerTestSharedCheck(JavaCheck check, Collection<RuleKey> ruleKeys) {
    testCheckClasses.add(check.getClass());
    testCheckInstances.add(check);
    testRuleKeys.addAll(ruleKeys);
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
