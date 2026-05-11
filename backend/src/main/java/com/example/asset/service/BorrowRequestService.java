package com.example.asset.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.asset.entity.BorrowRequest;
import com.example.asset.mapper.BorrowRequestMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BorrowRequestService extends ServiceImpl<BorrowRequestMapper, BorrowRequest> {

    public List<BorrowRequest> listWithDetail() {
        return baseMapper.selectListWithDetail();
    }

    public List<BorrowRequest> listByApplicantId(Long applicantId) {
        return baseMapper.selectByApplicantId(applicantId);
    }
}
