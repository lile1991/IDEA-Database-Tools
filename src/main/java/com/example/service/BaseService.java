package com.example.service;

import com.example.dao.mapper.MapperExt;
import com.example.service.dto.req.PageReqDTO;
import com.example.service.dto.resp.PageRespDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Slf4j
public abstract class BaseService<E, ID extends Serializable, ReqDTO, RespDTO, MapperEx extends MapperExt<E, ID, ReqDTO, RespDTO>>/* implements ApplicationListener<ContextRefreshedEvent>*/ {

//    protected Mapper mapper;
    @Autowired
    protected MapperEx mapperEx;

    /*public void onApplicationEvent(ContextRefreshedEvent e) {
        Class<?> mapperExClass = mapperEx.getClass().getInterfaces()[0];
        Class<?>[] mapperSupperInterfaces = mapperExClass.getInterfaces();
        for(Class<?> mapperSupperInterface: mapperSupperInterfaces) {
            if(mapperExClass.getSimpleName().equals(mapperSupperInterface.getSimpleName() + "Ext")) {
                e.getApplicationContext().getBeansOfType(mapperSupperInterface);
                break;
            }
        }
    }*/

    @Transactional
    public int deleteByPrimaryKey(ID id) {
        return mapperEx.deleteByPrimaryKey(id);
    }

    @Transactional
    public int insertIfAbsent(E record) {
        try {
            return mapperEx.insert(record);
        } catch (Exception e) {
            if(e instanceof org.springframework.dao.DuplicateKeyException || e.getMessage().contains("Duplicate entry")) {
                log.debug("DuplicateKeyException", e);
                return -1;
            }
            throw e;
        }
    }

    @Transactional
    public int insert(E record) {
        return mapperEx.insert(record);
    }

    @Transactional
    public int insertSelective(E record) {
        return mapperEx.insertSelective(record);
    }

    @Transactional(readOnly = true)
    public E selectByPrimaryKey(ID id) {
        return mapperEx.selectByPrimaryKey(id);
    }

    @Transactional
    public int updateByPrimaryKeySelective(E record) {
        return mapperEx.updateByPrimaryKeySelective(record);
    }

    @Transactional
    public int updateByPrimaryKey(E record) {
        return mapperEx.updateByPrimaryKey(record);
    }


    // MapperEx中新增的方法
    @Transactional(readOnly = true)
    public long selectCount(PageReqDTO<ReqDTO> reqDTO) {
        PageReqDTO<ReqDTO> clonePageReqDTO = new PageReqDTO<>(reqDTO.getParams());
        clonePageReqDTO.setPage(1);
        clonePageReqDTO.setPageSize(1);
        return mapperEx.selectCount(clonePageReqDTO);
    }

    @Transactional(readOnly = true)
    public List<RespDTO> selectList(ReqDTO params, int size) {
        PageReqDTO<ReqDTO> pageReqDTO = new PageReqDTO<>(params);
        pageReqDTO.setPageSize(size);
        return mapperEx.selectList(pageReqDTO);
    }

    public List<RespDTO> selectList(ReqDTO params) {
        return selectList(params, 1000);
    }

    @Transactional(readOnly = true)
    public List<RespDTO> selectList(PageReqDTO<ReqDTO> pageReqDTO) {
        if(pageReqDTO.getPageSize() == null) {
            pageReqDTO.setPageSize(25);
        }

        if(pageReqDTO.getOffset() == null) {
            pageReqDTO.setPage(1);
        }

        return mapperEx.selectList(pageReqDTO);
    }

    public PageRespDTO<RespDTO> selectPage(PageReqDTO<ReqDTO> pageReqDTO) {
        return selectPage(pageReqDTO, this::selectCount, this::selectList);
    }

    public static <ReqDTO, RespDTO> PageRespDTO<RespDTO> selectPage(PageReqDTO<ReqDTO> pageReqDTO, Function<PageReqDTO<ReqDTO>, Long> selectCount, Function<PageReqDTO<ReqDTO>, List<RespDTO>> selectList) {
        Long count = selectCount.apply(pageReqDTO);
        if(count == null) {
            count = 0L;
        }
        PageRespDTO<RespDTO> pageRespDTO = new PageRespDTO<>(pageReqDTO.getPage(), pageReqDTO.getPageSize());
        pageRespDTO.setTotalRecord(count.intValue());
        if(count > 0) {
            List<RespDTO> respDTOS = selectList.apply(pageReqDTO);
            pageRespDTO.setData(respDTOS);
        }
        return pageRespDTO;
    }

    @Transactional(readOnly = true)
    public RespDTO selectOne(ReqDTO reqDTO) {
        PageReqDTO<ReqDTO> pageReqDTO = new PageReqDTO<>();
        pageReqDTO.setParams(reqDTO);
        pageReqDTO.setPage(1);
        pageReqDTO.setPageSize(1);
        List<RespDTO> respDTOS = selectList(pageReqDTO);
        return CollectionUtils.isEmpty(respDTOS) ? null : respDTOS.get(0);
    }


    // 本Service中新增的方法
    @Transactional
    public int insertAll(List<? extends E> eList) {
        if(eList == null || eList.isEmpty()) {
            return 0;
        }

        int count = 0;
        for(E e: eList) {
            count += insert(e);
        }
        return count;
    }

    // 本Service中新增的方法
    @Transactional
    public int batchInsert(List<? extends E> eList) {
        if(eList == null || eList.isEmpty()) {
            return 0;
        }

        return mapperEx.batchInsert(eList);
    }

    /**
     * 批量保存
     * @param eList 数据
     * @param batchSize 批次提交的事务大小
     */
    @Transactional
    public int batchInsert(List<? extends E> eList, int batchSize) {
        if(eList == null || eList.isEmpty()) {
            return 0;
        }

        if(eList.size() <= batchSize) {
            return batchInsert(eList);
        }

        int insertCnt = 0;
        List<E> oneBatch = new ArrayList<>(batchSize);
        for(int i = 0; i < eList.size(); i ++) {
            E e = eList.get(i);
            oneBatch.add(e);
            if(oneBatch.size() >= batchSize || i >= eList.size() - 1) {
                insertCnt += batchInsert(oneBatch);
                oneBatch.clear();
            }
        }
        return insertCnt;
    }
}
