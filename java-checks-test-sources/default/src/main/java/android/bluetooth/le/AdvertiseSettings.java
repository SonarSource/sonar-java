/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
 * long with this program; if not, see https://sonarsource.com/license/ssal/
 */

package android.bluetooth.le;

public final class AdvertiseSettings {

  public static final int ADVERTISE_MODE_BALANCED = 1;
  public static final int ADVERTISE_MODE_LOW_LATENCY = 2;
  public static final int ADVERTISE_MODE_LOW_POWER = 0;

  public static final class Builder {

    public AdvertiseSettings build() {
      // mock implementation
      return new AdvertiseSettings();
    }

    public Builder setAdvertiseMode(int advertiseMode) {
      // mock implementation
      return this;
    }
  }
}
