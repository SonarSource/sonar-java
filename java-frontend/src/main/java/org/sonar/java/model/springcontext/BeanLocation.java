/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model.springcontext;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.reporting.AnalyzerMessage;

/**
 * Source location of a Spring bean definition within the project.
 *
 * @param inputFile    the file in which the bean is declared
 * @param mainLocation the precise text span of the bean declaration, used for reporting issues
 */
public record BeanLocation(InputFile inputFile, AnalyzerMessage.TextSpan mainLocation) {
}
