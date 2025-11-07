/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.java;

import java.util.Arrays;
import org.sonar.api.config.Configuration;
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
   * Key of the file suffix parameter
   */
  public static final String FILE_SUFFIXES_KEY = "sonar.java.file.suffixes";

  /**
   * Default Java files knows suffixes
   */
  public static final String DEFAULT_FILE_SUFFIXES = ".java,.jav";

  /**
   * Settings of the plugin.
   */
  private final Configuration settings;

  /**
   * Default constructor
   */
  public Java(Configuration settings) {
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
    return suffixes.length > 0 ? suffixes : DEFAULT_FILE_SUFFIXES.split(",");
  }

}
