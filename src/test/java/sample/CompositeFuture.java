package sample;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;

import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;

/**
 * See https://stackoverflow.com/questions/44495763/compose-with-vertx-for-sequential-code
 */
@RunWith(VertxUnitRunner.class)
@ExtendWith(VertxExtension.class)
public class CompositeFuture {  

  private static class TimeoutJob {
    
    private Promise<Long> promise;

    private Long delay; //milli-seconds

    private TimeoutJob() {
      super();        
      this.delay = Long.valueOf(1000L);
    }

    public static TimeoutJob create() {
      TimeoutJob job = new TimeoutJob();
      return job;
    }

    private void onTimeout(Long timeout) {
      System.out.println(this.delay.toString() + " milli-second timer fired.");  
      if ((this.delay.intValue() % 3) != 0) {
        this.promise.fail("delay not divisible by 3.");        
      } else {
        this.promise.complete(this.delay);
      }   
  
      return;
    }

    public Future<Long> fire(Vertx vertx) {
      this.promise = Promise.promise();
      vertx.setTimer(this.delay, this::onTimeout);
      return this.promise.future();
    }

    private TimeoutJob delay(Long delay) {
      this.delay = delay;
      return this;
    }

  }

  private Async async;

  private TimeoutJob job1;

  private TimeoutJob job2a;

  private TimeoutJob job2b;

  private TimeoutJob job3;

  private Long totalDelay;

  private Vertx vertx;

  @After
  public void destroy() {
    this.vertx.close();
    return;
  }

  @Before
  public  void init() {
    VertxOptions options = new VertxOptions(); 
    options.setMaxEventLoopExecuteTime(Long.MAX_VALUE);  //for debugging unit tests      
    this.vertx = Vertx.vertx(options);
    return;
  }

  private Future<Long> compose() {
    this.job1 = TimeoutJob.create().delay(3000L);
    this.job2a = TimeoutJob.create().delay(6000L);
    this.job2b = TimeoutJob.create().delay(9000L);
    this.job3 = TimeoutJob.create().delay(3000L);
    this.totalDelay = Long.valueOf(0L);

    Function<Long, Future<Long>> job1Succeeded = (Long i) -> { 
      return this.onJobSuccess("job1", i);
    };
    Function<Throwable, Future<Long>> job1Failed = (Throwable t) -> { 
      return this.onJobFailure("job1", t);      
    };
    Function<Long, Future<Long>> job2Succeeded = (Long i) -> { 
      return this.onJobSuccess("job2", i);
    };    

    Future<Long> compositeJob =  this.job1
        .fire(this.vertx)
        .compose(job1Succeeded::apply, job1Failed::apply)
        .compose(job2Succeeded::apply);

    return compositeJob;
  }

  private Future<Long> onJobSuccess(String jobName, Long jobResult) {
    this.totalDelay = Long.valueOf(this.totalDelay.longValue() + jobResult.longValue());
    TimeoutJob nextJob;
    switch (jobName) {
      case "job1":
        nextJob = this.job2a;
        break;
      default:
        nextJob = this.job3;
        break;
    }
    return nextJob.fire(this.vertx);
  }

  private Future<Long> onJobFailure(String jobName, Throwable t) {
    return this.job2b.fire(this.vertx);    
  }

  private void onSuccess(String jobName, Long jobResult) {
    this.totalDelay = Long.valueOf(this.totalDelay.longValue() + jobResult.longValue());
    System.out.println("Total delay: " + this.totalDelay.longValue() + "ms");
    this.async.complete();
    return;
  }

  @Test
  public void sequentialJobs(TestContext testContext) throws Throwable {
    this.async = testContext.async();
    
    Consumer<Long> onSuccess = (Long i) -> { 
      this.onSuccess("job3", i);
    };
    Consumer<Throwable> onFailure = (Throwable t) -> { 
      this.async.complete(); 
    };
    Consumer<AsyncResult<Long>> onComplete = (AsyncResult<Long> i) -> { 
      System.out.println("done!"); 
    };

    Future<Long> compositeJob = this.compose();
 
    compositeJob
        .onComplete(onComplete::accept)
        .onSuccess(onSuccess::accept)
        .onFailure(onFailure::accept);
    return;
  }

}
