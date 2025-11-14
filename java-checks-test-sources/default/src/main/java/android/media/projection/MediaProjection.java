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

package android.media.projection;

import android.hardware.display.VirtualDisplay;
import android.os.Handler;
import android.view.Surface;

public class MediaProjection {

  public VirtualDisplay createVirtualDisplay(
    String name,
    int width,
    int height,
    int dpi,
    int flags,
    Surface surface,
    VirtualDisplay.Callback callback,
    Handler handler) {
    // mock implementation
    return new VirtualDisplay();
  }
}
