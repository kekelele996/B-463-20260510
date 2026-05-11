package com.example.asset.controller;

import com.example.asset.common.Result;
import com.example.asset.entity.Equipment;
import com.example.asset.service.EquipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/equipments", produces = "application/json;charset=UTF-8")
@CrossOrigin(origins = "*")
public class EquipmentController {

    @Autowired
    private EquipmentService equipmentService;

    @GetMapping
    public Result<List<Equipment>> getAll() {
        return Result.success(equipmentService.list());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> add(@RequestBody Equipment equipment) {
        if (equipment.getName() == null || equipment.getName().trim().isEmpty()) {
            return Result.error("设备名称不能为空");
        }
        if (equipment.getType() == null || equipment.getType().trim().isEmpty()) {
            return Result.error("设备类型不能为空");
        }
        return Result.success(equipmentService.save(equipment));
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody Equipment equipment) {
        if (equipment.getName() != null && equipment.getName().trim().isEmpty()) {
            return Result.error("设备名称不能为空");
        }
        equipment.setId(id);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        equipment.setLastMaintenanceBy(username);
        return Result.success(equipmentService.updateById(equipment));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(equipmentService.removeById(id));
    }
}
