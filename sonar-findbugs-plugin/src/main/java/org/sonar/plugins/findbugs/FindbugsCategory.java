/*
 * SonarQube Java
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

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public final class FindbugsCategory {
  private static final Map<String, String> FINDBUGS_TO_SONAR = ImmutableMap.<String, String> builder()
    .put("BAD_PRACTICE", "Bad practice")
    .put("CORRECTNESS", "Correctness")
    .put("MT_CORRECTNESS", "Multithreaded correctness")
    .put("I18N", "Internationalization")
    .put("EXPERIMENTAL", "Experimental")
    .put("MALICIOUS_CODE", "Malicious code")
    .put("PERFORMANCE", "Performance")
    .put("SECURITY", "Security")
    .put("STYLE", "Style")
    .build();

  public static String findbugsToSonar(String findbugsCategKey) {
    return FINDBUGS_TO_SONAR.get(findbugsCategKey);
  }

  private FindbugsCategory() {
  }

}
