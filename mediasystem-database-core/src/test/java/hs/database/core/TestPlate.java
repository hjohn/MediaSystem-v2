package hs.database.core;

import hs.database.annotations.Embeddable;
import hs.database.annotations.EmbeddableColumn;

@Embeddable
public class TestPlate {

  @EmbeddableColumn(1)
  private final String countryCode;

  @EmbeddableColumn(2)
  private final String licensePlate;

  public TestPlate(String countryCode, String licensePlate) {
    this.countryCode = countryCode;
    this.licensePlate = licensePlate;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((countryCode == null) ? 0 : countryCode.hashCode());
    result = prime * result + ((licensePlate == null) ? 0 : licensePlate.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null) {
      return false;
    }
    if(getClass() != obj.getClass()) {
      return false;
    }
    TestPlate other = (TestPlate) obj;
    if(countryCode == null) {
      if(other.countryCode != null) {
        return false;
      }
    }
    else if(!countryCode.equals(other.countryCode)) {
      return false;
    }
    if(licensePlate == null) {
      if(other.licensePlate != null) {
        return false;
      }
    }
    else if(!licensePlate.equals(other.licensePlate)) {
      return false;
    }
    return true;
  }
}
