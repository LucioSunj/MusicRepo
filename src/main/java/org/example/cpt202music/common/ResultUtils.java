package org.example.cpt202music.common;

import org.example.cpt202music.exception.ErrorCode;

public class ResultUtils {

    /**
     * Success
     *
     * @param data Data
     * @param <T>  Data type
     * @return Response
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, "ok");
    }

    /**
     * Failure
     *
     * @param errorCode Error code
     * @return Response
     */
    public static BaseResponse<?> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    /**
     * Failure
     *
     * @param code    Error code
     * @param message Error message
     * @return Response
     */
    public static BaseResponse<?> error(int code, String message) {
        return new BaseResponse<>(code, null, message);
    }

    /**
     * Failure
     *
     * @param errorCode Error code
     * @return Response
     */
    public static BaseResponse<?> error(ErrorCode errorCode, String message) {
        return new BaseResponse<>(errorCode.getCode(), null, message);
    }
}
