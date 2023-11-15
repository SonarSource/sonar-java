package net.sqlcipher.database;

import java.io.File;
import net.sqlcipher.DatabaseErrorHandler;

public class SQLiteDatabase {
  public SQLiteDatabase(String path, char[] password, CursorFactory factory, int flags) {}

  public SQLiteDatabase(String path, char[] password, CursorFactory factory, int flags, SQLiteDatabaseHook databaseHook) {}

  public SQLiteDatabase(String path, byte[] password, CursorFactory factory, int flags, SQLiteDatabaseHook databaseHook) {}

  public void changePassword(String password) {}

  public void changePassword(char[] password) {}

  public static SQLiteDatabase openDatabase(String path, String password, CursorFactory factory, int flags) {
    return null;
  }

  public static SQLiteDatabase openDatabase(String path, char[] password, CursorFactory factory, int flags) {
    return null;
  }

  public static SQLiteDatabase openDatabase(String path, String password, CursorFactory factory, int flags, SQLiteDatabaseHook hook) {
    return null;
  }

  public static SQLiteDatabase openDatabase(String path, char[] password, CursorFactory factory, int flags, SQLiteDatabaseHook hook) {
    return null;
  }

  public static SQLiteDatabase openDatabase(String path, String password, CursorFactory factory, int flags, SQLiteDatabaseHook hook, DatabaseErrorHandler errorHandler) {
    return null;
  }

  public static SQLiteDatabase openDatabase(String path, char[] password, CursorFactory factory, int flags, SQLiteDatabaseHook hook, DatabaseErrorHandler errorHandler) {
    return null;
  }

  public static SQLiteDatabase openDatabase(String path, byte[] password, CursorFactory factory, int flags, SQLiteDatabaseHook hook, DatabaseErrorHandler errorHandler) {
    return null;
  }

  public static SQLiteDatabase openOrCreateDatabase(File file, String password, CursorFactory factory, SQLiteDatabaseHook databaseHook) {
    return null;
  }

  public static SQLiteDatabase openOrCreateDatabase(File file, String password, CursorFactory factory, SQLiteDatabaseHook databaseHook, DatabaseErrorHandler errorHandler) {
    return null;
  }

  public static SQLiteDatabase openOrCreateDatabase(String path, String password, CursorFactory factory, SQLiteDatabaseHook databaseHook) {
    return null;
  }

  public static SQLiteDatabase openOrCreateDatabase(String path, String password, CursorFactory factory, SQLiteDatabaseHook databaseHook, DatabaseErrorHandler errorHandler) {
    return null;
  }

  public static SQLiteDatabase openOrCreateDatabase(String path, char[] password, CursorFactory factory, SQLiteDatabaseHook databaseHook) {
    return null;
  }

  public static SQLiteDatabase openOrCreateDatabase(String path, char[] password, CursorFactory factory, SQLiteDatabaseHook databaseHook, DatabaseErrorHandler errorHandler) {
    return null;
  }

  public static SQLiteDatabase openOrCreateDatabase(String path, byte[] password, CursorFactory factory, SQLiteDatabaseHook databaseHook) {
    return null;
  }

  public static SQLiteDatabase openOrCreateDatabase(String path, byte[] password, CursorFactory factory, SQLiteDatabaseHook databaseHook, DatabaseErrorHandler errorHandler) {
    return null;
  }

  public static SQLiteDatabase openOrCreateDatabase(File file, String password, CursorFactory factory) {
    return null;
  }

  public static SQLiteDatabase openOrCreateDatabase(String path, String password, CursorFactory factory) {
    return null;
  }

  public static SQLiteDatabase openOrCreateDatabase(String path, char[] password, CursorFactory factory) {
    return null;
  }

  public static SQLiteDatabase openOrCreateDatabase(String path, byte[] password, CursorFactory factory) {
    return null;
  }

  public static SQLiteDatabase create(CursorFactory factory, String password) {
    return null;
  }

  public static SQLiteDatabase create(CursorFactory factory, char[] password) {
    return null;
  }

  public interface CursorFactory {

  }
}
