package model;
import jakarta.persistence.*;
@Entity @Table(name = "states")
public class State {
  @Id private Long id;
  private String name;
  @Column(name = "country_id") private Integer countryId;
  public Long getId(){ return id; }
  public String getName(){ return name; }
}
