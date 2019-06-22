package hs.mediasystem.util.javafx;

import javafx.application.Platform;

import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A JUnit {@link Rule} for performing JavaFX initialisation. To include in your
 * test case, add the following code:
 *
 * <pre>
 * {@literal @}Rule
 * public JavaFXRunningRule jfxRunningRule = new JavaFXRunningRule();
 * </pre>
 *
 * Note that this does not execute tests on the JavaFX thread.
 */
public class JavaFXRunningRule implements TestRule {

  /**
   * Flag for setting up the JavaFX, we only need to do this once for all tests.
   */
  private static boolean jfxIsSetup;

  @Override
  public Statement apply(Statement statement, Description description) {
    return new OnJFXThreadStatement(statement);
  }

  protected void evaluateStatement(Statement statement) throws Throwable {
    statement.evaluate();
  }

  private class OnJFXThreadStatement extends Statement {
    private final Statement statement;

    public OnJFXThreadStatement(Statement aStatement) {
      statement = aStatement;
    }

    @Override
    public void evaluate() throws Throwable {
      if(!jfxIsSetup) {
        Platform.startup(() -> {});
        jfxIsSetup = true;
      }

      evaluateStatement(statement);
    }
  }
}