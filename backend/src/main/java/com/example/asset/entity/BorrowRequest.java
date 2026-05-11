package com.example.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("borrow_request")
public class BorrowRequest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String assetType;
    private Long assetId;
    private Long applicantId;
    private String reason;
    private LocalDateTime expectedReturnDate;
    private LocalDateTime actualReturnDate;
    private String status;
    private Long approvedBy;
    private String approveRemark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String assetName;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String applicantName;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String approverName;
}
