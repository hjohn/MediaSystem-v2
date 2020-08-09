package hs.database.core;

import hs.database.annotations.Column;
import hs.database.annotations.Id;
import hs.database.annotations.Table;

@Table(name = "employees")
public class TestBadEmployeeNoEmptyConstructor extends DatabaseObject {
  public enum Hours {PART_TIME, FULL_TIME}

  @Id
  private Integer id;

  @Column
  private String name;

  @Column(name = "employers_id")
  private TestEmployer employer;

  @Column
  private Hours hours;

  public TestBadEmployeeNoEmptyConstructor(String name) {
    this.name = name;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public TestEmployer getEmployer() {
    return employer;
  }

  public void setEmployer(TestEmployer employer) {
    this.employer = employer;
  }

  public Hours getHours() {
    return hours;
  }

  public void setHours(Hours hours) {
    this.hours = hours;
  }
}
