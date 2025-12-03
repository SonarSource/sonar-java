/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
package org.sonar.java.checks.security;

import java.util.Optional;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.checks.AbstractHashAlgorithmChecker;
import org.sonarsource.analyzer.commons.collections.SetUtils;

@Rule(key = "S4790")
public class DataHashingCheck extends AbstractHashAlgorithmChecker {

  private static final Set<String> DEPRECATED_HASH_CLASSES = SetUtils.immutableSetOf(
    DeprecatedSpringPasswordEncoder.MD5.classFqn,
    DeprecatedSpringPasswordEncoder.SHA.classFqn,
    DeprecatedSpringPasswordEncoder.LDAP.classFqn,
    DeprecatedSpringPasswordEncoder.MD4.classFqn,
    DeprecatedSpringPasswordEncoder.MESSAGE_DIGEST.classFqn,
    DeprecatedSpringPasswordEncoder.NO_OP.classFqn,
    DeprecatedSpringPasswordEncoder.STANDARD.classFqn
  );

  private static final String MESSAGE = "Make sure this weak hash algorithm is not used in a sensitive context here.";

  @Override
  protected Optional<String> getMessageForClass(String className) {
    return DEPRECATED_HASH_CLASSES.contains(className) ? Optional.of(MESSAGE) : Optional.empty();
  }

  @Override
  protected String getMessageForAlgorithm(String algorithmName) {
    return MESSAGE;
  }
}
