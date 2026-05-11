package com.example.asset.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.sql.SQLIntegrityConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        e.printStackTrace();
        return Result.error("系统错误: " + e.getMessage());
    }

    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public Result<?> handleSQLException(SQLIntegrityConstraintViolationException e) {
        if (e.getMessage().contains("Duplicate entry")) {
            return Result.error("操作失败: 数据已存在 (如用户名或编号重复)");
        }
        return Result.error("数据库操作失败: " + e.getMessage());
    }
    
    // Catch generic RuntimeException which might wrap SQL exceptions
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        e.printStackTrace();
        String msg = e.getMessage();
        if (msg != null && msg.contains("Duplicate entry")) {
             return Result.error("操作失败: 数据已存在 (如用户名或编号重复)");
        }
        return Result.error("操作失败: " + msg);
    }
}
