package hs.mediasystem.plugin.library.scene.overview;

import hs.mediasystem.domain.media.AudioTrack;
import hs.mediasystem.domain.media.Snapshot;
import hs.mediasystem.domain.media.SubtitleTrack;
import hs.mediasystem.domain.media.VideoTrack;
import hs.mediasystem.domain.work.Match;
import hs.mediasystem.domain.work.Match.Type;
import hs.mediasystem.runner.presentation.Presentations;
import hs.mediasystem.runner.util.LessLoader;
import hs.mediasystem.ui.api.domain.MediaStream;
import hs.mediasystem.ui.api.domain.Context;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.image.ImageHandle;
import hs.mediasystem.util.image.ImageHandleFactory;
import hs.mediasystem.util.javafx.AsyncImageProperty;
import hs.mediasystem.util.javafx.control.BiasedImageView;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.GridPane;
import hs.mediasystem.util.javafx.control.Labels;
import hs.mediasystem.util.natural.SizeFormatter;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javafx.event.Event;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ShowInfoEventHandler {
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.UK).withZone(ZoneOffset.systemDefault());
  private static final String STYLES_URL = LessLoader.compile(ShowInfoEventHandler.class, "show-info-styles.less");

  @Inject private ImageHandleFactory imageHandleFactory;

  public void handle(Event event, Work work) {
    Label titleLabel = Labels.create("title", work.getDetails().getTitle());

    if(titleLabel.getText().length() > 40) {
      titleLabel.getStyleClass().add("smaller");
    }

    VBox titleBox = Containers.vbox("title-panel",
      Labels.create("serie-title", work.getType().isComponent() ? work.getContext().map(Context::title).orElse("") : "", Labels.HIDE_IF_EMPTY),
      titleLabel
    );

    TabPane tabPane = new TabPane();
    VBox vbox = Containers.vbox("main-panel");

    vbox.getStylesheets().add(STYLES_URL);
    vbox.getChildren().addAll(titleBox, tabPane);

    List<MediaStream> streams = work.getStreams();

    for(int streamIndex = 0; streamIndex < streams.size(); streamIndex++) {
      MediaStream stream = streams.get(streamIndex);
      GridPane gridPane = Containers.grid("item");
      Tab tab = new Tab("Stream " + (streamIndex + 1), gridPane);

      tab.setClosable(false);

      tabPane.getTabs().add(tab);

      String path = URLDecoder.decode(stream.location().toString().trim(), StandardCharsets.UTF_8);

      if(path.startsWith("file://")) {
        path = path.substring(5);
      }

      // path often contains too few spaces for decent wrapping... add LF's every 80 chars:
      path = addLineFeeds(path, 100);

      gridPane.addRow(
        Labels.create("title", "Path"),
        Labels.create("value", path),
        GridPane.FILL,
        GridPane.FILL
      );

      stream.size().ifPresent(size -> {
        gridPane.addRow(
          Labels.create("title", "File Size"),
          Labels.create("value", SizeFormatter.BYTES_THREE_SIGNIFICANT.format(size)),
          GridPane.FILL,
          GridPane.FILL
        );
      });

      gridPane.addRow(
        Labels.create("title", "Last Modified"),
        Labels.create("value", "" + DATE_TIME_FORMATTER.format(stream.lastModificationTime().atOffset(ZoneOffset.UTC))),
        GridPane.FILL,
        GridPane.FILL
      );

      gridPane.addRow(
        Labels.create("title", "First Seen"),
        Labels.create("value", "" + DATE_TIME_FORMATTER.format(stream.discoveryTime().atOffset(ZoneOffset.UTC))),
        GridPane.FILL,
        GridPane.FILL
      );

      gridPane.addRow(
        Labels.create("title", "Identification"),
        Labels.create("value", "" + toText(stream.match())),
        GridPane.FILL,
        GridPane.FILL
      );

      stream.duration().ifPresent(d -> {
        gridPane.addRow(
          Labels.create("title", "Duration"),
          Labels.create("value", SizeFormatter.DURATION.format(d.toSeconds())),
          GridPane.FILL,
          GridPane.FILL
        );
      });

      stream.mediaStructure().ifPresent(ms -> {
        List<VideoTrack> videoTracks = ms.videoTracks();

        for(int i = 0; i < videoTracks.size(); i++) {
          VideoTrack videoTrack = videoTracks.get(i);

          gridPane.at(0).align(VPos.TOP).add(i != 0 ? null : Labels.create("title", "Video Streams"));
          gridPane.at(1).align(VPos.TOP).add(Labels.create("title", "#" + (i + 1)));

          if(videoTrack.title() != null) {
            gridPane.at(2).align(VPos.TOP).add(Labels.create("title", "Title"));
            gridPane.at(3).add(Labels.create("value", addLineFeeds(videoTrack.title(), 80)));
            gridPane.nextRow();
          }

          gridPane.at(2).align(VPos.TOP).add(Labels.create("title", "Format"));
          gridPane.at(3).add(Labels.create("value", addLineFeeds(videoTrack.codec(), 80)));
          gridPane.nextRow();
        }

        List<AudioTrack> audioTracks = ms.audioTracks();

        for(int i = 0; i < audioTracks.size(); i++) {
          AudioTrack audioTrack = audioTracks.get(i);

          gridPane.at(0).align(VPos.TOP).add(i != 0 ? null : Labels.create("title", "Audio Streams"));
          gridPane.at(1).align(VPos.TOP).add(Labels.create("title", "#" + (i + 1)));

          if(audioTrack.title() != null) {
            gridPane.at(2).align(VPos.TOP).add(Labels.create("title", "Title"));
            gridPane.at(3).add(Labels.create("value", addLineFeeds(audioTrack.title(), 80)));
            gridPane.nextRow();
          }

          gridPane.at(2).align(VPos.TOP).add(Labels.create("title", "Format"));
          gridPane.at(3).add(Labels.create("value", addLineFeeds(audioTrack.codec(), 80)));
          gridPane.nextRow();

          if(audioTrack.language() != null) {
            gridPane.at(2).align(VPos.TOP).add(Labels.create("title", "Language"));
            gridPane.at(3).add(Labels.create("value", audioTrack.language()));
            gridPane.nextRow();
          }
        }

        List<SubtitleTrack> subtitleTracks = ms.subtitleTracks();

        gridPane.at(0).align(VPos.TOP).add(Labels.create("title", "Subtitle Streams"));
        gridPane.at(1).spanning(3, 1).add(Labels.create("value", addLineFeeds(
          subtitleTracks.stream()
            .map(s -> s.title() == null ? s.language() : s.language() + " (" + s.title() + ")")
            .collect(Collectors.joining(", ")),
          100
        )));
        gridPane.nextRow();
      });

      List<ImageHandle> handles = stream.snapshots().stream()
        .map(Snapshot::imageUri)
        .map(imageHandleFactory::fromURI)
        .collect(Collectors.toList());

      if(!handles.isEmpty()) {
        GridPane snapshotsBox = Containers.grid("snapshots-box");

        for(int i = 0; i < handles.size(); i++) {
          ImageHandle handle = handles.get(i);
          BiasedImageView imageView = new BiasedImageView();
          AsyncImageProperty property = new AsyncImageProperty(600, 400);

          imageView.setOrientation(Orientation.HORIZONTAL);
          imageView.imageProperty().bind(property);
          property.imageHandleProperty().set(handle);

          snapshotsBox.at(i % 4, i / 4).add(imageView);
        }

        gridPane.addRow(Labels.create("title", "Snapshots"), snapshotsBox, GridPane.FILL, GridPane.FILL);
      }
    }

    Presentations.showWindow(event, vbox);

    event.consume();
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

    builder.append(match.type());

    if(match.type() == Type.NAME || match.type() == Type.NAME_AND_RELEASE_DATE) {
      builder.append(" (" + SizeFormatter.DOUBLE_THREE_SIGNIFICANT.format(match.accuracy() * 100.0) + "% match)");
    }

    builder.append(" at ");
    builder.append(DATE_TIME_FORMATTER.format(match.creationTime().atOffset(ZoneOffset.UTC)));

    return builder.toString();
  }
}
