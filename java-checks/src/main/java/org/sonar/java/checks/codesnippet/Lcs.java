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

import java.util.Comparator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class Lcs<T> {

  private final ElementSequence<T> inputI;
  private final ElementSequence<T> inputJ;
  private final Comparator<T> comparator;
  private boolean areCAndDComputed = false;
  /* c contains the longest common subsequence length */
  private final int[][] c;
  /* d contains the longest valid distance to a match (used when backtracking in C) */
  private final int[][] d;
  private boolean areCommonGroupsComputed = false;
  private final List<CommonGroup> commonGroups = Lists.newArrayList();
  private boolean areVaryingGroupsComputed = false;
  private final List<Group> groups = Lists.newArrayList();
  private final List<VaryingGroup> varyingGroups = Lists.newArrayList();

  public Lcs(ElementSequence<T> inputI, ElementSequence<T> inputJ, Comparator<T> comparator) {
    checkNotNull(inputI);
    checkNotNull(inputJ);
    checkNotNull(comparator);

    this.inputI = inputI;
    this.inputJ = inputJ;
    this.comparator = comparator;
    this.c = new int[inputI.length() + 1][inputJ.length() + 1];
    this.d = new int[inputI.length() + 1][inputJ.length() + 1];
  }

  private void ensureCAndDComputed() {
    if (!areCAndDComputed) {
      computeCAndD();
      areCAndDComputed = true;
    }
  }

  private void computeCAndD() {
    for (int i = 1; i <= inputI.length(); i++) {
      for (int j = 1; j <= inputJ.length(); j++) {
        if (comparator.compare(inputI.elementAt(i - 1), inputJ.elementAt(j - 1)) == 0) {
          c[i][j] = c[i - 1][j - 1] + 1;
          d[i][j] = 0;
        } else {
          c[i][j] = Math.max(c[i - 1][j], c[i][j - 1]);

          if (c[i - 1][j] > c[i][j - 1]) {
            d[i][j] = d[i - 1][j] + 1;
          } else if (c[i - 1][j] < c[i][j - 1]) {
            d[i][j] = d[i][j - 1] + 1;
          } else {
            d[i][j] = Math.max(d[i - 1][j], d[i][j - 1]) + 1;
          }
        }
      }
    }
  }

  public int getLength() {
    ensureCAndDComputed();
    return c[inputI.length()][inputJ.length()];
  }

  private void ensureCommonGroupsComputed() {
    if (!areCommonGroupsComputed) {
      computeCommonGroups();
      areCommonGroupsComputed = true;
    }
  }

  private void computeCommonGroups() {
    ensureCAndDComputed();

    int i = inputI.length();
    int j = inputJ.length();

    while (i != 0 && j != 0) {
      if (comparator.compare(inputI.elementAt(i - 1), inputJ.elementAt(j - 1)) == 0) {
        CommonGroup currentCommonGroup = new CommonGroup();

        do {
          currentCommonGroup.prepend(i - 1, j - 1);
          i--;
          j--;
        } while (i != 0 && j != 0 && comparator.compare(inputI.elementAt(i - 1), inputJ.elementAt(j - 1)) == 0);

        commonGroups.add(0, currentCommonGroup);
      } else if (c[i - 1][j] > c[i][j - 1] || d[i - 1][j] > d[i][j - 1]) {
        i--;
      } else {
        j--;
      }
    }
  }

  public List<CommonGroup> getCommonGroups() {
    ensureCommonGroupsComputed();
    return commonGroups;
  }

  private void ensureVaryingGroupsComputed() {
    if (!areVaryingGroupsComputed) {
      computeVaryingGroups();
      areVaryingGroupsComputed = true;
    }
  }

  private void computeVaryingGroups() {
    ensureCommonGroupsComputed();

    int i = 0;
    int j = 0;

    for (CommonGroup commonGroup : commonGroups) {
      List<Integer> indexesI = commonGroup.getIndexesI();
      List<Integer> indexesJ = commonGroup.getIndexesJ();

      VaryingGroup varyingGroup = getVaryingGroup(i, j, indexesI.get(0), indexesJ.get(0));
      if (!varyingGroup.isEmpty()) {
        groups.add(varyingGroup);
        varyingGroups.add(varyingGroup);
      }

      groups.add(commonGroup);

      i = indexesI.get(indexesI.size() - 1) + 1;
      j = indexesJ.get(indexesJ.size() - 1) + 1;
    }

    VaryingGroup varyingGroup = getVaryingGroup(i, j, inputI.length(), inputJ.length());
    if (!varyingGroup.isEmpty()) {
      groups.add(varyingGroup);
      varyingGroups.add(varyingGroup);
    }
  }

  private VaryingGroup getVaryingGroup(int firstI, int firstJ, int lastI, int lastJ) {
    VaryingGroup varyingGroup = new VaryingGroup();

    for (int i = firstI; i < lastI; i++) {
      varyingGroup.appendI(i);
    }

    for (int j = firstJ; j < lastJ; j++) {
      varyingGroup.appendJ(j);
    }

    return varyingGroup;
  }

  public List<VaryingGroup> getVaryingGroups() {
    ensureVaryingGroupsComputed();
    return varyingGroups;
  }

  public List<Group> getGroups() {
    ensureVaryingGroupsComputed();
    return groups;
  }

}
