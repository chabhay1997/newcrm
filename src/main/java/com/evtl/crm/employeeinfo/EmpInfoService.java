package com.evtl.crm.employeeinfo;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmpInfoService {

    private final EmpInfoRepository repository;

    public EmpInfoService(EmpInfoRepository repository) {
        this.repository = repository;
    }

    public List<EmpInfo> findAll() {
        return repository.findAllByOrderByJoinDateDesc();
    }

    public EmpInfo findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee information not found"));
    }

    public EmpInfo save(EmpInfo empInfo) {
        return repository.save(empInfo);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
