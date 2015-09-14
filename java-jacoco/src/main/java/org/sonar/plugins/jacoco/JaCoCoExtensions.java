/*
 * SonarQube Java
 * Copyright (C) 2010 SonarSource
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
package org.sonar.plugins.jacoco;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class JaCoCoExtensions {


  public static final Logger LOG = LoggerFactory.getLogger(JaCoCoExtensions.class.getName());

  private JaCoCoExtensions(){
  }

  public static List getExtensions() {
    ImmutableList.Builder<Object> extensions = ImmutableList.builder();

    extensions.addAll(JacocoConfiguration.getPropertyDefinitions());
    extensions.add(
      JacocoConfiguration.class,
      // Unit tests
      JaCoCoSensor.class,
      // Integration tests
      JaCoCoItSensor.class,
      JaCoCoOverallSensor.class);

    return extensions.build();
  }

}
