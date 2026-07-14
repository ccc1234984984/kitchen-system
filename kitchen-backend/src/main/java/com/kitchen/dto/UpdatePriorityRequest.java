package com.kitchen.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class UpdatePriorityRequest {

    @NotNull(message = "优先级分数不能为空")
    private Double score;
}
