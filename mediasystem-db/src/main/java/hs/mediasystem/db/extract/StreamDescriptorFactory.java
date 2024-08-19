package hs.mediasystem.db.extract;

import hs.mediasystem.db.extract.grabber.FFmpegFrameGrabber;
import hs.mediasystem.domain.media.AudioTrack;
import hs.mediasystem.domain.media.SubtitleTrack;
import hs.mediasystem.domain.media.VideoTrack;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.inject.Singleton;

import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

@Singleton
public class StreamDescriptorFactory {
  record RawSnapshot(byte[] imageData, int frameNumber, int index) {}
  record RawDescriptor(Optional<Duration> duration, List<VideoTrack> videoTracks, List<AudioTrack> audioTracks, List<SubtitleTrack> subtitleTracks, List<RawSnapshot> snapshots) {}

  private static final Logger LOGGER = System.getLogger(StreamDescriptorFactory.class.getName());

  {
    avutil.av_log_set_level(avutil.AV_LOG_FATAL);
  }

  public RawDescriptor create(File file) throws Exception {
    @SuppressWarnings("resource")
    FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file);

    try {
      grabber.start();

      Duration duration = Duration.ofNanos(grabber.getLengthInTime() * 1000L);
      long frameCount = grabber.getLengthInFrames();

      // Grab 12 snapshots from 5, 12.5, 20, 27.5, 35, 42.5, 50, 57.5, 65, 72.5, 80, 87.5% of stream.
      List<RawSnapshot> snapshots = new ArrayList<>();
      int index = 1;

      for(long offset = frameCount / 20; offset < frameCount * 9 / 10; offset += frameCount * 75 / 1000) {
        LOGGER.log(Level.TRACE, "Grabbing frame #" + index + " at frame " + offset + "/" + frameCount + " with aspect " + grabber.getAspectRatio());

        grabber.setFrameNumber((int)offset);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Frame frame = grabber.grabKeyFrame();  // returns null sometimes on bad files?

        if(frame == null) {
          LOGGER.log(Level.WARNING, "Couldn't grab frame " + offset + "/" + frameCount + " from " + file + ", unknown error");
        }
        else {
          ImageIO.write(toBufferedImage(frame), "jpg", baos);

          snapshots.add(new RawSnapshot(baos.toByteArray(), (int)offset, index));
        }

        index++;
      }

      return new RawDescriptor(Optional.of(duration), grabber.getVideoTracks(), grabber.getAudioTracks(), grabber.getSubtitleTracks(), snapshots);
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

