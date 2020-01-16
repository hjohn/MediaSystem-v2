package hs.mediasystem.db.extract;

import hs.mediasystem.db.extract.grabber.FFmpegFrameGrabber;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.domain.work.Snapshot;
import hs.mediasystem.domain.work.StreamMetaData;
import hs.mediasystem.util.ImageURI;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

@Singleton
public class StreamMetaDataFactory {
  private static final Logger LOGGER = Logger.getLogger(StreamMetaDataFactory.class.getName());

  @Inject private DefaultStreamMetaDataStore store;

  public StreamMetaData generatePreviewImage(StreamID streamId, File file) throws IOException, Exception {
    @SuppressWarnings("resource")
    FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file);

    try {
      grabber.start();

      Duration duration = Duration.ofNanos(grabber.getLengthInTime() * 1000L);
      long frameCount = grabber.getLengthInFrames();

      // Grab 5 snapshots from 20, 35, 50, 65, 80% of stream.
      List<Snapshot> snapshots = new ArrayList<>();
      int index = 0;

      for(long offset = frameCount / 5; offset < frameCount * 9 / 10; offset += frameCount * 3 / 20) {
        if(!store.existsSnapshot(streamId, index)) {
          LOGGER.finest("Grabbing frame #" + index + " at frame " + offset + "/" + frameCount);

          grabber.setFrameNumber((int)offset);

          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          ImageIO.write(toBufferedImage(grabber.grabKeyFrame()), "jpg", baos);

          store.storeImage(streamId, index, baos.toByteArray());
        }

        index++;
        snapshots.add(new Snapshot(new ImageURI("localdb://" + streamId.asInt() + "/" + index), (int)offset));
      }

      return new StreamMetaData(streamId, duration, grabber.getVideoStreams(), grabber.getAudioStreams(), grabber.getSubtitleStreams(), snapshots);
    }
    finally {
      grabber.stop();
    }
  }

  private static BufferedImage toBufferedImage(Frame inputFrame) {
    Java2DFrameConverter paintConverter = new Java2DFrameConverter();

    return paintConverter.getBufferedImage(inputFrame, 1);
  }
}

