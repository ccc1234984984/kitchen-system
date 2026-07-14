package com.kitchen.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class CreateOrderRequest {

    @NotNull(message = "餐桌ID不能为空")
    private Long tableId;

    @NotBlank(message = "桌号不能为空")
    private String tableNo;

    @NotEmpty(message = "至少选择一道菜品")
    private List<Long> dishIds;

    private Long userId;
}
