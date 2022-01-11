/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
