package hs.mediasystem.util.javafx;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;

import org.junit.Rule;
import org.junit.runners.model.Statement;

/**
 * A JUnit {@link Rule} for running tests on the JavaFX thread and performing JavaFX initialisation. To include in your
 * test case, add the following code:
 *
 * <pre>
 * {@literal @}Rule
 * public JavaFXThreadingRule jfxThreadingRule = new JavaFXThreadingRule();
 * </pre>
 */
public class JavaFXThreadingRule extends JavaFXRunningRule {

  @Override
  protected void evaluateStatement(Statement statement) throws Throwable {
    final CountDownLatch countDownLatch = new CountDownLatch(1);
    final AtomicReference<Throwable> rethrownExceptionReference = new AtomicReference<>();

    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        try {
          statement.evaluate();
        }
        catch(Throwable e) {
          rethrownExceptionReference.set(e);
        }
        countDownLatch.countDown();
      }
    });
    countDownLatch.await();
    // if an exception was thrown by the statement during evaluation,
    // then re-throw it to fail the test
    if(rethrownExceptionReference.get() != null) {
      throw rethrownExceptionReference.get();
    }
  }
}