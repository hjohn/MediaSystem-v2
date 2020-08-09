package hs.database.core;

import hs.database.annotations.Column;
import hs.database.annotations.Id;
import hs.database.annotations.Table;

@Table(name = "cars")
public class TestCar extends DatabaseObject {

  @Id
  @Column(name = {"countryCode", "licensePlate"})
  private TestPlate id;

  @Column(name = "owners_id")
  private TestEmployee owner;

  @Column
  private String brand;

  @Column
  private String model;

  public TestPlate getId() {
    return id;
  }

  public void setId(TestPlate id) {
    this.id = id;
  }

  public String getBrand() {
    return brand;
  }

  public void setBrand(String brand) {
    this.brand = brand;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public TestEmployee getOwner() {
    //return AnnotatedRecordMapper.fetch(owner);
    return owner;
  }

  public void setOwner(TestEmployee owner) {
    this.owner = owner;
  }
}
