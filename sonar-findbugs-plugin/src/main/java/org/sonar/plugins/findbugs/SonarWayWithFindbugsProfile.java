/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.plugins.findbugs;

import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Java;
import org.sonar.api.utils.ValidationMessages;

import java.io.InputStreamReader;
import java.io.Reader;

public class SonarWayWithFindbugsProfile extends ProfileDefinition {

  private final FindbugsProfileImporter importer;

  public SonarWayWithFindbugsProfile(FindbugsProfileImporter importer) {
    this.importer = importer;
  }

  @Override
  public RulesProfile createProfile(ValidationMessages messages) {
    Reader pmdSonarWayProfile = new InputStreamReader(this.getClass().getResourceAsStream(
      "/org/sonar/plugins/findbugs/profile-sonar-way-findbugs.xml"));
    RulesProfile profile = importer.importProfile(pmdSonarWayProfile, messages);
    profile.setLanguage(Java.KEY);
    profile.setName(RulesProfile.SONAR_WAY_FINDBUGS_NAME);
    return profile;
  }

}
