/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.java;

import com.google.common.annotations.Beta;

import javax.annotation.Nullable;

/**
 * Implementing this interface allows a check to be executed - or not - during analysis, depending
 * of expected java version.
 * <br />
 * In order to be taken into account during analysis, the property <code>sonar.java.source</code> must be set.
 */
@Beta
public interface JavaVersionAwareVisitor {
  /**
   * Control if the check is compatible with the java version of the project being analyzed. The version used as parameter depends of the
   * property <code>sonar.java.source</code>. Note that if the property is not set, The method will be called with <code>null</code> as parameter.
   *
   * @param version The java version of the sources (6 for java 1.6, 7 for 1.7, etc.), <code>null</code> if property not set.
   * @return true if the check is compatible with detected java version and should be executed on sources, false otherwise.
   */
  boolean isCompatibleWithJavaVersion(@Nullable Integer version);
}
