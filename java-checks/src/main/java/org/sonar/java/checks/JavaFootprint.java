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
package org.sonar.java.checks;

import com.google.common.collect.Sets;
import org.sonar.squidbridge.recognizer.CamelCaseDetector;
import org.sonar.squidbridge.recognizer.ContainsDetector;
import org.sonar.squidbridge.recognizer.Detector;
import org.sonar.squidbridge.recognizer.EndWithDetector;
import org.sonar.squidbridge.recognizer.KeywordsDetector;
import org.sonar.squidbridge.recognizer.LanguageFootprint;

import java.util.Set;

public final class JavaFootprint implements LanguageFootprint {

  private final Set<Detector> detectors = Sets.newHashSet();

  public JavaFootprint() {
    detectors.add(new EndWithDetector(0.95, '}', ';', '{'));
    detectors.add(new KeywordsDetector(0.7, "||", "&&"));
    detectors.add(new KeywordsDetector(0.3, "public", "abstract", "class", "implements", "extends", "return", "throw",
        "private", "protected", "enum", "continue", "assert", "package", "synchronized", "boolean", "this", "double", "instanceof",
        "final", "interface", "static", "void", "long", "int", "float", "super", "true", "case:"));
    detectors.add(new ContainsDetector(0.95, "++", "for(", "if(", "while(", "catch(", "switch(", "try{", "else{"));
    detectors.add(new CamelCaseDetector(0.5));
  }

  @Override
  public Set<Detector> getDetectors() {
    return detectors;
  }

}
