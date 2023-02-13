package sample;

import io.vertx.core.json.JsonObject;

public class Sql {

  private JsonObject config;

  private static Sql singleton;

  public static String SELECT_VERSION = "SELECT_VERSION"; //points to name of static field.

  private Sql() {
    super();
    this.config = new JsonObject();
    String vendor = this.getVendor();
    this.config.put("vendor", vendor);  //Default value; use vendor() to customize    
  }

  public static Sql create() {
    if (Sql.singleton == null) {
      Sql.singleton = new Sql();
    }

    return Sql.singleton;
  }

  private String getVendor() {
    //Hint: set system property in AbstractVerticle.start()/UnitTest.before()
    String vendor = System.getProperty("vendor");
    vendor = (vendor == null) ? "MySql" : vendor;
    return vendor;
  }

  public String query(String fieldName) throws Throwable {
    String vendor = 
        (this.config.getString("vendor") == null) 
        ? "mysql" 
        : this.config.getString("vendor").toLowerCase();

    String className = "sample.Mysql";
    switch (vendor) {
      case "mysql":  //vendor
        className = "sample.MySql";
        break;
      case "oracle": //vendor
        className = "sample.Oracle";
        break;
      default:          //"mysql"
        className = "sample.MySql";
    }    

    String query = (String)Class.forName(className).getField(fieldName).get(null);
    return query;
  }

  public Sql vendor(String vendor) {
    this.config.put("vendor", vendor);
    return this;
  }  
  
}
