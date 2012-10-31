/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
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
