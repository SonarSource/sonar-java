/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.java.api;

import java.util.Collection;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ServerSide;
import org.sonar.java.annotations.Beta;
import org.sonarsource.api.sonarlint.SonarLintSide;

/**
 * This class can be extended to provide additional rule keys in builtin quality profiles.
 *
 * <pre>
 *   {@code
 *     public void register(RegistrarContext registrarContext) {
 *       registrarContext.registerDefaultQualityProfileRules(ruleKeys);
 *       registrarContext.registerRules("Sonar agentic AI", ruleKeys);
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
   * This method is called on server side and during an analysis to modify builtin quality profiles for Java.
   */
  void register(RegistrarContext registrarContext);

  interface RegistrarContext {

    /**
     * Registers additional rules into the "Sonar way" default quality profile for the language "java".
     */
    default void registerDefaultQualityProfileRules(Collection<RuleKey> ruleKeys) {
      registerRules("Sonar way", ruleKeys);
    }

    /**
     * Registers additional rules into a builtin quality profile for the language "java".
     *
     * <p>
     * The profile name is expected to match the profile string coming from RSPEC metadata ({@code defaultQualityProfiles}).
     * </p>
     */
    void registerRules(String qualityProfileName, Collection<RuleKey> ruleKeys);

  }

}
