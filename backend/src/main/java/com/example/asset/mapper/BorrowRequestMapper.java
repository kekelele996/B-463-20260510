package com.example.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.asset.entity.BorrowRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface BorrowRequestMapper extends BaseMapper<BorrowRequest> {

    @Select("SELECT br.*, " +
            "CASE WHEN br.asset_type = 'equipment' THEN e.name ELSE v.license_plate END AS assetName, " +
            "u1.username AS applicantName, " +
            "u2.username AS approverName " +
            "FROM borrow_request br " +
            "LEFT JOIN equipment e ON br.asset_type = 'equipment' AND br.asset_id = e.id " +
            "LEFT JOIN vehicle v ON br.asset_type = 'vehicle' AND br.asset_id = v.id " +
            "LEFT JOIN sys_user u1 ON br.applicant_id = u1.id " +
            "LEFT JOIN sys_user u2 ON br.approved_by = u2.id " +
            "ORDER BY br.create_time DESC")
    List<BorrowRequest> selectListWithDetail();

    @Select("SELECT br.*, " +
            "CASE WHEN br.asset_type = 'equipment' THEN e.name ELSE v.license_plate END AS assetName, " +
            "u1.username AS applicantName, " +
            "u2.username AS approverName " +
            "FROM borrow_request br " +
            "LEFT JOIN equipment e ON br.asset_type = 'equipment' AND br.asset_id = e.id " +
            "LEFT JOIN vehicle v ON br.asset_type = 'vehicle' AND br.asset_id = v.id " +
            "LEFT JOIN sys_user u1 ON br.applicant_id = u1.id " +
            "LEFT JOIN sys_user u2 ON br.approved_by = u2.id " +
            "WHERE br.applicant_id = #{applicantId} " +
            "ORDER BY br.create_time DESC")
    List<BorrowRequest> selectByApplicantId(Long applicantId);
}
