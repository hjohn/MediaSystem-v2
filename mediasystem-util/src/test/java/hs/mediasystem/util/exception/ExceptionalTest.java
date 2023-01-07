package hs.mediasystem.util.exception;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExceptionalTest {
    private final Exceptional<String> text = Exceptional.of("123");
    private final Exceptional<String> empty = Exceptional.empty();
    private final Exceptional<String> exception = Exceptional.ofException(new IllegalStateException());

    @SafeVarargs
    private static <T> List<T> of(T... elements) {
        return Arrays.asList(elements);
    }

    @Test
    public void emptyShouldReturnEmptyExceptional() {
        Exceptional<Object> exceptional = Exceptional.empty();

        assertNotNull(exceptional);
        assertFalse(exceptional.isPresent());
    }

    @Test
    public void ofNullableShouldAcceptAllValues() {
        assertEquals("test", Exceptional.ofNullable("test").get());
        assertEquals((Integer)1, Exceptional.ofNullable(1).get());
        assertFalse(Exceptional.ofNullable(null).isPresent());
    }

    @Test
    public void ofShouldThrowExceptionWhenSupplyingNull() {
      assertThrows(NullPointerException.class, () -> Exceptional.of(null));
    }

    @Test
    public void ofExceptionShouldAcceptValidValues() {
        Exceptional<Object> exceptional = Exceptional.ofException(new IllegalStateException());

        assertNotNull(exceptional);
        assertTrue(exceptional.isException());
    }

    @Test
    public void ofExceptionShouldThrowExceptionWhenSupplyingNull() {
      assertThrows(NullPointerException.class, () -> Exceptional.ofException(null));
    }

    @Test
    public void ofNullableExceptionShouldAcceptExceptionsAndNull() {
        IllegalStateException exception = new IllegalStateException();

        assertEquals(exception, Exceptional.ofNullableException(exception).getException());
        assertFalse(Exceptional.ofNullableException(null).isPresent());
    }

    @Test
    public void fromShouldAcceptAllValues() {
        assertFalse(Exceptional.from(() -> null).isPresent());
        assertTrue(Exceptional.from(() -> "filled").isPresent());
        assertTrue(Exceptional.from(() -> { throw new IllegalStateException(); }).isException());
    }

    @Test
    public void fromVoidShouldAcceptAllValues() {
        assertFalse(Exceptional.fromVoid(() -> {}).isPresent());
        assertTrue(Exceptional.fromVoid(() -> { throw new IllegalStateException(); }).isException());
    }

    @Test
    public void getShouldReturnContainedValue() {
        assertEquals("123", text.get());
    }

    @Test
    public void getShouldThrowExceptionWhenEmpty() {
      assertThrows(NoSuchElementException.class, () -> empty.get());
    }

    @Test
    public void getShouldThrowExceptionalExceptionWhenExceptionIsPresent() {
      assertThrows(ExceptionalException.class, () -> exception.get());
    }

    @Test
    public void getExceptionShouldReturnContainedException() {
        assertTrue(exception.getException() instanceof IllegalStateException);
    }

    @Test
    public void getExceptionShouldThrowExceptionWhenNoContainedExceptionIsPresent() {
        assertThrows(NoSuchElementException.class, () -> text.getException());
    }

    @Test
    public void orElseShouldReturnCorrectValues() {
        assertEquals("123", text.orElse("abc"));
        assertEquals("abc", empty.orElse("abc"));
    }

    @Test
    public void orElseShouldThrowExceptionalExceptionWhenExceptionIsPresent() {
      assertThrows(ExceptionalException.class, () -> exception.orElse("abc"));
    }

    @Test
    public void orElseGetShouldReturnCorrectValues() {
        assertEquals("123", text.orElseGet(() -> "abc"));
        assertEquals("abc", empty.orElseGet(() -> "abc"));
    }

    @Test
    public void orElseGetShouldThrowExceptionalExceptionWhenExceptionIsPresent() {
      assertThrows(ExceptionalException.class, () -> exception.orElseGet(() -> "abc"));
    }

    @Test
    public void orElseGetShouldThrowExceptionWhenSupplierIsNull() {
      assertThrows(NullPointerException.class, () -> exception.orElseGet(null));
    }

    @Test
    public void orElseThrowShouldReturnCorrectValues() {
        assertEquals("123", text.orElseThrow(IllegalArgumentException::new));
    }

    @Test
    public void orElseThrowShouldThrowSuppliedExceptionWhenEmpty() {
      assertThrows(IllegalArgumentException.class, () -> empty.orElseThrow(IllegalArgumentException::new));
    }

    @Test
    public void orElseThrowShouldThrowContainedExceptionWhenExceptionIsPresent() {
      assertThrows(ExceptionalException.class, () -> exception.orElseThrow(IllegalArgumentException::new));
    }

    @Test
    public void orElseThrowShouldThrowNullPointerExceptionWhenSupplierIsNull() {
      assertThrows(NullPointerException.class, () -> exception.orElseThrow(null));
    }

    @Test
    public void orShouldReturnValueOfExceptionalIfPresentOtherwiseValueOfSuppliedExceptional() {
        assertEquals("123", text.or(() -> Exceptional.of("abc")).get());
        assertEquals("abc", empty.or(() -> Exceptional.of("abc")).get());
        assertTrue(exception.or(() -> Exceptional.of("abc")).isException());
    }

    @Test
    public void orShouldThrowExceptionWhenSuppliedNull() {
      assertThrows(NullPointerException.class, () -> text.or(null));
    }

    @Test
    public void orShouldThrowExceptionWhenValueNotPresentAndSupplierReturnsNull() {
      assertThrows(NullPointerException.class, () -> empty.or(() -> null));
    }

    @Test
    public void mapShouldHandleValidCases() {
        assertEquals((Integer)123, text.map(Integer::parseInt).get());
        assertFalse(empty.map(Integer::parseInt).isPresent());
        assertEquals(exception, exception.map(Integer::parseInt));
        assertTrue(text.map(x -> { throw new IllegalStateException(); }).isException());
    }

    @Test
    public void mapShouldThrowExceptionWhenSuppliedNull() {
      assertThrows(NullPointerException.class, () -> empty.map(null));
    }

    @Test
    public void flatMapShouldApplyMappingFunctionAndUnwrapNestedExceptional() {
        assertEquals(Exceptional.of("1234"), text.flatMap(x -> Exceptional.of(x + "4")));  // Map would have returned Exceptional.of(Exceptional.of("1234"))
        assertEquals(empty, empty.flatMap(x -> Exceptional.of(x + "4")));
        assertEquals(exception, exception.flatMap(x -> Exceptional.of(x + "4")));
        assertTrue(text.flatMap(x -> { throw new IllegalStateException(); }).isException());
    }

    @Test
    public void flatMapShouldThrowExceptionWhenSuppliedNull() {
      assertThrows(NullPointerException.class, () -> text.flatMap(null));
    }

    @Test
    public void flatMapShouldThrowExceptionWhenMappingFunctionReturnsNull() {
      assertThrows(NullPointerException.class, () -> text.flatMap(x -> null));
    }

    @Test
    public void streamShouldReturnEmptyOrOneElementStream() {
        assertEquals(1, text.stream().count());
        assertEquals(0, empty.stream().count());
    }

    @Test
    public void streamShouldThrowExceptionalExceptionWhenContainsException() {
      assertThrows(ExceptionalException.class, () -> exception.stream());
    }

    @Test
    public void ignoreAllAndStreamShouldReturnEmptyOrOneElementStreamEvenIfItContainsException() {
        assertEquals(1, text.ignoreAllAndStream().count());
        assertEquals(0, empty.ignoreAllAndStream().count());
        assertEquals(0, exception.ignoreAllAndStream().count());
    }

    @Test
    public void filterShouldReturnCorrectValues() {
        assertEquals(text, text.filter(x -> x.equals("123")));
        assertEquals(empty, text.filter(x -> x.equals("1234")));
        assertEquals(empty, empty.filter(x -> x.equals("1234")));
        assertEquals(exception, exception.filter(x -> x.equals("1234")));
        assertTrue(exception.filter(x -> { throw new IllegalStateException(); }).isException());
    }

    @Test
    public void filterShouldThrowExceptionWhenFilterIsNull() {
      assertThrows(NullPointerException.class, () -> text.filter(null));
    }

    @Test
    public void recoverShouldHandleExceptionIfPresent() {
        assertEquals(text, text.recover(x -> null));
        assertEquals(text, text.recover(x -> "abc"));
        assertEquals(empty, empty.recover(x -> "abc"));
        assertEquals("abc", exception.recover(x -> "abc").get());
        assertEquals(empty, exception.recover(x -> null));
        assertTrue(exception.recover(x -> { throw new IllegalArgumentException(); }).getException() instanceof IllegalArgumentException);

        assertEquals(text, text.recover(Exception.class, x -> null));
        assertEquals(text, text.recover(Exception.class, x -> "abc"));
        assertEquals(empty, empty.recover(Exception.class, x -> "abc"));
        assertEquals("abc", exception.recover(Exception.class, x -> "abc").get());
        assertEquals(empty, exception.recover(Exception.class, x -> null));
        assertTrue(exception.recover(Exception.class, x -> { throw new IllegalArgumentException(); }).getException() instanceof IllegalArgumentException);

        assertEquals(text, text.recover(of(IllegalArgumentException.class), x -> null));
        assertEquals(text, text.recover(of(IllegalArgumentException.class), x -> "abc"));
        assertEquals(empty, empty.recover(of(IllegalArgumentException.class), x -> "abc"));
        assertTrue(exception.recover(of(IllegalArgumentException.class), x -> "abc").getException()  instanceof IllegalStateException);  // did not recover, as didn't match type
        assertEquals(exception, exception.recover(of(IllegalArgumentException.class), x -> null));
        assertTrue(exception.recover(of(IllegalArgumentException.class), x -> { throw new IllegalArgumentException(); }).getException() instanceof IllegalStateException);  // did not recover, as didn't match type

        assertEquals(text, text.recover(of(IllegalArgumentException.class, IllegalStateException.class), x -> null));
        assertEquals(text, text.recover(of(IllegalArgumentException.class, IllegalStateException.class), x -> "abc"));
        assertEquals(empty, empty.recover(of(IllegalArgumentException.class, IllegalStateException.class), x -> "abc"));
        assertEquals("abc", exception.recover(of(IllegalArgumentException.class, IllegalStateException.class), x -> "abc").get());
        assertEquals(empty, exception.recover(of(IllegalArgumentException.class, IllegalStateException.class), x -> null));
        assertTrue(exception.recover(of(IllegalArgumentException.class, IllegalStateException.class), x -> { throw new IllegalArgumentException(); }).getException() instanceof IllegalArgumentException);
    }

    @Test
    public void recoverShouldThrowExceptionWhenSuppliedConsumerIsNull() {
      assertThrows(NullPointerException.class, () -> text.recover(null));
    }

    @Test
    public void recover2ShouldThrowExceptionWhenSuppliedConsumerIsNull() {
      assertThrows(NullPointerException.class, () -> text.recover(Exception.class, null));
    }

    @Test
    public void recover2ShouldThrowExceptionWhenSuppliedExceptionTypeIsNull() {
      assertThrows(NullPointerException.class, () -> text.recover((Class<? extends Throwable>)null, x -> "123"));
    }

    @Test
    public void recover3ShouldThrowExceptionWhenSuppliedConsumerIsNull() {
      assertThrows(NullPointerException.class, () -> text.recover(of(Exception.class), null));
    }

    @Test
    public void recover3ShouldThrowExceptionWhenSuppliedExceptionTypeIsNull() {
      assertThrows(NullPointerException.class, () -> text.recover((Iterable<Class<? extends Throwable>>)null, x -> "123"));
    }

    @Test
    public void handleShouldHandleExceptionIfPresent() {
        assertEquals(text, text.handle(x -> {}));
        assertEquals(empty, empty.handle(x -> {}));
        assertEquals(empty, exception.handle(x -> {}));
        assertTrue(exception.handle(x -> { throw new IllegalArgumentException(); }).getException() instanceof IllegalArgumentException);

        assertEquals(text, text.handle(Exception.class, x -> {}));
        assertEquals(empty, empty.handle(Exception.class, x -> {}));
        assertEquals(empty, exception.handle(Exception.class, x -> {}));
        assertTrue(exception.handle(Exception.class, x -> { throw new IllegalArgumentException(); }).getException() instanceof IllegalArgumentException);

        assertEquals(text, text.handle(of(IllegalArgumentException.class), x -> {}));
        assertEquals(empty, empty.handle(of(IllegalArgumentException.class), x -> {}));
        assertTrue(exception.handle(of(IllegalArgumentException.class), x -> {}).getException()  instanceof IllegalStateException);  // did not recover, as didn't match type
        assertEquals(exception, exception.handle(of(IllegalArgumentException.class), x -> {}));
        assertTrue(exception.handle(of(IllegalArgumentException.class), x -> { throw new IllegalArgumentException(); }).getException() instanceof IllegalStateException);  // did not recover, as didn't match type

        assertEquals(text, text.handle(of(IllegalArgumentException.class, IllegalStateException.class), x -> {}));
        assertEquals(empty, empty.handle(of(IllegalArgumentException.class, IllegalStateException.class), x -> {}));
        assertEquals(empty, exception.handle(of(IllegalArgumentException.class, IllegalStateException.class), x -> {}));
        assertTrue(exception.handle(of(IllegalArgumentException.class, IllegalStateException.class), x -> { throw new IllegalArgumentException(); }).getException() instanceof IllegalArgumentException);
    }

    @Test
    public void handleShouldThrowExceptionWhenSuppliedConsumerIsNull() {
      assertThrows(NullPointerException.class, () -> text.handle(null));
    }

    @Test
    public void handle2ShouldThrowExceptionWhenSuppliedConsumerIsNull() {
      assertThrows(NullPointerException.class, () -> text.handle(Exception.class, null));
    }

    @Test
    public void handle2ShouldThrowExceptionWhenSuppliedExceptionTypeIsNull() {
      assertThrows(NullPointerException.class, () -> text.handle((Class<? extends Throwable>)null, x -> {}));
    }

    @Test
    public void handle3ShouldThrowExceptionWhenSuppliedConsumerIsNull() {
      assertThrows(NullPointerException.class, () -> text.handle(of(Exception.class), null));
    }

    @Test
    public void handle3ShouldThrowExceptionWhenSuppliedExceptionTypeIsNull() {
      assertThrows(NullPointerException.class, () -> text.handle((Iterable<Class<? extends Throwable>>)null, x -> {}));
    }

    @Test
    public void isNotEmptyShouldReturnCorrectValues() {
        assertTrue(text.isNotEmpty());
        assertFalse(empty.isNotEmpty());
        assertTrue(exception.isNotEmpty());
    }

    @Test
    public void isExceptionShouldReturnCorrectValues() {
        assertFalse(text.isException());
        assertFalse(empty.isException());
        assertTrue(exception.isException());
    }

    @Test
    public void isNotExceptionShouldReturnCorrectValues() {
        assertTrue(text.isNotException());
        assertTrue(empty.isNotException());
        assertFalse(exception.isNotException());
    }

    @Test
    public void isPresentShouldReturnCorrectValues() {
        assertTrue(text.isPresent());
        assertFalse(empty.isPresent());
        assertFalse(exception.isPresent());
    }

    @Test
    public void ifPresentShouldExecuteActionIfValueIsPresent() {
        AtomicBoolean ab = new AtomicBoolean();

        text.ifPresent(x -> ab.set(true));

        assertTrue(ab.get());
    }

    @Test
    public void ifPresentShouldSkipActionIfNoValueOrExceptionIsPresent() {
        AtomicBoolean ab = new AtomicBoolean();

        empty.ifPresent(x -> ab.set(true));

        assertFalse(ab.get());
    }

    @Test
    public void ifPresentShouldThrowContainedExceptionIfPresent() {
      assertThrows(ExceptionalException.class, () -> exception.ifPresent(x -> {}));
    }

    @Test @SuppressWarnings("unlikely-arg-type")
    public void equalsAndHashCodeShouldFollowContract() {
        Exceptional<String> e1 = Exceptional.of("abc");
        Exceptional<String> e2 = Exceptional.of("abc");
        Exceptional<String> e3 = Exceptional.of("def");
        Exceptional<String> e4 = Exceptional.ofException(new IllegalArgumentException());

        assertTrue(e1.equals(e2));
        assertTrue(e1.equals(e1));
        assertTrue(e2.equals(e1));

        assertFalse(e1.equals(null));
        assertFalse(e1.equals(e3));
        assertFalse(e1.equals(e4));
        assertFalse(e3.equals(e1));
        assertFalse(e4.equals(e1));
        assertFalse(e1.equals("String"));

        assertTrue(e1.hashCode() == e2.hashCode());
        assertFalse(e1.hashCode() == e3.hashCode());
        assertFalse(e1.hashCode() == e4.hashCode());
    }

    @Test
    public void toStringShouldReturnSensibleValues() {
        assertEquals("Exceptional[123]", text.toString());
        assertEquals("Exceptional:empty", empty.toString());
        assertEquals("Exceptional[java.lang.IllegalStateException]", exception.toString());
    }
}


