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
package org.sonar.plugins.java.bridges;

import org.sonar.api.batch.SensorContext;
import org.sonar.api.checks.CheckFactory;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.java.JavaSquid;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.api.SourcePackage;
import org.sonar.squidbridge.api.SourceProject;

/**
 * Pattern visitor : project -> packages -> files
 */
public abstract class Bridge {

  JavaSquid squid;
  ResourceIndex resourceIndex;
  SensorContext context;
  CheckFactory checkFactory;
  RulesProfile profile;

  public boolean needsBytecode() {
    return false;
  }

  protected final void setSquid(JavaSquid squid) {
    this.squid = squid;
  }

  protected final void setCheckFactory(CheckFactory checkFactory) {
    this.checkFactory = checkFactory;
  }

  public void setProfile(RulesProfile profile) {
    this.profile = profile;
  }

  protected final void setResourceIndex(ResourceIndex resourceIndex) {
    this.resourceIndex = resourceIndex;
  }

  protected final void setContext(SensorContext context) {
    this.context = context;
  }

  public void onProject(SourceProject squidProject, Project sonarProject) {

  }

  public void onPackage(SourcePackage squidPackage, Resource sonarPackage) {

  }

  public void onFile(SourceFile squidFile, Resource sonarFile) {

  }

}
