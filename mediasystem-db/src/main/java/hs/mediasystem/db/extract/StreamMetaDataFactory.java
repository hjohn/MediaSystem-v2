package hs.mediasystem.db.extract;

import hs.mediasystem.db.extract.grabber.FFmpegFrameGrabber;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.work.Snapshot;
import hs.mediasystem.domain.work.StreamMetaData;
import hs.mediasystem.util.ImageURI;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

  public StreamMetaData generatePreviewImage(ContentID contentId, File file) throws Exception {
    @SuppressWarnings("resource")
    FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file);

    try {
      grabber.start();

      Duration duration = Duration.ofNanos(grabber.getLengthInTime() * 1000L);
      long frameCount = grabber.getLengthInFrames();

      // Grab 12 snapshots from 5, 12.5, 20, 27.5, 35, 42.5, 50, 57.5, 65, 72.5, 80, 87.5% of stream.
      List<Snapshot> snapshots = new ArrayList<>();
      int index = 0;

      for(long offset = frameCount / 20; offset < frameCount * 9 / 10; offset += frameCount * 75 / 1000) {
        if(!store.existsSnapshot(contentId, index)) {
          LOGGER.finest("Grabbing frame #" + index + " at frame " + offset + "/" + frameCount + " with aspect " + grabber.getAspectRatio());

          grabber.setFrameNumber((int)offset);

          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          Frame frame = grabber.grabKeyFrame();  // returns null sometimes on bad files?

          if(frame == null) {
            throw new IllegalStateException("Couldn't grab frame " + offset + "/" + frameCount + " from " + file + ", unknown error");
          }

          ImageIO.write(toBufferedImage(frame), "jpg", baos);

          store.storeImage(contentId, index, baos.toByteArray());
        }

        index++;
        snapshots.add(new Snapshot(new ImageURI("localdb://" + contentId.asInt() + "/" + index, null), (int)offset));
      }

      return new StreamMetaData(contentId, Optional.of(duration), grabber.getVideoTracks(), grabber.getAudioTracks(), grabber.getSubtitleTracks(), snapshots);
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

