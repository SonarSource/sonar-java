package net.sqlcipher;

import net.sqlcipher.database.SQLiteDatabase;

public interface DatabaseErrorHandler {

  void onCorruption(SQLiteDatabase dbObj);
}
