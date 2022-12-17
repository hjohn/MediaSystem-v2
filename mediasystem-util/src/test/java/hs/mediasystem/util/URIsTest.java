package hs.mediasystem.util;

import java.net.URI;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

public class URIsTest {

  @ParameterizedTest
  @ValueSource(strings = {
    "http://host.com/root/child/",
    "http://HOST.com/root/child/",
    "HTTP://host.com/root/child/",
    "http://host.com/root/child/a/",
    "http://host.com/root/child/a/b/c/d",
    "http://host.com/root/child/a/b/c/d?param1=2"
  })
  void isParentOfShouldBeTrue(String child) {
    URI parent = URI.create("http://host.com/root/child");
    URI c = URI.create(child);

    assertThat(URIs.isAncestorOf(parent, c)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "http://host.com/root/child/a/",
    "http://host.com/root/child/a/b/c/d",
    "http://host.com/root/child/a/b/c/d?param1=2"
  })
  void isParentOfShouldBeTrueWhenParentEndsWithSlash(String child) {
    URI parent = URI.create("http://host.com/root/child/");
    URI c = URI.create(child);

    assertThat(URIs.isAncestorOf(parent, c)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "http://host.com/root/child",
    "http://HOST.com/root/child",
    "HTTP://host.com/root/child",
    "http://host.com/root/chil",
    "http://host.com/root/children",
    "http://host.com/root/CHILD/a/b/c/d",
    "http://host.com/root/sibling",
    "https://host.com/root/child/a/b",
    "http://otherhost.com/root/child/a/b",
    "http://host.com:134/root/child/a/b",
    "http://host.com:80/root/child/a/b",
    "http://foo@host.com/root/child/a/b",
  })
  void isParentOfShouldBeFalse(String child) {
    URI parent = URI.create("http://host.com/root/child");
    URI c = URI.create(child);

    assertThat(URIs.isAncestorOf(parent, c)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "http://host.com/root/child",
    "http://HOST.com/root/child",
    "HTTP://host.com/root/child",
    "http://host.com/root/child/",
    "http://HOST.com/root/child/",
    "HTTP://host.com/root/child/",
    "http://host.com/root/chil",
    "http://host.com/root/children",
    "http://host.com/root/CHILD/a/b/c/d",
    "http://host.com/root/sibling",
    "https://host.com/root/child/a/b",
    "http://otherhost.com/root/child/a/b",
    "http://host.com:134/root/child/a/b",
    "http://host.com:80/root/child/a/b",
    "http://foo@host.com/root/child/a/b",
  })
  void isParentOfShouldBeFalseWhenParentEndsWithSlash(String child) {
    URI parent = URI.create("http://host.com/root/child/");
    URI c = URI.create(child);

    assertThat(URIs.isAncestorOf(parent, c)).isFalse();
  }
}
