import java.sql.Timestamp;

public class SqlTimestamp {
  void initTimestamp() {
    Timestamp ts = new Timestamp(System.currentTimeMillis());
  }
}

// Compliant because this class is still used for old JDBC drivers compatibility
