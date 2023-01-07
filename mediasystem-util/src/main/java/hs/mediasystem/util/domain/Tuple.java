package hs.mediasystem.util.domain;

import java.util.Objects;

public interface Tuple {

  static <A, B> Tuple2<A, B> of(A a, B b) {
    return new Tuple2<>(a, b);
  }

  static <A, B, C> Tuple3<A, B, C> of(A a, B b, C c) {
    return new Tuple3<>(a, b, c);
  }

  class Tuple2<A, B> {
    public final A a;
    public final B b;

    Tuple2(A a, B b) {
      this.a = a;
      this.b = b;
    }

    @Override
    public String toString() {
      return "(" + a + ", " + b + ")";
    }

    @Override
    public int hashCode() {
      return Objects.hash(a, b);
    }

    @Override
    public boolean equals(Object obj) {
      if(this == obj) {
        return true;
      }
      if(obj == null || getClass() != obj.getClass()) {
        return false;
      }

      @SuppressWarnings("unchecked")
      Tuple2<A, B> other = (Tuple2<A, B>)obj;

      return Objects.equals(a, other.a)
        && Objects.equals(b, other.b);
    }
  }

  class Tuple3<A, B, C> {
    public final A a;
    public final B b;
    public final C c;

    Tuple3(A a, B b, C c) {
      this.a = a;
      this.b = b;
      this.c = c;
    }

    @Override
    public String toString() {
      return "(" + a + ", " + b + ", " + c + ")";
    }

    @Override
    public int hashCode() {
      return Objects.hash(a, b, c);
    }

    @Override
    public boolean equals(Object obj) {
      if(this == obj) {
        return true;
      }
      if(obj == null || getClass() != obj.getClass()) {
        return false;
      }

      @SuppressWarnings("unchecked")
      Tuple3<A, B, C> other = (Tuple3<A, B, C>)obj;

      return Objects.equals(a, other.a)
        && Objects.equals(b, other.b)
        && Objects.equals(c, other.c);
    }


  }
}
