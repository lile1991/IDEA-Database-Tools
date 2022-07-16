package com.example.service.dto.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PageReqDTO<T>  {
    @ApiModelProperty(value = "分页大小", example = "25")
    private Integer pageSize = 25;
    @ApiModelProperty(value = "页号", example = "1")
    private Integer page = 1;
    private T params;

    public PageReqDTO(T params) {
        this.params = params;
    }

    // public void setPageNo(Integer pageNo) {
    //     this.page = pageNo;
    // }

    public Integer getOffset() {
        return (page - 1) * pageSize;
    }
}
