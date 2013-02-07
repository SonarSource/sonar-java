/*
 * Sonar Java
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

import org.sonar.api.rules.RulePriority;

public class FindbugsLevelUtils {

  public RulePriority from(String priority) {
    if ("1".equals(priority)) {
      return RulePriority.BLOCKER;
    }
    if ("2".equals(priority)) {
      return RulePriority.MAJOR;
    }
    if ("3".equals(priority)) {
      return RulePriority.INFO;
    }
    throw new IllegalArgumentException("Priority not supported: " + priority);
  }

  public String from(RulePriority priority) {
    switch (priority) {
      case BLOCKER:
      case CRITICAL:
        return "1";
      case MAJOR:
      case MINOR:
        return "2";
      case INFO:
        return "3";
      default:
        throw new IllegalArgumentException();
    }
  }

}
