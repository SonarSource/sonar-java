package checks.security;

import io.realm.RealmConfiguration;
import java.nio.charset.StandardCharsets;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;

class AndroidMobileDatabaseEncryptionKeysCheck {

  byte[] a = new byte[0];

  String getPassword() {
    return null;
  }

  void changePassword() {
  }

  void changePassword(String pwd) {
  }

  void SQLiteDatabase_openOrCreateDatabase(String databasePath, SQLiteDatabaseHook hook, SQLiteDatabase.CursorFactory cursorFactory) {
    String password = "pwd";
    SQLiteDatabase database;

    database = new SQLiteDatabase("path", "pwd".toCharArray(), cursorFactory, 0); // Noncompliant
    database = new SQLiteDatabase("path", password.toCharArray(), cursorFactory, 0); // Noncompliant

    char[] charArray = "pwd".toCharArray();
    database = new SQLiteDatabase("path", charArray, cursorFactory, 0); // Noncompliant

    database = SQLiteDatabase.openOrCreateDatabase(databasePath, password, null, hook); // Noncompliant [[sc=66;ec=74;secondary=23]] {{The "password" parameter should not be hardcoded.}}
    database = SQLiteDatabase.openOrCreateDatabase(databasePath, "test123", null); // Noncompliant
    database = SQLiteDatabase.openDatabase(databasePath, password, cursorFactory, 0); // Noncompliant

    database = SQLiteDatabase.create(cursorFactory, password); // Noncompliant

    database.changePassword("a"); // Noncompliant

    database.changePassword(getPassword());

    changePassword();
    changePassword("a");
  }

  byte[] getKey() {
    return new byte[0];
  }

  RealmConfiguration getConfig() {
    String key = "gb09ym9ydoolp3w886d0tciczj6ve9kszqd65u7d126040gwy86xqimjpuuc788g";
    RealmConfiguration config = new RealmConfiguration.Builder()
      .encryptionKey(key.getBytes(StandardCharsets.UTF_8)) // Noncompliant {{The "encryptionKey" parameter should not be hardcoded.}}
      .build();
    config = new RealmConfiguration.Builder()
      .encryptionKey("key".getBytes(StandardCharsets.UTF_8)) // Noncompliant
      .build();
    config = new RealmConfiguration.Builder()
      .encryptionKey(getKey())
      .build();

    byte[] secondKey = getKey();
    config = new RealmConfiguration.Builder()
      .encryptionKey(secondKey)
      .build();

    byte[] thirdKey = new byte[0];
    thirdKey = new byte[0];
    config = new RealmConfiguration.Builder()
      .encryptionKey(thirdKey)
      .build();

    String fourthKey = "a";
    fourthKey = "b";
    config = new RealmConfiguration.Builder()
      .encryptionKey(fourthKey.getBytes(StandardCharsets.UTF_8))
      .build();

    byte[] fifthKey = this.a;
    config = new RealmConfiguration.Builder()
      .encryptionKey(fifthKey)
      .build();

    config = new RealmConfiguration.Builder()
      .encryptionKey(this.a)
      .build();

    return config;
  }
}
