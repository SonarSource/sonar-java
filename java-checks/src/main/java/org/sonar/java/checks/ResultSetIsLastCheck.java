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
package org.sonar.java.checks;

import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S2232",
    priority = Priority.CRITICAL,
    tags = {"performance", "pitfall"})
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class ResultSetIsLastCheck extends AbstractMethodDetection {
  public ResultSetIsLastCheck() {
    super(AbstractMethodDetection.MethodDefinition.create().type("java.sql.ResultSet").name("isLast"));
  }

  @Override
  protected void onMethodFound(MethodInvocationTree mit) {
    addIssue(mit, "Remove this call to \"isLast()\".");
  }
}
