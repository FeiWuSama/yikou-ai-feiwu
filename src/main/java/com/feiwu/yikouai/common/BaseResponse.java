package com.feiwu.yikouai.common;

import com.feiwu.yikouai.exception.ErrorCode;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class BaseResponse<T> implements Serializable {

    private int code;

    private T data;

    private String message;

    /**
     * 通用响应类
     *
     * @param code    响应状态码，用于表示请求处理的结果状态
     * @param data    响应数据，泛型类型，可根据实际需求返回不同类型的数据
     * @param message 响应消息，对响应状态的补充说明
     */
    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
