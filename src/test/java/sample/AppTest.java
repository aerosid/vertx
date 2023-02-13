package sample;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AppTest {
  @AfterAll
  public static void destroy() {
    return;
  }

  @BeforeAll
  public static void init() {
    return;
  }

  @Test
  public void sampleTest() {
    String msg = "hello";
    Assertions.assertEquals("hello", msg);
    return;
  }
}