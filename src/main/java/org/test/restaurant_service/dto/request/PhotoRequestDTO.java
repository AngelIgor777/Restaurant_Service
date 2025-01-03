package org.test.restaurant_service.dto.request;


import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class PhotoRequestDTO {
    @NotNull
    private Integer productId;

    @NotBlank
    @Size(max = 256)
    private String url;
}