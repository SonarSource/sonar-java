/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package android.content;

import android.os.Handler;
import java.io.File;

public abstract class Context {

  public abstract File getExternalFilesDir(String type);
  public abstract File[] getExternalFilesDirs(String type);
  public abstract File[] getExternalMediaDirs();
  public abstract File getExternalCacheDir();
  public abstract File[] getExternalCacheDirs();
  public abstract File getObbDir();
  public abstract File[] getObbDirs();

  public abstract Intent registerReceiver (BroadcastReceiver receiver, IntentFilter filter);
  public abstract Intent registerReceiver (BroadcastReceiver receiver, IntentFilter filter, int flags);
  public abstract Intent registerReceiver (BroadcastReceiver receiver, IntentFilter filter, String broadcastPermission, Handler scheduler, int flags);
  public abstract Intent registerReceiver (BroadcastReceiver receiver, IntentFilter filter, String broadcastPermission, Handler scheduler);
}


class Intent {

}

class IntentFilter {

}

class BroadcastReceiver {

}
