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

package android.bluetooth;

public class BluetoothGatt {

  public static final int CONNECTION_PRIORITY_HIGH = 1;
  public static final int CONNECTION_PRIORITY_LOW_POWER = 2;

  public boolean requestConnectionPriority (int connectionPriority){
    // mock implementation
    return true;
  }
}
