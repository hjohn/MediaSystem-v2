package hs.mediasystem.db.events;

public interface EventSerializer<T> extends Serializer<T> {
  enum Type { FULL, DELETE }

  Type extractType(T event);
  String extractAggregateId(T event);
}
