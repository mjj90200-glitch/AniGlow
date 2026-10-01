package com.aniglow.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 24, message = "用户名长度必须在 3-24 之间")
    @Pattern(regexp = "^[\\p{IsHan}A-Za-z0-9_]+$", message = "用户名只能包含中文、字母、数字和下划线")
    private String username;

    @Size(max = 80, message = "昵称不能超过 80 个字符")
    private String displayName;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 120, message = "密码长度必须在 8-120 之间")
    private String password;
}
