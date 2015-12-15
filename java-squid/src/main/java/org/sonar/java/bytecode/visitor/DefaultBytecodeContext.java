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
package org.sonar.java.bytecode.visitor;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.resources.Resource;
import org.sonar.java.SonarComponents;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaResourceLocator;

import javax.annotation.Nullable;
import java.io.File;

public class DefaultBytecodeContext implements BytecodeContext {

  private final SonarComponents sonarComponents;
  JavaResourceLocator javaResourceLocator;

  @VisibleForTesting
  public DefaultBytecodeContext(JavaResourceLocator javaResourceLocator) {
    this(null, javaResourceLocator);
  }

  public DefaultBytecodeContext(@Nullable SonarComponents sonarComponents, JavaResourceLocator javaResourceLocator) {
    this.sonarComponents = sonarComponents;
    this.javaResourceLocator = javaResourceLocator;
  }

  @Override
  public void setJavaResourceLocator(JavaResourceLocator javaResourceLocator) {
    this.javaResourceLocator = javaResourceLocator;
  }

  @Override
  public JavaResourceLocator getJavaResourceLocator() {
    return javaResourceLocator;
  }

  @Override
  public void reportIssue(JavaCheck check, Resource resource, String message, int line) {
    if (sonarComponents != null) {
      sonarComponents.addIssue(new File(resource.getPath()), check, line, message, null);
    }
  }

}
