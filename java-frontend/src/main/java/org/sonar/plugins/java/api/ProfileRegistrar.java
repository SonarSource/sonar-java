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
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ServerSide;
import org.sonar.java.annotations.Beta;
import org.sonarsource.api.sonarlint.SonarLintSide;

/**
 * This class can be extended to provide additional rule keys in the builtin default quality profile.
 *
 * <pre>
 *   {@code
 *     public void register(RegistrarContext registrarContext) {
 *       registrarContext.registerDefaultQualityProfileRules(ruleKeys);
 *     }
 *   }
 * </pre>
 *
 *  Note: It's possible to convert checkClass to RuleKey using:
 * <pre>
 *   {@code
 *     RuleKey.of(repositoryKey, RuleAnnotationUtils.getRuleKey(checkClass))
 *   }
 * </pre>
 */
@Beta
@SonarLintSide
@ServerSide
public interface ProfileRegistrar {

  /**
   * This method is called on server side and during an analysis to modify the builtin default quality profile for java.
   */
  void register(RegistrarContext registrarContext);

  interface RegistrarContext {

    /**
     * Registers additional rules into the "Sonar Way" default quality profile for the language "java".
     */
    void registerDefaultQualityProfileRules(Collection<RuleKey> ruleKeys);

  }

}
