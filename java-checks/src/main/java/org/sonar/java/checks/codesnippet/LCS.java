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
package org.sonar.java.checks.codesnippet;

import com.google.common.collect.Lists;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class LCS {

  private final String inputI;
  private final String inputJ;
  private boolean isCComputed;
  private final int[][] c;
  private boolean areGroupsComputed;
  private final List<Group> groups = Lists.newArrayList();

  public LCS(String inputI, String inputJ) {
    checkNotNull(inputI);
    checkNotNull(inputJ);

    this.inputI = inputI;
    this.inputJ = inputJ;
    this.c = new int[inputI.length() + 1][inputJ.length() + 1];
  }

  private void ensureCComputed() {
    if (!isCComputed) {
      computeC();
      isCComputed = true;
    }
  }

  private void computeC() {
    for (int i = 1; i <= inputI.length(); i++) {
      for (int j = 1; j <= inputJ.length(); j++) {
        if (inputI.charAt(i - 1) == inputJ.charAt(j - 1)) {
          c[i][j] = c[i - 1][j - 1] + 1;
        } else {
          c[i][j] = Math.max(c[i - 1][j], c[i][j - 1]);
        }
      }
    }
  }

  public int getLength() {
    ensureCComputed();
    return c[inputI.length()][inputJ.length()];
  }

  private void ensureGroupsComputed() {
    if (!areGroupsComputed) {
      computeGroups();
      areGroupsComputed = true;
    }
  }

  private void computeGroups() {
    ensureCComputed();

    int i = inputI.length();
    int j = inputJ.length();

    while (i != 0 && j != 0) {
      if (inputI.charAt(i - 1) == inputJ.charAt(j - 1)) {
        Group currentGroup = new Group();

        do {
          currentGroup.prepend(i - 1, j - 1);
          i--;
          j--;
        } while (i != 0 && j != 0 && inputI.charAt(i - 1) == inputJ.charAt(j - 1));

        groups.add(0, currentGroup);
      } else if (c[i - 1][j] == c[i][j]) {
        i--;
      } else {
        j--;
      }
    }
  }

  public List<Group> getGroups() {
    ensureGroupsComputed();
    return groups;
  }

}
