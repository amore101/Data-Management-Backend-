package flightapp;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.ibatis.jdbc.ScriptRunner;

public class TestUtils {
  public static void dropTables(Connection conn) throws SQLException {
    try (Statement st1 = conn.createStatement()) {
      try (Statement st2 = conn.createStatement()) {
        // Drop foreign key
        try (ResultSet rs = st1.executeQuery(
            " SELECT 'ALTER TABLE ' + TABLE_NAME + ' DROP CONSTRAINT ' + CONSTRAINT_NAME +';' AS query"
                + " FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS"
                + " WHERE CONSTRAINT_TYPE = 'FOREIGN KEY'" + "     AND TABLE_NAME != 'Flights'"
                + "     AND TABLE_NAME != 'Months'" + "     AND TABLE_NAME != 'Carriers'"
                + "     AND TABLE_NAME != 'Weekdays';")) {
          while (rs.next()) {
            st2.execute(rs.getString("query"));
          }
        }

        // Drop tables
        try (ResultSet rs = st1.executeQuery(" SELECT 'DROP TABLE ' + TABLE_NAME +';' AS query"
            + " FROM INFORMATION_SCHEMA.TABLES" + " WHERE TABLE_TYPE = 'BASE TABLE'"
            + "     AND TABLE_NAME != 'Flights'" + "     AND TABLE_NAME != 'Months'"
            + "     AND TABLE_NAME != 'Carriers'" + "     AND TABLE_NAME != 'Weekdays';")) {
          while (rs.next()) {
            st2.execute(rs.getString("query"));
          }
        }
      }
    }
  };

  public static void runCreateTables(Connection conn) throws SQLException, IOException {
    ScriptRunner scriptRunner = new ScriptRunner(conn);
    scriptRunner.setStopOnError(true);
    scriptRunner.setLogWriter(null);
    scriptRunner.setErrorLogWriter(null);
    FileReader reader = new FileReader("createTables.sql");
    scriptRunner.runScript(reader);
  }
}
