package hs.mediasystem.ext.local;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import hs.mediasystem.util.exception.Throwables;
import hs.mediasystem.util.image.ImageURI;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
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

  public Optional<Description> loadDescription(URI uri) {
    String urlText = uri.toString() + "/description.yaml";

    try {
      // TODO this mapping takes up to 4 seconds to "Connect" to the URL, whether the file exists or not...
      DescriptionInternal d = OBJECT_MAPPER.readValue(new URI(urlText).toURL(), DescriptionInternal.class);

      return Optional.of(new Description(d.title, d.subtitle, d.description, d.tagLine, d.genres, d.date));
    }
    catch(ConnectException e) {
      // ignore, file just doesn't exist
      return Optional.empty();
    }
    catch(IOException | URISyntaxException e) {
      LOGGER.warning("Exception while parsing " + urlText + ": " + Throwables.formatAsOneLine(e));
      return Optional.empty();
    }
  }

  public Optional<ImageURI> getCover(URI uri) {
    Path base = Paths.get(uri);
    Path cover = base.resolve("cover.jpg");

    return Optional.ofNullable(Files.isRegularFile(cover) ? new ImageURI(cover.toUri().toString(), null) : null);
  }

  public Optional<ImageURI> getBackdrop(URI uri) {
    Path base = Paths.get(uri);
    Path backdrop = base.resolve("backdrop.jpg");

    return Optional.ofNullable(Files.isRegularFile(backdrop) ? new ImageURI(backdrop.toUri().toString(), null) : null);
  }

  private static class DescriptionInternal {
    public String title;
    public String subtitle;
    public String description;
    public String tagLine;
    public List<String> genres;
    public LocalDate date;
  }
}
