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
package org.sonar.java.checks.security;

import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S5324")
public class AndroidExternalStorageCheck extends AbstractMethodDetection {

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      MethodMatchers.create()
        .ofTypes("android.os.Environment")
        .names("getExternalStorageDirectory", "getExternalStoragePublicDirectory")
        .withAnyParameters()
        .build(),
      MethodMatchers.create()
        .ofTypes("android.content.Context")
        .names("getExternalFilesDir", "getExternalFilesDirs", "getExternalMediaDirs", "getExternalCacheDir", "getExternalCacheDirs", "getObbDir", "getObbDirs")
        .withAnyParameters()
        .build()
    );
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    reportIssue(mit, "Make sure that external files are accessed safely here.");
  }
}
