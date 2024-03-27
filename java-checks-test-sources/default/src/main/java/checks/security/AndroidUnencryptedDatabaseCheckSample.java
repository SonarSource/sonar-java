package checks.security;

import android.app.Activity;
import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import io.realm.RealmConfiguration;
import java.io.File;

public class AndroidUnencryptedDatabaseCheckSample {

  RealmConfiguration.Builder builderAsField;

  void testSharedPreferences(Activity activity, Context context, PreferenceManager preferenceManager) {
    activity.getPreferences(1); // Noncompliant [[sc=14;ec=28]] {{Make sure using an unencrypted database is safe here.}}
    activity().getPreferences(2); // Noncompliant
    myActivity().getPreferences(3); // Noncompliant
    myActivity().getPreferences(3, 4); // Compliant, unrelated method

    context.getSharedPreferences(new File(""), 1); // Noncompliant [[sc=13;ec=33]] {{Make sure using an unencrypted database is safe here.}}
    context.getSharedPreferences("file", 1); // Noncompliant

    PreferenceManager.getDefaultSharedPreferences(context); // Noncompliant
  }

  void testSQLiteDatabase(Context context, SQLiteDatabase.CursorFactory cursorFactory, DatabaseErrorHandler databaseErrorHandler) {
    context.openOrCreateDatabase("name", 1, cursorFactory); // Noncompliant [[sc=13;ec=33]] {{Make sure using an unencrypted database is safe here.}}
    context.openOrCreateDatabase("name", 1, cursorFactory, databaseErrorHandler); // Noncompliant
  }

  void testRealm() {
    new RealmConfiguration.Builder()
      .build(); // Noncompliant [[sc=8;ec=13]]

    new RealmConfiguration.Builder()
      .name("")
      .build(); // Noncompliant

    new RealmConfiguration.Builder()
      .name("")
      .encryptionKey(new byte[1])
      .build(); // Compliant

    RealmConfiguration.Builder builder = new RealmConfiguration.Builder();
    builder.name("");
    builder.build(); // Noncompliant

    RealmConfiguration.Builder builder2 = new RealmConfiguration.Builder();
    builder2.encryptionKey(new byte[1]);
    builder2.build(); // Compliant

    RealmConfiguration.Builder builder3 = new RealmConfiguration.Builder();
    builder3.name("").encryptionKey(new byte[1]);
    builder3.build(); // Compliant

    RealmConfiguration.Builder builder3_2 = new RealmConfiguration.Builder();
    builder3_2.encryptionKey(new byte[1]).name("");
    builder3_2.build(); // Compliant

    RealmConfiguration.Builder builder4 = new RealmConfiguration.Builder().encryptionKey(new byte[1]);
    builder4.build(); // Compliant

    RealmConfiguration.Builder builder5 = new RealmConfiguration.Builder().name("");
    builder5.name("");
    builder5.build(); // Noncompliant

    RealmConfiguration.Builder builder6 = new RealmConfiguration.Builder().name("");
    addProperty(builder6);
    builder6.build(); // Compliant

    new BuilderProvider()
      .getBuilder()
      .name("")
      .build(); // Compliant

    builderAsField.build(); // Compliant, field can be modified somewhere else
  }

  void addProperty(RealmConfiguration.Builder builder) {
    builder.encryptionKey(new byte[0]);
  }

  Activity activity() {
    return new Activity();
  }

  MyActivity myActivity() {
    return new MyActivity();
  }

  class MyActivity extends Activity {
    void getPreferences(int i, int j) {

    }
  }

  class BuilderProvider {
    RealmConfiguration.Builder getBuilder() {
     return new RealmConfiguration.Builder();
    }
  }

}
