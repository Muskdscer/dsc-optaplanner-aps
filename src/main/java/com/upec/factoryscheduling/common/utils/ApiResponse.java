package com.upec.factoryscheduling.common.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.Collection;
import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private Integer code;
    private String msg;
    private T data;
    private T rows;
    private String reqId;
    private Long total;


    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.success(data, "success");
    }

    public static <T> ApiResponse<T> success() {
        return ApiResponse.success(null, "success");
    }

    public static <T> ApiResponse<T> success(T data, String msg) {
        return ApiResponse.response(data, 200, msg);
    }

    public static <T> ApiResponse<T> error(String msg) {
        return response(null, -1, msg);
    }
    public static <T> ApiResponse<T> error(Integer code,String msg) {
        return response(null, code, msg);
    }


    private static <T> ApiResponse<T> response(T data, Integer code, String msg) {
        ApiResponse<T> apiResponse = new ApiResponse<>();
        apiResponse.setData(data);
        apiResponse.setCode(code);
        apiResponse.setMsg(msg);
        if (data != null) {
            if (data instanceof Collection) {
                List<T> list = (List<T>) data;
                apiResponse.setTotal((long) list.size());
            }
            if (data instanceof Page) {
                Page<T> page = (Page<T>) data;
                apiResponse.setTotal(page.getTotalElements());
            }
        }
        return apiResponse;
    }


    public static <T> ApiResponse<T> rows(T data) {
        return ApiResponse.rows(data, "success");
    }

    public static <T> ApiResponse<T> rows() {
        return ApiResponse.rows(null, "success");
    }

    public static <T> ApiResponse<T> rows(T data, String msg) {
        return ApiResponse.responseRows(data, 200, msg);
    }



    private static <T> ApiResponse<T> responseRows(T data, Integer code, String msg) {
        ApiResponse<T> apiResponse = new ApiResponse<>();
        apiResponse.setRows(data);
        apiResponse.setCode(code);
        apiResponse.setMsg(msg);
        if (data != null) {
            if (data instanceof Collection) {
                List<T> list = (List<T>) data;
                apiResponse.setTotal((long) list.size());
            }
        }
        return apiResponse;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(data);
    }
}
