package hs.mediasystem.util.events;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class InMemoryEventStreamTest {
  private EventStream<String> stream = new InMemoryEventStream<>();

  @Test
  void shouldSendAndReceiveEvents() {
    List<String> processed = new ArrayList<>();

    stream.subscribe(x -> processed.add(x));

    stream.push("A");

    await().until(() -> processed.size() == 1);

    assertThat(processed).containsExactly("A");

    List<String> processed2 = new ArrayList<>();

    stream.subscribe(x -> processed2.add(x));

    await().until(() -> processed2.size() == 1);

    assertThat(processed2).containsExactly("A");

    stream.push("B");

    await().until(() -> processed.size() == 2 && processed2.size() == 2);

    assertThat(processed).containsExactly("A", "B");
    assertThat(processed2).containsExactly("A", "B");
  }

  @RepeatedTest(10)
  void stressTest() {
    List<List<String>> lists = new ArrayList<>();

    for(int i = 0; i < 100; i++) {
      List<String> list = new ArrayList<>();

      lists.add(list);
      stream.subscribe(x -> list.add(x));
    }

    new Thread(() -> createEvents(0, 1000)).start();

    await().until(() -> lists.stream().allMatch(x -> x.size() == 1000));

    assertThat(lists).allMatch(x -> x.size() == 1000);

    List<String> expected = new ArrayList<>();

    for(int i = 0; i < 1000; i++) {
      expected.add("" + i);
    }

    for(List<String> list : lists) {
      assertThat(list).containsExactlyElementsOf(expected);
    }
  }

  @RepeatedTest(3)
  void slowTest() {
    List<List<String>> lists = new ArrayList<>();

    for(int i = 0; i < 1000; i++) {
      List<String> list = new ArrayList<>();

      lists.add(list);
      stream.subscribe(x -> list.add(x));
    }

    int max = 10;
    int count = 0;

    for(int i = 0; i < max; i++) {
      createEvents(count, 1);

      long end = System.nanoTime() + (long)(Math.random() * 100000000);

      while(System.nanoTime() < end) {}

      createEvents(count + 1, 1);
      count += 2;

      final int finalCount = count;

      await().until(() -> lists.stream().allMatch(x -> x.size() == finalCount));

      assertThat(lists).allMatch(x -> x.size() == finalCount);
    }

    List<String> expected = new ArrayList<>();

    for(int i = 0; i < max * 2; i++) {
      expected.add("" + i);
    }

    for(List<String> list : lists) {
      assertThat(list).containsExactlyElementsOf(expected);
    }
  }

  void createEvents(int offset, int count) {
    for(int i = 0; i < count; i++) {
      stream.push("" + (i + offset));
    }
  }
}
