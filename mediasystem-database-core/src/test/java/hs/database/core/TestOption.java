package hs.database.core;

import hs.database.annotations.Column;
import hs.database.annotations.Id;
import hs.database.annotations.Table;

@Table(name = "options")
public class TestOption {

  @Id
  private Integer id;

  @Column(name = {"car_cc", "car_plate"})
  private TestCar car;

  @Column
  private String optionDescription;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public TestCar getCar() {
    return car;
  }

  public void setCar(TestCar car) {
    this.car = car;
  }

  public String getOptionDescription() {
    return optionDescription;
  }

  public void setOptionDescription(String optionDescription) {
    this.optionDescription = optionDescription;
  }
}
