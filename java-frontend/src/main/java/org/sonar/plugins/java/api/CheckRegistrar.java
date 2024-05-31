/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.plugins.java.api;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.java.Preconditions;
import org.sonar.java.annotations.Beta;
import org.sonarsource.api.sonarlint.SonarLintSide;

/**
 * This batch extension should be extended to provide the classes to be used to instantiate checks.
 * The register method has to be implemented and the registrarContext should register the repository keys.
 *
 * <pre>
 *   {@code
 *   public void register(RegistrarContext registrarContext) {
 *     registrarContext.registerClassesForRepository("RepositoryKey", listOfCheckClasses);
 *   }
 *   }
 * </pre>
 */
@Beta
@SonarLintSide
@ScannerSide
public interface CheckRegistrar {

  /**
   * This method is called during an analysis to get the classes to use to instantiate checks.
   * @param registrarContext the context that will be used by the java-plugin to retrieve the classes for checks.
   */
  void register(RegistrarContext registrarContext);

  /**
   * This method is called during the definition of the SonarJava rules, implementing it allows to register
   * rules in the same rule repository as SonarJava while its being defined.
   * @param context the context that will be used by the java-plugin to define the rules in its repository.
   * @param javaRepository the repository currently being defined
   */
  default void customRulesDefinition(RulesDefinition.Context context, RulesDefinition.NewRepository javaRepository) {

  }

  default void register(RegistrarContext registrarContext, CheckFactory checkFactory) {
    register(registrarContext);
  }

  /**
   * Context for checks registration.
   */
  class RegistrarContext {
    private String repositoryKey;
    private Iterable<Class<? extends JavaCheck>> mainCheckClassList;
    private Iterable<Class<? extends JavaCheck>> testCheckClassList;

    /**
     * Registers java checks for a given repository.
     * @param repositoryKey key of rule repository
     * @param checkClasses classes of checks for main sources
     * @param testCheckClasses classes of checks for test sources
     */
    public void registerClassesForRepository(String repositoryKey, Iterable<Class<? extends JavaCheck>> checkClasses, Iterable<Class<? extends JavaCheck>> testCheckClasses) {
      Preconditions.checkArgument(StringUtils.isNotBlank(repositoryKey), "Please specify a valid repository key to register your custom rules");
      this.repositoryKey = repositoryKey;
      this.mainCheckClassList = checkClasses;
      this.testCheckClassList = testCheckClasses;
      registerMainChecks(repositoryKey, asCollection(checkClasses));
      registerTestChecks(repositoryKey, asCollection(testCheckClasses));
    }

    /**
     * getter for repository key.
     * @return the repository key.
     * @deprecated RegistrarContext should just forward the registration and not have any getters
     */
    @Deprecated(since = "7.25", forRemoval = true)
    public String repositoryKey() {
      return repositoryKey;
    }

    /**
     * get main source check classes
     * @return iterable of main checks classes
     * @deprecated RegistrarContext should just forward the registration and not have any getters
     */
    @Deprecated(since = "7.25", forRemoval = true)
    public Iterable<Class<? extends JavaCheck>> checkClasses() {
      return mainCheckClassList;
    }

    /**
     * get test source check classes
     * @return iterable of test checks classes
     * @deprecated RegistrarContext should just forward the registration and not have any getters
     */
    @Deprecated(since = "7.25", forRemoval = true)
    public Iterable<Class<? extends JavaCheck>> testCheckClasses() {
      return testCheckClassList;
    }

    /**
     * Registers main code java checks for a given repository.
     * @param repositoryKey key of rule repository
     * @param javaCheckClassesAndInstances a collection of <code>Class<? extends JavaCheck></code> and
     *        <code>JavaCheck></code> instances
     */
    public void registerMainChecks(String repositoryKey, Collection<?> javaCheckClassesAndInstances) {
      // to be overridden
    }

    /**
     * Register main code java checks which have already been initialized by a CheckFactory.
     */
    @Beta
    public void registerMainChecks(Checks<JavaCheck> checks, Collection<?> javaCheckClassesAndInstances){
      // to be overridden
    }

    /**
     * Register test code java checks which have already been initialized by a CheckFactory.
     */
    @Beta
    public void registerTestChecks(Checks<JavaCheck> checks, Collection<?> javaCheckClassesAndInstances){
      // to be overridden
    }

    /**
     * Registers test code java checks for a given repository.
     * @param repositoryKey key of rule repository
     * @param javaCheckClassesAndInstances a collection of <code>Class<? extends JavaCheck></code> and
     *        <code>JavaCheck></code> instances
     */
    public void registerTestChecks(String repositoryKey, Collection<?> javaCheckClassesAndInstances) {
      // to be overridden
    }

    /**
     * Registers one main code check related to not one but a list of rules. The check will be active if at least one
     * of the given rule key is active. In this context injection of @RuleProperty and auto instantiation of rules
     * defined as template in RulesDefinition will not work. And the reportIssue mechanism will not be able to find the
     * RuleKey automatically.
     */
    public void registerMainSharedCheck(JavaCheck check, Collection<RuleKey> ruleKeys) {
      // to be overridden
    }

    /**
     * Registers one test code check related to not one but a list of rules. The check will be active if at least one
     * of the given rule key is active. In this context injection of @RuleProperty and auto instantiation of rules
     * defined as template in RulesDefinition will not work.
     */
    public void registerTestSharedCheck(JavaCheck check, Collection<RuleKey> ruleKeys) {
      // to be overridden
    }

    /**
     * Cannot be used outside of Sonar Products. Registers rules compatible with the autoscan context.
     * Note: It's possible to convert checkClass to RuleKey using:
     * <pre>
     *   RuleKey.of(repositoryKey, RuleAnnotationUtils.getRuleKey(checkClass))
     * </pre>
     */
    public void registerAutoScanCompatibleRules(Collection<RuleKey> ruleKeys) {
      // to be overridden
    }

    private static <T> Collection<T> asCollection(@Nullable Iterable<T> iterable) {
      return iterable != null ?
        StreamSupport.stream(iterable.spliterator(), false).toList() :
        Collections.emptyList();
    }
  }

}
