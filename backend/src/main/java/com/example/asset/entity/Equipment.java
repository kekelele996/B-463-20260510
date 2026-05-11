package com.example.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("equipment")
public class Equipment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String type;
    private String status; // e.g., "online", "offline", "maintenance"
    private String maintenanceRecord;
    private String lastMaintenanceBy;
    private LocalDateTime lastMaintenanceDate;

    public String getLastMaintenanceBy() {
        return lastMaintenanceBy;
    }

    public void setLastMaintenanceBy(String lastMaintenanceBy) {
        this.lastMaintenanceBy = lastMaintenanceBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMaintenanceRecord() {
        return maintenanceRecord;
    }

    public void setMaintenanceRecord(String maintenanceRecord) {
        this.maintenanceRecord = maintenanceRecord;
    }

    public LocalDateTime getLastMaintenanceDate() {
        return lastMaintenanceDate;
    }

    public void setLastMaintenanceDate(LocalDateTime lastMaintenanceDate) {
        this.lastMaintenanceDate = lastMaintenanceDate;
    }
}
