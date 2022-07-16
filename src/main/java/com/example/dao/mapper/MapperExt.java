package com.example.dao.mapper;

import com.example.service.dto.req.PageReqDTO;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.Serializable;
import java.util.List;

public interface MapperExt<E, ID extends Serializable, ReqDTO, RespDTO> {
    int deleteByPrimaryKey(ID id);

    int insert(E record);

    int insertSelective(E record);

    E selectByPrimaryKey(ID id);

    int updateByPrimaryKeySelective(E record);

    int updateByPrimaryKey(E record);


    long selectCount(PageReqDTO<ReqDTO> reqDTO);

    List<RespDTO> selectList(PageReqDTO<ReqDTO> reqDTO);

    int batchInsert(@RequestParam("list") List<? extends E> eList);
}
