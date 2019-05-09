package hs.mediasystem.db;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import hs.mediasystem.ext.basicmediatypes.MediaStream;
import hs.mediasystem.util.Throwables;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LocalMediaCodec {
  private static final Logger LOGGER = Logger.getLogger(LocalMediaCodec.class.getName());

  @Inject private StreamPrintModule streamPrintModule;

  private ObjectMapper objectMapper;

  @PostConstruct
  private void postConstruct() {
    objectMapper = new ObjectMapper()
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
      .setVisibility(PropertyAccessor.GETTER, Visibility.NONE)
      .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
      .registerModule(new ParameterNamesModule(Mode.PROPERTIES))
      .registerModule(new JavaTimeModule())
      .registerModule(new RecordGroupModule())
      .registerModule(streamPrintModule);
  }

  public MediaStream toMediaStream(LocalMedia localMedia) {
    try {
      //System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(localMedia.getJson(), Map.class)));
      return objectMapper.readValue(localMedia.getJson(), MediaStream.class);
    }
    catch(JsonProcessingException e) {
      LOGGER.warning("Exception while decoding LocalMedia: " + Throwables.formatAsOneLine(e));

      return null;
    }
    catch(IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public LocalMedia toLocalMedia(long scannerId, LocalDateTime deleteTime, MediaStream mediaStream) {
    try {
      LocalMedia localMedia = new LocalMedia();

      localMedia.setStreamId(mediaStream.getStreamPrint().getId().asInt());
      localMedia.setScannerId(scannerId);
      localMedia.setDeleteTime(deleteTime);
      localMedia.setJson(objectMapper.writeValueAsBytes(mediaStream));

      return localMedia;
    }
    catch(JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
