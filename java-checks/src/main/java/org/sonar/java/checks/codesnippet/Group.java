/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonar.java.checks.codesnippet;

import com.google.common.collect.Lists;

import java.util.List;

public class Group {

  private final List<Integer> indexesI = Lists.newLinkedList();
  private final List<Integer> indexesJ = Lists.newLinkedList();

  public Group prepend(int i, int j) {
    indexesI.add(0, i);
    indexesJ.add(0, j);

    return this;
  }

  public Group append(int i, int j) {
    indexesI.add(i);
    indexesJ.add(j);

    return this;
  }

  public List<Integer> getIndexesI() {
    return indexesI;
  }

  public List<Integer> getIndexesJ() {
    return indexesJ;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + indexesI.hashCode();
    result = prime * result + indexesJ.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Group other = (Group) obj;
    if (!indexesI.equals(other.indexesI)) {
      return false;
    }
    if (!indexesJ.equals(other.indexesJ)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "i = " + indexesI + ", j = " + indexesJ;
  }

}
