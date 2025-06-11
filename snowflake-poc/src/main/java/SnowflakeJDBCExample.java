import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

public class SnowflakeJDBCExample {
    public static void main(String[] args) {
        String url = "jdbc:snowflake://hkfzecc-rg21336.snowflakecomputing.com/";
        Properties props = new Properties();
        props.put("user", "sanrai");
        props.put("password", "Ganesh@789012345678");
        props.put("warehouse", "SNOWFLAKE_LEARNING_WH");
        props.put("db", "SNOWFLAKE");
        props.put("schema", "LOCAL");

        try (Connection conn = DriverManager.getConnection(url, props)) {
            System.out.println("Connected to Snowflake!");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT CURRENT_VERSION()");
            while (rs.next()) {
                System.out.println("Snowflake Version: " + rs.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}