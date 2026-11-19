package com.pixid.joboffer.presentation.error;

public class BadRequestException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String parameter;

    public BadRequestException(String parameter, String message) {
        super(message);
        this.parameter = parameter;
    }

    public String getParameter() {
        return parameter;
    }
}
