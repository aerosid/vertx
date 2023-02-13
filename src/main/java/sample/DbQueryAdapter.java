package sample;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class DbQueryAdapter {

  private SQLClient client;

  private JsonObject config;

  private SQLConnection connection;

  private boolean fired;

  private Handler<List<JsonObject>> success;

  private Handler<Throwable> failure;

  private Vertx vertx;

  private DbQueryAdapter(Vertx v) {
    super();
    this.config = new JsonObject();
    this.fired = false;
    this.vertx = v;
  }

  private Future<SQLClient> client() {
    Promise<SQLClient> promise = Promise.promise();
    this.client = JDBCClient.create(this.vertx, this.config);
    promise.complete(this.client);
    return promise.future();
  }

  private void close() {
    if (this.connection != null) {
      this.connection.close();
      this.connection = null; //prevents close() being called again
    }
    if (this.client != null) {
      this.client.close();
      this.client = null; //prevents close() being called again
    }
    return;
  }

  private Function<SQLClient, Future<SQLConnection>> connection() {
    Function<SQLClient, Future<SQLConnection>> connection = (SQLClient client) -> {
      Promise<SQLConnection> promise = Promise.promise();
      this.client.getConnection(promise);
      return promise.future();
    };
    return connection;
  }

  public static DbQueryAdapter create(Vertx v) {
    DbQueryAdapter adapter = new DbQueryAdapter(v);
    //Hint: set system property/shared object in AbstractVerticle.start()/UnitTest.before()
    String providerClass = "io.vertx.ext.jdbc.spi.impl.HikariCPDataSourceProvider";
    adapter.config.put("provider_class", providerClass);
    adapter.config.put("driver_class", "com.mysql.jdbc.Driver");
    adapter.config.put("jdbcUrl", "jdbc:mysql://localhost:3306");
    adapter.config.put("username", "ubuntu");  //Default value; use user() to customize
    adapter.config.put("password", "hello");
    adapter.config.put("maximumPoolSize", 3);
    adapter.config.put("minimumIdle", 1);
    adapter.config.put("leakDetectionThreshold", 60000);

    return adapter;
  }

  public DbQueryAdapter failure(Handler<Throwable> handler) {
    this.failure = handler;
    return this;
  }

  public Future<List<JsonObject>> query(String sql) {
    if (this.fired) {
      throw new IllegalStateException("Instance has already fired.");
    }
    this.fired = true;
    this.config.put("query", sql);
    Future<List<JsonObject>> job = this
        .client()
        .compose(this.connection()::apply)        //async op; returns Future<T>
        .compose(this.resultSet()::apply)         //async op; returns Function<T, Future<U>>
        .compose(this.rows()::apply)              //sync op; ; returns Function<T, Future<U>>
        .onComplete(this.onComplete()::accept)    //release resources; returns Consumer<T>
        .onSuccess(this.onSuccess()::accept)      //call registered callback; returns Consumer<T>
        .onFailure(this.onFailure()::accept);     //call registered callback; returns Consumer<T>
    return job;
  }

  private Consumer<AsyncResult<List<JsonObject>>> onComplete() {
    Consumer<AsyncResult<List<JsonObject>>> onComplete = (AsyncResult<List<JsonObject>> i) -> {
      this.close();
      return;
    };
    return onComplete;
  }

  private Consumer<Throwable> onFailure() {
    Consumer<Throwable> onFailure = (Throwable t) -> {
      if (this.failure != null) {
        this.failure.handle(t);
      }
      return;
    };
    return onFailure;
  }

  private Consumer<List<JsonObject>> onSuccess() {
    Consumer<List<JsonObject>> onSuccess = (List<JsonObject> list) -> {
      if (this.success != null) {
        this.success.handle(list);
      }
      return;
    };
    return onSuccess;
  }

  private Function<ResultSet, Future<List<JsonObject>>> rows() {
    Function<ResultSet, Future<List<JsonObject>>> queryFuture = (ResultSet resultSet) -> {
      Promise<List<JsonObject>> promise = Promise.promise();
      promise.complete(resultSet.getRows());
      return promise.future();
    };
    return queryFuture;
  }

  private Function<SQLConnection, Future<ResultSet>> resultSet() {
    Function<SQLConnection, Future<ResultSet>> resultSetFuture = (SQLConnection connection) -> {
      this.connection = connection;
      Promise<ResultSet> promise = Promise.promise();
      String query = this.config.getString("query");
      this.connection.query(query, promise);
      return promise.future();
    };
    return resultSetFuture;
  }

  public DbQueryAdapter success(Handler<List<JsonObject>> handler) {
    this.success = handler;
    return this;
  }

  public DbQueryAdapter user(String user) {
    this.config.put("username", user);
    return this;
  }

}
