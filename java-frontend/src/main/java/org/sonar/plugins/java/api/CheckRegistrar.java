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
package org.sonar.plugins.java.api;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.ScannerSide;
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
   * Context for checks registration.
   */
  class RegistrarContext {
    private String repositoryKey;
    private Iterable<Class<? extends JavaCheck>> checkClasses;
    private Iterable<Class<? extends JavaCheck>> testCheckClasses;

    /**
     * Registers java checks for a given repository.
     * @param repositoryKey key of rule repository
     * @param checkClasses classes of checks for main sources
     * @param testCheckClasses classes of checks for test sources
     */
    public void registerClassesForRepository(String repositoryKey, Iterable<Class<? extends JavaCheck>> checkClasses, Iterable<Class<? extends JavaCheck>> testCheckClasses) {
      Preconditions.checkArgument(StringUtils.isNotBlank(repositoryKey), "Please specify a valid repository key to register your custom rules");
      this.repositoryKey = repositoryKey;
      this.checkClasses = checkClasses;
      this.testCheckClasses = testCheckClasses;
    }

    /**
     * getter for repository key.
     * @return the repository key.
     */
    public String repositoryKey() {
      return repositoryKey;
    }

    /**
     * get main source check classes
     * @return iterable of main checks classes
     */
    public Iterable<Class<? extends JavaCheck>> checkClasses() {
      return checkClasses;
    }

    /**
     * get test source check classes
     * @return iterable of test checks classes
     */
    public Iterable<Class<? extends JavaCheck>> testCheckClasses() {
      return testCheckClasses;
    }

  }

}
