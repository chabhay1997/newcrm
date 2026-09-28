package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "projects")
public class ProjectStatus {
    @Id private Long id;
    @Column(name = "project_status") private String projectStatus;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProjectStatus() { return projectStatus; }
    public void setProjectStatus(String projectStatus) { this.projectStatus = projectStatus; }
}
