package hs.mediasystem.db.extract;

import hs.mediasystem.domain.media.AudioTrack;
import hs.mediasystem.domain.media.Resolution;
import hs.mediasystem.domain.media.Snapshot;
import hs.mediasystem.domain.media.StreamMetaData;
import hs.mediasystem.domain.media.SubtitleTrack;
import hs.mediasystem.domain.media.VideoTrack;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.util.image.ImageURI;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.int4.dirk.api.Injector;
import org.int4.dirk.jsr330.Injectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StreamMetaDataCodecTest {
  private StreamMetaDataCodec codec;

  @BeforeEach
  void beforeEach() {
    Injector injector = Injectors.autoDiscovering();

    injector.register(StreamMetaDataCodec.class);

    codec = injector.getInstance(StreamMetaDataCodec.class);
  }

  @Test
  void test() throws IOException {
    byte[] data = codec.encode(new StreamMetaData(
      new ContentID(120),
      Optional.of(Duration.ofSeconds(999)),
      List.of(
        new VideoTrack("title", "lang", "mp4", new Resolution(100, 200, 1.2f), 1234567L, 23.93f)
      ),
      List.of(
        new AudioTrack("audiotitle", "audiolang", "mp3", 3)
      ),
      List.of(
        new SubtitleTrack("subtitle", "sublang", "srt")
      ),
      List.of(
        new Snapshot(new ImageURI("localdb://12345/1", null), 34222)
      )
    ));

    StreamMetaData metaData = codec.decode(data);

    assertEquals(new ContentID(120), metaData.contentId());
    assertEquals(Optional.of(Duration.ofSeconds(999)), metaData.length());
    assertEquals("title", metaData.videoTracks().get(0).title());
    assertEquals("mp3", metaData.audioTracks().get(0).codec());
    assertEquals("srt", metaData.subtitleTracks().get(0).codec());
    assertEquals(new ImageURI("localdb://12345/1", null), metaData.snapshots().get(0).imageUri());
  }

}
