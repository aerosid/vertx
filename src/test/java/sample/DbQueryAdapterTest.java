package sample;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
@ExtendWith(VertxExtension.class)
public class DbQueryAdapterTest {

  private Async async;

  private Vertx vertx;

  @After
  public void destroy() {
    this.vertx.close();
    return;
  }

  @Before
  public void init() {
    VertxOptions options = new VertxOptions();
    options.setMaxEventLoopExecuteTime(Long.MAX_VALUE);  //for debugging unit tests
    this.vertx = Vertx.vertx(options);
    return;
  }

  private void onFailure(Throwable t) {
    t.printStackTrace();
    this.async.complete();
    return;
  }

  private void onSuccess(List<JsonObject> resultSet) {
    JsonObject jsonObject = resultSet.get(0);
    String version = jsonObject.getString("version");
    System.out.println("version: " + version);
    this.async.complete();
    return;
  }

  @Test
  public void testAdapter(TestContext testContext) throws Throwable {
    this.async = testContext.async();
    String sql = Sql.create().query(Sql.SELECT_VERSION);
    DbQueryAdapter                                          //Db, Cache, Queue, REST/SOAP endpoint
        .create(this.vertx).user("ubuntu")            //create and customize
        .success(this::onSuccess).failure(this::onFailure)  //register callbacks
        .query(sql);                                        //query
    return;
  }

}
