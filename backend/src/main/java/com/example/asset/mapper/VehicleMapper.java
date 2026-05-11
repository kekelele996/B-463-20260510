package com.example.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.asset.entity.Vehicle;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VehicleMapper extends BaseMapper<Vehicle> {
}
