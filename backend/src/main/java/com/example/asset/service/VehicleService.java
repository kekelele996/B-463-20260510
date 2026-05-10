package com.example.asset.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.asset.entity.Vehicle;
import com.example.asset.mapper.VehicleMapper;
import org.springframework.stereotype.Service;

@Service
public class VehicleService extends ServiceImpl<VehicleMapper, Vehicle> {
}
