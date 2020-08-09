package hs.database.core;

import hs.database.annotations.Column;
import hs.database.annotations.Id;
import hs.database.annotations.Table;

import java.util.Date;

@Table(name = "employees")
public class TestEmployee extends DatabaseObject {
  public enum Hours {PART_TIME, FULL_TIME}

  @Id
  private Integer id;

  private String name;

  @Column(name = "employers_id")
  private TestEmployer employer;

  @Column
  private Hours hours;

  private boolean fired;

  private Date lastLoad;

  public TestEmployee() {
  }

  public TestEmployee(String name) {
    this.name = name;
  }

  public void afterLoadStore(@SuppressWarnings("unused") Database database) {
    setLastLoad(new Date(2));
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  @Column
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

  @Column
  public boolean isFired() {
    return fired;
  }

  public void setFired(boolean fired) {
    this.fired = fired;
  }

  public Date getLastLoad() {
    return lastLoad;
  }

  public void setLastLoad(Date lastLoad) {
    this.lastLoad = lastLoad;
  }
}
