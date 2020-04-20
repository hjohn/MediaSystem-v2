package hs.mediasystem.ext.local;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.util.ImageURI;
import hs.mediasystem.util.Throwables;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import javax.inject.Singleton;

@Singleton
public class DescriptionService {
  private static final Logger LOGGER = Logger.getLogger(DescriptionService.class.getName());
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(new YAMLFactory())
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .setVisibility(PropertyAccessor.GETTER, Visibility.NONE)
    .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
    .registerModule(new JavaTimeModule())
    .registerModule(new ParameterNamesModule(Mode.PROPERTIES));

  public Optional<Description> loadDescription(Streamable streamable) {
    String urlText = streamable.getUri().toString() + "/description.yaml";

    try {
      DescriptionInternal d = OBJECT_MAPPER.readValue(new URL(urlText), DescriptionInternal.class);

      Path base = Paths.get(streamable.getUri().toURI());
      Path cover = base.resolve("cover.jpg");
      Path backdrop = base.resolve("backdrop.jpg");

      ImageURI coverImage = Files.isRegularFile(cover) ? new ImageURI(cover.toUri().toString()) : null;
      ImageURI backdropImage = Files.isRegularFile(backdrop) ? new ImageURI(cover.toUri().toString()) : null;

      return Optional.of(new Description(d.title, d.subtitle, d.description, d.genres, d.date, coverImage, backdropImage));
    }
    catch(ConnectException e) {
      // ignore, file just doesn't exist
      return Optional.empty();
    }
    catch(IOException e) {
      LOGGER.warning("Exception while parsing " + urlText + ": " + Throwables.formatAsOneLine(e));
      return Optional.empty();
    }
  }

  private static class DescriptionInternal {
    public String title;
    public String subtitle;
    public String description;
    public List<String> genres;
    public LocalDate date;
  }
}
