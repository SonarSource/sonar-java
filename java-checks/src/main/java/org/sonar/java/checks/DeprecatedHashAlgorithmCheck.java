/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.checks;

import java.util.Map;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MapBuilder;

@Rule(key = "S2070")
public class DeprecatedHashAlgorithmCheck extends AbstractHashAlgorithmChecker {

  private static final String MESSAGE_FORMAT = "Don't rely on %s because it is deprecated and use a stronger hashing algorithm.";
  
  private static final Map<String, String> MESSAGE_PER_CLASS = MapBuilder.<String, String>newMap()
    .add(DeprecatedSpringPasswordEncoder.MD5.classFqn, "Use a stronger hashing algorithm than MD5.")
    .add(DeprecatedSpringPasswordEncoder.SHA.classFqn, "Don't rely on " + DeprecatedSpringPasswordEncoder.SHA.className + " because it is deprecated.")
    .add(DeprecatedSpringPasswordEncoder.LDAP.classFqn, String.format(MESSAGE_FORMAT, DeprecatedSpringPasswordEncoder.LDAP.className))
    .add(DeprecatedSpringPasswordEncoder.MD4.classFqn, String.format(MESSAGE_FORMAT, DeprecatedSpringPasswordEncoder.MD4.className))
    .add(DeprecatedSpringPasswordEncoder.MESSAGE_DIGEST.classFqn,
      String.format(MESSAGE_FORMAT, DeprecatedSpringPasswordEncoder.MESSAGE_DIGEST.className))
    .add(DeprecatedSpringPasswordEncoder.NO_OP.classFqn, "Use a stronger hashing algorithm than this fake one.")
    .add(DeprecatedSpringPasswordEncoder.STANDARD.classFqn, "Use a stronger hashing algorithm.")
    .build();


  @Override
  protected Optional<String> getMessageForClass(String className) {
    return Optional.ofNullable(MESSAGE_PER_CLASS.get(className));
  }

  @Override
  protected String getMessageForAlgorithm(String algorithmName) {
    return "Use a stronger hashing algorithm than " + algorithmName + ".";
  }
}
