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
package android.content;

import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import java.io.File;

public abstract class Context {

  public static final String LOCALE_SERVICE = "locale";
  public static final String LOCATION_SERVICE = "location";

  public abstract File getExternalFilesDir(String type);
  public abstract File[] getExternalFilesDirs(String type);
  public abstract File[] getExternalMediaDirs();
  public abstract File getExternalCacheDir();
  public abstract File[] getExternalCacheDirs();
  public abstract File getObbDir();
  public abstract File[] getObbDirs();

  public abstract Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter);
  public abstract Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, int flags);
  public abstract Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, String broadcastPermission, Handler scheduler, int flags);
  public abstract Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, String broadcastPermission, Handler scheduler);

  public abstract void sendBroadcast(Intent intent);
  public abstract void sendBroadcast(Intent intent, String receiverPermission);
  public abstract void sendBroadcastAsUser(Intent intent, UserHandle user);
  public abstract void sendBroadcastAsUser (Intent intent, UserHandle user, String receiverPermission);
  public abstract void sendOrderedBroadcast(Intent intent, String receiverPermission);
  public abstract void sendOrderedBroadcast(Intent intent, String receiverPermission, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras);
  public abstract void sendOrderedBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras);

  public abstract void sendStickyBroadcast(Intent intent);
  public abstract void sendStickyBroadcastAsUser(Intent intent, UserHandle user);
  public abstract void sendStickyOrderedBroadcast(Intent intent, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras);
  public abstract void sendStickyOrderedBroadcastAsUser(Intent intent, UserHandle user, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras);

  public abstract SharedPreferences getSharedPreferences(String name, int mode);
  public abstract SharedPreferences getSharedPreferences(File file, int mode);

  public abstract SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory);
  public abstract SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler errorHandler);

  public abstract Object getSystemService(String name);
}
