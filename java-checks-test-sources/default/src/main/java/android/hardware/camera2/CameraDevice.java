/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

package android.hardware.camera2;

public class CameraDevice {
  public void close() {
    // mock implementation
  }

  public abstract static class StateCallback {
    public StateCallback() {
      // mock implementation
    }

    public void onClosed(CameraDevice camera) {
      // mock implementation
    }

    public abstract void onDisconnected(CameraDevice camera);

    public abstract void onError(CameraDevice camera, int error);

    public abstract void onOpened(CameraDevice camera);

  }
}
