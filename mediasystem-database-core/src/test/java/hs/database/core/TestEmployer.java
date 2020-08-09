package hs.database.core;

import hs.database.annotations.Column;
import hs.database.annotations.Id;
import hs.database.annotations.Table;

@Table(name = "employers")
public class TestEmployer extends DatabaseObject {

  @Id
  private Integer id;

  @Column
  private String name;

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
}
