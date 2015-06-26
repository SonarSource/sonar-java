/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.java.api;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchExtension;

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
public interface CheckRegistrar extends BatchExtension {

  /**
   * This method is called during an analysis to get the classes to use to instantiate checks.
   * @param registrarContext the context that will be used by the java-plugin to retrieve the classes for checks.
   */
  void register(RegistrarContext registrarContext);

  class RegistrarContext {
    private String repositoryKey;
    private Iterable<Class<? extends JavaCheck>> checkClasses;
    private Iterable<Class<? extends JavaCheck>> testCheckClasses;

    public void registerClassesForRepository(String repositoryKey, Iterable<Class<? extends JavaCheck>> checkClasses, Iterable<Class<? extends JavaCheck>> testCheckClasses) {
      Preconditions.checkArgument(StringUtils.isNotBlank(repositoryKey), "Please specify a valid repository key to register your custom rules");
      this.repositoryKey = repositoryKey;
      this.checkClasses = checkClasses;
      this.testCheckClasses = testCheckClasses;
    }

    public String repositoryKey() {
      return repositoryKey;
    }

    public Iterable<Class<? extends JavaCheck>> checkClasses() {
      return checkClasses;
    }

    public Iterable<Class<? extends JavaCheck>> testCheckClasses() {
      return testCheckClasses;
    }

  }

}
