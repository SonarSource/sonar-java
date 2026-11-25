import java.sql.Driver;

@Deprecated
open module org.example.app {
  requires java.base;
  requires transitive java.logging;
  requires static java.sql;
  exports org.example.api;
  exports org.example.internal to org.example.friend, org.example.other;
  opens org.example.model;
  opens org.example.data to org.example.orm;
  uses Driver;
  provides java.lang.Runnable with org.example.Task, org.example.OtherTask;
}
