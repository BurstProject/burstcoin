package brs.http;

import brs.NxtException;
import org.json.simple.JSONStreamAware;

final class ParameterException extends NxtException {

    private final JSONStreamAware errorResponse;

    ParameterException(JSONStreamAware errorResponse) {
        this.errorResponse = errorResponse;
    }

    JSONStreamAware getErrorResponse() {
        return errorResponse;
    }

}
