package com.example.asset.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.asset.entity.Equipment;
import com.example.asset.mapper.EquipmentMapper;
import org.springframework.stereotype.Service;

@Service
public class EquipmentService extends ServiceImpl<EquipmentMapper, Equipment> {
}
