package android.content;

import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import java.io.File;

public class ContextWrapper extends Context {
  @Override
  public File getExternalFilesDir(String type) {
    return null;
  }

  @Override
  public File[] getExternalFilesDirs(String type) {
    return new File[0];
  }

  @Override
  public File[] getExternalMediaDirs() {
    return new File[0];
  }

  @Override
  public File getExternalCacheDir() {
    return null;
  }

  @Override
  public File[] getExternalCacheDirs() {
    return new File[0];
  }

  @Override
  public File getObbDir() {
    return null;
  }

  @Override
  public File[] getObbDirs() {
    return new File[0];
  }

  @Override
  public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
    return null;
  }

  @Override
  public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, int flags) {
    return null;
  }

  @Override
  public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, String broadcastPermission, Handler scheduler, int flags) {
    return null;
  }

  @Override
  public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, String broadcastPermission, Handler scheduler) {
    return null;
  }

  @Override
  public void sendBroadcast(Intent intent) {

  }

  @Override
  public void sendBroadcast(Intent intent, String receiverPermission) {

  }

  @Override
  public void sendBroadcastAsUser(Intent intent, UserHandle user) {

  }

  @Override
  public void sendBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission) {

  }

  @Override
  public void sendOrderedBroadcast(Intent intent, String receiverPermission) {

  }

  @Override
  public void sendOrderedBroadcast(Intent intent, String receiverPermission, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {

  }

  @Override
  public void sendOrderedBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {

  }

  @Override
  public void sendStickyBroadcast(Intent intent) {

  }

  @Override
  public void sendStickyBroadcastAsUser(Intent intent, UserHandle user) {

  }

  @Override
  public void sendStickyOrderedBroadcast(Intent intent, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {

  }

  @Override
  public void sendStickyOrderedBroadcastAsUser(Intent intent, UserHandle user, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {

  }

  @Override
  public SharedPreferences getSharedPreferences(String name, int mode) {
    return null;
  }

  @Override
  public SharedPreferences getSharedPreferences(File file, int mode) {
    return null;
  }

  @Override
  public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
    return null;
  }

  @Override
  public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler errorHandler) {
    return null;
  }

  @Override
  public Object getSystemService(String name) {
    return null;
  }
}
