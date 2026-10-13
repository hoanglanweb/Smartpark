package com.example.SmartPark.repository;

import com.example.SmartPark.model.AccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {
    //?: Truy vấn danh sách bản ghi truy cập theo biển số xe, sắp xếp theo thời gian sự kiện giảm dần
    List<AccessLog> findByPlateNumberOrderByEventTimeDesc(String plateNumber);
    //?: Truy vấn danh sách 50 bản ghi truy cập gần nhất, sắp xếp theo thời gian sự kiện giảm dần
    List<AccessLog> findTop50ByOrderByEventTimeDesc();
}