package com.example.service.dto.resp;

import com.example.service.dto.req.PageReqDTO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class PageRespDTO<T> {
    @ApiModelProperty("分页大小")
    private int pageSize;
    @ApiModelProperty(value = "页号")
    private Integer page;

    public PageRespDTO(int page, int pageSize) {
        this.page = page;
        this.pageSize = pageSize;
    }
    public PageRespDTO(PageReqDTO<?> pageReqDTO) {
        this.page = pageReqDTO.getPage();
        this.pageSize = pageReqDTO.getPageSize();
    }

    @ApiModelProperty("总记录数")
    private int totalRecord;
    @ApiModelProperty("总页数")
    public int getPages() {
        return (totalRecord + pageSize - 1) / pageSize;
    }
    @ApiModelProperty("结果集")
    private List<T> data = Collections.emptyList();

    public boolean isLastPage() {
        return page >= getPages();
    }
}
