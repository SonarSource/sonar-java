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
package org.sonar.java.it;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a ruling test project configuration loaded from ruling-projects.json.
 */
public record RulingProject(
  String projectName,
  String projectKey,
  String path,
  BuildType buildType
) {

  public enum BuildType {
    @SerializedName("maven")
    MAVEN,
    @SerializedName("maven-existing")
    MAVEN_EXISTING,
    @SerializedName("sonar-scanner")
    SONAR_SCANNER
  }

  public boolean isMavenBuild() {
    return buildType == BuildType.MAVEN || buildType == BuildType.MAVEN_EXISTING;
  }

  public boolean isExistingProject() {
    return buildType == BuildType.MAVEN_EXISTING;
  }
}
