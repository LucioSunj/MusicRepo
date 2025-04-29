package org.example.cpt202music.exception;

public class ThrowUtils {

    /**
     * Throw exception if condition is true
     *
     * @param condition        Condition
     * @param runtimeException Exception
     */

    public static void throwIf(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    /**
     * Throw exception if condition is true
     *
     * @param condition Condition
     * @param errorCode Error code
     */
    public static void throwIf(boolean condition, ErrorCode errorCode) {
        throwIf(condition, new BusinessException(errorCode));
    }

    /**
     * Throw exception if condition is true
     *
     * @param condition Condition
     * @param errorCode Error code
     * @param message   Error message
     */
    public static void throwIf(boolean condition, ErrorCode errorCode, String message) {
        throwIf(condition, new BusinessException(errorCode, message));
    }
}
