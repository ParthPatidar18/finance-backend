package com.zorvyn.finance.dto;

import com.zorvyn.finance.enums.Role;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private Role role;
    private Boolean active;
}
