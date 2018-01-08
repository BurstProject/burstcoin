package brs.http;

import brs.BurstException;
import org.json.simple.JSONStreamAware;

public final class ParameterException extends BurstException {

  private final JSONStreamAware errorResponse;

  public ParameterException(JSONStreamAware errorResponse) {
    this.errorResponse = errorResponse;
  }

  JSONStreamAware getErrorResponse() {
    return errorResponse;
  }

}
