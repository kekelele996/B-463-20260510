package com.example.asset.controller;

import com.example.asset.common.Result;
import com.example.asset.entity.Vehicle;
import com.example.asset.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/vehicles", produces = "application/json;charset=UTF-8")
@CrossOrigin(origins = "*")
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    @GetMapping
    public Result<List<Vehicle>> getAll() {
        return Result.success(vehicleService.list());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> add(@RequestBody Vehicle vehicle) {
        if (vehicle.getLicensePlate() == null || vehicle.getLicensePlate().trim().isEmpty()) {
            return Result.error("车牌号不能为空");
        }
        if (vehicle.getModel() == null || vehicle.getModel().trim().isEmpty()) {
            return Result.error("车辆型号不能为空");
        }
        return Result.success(vehicleService.save(vehicle));
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody Vehicle vehicle) {
        if (vehicle.getLicensePlate() != null && vehicle.getLicensePlate().trim().isEmpty()) {
            return Result.error("车牌号不能为空");
        }
        vehicle.setId(id);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        vehicle.setLastMaintenanceBy(username);
        return Result.success(vehicleService.updateById(vehicle));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(vehicleService.removeById(id));
    }
}
