package com.example.asset.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.asset.entity.BorrowApplication;
import com.example.asset.entity.Equipment;
import com.example.asset.entity.User;
import com.example.asset.entity.Vehicle;
import com.example.asset.mapper.BorrowApplicationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class BorrowApplicationService extends ServiceImpl<BorrowApplicationMapper, BorrowApplication> {

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private UserService userService;

    public List<BorrowApplication> listWithOverdue(Long userId, boolean isAdmin) {
        QueryWrapper<BorrowApplication> wrapper = new QueryWrapper<>();
        if (!isAdmin && userId != null) {
            wrapper.eq("applicant_id", userId);
        }
        List<BorrowApplication> list = this.list(wrapper);
        LocalDate today = LocalDate.now();
        for (BorrowApplication app : list) {
            if ("approved".equals(app.getStatus()) && app.getExpectedReturnDate() != null
                    && app.getExpectedReturnDate().isBefore(today)) {
                app.setOverdue(true);
                app.setStatus("overdue");
                this.updateById(app);
            } else {
                app.setOverdue("overdue".equals(app.getStatus()));
            }
        }
        return list;
    }

    public BorrowApplication create(BorrowApplication app, Long applicantId) {
        if ("equipment".equals(app.getAssetType())) {
            Equipment equip = equipmentService.getById(app.getAssetId());
            if (equip == null) {
                throw new RuntimeException("设备不存在");
            }
            if ("borrowed".equals(equip.getStatus())) {
                throw new RuntimeException("该设备已被借出");
            }
            app.setAssetName(equip.getName());
        } else if ("vehicle".equals(app.getAssetType())) {
            Vehicle vehicle = vehicleService.getById(app.getAssetId());
            if (vehicle == null) {
                throw new RuntimeException("车辆不存在");
            }
            if ("borrowed".equals(vehicle.getStatus())) {
                throw new RuntimeException("该车辆已被借出");
            }
            app.setAssetName(vehicle.getLicensePlate() + " - " + vehicle.getModel());
        }
        User user = userService.getById(applicantId);
        app.setApplicantId(applicantId);
        if (user != null) {
            app.setApplicantName(user.getUsername());
        }
        app.setStatus("pending");
        this.save(app);
        return app;
    }

    public boolean approve(Long id, Long approverId, String reason) {
        BorrowApplication app = this.getById(id);
        if (app == null || !"pending".equals(app.getStatus())) {
            return false;
        }
        User user = userService.getById(approverId);
        app.setApprovedBy(approverId);
        if (user != null) {
            app.setApproverName(user.getUsername());
        }
        app.setApproveRejectReason(reason);
        app.setStatus("approved");
        boolean result = this.updateById(app);
        if (result) {
            updateAssetStatus(app, "borrowed");
        }
        return result;
    }

    public boolean reject(Long id, Long approverId, String reason) {
        BorrowApplication app = this.getById(id);
        if (app == null || !"pending".equals(app.getStatus())) {
            return false;
        }
        User user = userService.getById(approverId);
        app.setApprovedBy(approverId);
        if (user != null) {
            app.setApproverName(user.getUsername());
        }
        app.setApproveRejectReason(reason);
        app.setStatus("rejected");
        return this.updateById(app);
    }

    public boolean returnAsset(Long id) {
        BorrowApplication app = this.getById(id);
        if (app == null || (!"approved".equals(app.getStatus()) && !"overdue".equals(app.getStatus()))) {
            return false;
        }
        app.setStatus("returned");
        app.setActualReturnDate(LocalDate.now());
        boolean result = this.updateById(app);
        if (result) {
            updateAssetStatus(app, "online");
        }
        return result;
    }

    private void updateAssetStatus(BorrowApplication app, String status) {
        if ("equipment".equals(app.getAssetType())) {
            Equipment equip = new Equipment();
            equip.setId(app.getAssetId());
            equip.setStatus(status);
            equipmentService.updateById(equip);
        } else if ("vehicle".equals(app.getAssetType())) {
            Vehicle vehicle = new Vehicle();
            vehicle.setId(app.getAssetId());
            vehicle.setStatus(status);
            vehicleService.updateById(vehicle);
        }
    }
}
