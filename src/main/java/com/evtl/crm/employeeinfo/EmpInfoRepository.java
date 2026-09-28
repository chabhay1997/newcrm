package com.evtl.crm.employeeinfo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmpInfoRepository extends JpaRepository<EmpInfo, Long> {

    List<EmpInfo> findAllByOrderByJoinDateDesc();
}
