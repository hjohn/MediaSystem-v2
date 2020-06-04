package hs.mediasystem.plugin.library.scene.overview;

import hs.mediasystem.domain.work.AudioStream;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.Match.Type;
import hs.mediasystem.domain.work.MediaStream;
import hs.mediasystem.domain.work.Parent;
import hs.mediasystem.domain.work.Snapshot;
import hs.mediasystem.domain.work.SubtitleStream;
import hs.mediasystem.domain.work.VideoStream;
import hs.mediasystem.runner.util.Dialogs;
import hs.mediasystem.runner.util.LessLoader;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.ImageHandleFactory;
import hs.mediasystem.util.SizeFormatter;
import hs.mediasystem.util.javafx.AsyncImageProperty;
import hs.mediasystem.util.javafx.control.BiasedImageView;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.GridPane;
import hs.mediasystem.util.javafx.control.Labels;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

import javafx.event.Event;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ShowInfoEventHandler {
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.UK).withZone(ZoneOffset.systemDefault());
  private static final LessLoader LESS_LOADER = new LessLoader(ShowInfoEventHandler.class);

  @Inject private ImageHandleFactory imageHandleFactory;

  public void handle(Event event, Work work) {
    Label titleLabel = Labels.create("title", work.getDetails().getTitle());

    if(titleLabel.getText().length() > 40) {
      titleLabel.getStyleClass().add("smaller");
    }

    VBox titleBox = Containers.vbox("title-panel",
      Labels.create("serie-title", work.getParent().map(Parent::getName).orElse(""), Labels.HIDE_IF_EMPTY),
      titleLabel
    );
    VBox listBox = Containers.vbox("list-panel");
    VBox vbox = Containers.vbox("main-panel");

    vbox.getStylesheets().add(LESS_LOADER.compile("show-info-styles.less"));
    vbox.getChildren().addAll(titleBox, listBox);

    for(MediaStream stream : work.getStreams()) {
      GridPane gridPane = Containers.grid("item");
      String path = stream.getAttributes().getUri().toString().trim();

      // path often contains too few spaces for decent wrapping... add LF's every 80 chars:
      path = addLineFeeds(path, 100);

      gridPane.addRow(
        Labels.create("title", "Path"),
        Labels.create("value", path),
        GridPane.FILL,
        GridPane.FILL
      );

      stream.getAttributes().getSize().ifPresent(size -> {
        gridPane.addRow(
          Labels.create("title", "File Size"),
          Labels.create("value", SizeFormatter.BYTES_THREE_SIGNIFICANT.format(size)),
          GridPane.FILL,
          GridPane.FILL
        );
      });

      gridPane.addRow(
        Labels.create("title", "Last Modified"),
        Labels.create("value", "" + DATE_TIME_FORMATTER.format(stream.getAttributes().getLastModificationTime().atOffset(ZoneOffset.UTC))),
        GridPane.FILL,
        GridPane.FILL
      );

      gridPane.addRow(
        Labels.create("title", "First Seen"),
        Labels.create("value", "" + DATE_TIME_FORMATTER.format(stream.getAttributes().getCreationTime().atOffset(ZoneOffset.UTC))),
        GridPane.FILL,
        GridPane.FILL
      );

      stream.getMatch().ifPresentOrElse(match -> {
        gridPane.addRow(
          Labels.create("title", "Identification"),
          Labels.create("value", "" + toText(match)),
          GridPane.FILL,
          GridPane.FILL
        );
      },
      () -> {
        gridPane.addRow(
          Labels.create("title", "Identification"),
          Labels.create("value", "- (Unable to identify)"),
          GridPane.FILL,
          GridPane.FILL
        );
      });

      stream.getMetaData().ifPresent(metaData -> {
        if(metaData.getLength() != Duration.ZERO) {
          gridPane.addRow(
            Labels.create("title", "Duration"),
            Labels.create("value", SizeFormatter.DURATION.format(metaData.getLength().toSeconds())),
            GridPane.FILL,
            GridPane.FILL
          );
        }

        List<VideoStream> videoStreams = metaData.getVideoStreams();

        for(int i = 0; i < videoStreams.size(); i++) {
          VideoStream videoStream = videoStreams.get(i);

          gridPane.at(0).align(VPos.TOP).add(i != 0 ? null : Labels.create("title", "Video Streams"));
          gridPane.at(1).align(VPos.TOP).add(Labels.create("title", "#" + (i + 1)));

          if(videoStream.getTitle() != null) {
            gridPane.at(2).align(VPos.TOP).add(Labels.create("title", "Title"));
            gridPane.at(3).add(Labels.create("value", addLineFeeds(videoStream.getTitle(), 80)));
            gridPane.nextRow();
          }

          gridPane.at(2).align(VPos.TOP).add(Labels.create("title", "Format"));
          gridPane.at(3).add(Labels.create("value", addLineFeeds(videoStream.getCodec(), 80)));
          gridPane.nextRow();
        }

        List<AudioStream> audioStreams = metaData.getAudioStreams();

        for(int i = 0; i < audioStreams.size(); i++) {
          AudioStream audioStream = audioStreams.get(i);

          gridPane.at(0).align(VPos.TOP).add(i != 0 ? null : Labels.create("title", "Audio Streams"));
          gridPane.at(1).align(VPos.TOP).add(Labels.create("title", "#" + (i + 1)));

          if(audioStream.getTitle() != null) {
            gridPane.at(2).align(VPos.TOP).add(Labels.create("title", "Title"));
            gridPane.at(3).add(Labels.create("value", addLineFeeds(audioStream.getTitle(), 80)));
            gridPane.nextRow();
          }

          gridPane.at(2).align(VPos.TOP).add(Labels.create("title", "Format"));
          gridPane.at(3).add(Labels.create("value", addLineFeeds(audioStream.getCodec(), 80)));
          gridPane.nextRow();

          if(audioStream.getLanguage() != null) {
            gridPane.at(2).align(VPos.TOP).add(Labels.create("title", "Language"));
            gridPane.at(3).add(Labels.create("value", audioStream.getLanguage()));
            gridPane.nextRow();
          }
        }

        List<SubtitleStream> subtitleStreams = metaData.getSubtitleStreams();

        for(int i = 0; i < subtitleStreams.size(); i++) {
          SubtitleStream subtitleStream = subtitleStreams.get(i);

          gridPane.at(0).align(VPos.TOP).add(i != 0 ? null : Labels.create("title", "Subtitle Streams"));
          gridPane.at(1).align(VPos.TOP).add(Labels.create("title", "#" + (i + 1)));

          if(subtitleStream.getTitle() != null) {
            gridPane.at(2).align(VPos.TOP).add(Labels.create("title", "Title"));
            gridPane.at(3).add(Labels.create("value", addLineFeeds(subtitleStream.getTitle(), 80)));
            gridPane.nextRow();
          }

          gridPane.at(2).align(VPos.TOP).add(Labels.create("title", "Format"));
          gridPane.at(3).add(Labels.create("value", addLineFeeds(subtitleStream.getCodec(), 80)));
          gridPane.nextRow();

          if(subtitleStream.getLanguage() != null) {
            gridPane.at(2).align(VPos.TOP).add(Labels.create("title", "Language"));
            gridPane.at(3).add(Labels.create("value", subtitleStream.getLanguage()));
            gridPane.nextRow();
          }
        }
      });

      HBox snapshotsBox = Containers.hbox("snapshots-box");

      stream.getMetaData().ifPresent(metaData -> {
        metaData.getSnapshots().stream().map(Snapshot::getImageUri).map(imageHandleFactory::fromURI).forEach(handle -> {
          BiasedImageView imageView = new BiasedImageView();
          AsyncImageProperty property = new AsyncImageProperty(600, 400);

          imageView.setOrientation(Orientation.HORIZONTAL);
          imageView.imageProperty().bind(property);
//          imageView.setPrefWidth(20);
//          imageView.setPrefHeight(20);
          property.imageHandleProperty().set(handle);

          snapshotsBox.getChildren().add(imageView);
        });
      });

      gridPane.addRow(Labels.create("title", "Snapshots"), snapshotsBox, GridPane.FILL, GridPane.FILL);

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

  private static String toText(Match match) {
    StringBuilder builder = new StringBuilder();

    builder.append(match.getType());

    if(match.getType() == Type.NAME || match.getType() == Type.NAME_AND_RELEASE_DATE) {
      builder.append(" (" + SizeFormatter.DOUBLE_THREE_SIGNIFICANT.format(match.getAccuracy() * 100.0) + "% match)");
    }

    builder.append(" at ");
    builder.append(DATE_TIME_FORMATTER.format(match.getCreationTime().atOffset(ZoneOffset.UTC)));

    return builder.toString();
  }
}
