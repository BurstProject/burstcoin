package brs.http;

import brs.BurstException;
import org.json.simple.JSONStreamAware;

final class ParameterException extends BurstException {

  private final JSONStreamAware errorResponse;

  ParameterException(JSONStreamAware errorResponse) {
    this.errorResponse = errorResponse;
  }

  JSONStreamAware getErrorResponse() {
    return errorResponse;
  }

}
