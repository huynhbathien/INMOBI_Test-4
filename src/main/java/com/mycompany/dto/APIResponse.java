package com.mycompany.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@Builder
public class APIResponse<S> {

    @Builder.Default
    Integer statusCode = 200;
    String message;
    Object data;
    String error;

    public static <T> APIResponse<T> success(int statusCode, String message, T data) {
        return APIResponse.<T>builder()
                .statusCode(statusCode)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> APIResponse<T> error(int statusCode, String message, String error) {
        return APIResponse.<T>builder()
                .statusCode(statusCode)
                .message(message)
                .error(error)
                .build();
    }
}
