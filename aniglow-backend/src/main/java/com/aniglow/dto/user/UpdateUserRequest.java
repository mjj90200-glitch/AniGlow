package com.aniglow.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Size(max = 500, message = "头像URL长度不能超过500")
    private String avatarUrl;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Size(min = 6, max = 120, message = "密码长度必须在 6-120 之间")
    private String password;
}
