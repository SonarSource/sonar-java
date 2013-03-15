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
package org.sonar.plugins.java;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.AbstractLanguage;

/**
 * Java language implementation
 *
 * @since 1.3
 */
public class Java extends AbstractLanguage {

  /**
   * Java key
   */
  public static final String KEY = "java";

  /**
   * Java name
   */
  public static final String NAME = "Java";

  /**
   * Java files knows suffixes
   */
  private static final String[] SUFFIXES = {".java", ".jav"};

  /**
   * Default constructor
   */
  public Java() {
    super(KEY, NAME);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sonar.api.resources.AbstractLanguage#getFileSuffixes()
   */
  public String[] getFileSuffixes() {
    return (String[]) ArrayUtils.clone(SUFFIXES);
  }

  public static boolean isJavaFile(java.io.File file) {
    String suffix = "." + StringUtils.substringAfterLast(file.getName(), ".");
    return ArrayUtils.contains(SUFFIXES, suffix);
  }

}
