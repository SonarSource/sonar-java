/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.verifier.internal;

import org.sonar.api.SonarEdition;
import org.sonar.api.SonarProduct;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.utils.Version;

final class InternalSonarRuntime implements SonarRuntime {

  static final Version VERSION_7_9 = Version.create(7, 9);

  @Override
  public SonarQubeSide getSonarQubeSide() {
    return SonarQubeSide.SCANNER;
  }

  @Override
  public SonarProduct getProduct() {
    return SonarProduct.SONARLINT;
  }

  @Override
  public SonarEdition getEdition() {
    return SonarEdition.COMMUNITY;
  }

  @Override
  public Version getApiVersion() {
    return VERSION_7_9;
  }
}
