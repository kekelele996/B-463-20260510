package com.example.asset.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.asset.entity.BorrowRecord;
import com.example.asset.mapper.BorrowRecordMapper;
import org.springframework.stereotype.Service;

@Service
public class BorrowRecordService extends ServiceImpl<BorrowRecordMapper, BorrowRecord> {
}
