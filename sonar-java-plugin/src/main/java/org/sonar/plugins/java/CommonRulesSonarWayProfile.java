/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.plugins.java;

import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.profiles.XMLProfileParser;
import org.sonar.api.utils.ValidationMessages;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class CommonRulesSonarWayProfile extends ProfileDefinition {

  private final XMLProfileParser parser;

  public CommonRulesSonarWayProfile(XMLProfileParser parser) {
    this.parser = parser;
  }

  @Override
  public RulesProfile createProfile(ValidationMessages validationMessages) {
    RulesProfile rulesProfile = null;
    
    try (InputStream input = getClass().getResourceAsStream("/org/sonar/plugins/java/common_rules_sonar_way.xml");
      InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
      rulesProfile = parser.parse(reader, validationMessages);
    } catch (IOException e) {
      // close Quietly
    }
    
    return rulesProfile;
  }

}
