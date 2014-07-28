package org.digidoc4j.api.exceptions;

/**
 * Generic exception for DigiDoc4J
 */
public class DigiDoc4JException extends RuntimeException {

  int errorCode = 0;

  /**
   * Constructs a new runtime exception with the specified detail message and
   * cause.  <p>Note that the detail message associated with
   * {@code cause} is <i>not</i> automatically incorporated in
   * this runtime exception's detail message.
   *
   * @param error   - error code
   * @param message the detail message (which is saved for later retrieval
   *                by the {@link #getMessage()} method).
   */
  public DigiDoc4JException(int error, String message) {
    super(message);
    errorCode = error;
  }

  /**
   * Constructs a new runtime exception with the specified detail message and
   * cause.  <p>Note that the detail message associated with
   * {@code cause} is <i>not</i> automatically incorporated in
   * this runtime exception's detail message.
   *
   * @param message the detail message (which is saved for later retrieval
   *                by the {@link #getMessage()} method).
   */
  public DigiDoc4JException(String message) {
    super(message);
  }

  /**
   * Creates new exception based on another exception
   *
   * @param e parent exception
   */
  public DigiDoc4JException(Exception e) {
    super(e.getMessage(), e.getCause());
  }

  @Override
  public String toString() {
    StringBuilder msg = new StringBuilder();
    if (errorCode != 0) msg.append("ERROR: ").append(errorCode).append(" - ");
    msg.append(getMessage());
    return msg.toString();
  }
}
