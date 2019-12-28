package hs.mediasystem.plugin.library.scene.grid;

import hs.mediasystem.plugin.library.scene.BinderBase;

public interface IDBinder<T> extends BinderBase<T> {
  String toId(T item);
}
