package hs.mediasystem.plugin.library.scene.serie;

import hs.mediasystem.ext.basicmediatypes.Identification;
import hs.mediasystem.ext.basicmediatypes.Identification.MatchType;
import hs.mediasystem.ext.basicmediatypes.domain.stream.AudioStream;
import hs.mediasystem.ext.basicmediatypes.domain.stream.StreamMetaData;
import hs.mediasystem.ext.basicmediatypes.domain.stream.VideoStream;
import hs.mediasystem.mediamanager.MediaService;
import hs.mediasystem.mediamanager.StreamMetaDataProvider;
import hs.mediasystem.plugin.library.scene.MediaItem;
import hs.mediasystem.runner.util.Dialogs;
import hs.mediasystem.runner.util.LessLoader;
import hs.mediasystem.scanner.api.BasicStream;
import hs.mediasystem.scanner.api.StreamPrint;
import hs.mediasystem.scanner.api.StreamPrintProvider;
import hs.mediasystem.util.SizeFormatter;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.GridPane;
import hs.mediasystem.util.javafx.control.Labels;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.Event;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ShowInfoEventHandler {
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.UK).withZone(ZoneOffset.systemDefault());

  @Inject private MediaService mediaService;
  @Inject private StreamMetaDataProvider metaDataProvider;
  @Inject private StreamPrintProvider streamPrintProvider;

  public void handle(Event event, MediaItem<?> mediaItem) {
    Label titleLabel = Labels.create(mediaItem.productionTitle.get(), "title");

    if(titleLabel.getText().length() > 40) {
      titleLabel.getStyleClass().add("smaller");
    }

    VBox titleBox = Containers.vbox("title-panel",
      Labels.create(mediaItem.getParent() != null ? mediaItem.getParent().productionTitle.get() : "", "serie-title", new SimpleBooleanProperty(mediaItem.getParent() != null)),
      titleLabel
    );
    VBox listBox = Containers.vbox("list-panel");
    VBox vbox = Containers.vbox("main-panel");

    vbox.getStylesheets().add(LessLoader.compile(getClass().getResource("show-info-styles.less")).toExternalForm());
    vbox.getChildren().addAll(titleBox, listBox);

    for(BasicStream stream : mediaItem.getStreams()) {
      Identification identification = mediaService.getIdentification(stream.getId(), List.of("TMDB", "LOCAL"));
      GridPane gridPane = Containers.grid("item");
      StreamPrint streamPrint = streamPrintProvider.get(stream.getId());

      String path = stream.getUri().asReadableString().trim();

      // path often contains too few spaces for decent wrapping... add LF's every 80 chars:
      path = addLineFeeds(path, 100);

      gridPane.addRow(
        Labels.create("Path", "title"),
        Labels.create(path, "value"),
        GridPane.FILL,
        GridPane.FILL
      );

      if(streamPrint.getSize() != null) {
        gridPane.addRow(
          Labels.create("File Size", "title"),
          Labels.create(SizeFormatter.BYTES_THREE_SIGNIFICANT.format(streamPrint.getSize()), "value"),
          GridPane.FILL,
          GridPane.FILL
        );
      }

      gridPane.addRow(
        Labels.create("Last Modified", "title"),
        Labels.create("" + DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(streamPrint.getLastModificationTime()).atOffset(ZoneOffset.UTC)), "value"),
        GridPane.FILL,
        GridPane.FILL
      );

      if(identification != null) {
        gridPane.addRow(
          Labels.create("Identification", "title"),
          Labels.create("" + toText(identification), "value"),
          GridPane.FILL,
          GridPane.FILL
        );
      }

      // TODO do in background
      StreamMetaData metaData = metaDataProvider.find(stream.getId());

      if(metaData != null) {
        if(metaData.getDuration() != Duration.ZERO) {
          gridPane.addRow(
            Labels.create("Duration", "title"),
            Labels.create(SizeFormatter.DURATION.format(metaData.getDuration().toSeconds()), "value"),
            GridPane.FILL,
            GridPane.FILL
          );
        }

        List<VideoStream> videoStreams = metaData.getVideoStreams();

        for(int i = 0; i < videoStreams.size(); i++) {
          VideoStream videoStream = videoStreams.get(i);

          gridPane.at(0).align(VPos.TOP).add(i != 0 ? null : Labels.create("Video Streams", "title"));
          gridPane.at(1).align(VPos.TOP).add(Labels.create("#" + (i + 1), "title"));

          if(videoStream.getTitle() != null) {
            gridPane.at(2).align(VPos.TOP).add(Labels.create("Title", "title"));
            gridPane.at(3).add(Labels.create(addLineFeeds(videoStream.getTitle(), 80), "value"));
            gridPane.nextRow();
          }

          gridPane.at(2).align(VPos.TOP).add(Labels.create("Format", "title"));
          gridPane.at(3).add(Labels.create(addLineFeeds(videoStream.getCodec(), 80), "value"));
          gridPane.nextRow();
        }

        List<AudioStream> audioStreams = metaData.getAudioStreams();

        for(int i = 0; i < audioStreams.size(); i++) {
          AudioStream audioStream = audioStreams.get(i);

          gridPane.at(0).align(VPos.TOP).add(i != 0 ? null : Labels.create("Audio Streams", "title"));
          gridPane.at(1).align(VPos.TOP).add(Labels.create("#" + (i + 1), "title"));

          if(audioStream.getTitle() != null) {
            gridPane.at(2).align(VPos.TOP).add(Labels.create("Title", "title"));
            gridPane.at(3).add(Labels.create(addLineFeeds(audioStream.getTitle(), 80), "value"));
            gridPane.nextRow();
          }

          gridPane.at(2).align(VPos.TOP).add(Labels.create("Format", "title"));
          gridPane.at(3).add(Labels.create(addLineFeeds(audioStream.getCodec(), 80), "value"));
          gridPane.nextRow();

          if(audioStream.getLanguage() != null) {
            gridPane.at(2).align(VPos.TOP).add(Labels.create("Language", "title"));
            gridPane.at(3).add(Labels.create(audioStream.getLanguage(), "value"));
            gridPane.nextRow();
          }
        }
      }

      listBox.getChildren().add(gridPane);
    }

    Dialogs.show(event, "", vbox);
  }

  private static String addLineFeeds(String text, int max) {
    String output = "";
    String tail = text;

    outer:
    while(tail.length() > max) {
      // Prefer break at space:
      for(int i = max; i > max - (max / 2); i--) {
        char c = tail.charAt(i);

        if(Character.isWhitespace(c)) {
          output += tail.substring(0, i + 1) + "\n";
          tail = tail.substring(i + 1);
          continue outer;
        }
      }

      // Prefer break at punctuation:
      for(int i = max; i > max - (max / 2); i--) {
        char c = tail.charAt(i);

        if(!Character.isLetterOrDigit(c)) {
          output += tail.substring(0, i + 1) + "\n";
          tail = tail.substring(i + 1);
          continue outer;
        }
      }

      // Couldn't find suitable break point, just break:
      output += tail.substring(0, max) + "\n";
      tail = tail.substring(max);
    }

    return output + tail;
  }

  private static String toText(Identification identification) {
    StringBuilder builder = new StringBuilder();

    builder.append(identification.getMatchType());

    if(identification.getMatchType() == MatchType.NAME || identification.getMatchType() == MatchType.NAME_AND_RELEASE_DATE) {
      builder.append(" (" + SizeFormatter.DOUBLE_THREE_SIGNIFICANT.format(identification.getMatchAccuracy() * 100) + "% match)");
    }

    builder.append(" at ");
    builder.append(DATE_TIME_FORMATTER.format(identification.getCreationTime().atOffset(ZoneOffset.UTC)));

    return builder.toString();
  }
}
