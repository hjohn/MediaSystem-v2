package hs.mediasystem.util.javafx;

import javafx.beans.binding.ObjectBinding;

public abstract class MonadicObjectBinding<T> extends ObjectBinding<T> implements MonadicObservableValue<T> {

}
