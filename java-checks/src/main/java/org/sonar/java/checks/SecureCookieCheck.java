/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.security.InstanceShouldBeInitializedCorrectlyBase;
import org.sonar.plugins.java.api.tree.NewClassTree;

@Rule(key = "S2092")
public class SecureCookieCheck extends InstanceShouldBeInitializedCorrectlyBase {

  @Override
  protected String getMessage() {
    return "Add the \"secure\" attribute to this cookie";
  }

  @Override
  protected boolean constructorInitializesCorrectly(NewClassTree newClassTree) {
    return false;
  }

  @Override
  protected List<String> getSetterNames() {
    return Arrays.asList("setSecure");
  }

  @Override
  protected List<String> getClasses() {
    return Collections.singletonList("javax.servlet.http.Cookie");
  }
}
