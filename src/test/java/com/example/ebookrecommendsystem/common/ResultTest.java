package com.example.ebookrecommendsystem.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Result 统一响应包装")
class ResultTest {

    @Test
    @DisplayName("success() 应返回 code=200, 携带数据和消息")
    void shouldReturnSuccessWithDataAndMsg() {
        Result<String> result = Result.success("hello", "操作成功");

        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMsg());
        assertEquals("hello", result.getData());
    }

    @Test
    @DisplayName("success() 支持 data 为 null")
    void shouldSupportNullData() {
        Result<Object> result = Result.success(null, "添加成功");

        assertEquals(200, result.getCode());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("error() 应返回 code=500, data 为 null")
    void shouldReturnErrorWithNullData() {
        Result<Object> result = Result.error("参数校验失败");

        assertEquals(500, result.getCode());
        assertEquals("参数校验失败", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("success() 泛型应正确携带复杂对象")
    void shouldSupportComplexGenericType() {
        java.util.Map<String, Integer> map = new java.util.HashMap<>();
        map.put("count", 10);

        Result<java.util.Map<String, Integer>> result = Result.success(map, "统计成功");

        assertEquals(200, result.getCode());
        assertEquals(10, result.getData().get("count"));
    }
}
