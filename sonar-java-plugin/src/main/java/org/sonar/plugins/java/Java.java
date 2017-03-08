/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.plugins.java;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.AbstractLanguage;

import java.util.Arrays;

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
   * Key of the file suffix parameter
   */
  public static final String FILE_SUFFIXES_KEY = "sonar.java.file.suffixes";

  /**
   * Default Java files knows suffixes
   */
  public static final String DEFAULT_FILE_SUFFIXES = ".java,.jav";

  /**
   * Key of the java version used for sources
   */
  public static final String SOURCE_VERSION = "sonar.java.source";

  /**
   * Settings of the plugin.
   */
  private final Settings settings;

  /**
   * Default constructor
   */
  public Java(Settings settings) {
    super(KEY, NAME);
    this.settings = settings;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sonar.api.resources.AbstractLanguage#getFileSuffixes()
   */
  @Override
  public String[] getFileSuffixes() {
    String[] suffixes = Arrays.stream(settings.getStringArray(Java.FILE_SUFFIXES_KEY)).filter(s -> s != null && !s.trim().isEmpty()).toArray(String[]::new);
    if (suffixes.length == 0) {
      suffixes = StringUtils.split(DEFAULT_FILE_SUFFIXES, ',');
    }
    return suffixes;
  }

}
