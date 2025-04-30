package org.example.cpt202music.exception;


import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "ok"),
    PARAMS_ERROR(40000, "The request parameter is incorrect"),
    NOT_LOGIN_ERROR(40100, "Not Login"),
    NO_AUTH_ERROR(40101, "No Auth"),
    NOT_FOUND_ERROR(40400, "The request data does not exist"),
    FORBIDDEN_ERROR(40300, "Prohibition of Access"),
    SYSTEM_ERROR(50000, "Exceptions within the system"),
    OPERATION_ERROR(50001, "The operation failed");

    
    private final int code;


    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}

