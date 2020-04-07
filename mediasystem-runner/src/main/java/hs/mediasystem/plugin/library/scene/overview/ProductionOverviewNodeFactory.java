package hs.mediasystem.plugin.library.scene.overview;

import hs.mediasystem.domain.work.MediaStream;
import hs.mediasystem.domain.work.Reception;
import hs.mediasystem.plugin.library.scene.AspectCorrectLabel;
import hs.mediasystem.plugin.library.scene.BinderProvider;
import hs.mediasystem.plugin.library.scene.MediaGridView;
import hs.mediasystem.plugin.library.scene.MediaGridViewCellFactory;
import hs.mediasystem.plugin.library.scene.MediaItemFormatter;
import hs.mediasystem.plugin.library.scene.MediaStatus;
import hs.mediasystem.plugin.library.scene.WorkBinder;
import hs.mediasystem.plugin.library.scene.overview.ProductionPresentation.State;
import hs.mediasystem.plugin.library.scene.overview.SeasonBar.Entry;
import hs.mediasystem.presentation.NodeFactory;
import hs.mediasystem.runner.util.LessLoader;
import hs.mediasystem.ui.api.domain.Details;
import hs.mediasystem.ui.api.domain.Sequence;
import hs.mediasystem.ui.api.domain.Sequence.Type;
import hs.mediasystem.ui.api.domain.Work;
import hs.mediasystem.util.ImageHandleFactory;
import hs.mediasystem.util.SizeFormatter;
import hs.mediasystem.util.javafx.AsyncImageProperty;
import hs.mediasystem.util.javafx.Nodes;
import hs.mediasystem.util.javafx.control.AutoVerticalScrollPane;
import hs.mediasystem.util.javafx.control.BiasedImageView;
import hs.mediasystem.util.javafx.control.Containers;
import hs.mediasystem.util.javafx.control.Labels;
import hs.mediasystem.util.javafx.control.StarRating;
import hs.mediasystem.util.javafx.control.gridlistviewskin.GridListViewSkin.GroupDisplayMode;
import hs.mediasystem.util.javafx.control.gridlistviewskin.Group;
import hs.mediasystem.util.javafx.control.status.StatusIndicator;
import hs.mediasystem.util.javafx.control.transition.StandardTransitions;
import hs.mediasystem.util.javafx.control.transition.TransitionPane;
import hs.mediasystem.util.javafx.control.transition.multi.Scroll;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.reactfx.EventStreams;
import org.reactfx.util.Interpolator;
import org.reactfx.value.Val;

@Singleton
public class ProductionOverviewNodeFactory implements NodeFactory<ProductionPresentation> {
  @Inject private ImageHandleFactory imageHandleFactory;
  @Inject private ShowInfoEventHandler showInfoEventHandler;
  @Inject private CastPaneFactory castPaneFactory;
  @Inject private NavigationButtonsFactory navigationButtonsFactory;
  @Inject private BinderProvider binderProvider;

  @Override
  public Node create(ProductionPresentation presentation) {
    presentation.toMainButtonState();  // Resets presentation to normal buttons when Node is created again

    MainPanel mainPanel = new MainPanel(presentation);

    presentation.showInfo.conditionOnShowing(mainPanel).subscribe(e -> showInfoEventHandler.handle(e, presentation.state.get() == State.OVERVIEW ? presentation.rootItem : presentation.episodeItem.getValue()));

    return mainPanel;
  }

  private class MainPanel extends HBox {
    private final ProductionPresentation presentation;

    public MainPanel(ProductionPresentation presentation) {
      this.presentation = presentation;

      presentation.update();

      Work work = presentation.rootItem;
      AsyncImageProperty imageProperty = new AsyncImageProperty(840, 840);

      imageProperty.imageHandleProperty().set(work.getDetails().getImage().map(imageHandleFactory::fromURI).orElse(null));

      BiasedImageView poster = new BiasedImageView();

      poster.setOrientation(Orientation.VERTICAL);
      poster.imageProperty().bind(imageProperty);

      Label titleLabel = Labels.create("title", work.getDetails().getName());

      if(titleLabel.getText().length() > 40) {
        titleLabel.getStyleClass().add("smaller");
      }

      VBox leftTitleBox = Containers.vbox(
        titleLabel,
        Labels.create("release-year", WorkBinder.createYearRange(work)),
        Labels.create("genres", work.getDetails().getClassification().getGenres().stream().collect(Collectors.joining(" / ")))
      );

      TransitionPane dynamicBoxContainer = new TransitionPane(StandardTransitions.fade(), createDynamicBox());

      HBox.setHgrow(leftTitleBox, Priority.ALWAYS);
      VBox.setVgrow(dynamicBoxContainer, Priority.ALWAYS);

      StackPane indicatorPane = createMediaStatusIndicatorPane(
        presentation.watchedPercentage.animate(Duration.ofSeconds(2), Interpolator.LINEAR_DOUBLE),
        presentation.missingFraction
      );

      VBox descriptionBox = new VBox(
        Containers.hbox(
          "title-panel",
          leftTitleBox,
          Containers.vbox(
            createStarRating(work.getDetails().getReception().orElse(null), 20, 8),
            indicatorPane
          )
        ),
        dynamicBoxContainer
      );

      EventStreams.invalidationsOf(presentation.state)
        .conditionOnShowing(this)
        .subscribe(obs -> dynamicBoxContainer.add(createDynamicBox()));

      getChildren().addAll(poster, descriptionBox);

      HBox.setHgrow(descriptionBox, Priority.ALWAYS);

      getStyleClass().add("main-panel");
      getStylesheets().add(LessLoader.compile(getClass().getResource("styles.less")).toExternalForm());
    }

    private Pane createDynamicBox() {
      switch(presentation.state.get()) {
      case LIST:
        return buildEpisodeListUI();
      case EPISODE:
        return buildEpisodeDynamicUI();
      default:
        return buildOverviewUI();
      }
    }

    private Pane buildOverviewUI() {
      Work work = presentation.rootItem;
      Details details = work.getDetails();

      VBox leftBox = Containers.vbox();

      details.getTagline().ifPresent(tl -> leftBox.getChildren().add(Labels.create("tag-line", "“" + tl + "”")));

      AutoVerticalScrollPane pane = new AutoVerticalScrollPane(Labels.create("description", details.getDescription().orElse("")), 12000, 40);

      /*
       * Limit height of ScrollPane, but at same time give it the rest of the space in the leftBox VBox; this allows
       * the tag-line to wrap if need be, instead of tag-line and description competing for max space
       */

      pane.setPrefHeight(100);
      VBox.setVgrow(pane, Priority.ALWAYS);

      leftBox.getChildren().add(pane);

      Region castPane = castPaneFactory.create(work.getId());

      leftBox.setPrefWidth(100);  // Limit pref width, so free space can be assigned according to Grow / Percentage settings
      castPane.setPrefWidth(100);  // Limit pref width, so free space can be assigned according to Grow / Percentage settings

      HBox.setHgrow(leftBox, Priority.ALWAYS);
      HBox.setHgrow(castPane, Priority.ALWAYS);

      HBox outer = Containers.hbox("overview-panel", leftBox, castPane);

      BorderPane borderPane = new BorderPane();

      borderPane.setMinHeight(1);
      borderPane.setPrefHeight(1);
      borderPane.setCenter(outer);
      borderPane.setBottom(work.getPrimaryStream()
        .filter(ms -> ms.getAttributes().getSize().isPresent())
        .map(ms -> Containers.stack(navigationButtonsFactory.create(presentation), buildStreamInfoPanel(ms)))
        .orElseGet(() -> Containers.stack(navigationButtonsFactory.create(presentation)))
      );
      borderPane.getStyleClass().add("overview-dynamic-panel");

      VBox.setVgrow(borderPane, Priority.ALWAYS);

      return Containers.vbox("dynamic-panel", borderPane);
    }

    private Pane buildStreamInfoPanel(MediaStream stream) {
      return Containers.vbox(
        "stream-info-panel",
        stream.getMetaData().map(md -> Containers.hbox(
          "duration-box",
          Labels.create("duration-icon", "🕑"),
          Labels.create("duration-text", SizeFormatter.SECONDS_AS_POSITION.format(md.getLength().toSeconds()))
        )).orElse(null),
        stream.getState().getLastConsumptionTime().map(time -> Containers.hbox(
          "last-watched-box",
          Labels.create("last-watched-icon", "📷"),
          Labels.create("last-watched-text", SizeFormatter.formatTimeAgo(LocalDateTime.ofInstant(time, ZoneId.systemDefault())))
        )).orElse(null)
      );
    }

    private Pane buildEpisodeListUI() {
      MediaGridView<Work> gridView = new MediaGridView<>();
      MediaGridViewCellFactory<Work> cellFactory = new MediaGridViewCellFactory<>(binderProvider);

      gridView.visibleRows.set(1);
      gridView.visibleColumns.set(3);
      gridView.setOrientation(Orientation.HORIZONTAL);
      gridView.onItemSelected.set(e -> presentation.toEpisodeState());
      gridView.pageByGroup.set(true);
      gridView.showHeaders.set(false);
      gridView.scrollBarVisible.set(false);
      gridView.groupDisplayMode.set(GroupDisplayMode.FOCUSED);
      gridView.setCellFactory(cellFactory);

      Nodes.visible(gridView).values().observe(visible -> {
        gridView.itemsProperty().set(visible ? FXCollections.observableList(presentation.episodeItems) : FXCollections.emptyObservableList());

        if(visible) {
          gridView.getSelectionModel().select(presentation.episodeItem.getValue());
        }
      });

      cellFactory.setPlaceHolderAspectRatio(16.0 / 9.0);
      cellFactory.setMinRatio(4.0 / 3.0);

      VBox.setVgrow(gridView, Priority.ALWAYS);

      Set<Integer> knownSeasons = new HashSet<>();
      List<Group> groups = new ArrayList<>();
      List<Entry> entries = new ArrayList<>();
      Map<Integer, Integer> seasonNumberToIndex = new HashMap<>();

      for(int i = 0; i < presentation.episodeItems.size(); i++) {
        Work episode = presentation.episodeItems.get(i);
        int seasonNumber = toSeasonBarIndex(episode);

        if(!knownSeasons.contains(seasonNumber)) {
          knownSeasons.add(seasonNumber);

          groups.add(new Group(seasonNumber == 0 ? "Specials" : seasonNumber == -1 ? "Extras" : "Season " + seasonNumber, i));

          Entry entry;

          switch(seasonNumber) {
          case 0:
            seasonNumberToIndex.put(0, entries.size());
            entry = new Entry("Specials", null, false);
            break;
          case -1:
            seasonNumberToIndex.put(-1, entries.size());
            entry = new Entry("Extras", null, false);
            break;
          default:
            seasonNumberToIndex.put(seasonNumber, entries.size());
            entry = new Entry("" + seasonNumber, null, true);
          }

          Val<SeasonWatchState> val = presentation.getSeasonWatchStates().get(seasonNumber).conditionOnShowing(gridView);

          // Unsafe binding, Presentation strongly refers to UI (solved with conditionOnShowing)
          entry.mediaStatus.bind(val.map(sws -> sws.totalEpisodes == sws.missingEpisodes ? MediaStatus.UNAVAILABLE : sws.totalEpisodes == sws.watchedEpisodes ? MediaStatus.WATCHED : MediaStatus.AVAILABLE));
          entries.add(entry);
        }
      }

      SeasonBar seasonsBar = new SeasonBar();

      seasonsBar.getStyleClass().add("season-bar");
      seasonsBar.setMinWidth(1);
      seasonsBar.setPrefWidth(1);
      seasonsBar.entries.setValue(entries);

      gridView.groups.set(groups);
      gridView.getSelectionModel().selectedItemProperty().addListener((obs, old, current) -> {
        if(current != null) {
          seasonsBar.activeIndex.setValue(seasonNumberToIndex.get(toSeasonBarIndex(current)));
          presentation.episodeItem.setValue(current);
        }
      });

      return Containers.vbox("dynamic-panel", Containers.vbox("episode-list-dynamic-panel", seasonsBar, gridView));
    }

    private Pane buildEpisodeDynamicUI() {
      presentation.episodeItem.unbind();

      List<Work> episodes = presentation.episodeItems;
      TransitionPane transitionPane = new TransitionPane(new Scroll(), buildEpisodeUI());
      TransitionPane streamInfoPane = new TransitionPane(StandardTransitions.fade(250, 500));
      BorderPane borderPane = new BorderPane();

      EventStreams.changesOf(presentation.episodeItem)
        .conditionOnShowing(borderPane)
        .subscribe(c -> {
          transitionPane.add(episodes.indexOf(c.getOldValue()) > episodes.indexOf(c.getNewValue()), buildEpisodeUI());

          updateStreamInfoPane(streamInfoPane);
        });

      updateStreamInfoPane(streamInfoPane);

      borderPane.setCenter(transitionPane);
      borderPane.setBottom(Containers.stack(navigationButtonsFactory.create(presentation), streamInfoPane));
      borderPane.getProperties().put("presentation2", new EpisodePresentation(presentation));
      borderPane.getStyleClass().add("episode-dynamic-panel");

      VBox.setVgrow(borderPane, Priority.ALWAYS);

      return Containers.vbox("dynamic-panel", borderPane);
    }

    private void updateStreamInfoPane(TransitionPane streamInfoPane) {
      presentation.episodeItem.getValue().getPrimaryStream().filter(ms -> ms.getAttributes().getSize().isPresent()).ifPresent(ms -> {
        streamInfoPane.add(buildStreamInfoPanel(ms));
      });
    }

    private HBox buildEpisodeUI() {
      Work work = presentation.episodeItem.getValue();
      AsyncImageProperty imageProperty = new AsyncImageProperty(840, 840);

      imageProperty.imageHandleProperty().set(work.getDetails().getImage().map(imageHandleFactory::fromURI).orElse(null));

      Label label = new AspectCorrectLabel("?", 0.75, Orientation.VERTICAL, 1000, 1000);
      BiasedImageView poster = new BiasedImageView(label);

      poster.setOrientation(Orientation.VERTICAL);
      poster.imageProperty().bind(imageProperty);

      Val<Double> percentage = Val.combine(work.getState().isConsumed(), Val.constant(work.getStreams().isEmpty()), presentation.resume.resumePosition.orElseConst(Duration.ZERO), presentation.totalDuration.orElseConst(1), (w, m, rp, td) -> {
        return w ? 1.0 :
               m ? -0.01 :
                   rp.toSeconds() / (double)td;
      }).conditionOnShowing(poster);

      StackPane indicatorPane = createMediaStatusIndicatorPane(percentage.animate(Duration.ofSeconds(2), Interpolator.EASE_BOTH_DOUBLE), Val.constant(0.0));

      poster.getOverlayRegion().getChildren().add(indicatorPane);

      String formattedDate = MediaItemFormatter.formattedLocalDate(work.getDetails().getReleaseDate().orElse(null));
      String subtitle = createSeasonEpisodeText(work) + (formattedDate == null ? "" : " • " + formattedDate);

      Label titleLabel = Labels.create("title", presentation.episodeItem.getValue().getDetails().getName());

      titleLabel.setMinHeight(Region.USE_PREF_SIZE);  // With reflowed labels, sometimes not enough vertical space is assigned and the reflow fails to use the next line and adds an ellipsis instead...

      VBox titleBox = Containers.vbox(titleLabel, Labels.create("subtitle", subtitle));
      HBox.setHgrow(titleBox, Priority.ALWAYS);

      VBox vbox = Containers.vbox(
        Containers.hbox(
          titleBox,
          createStarRating(work.getDetails().getReception().orElse(null), 10, 4)
        ),
        new AutoVerticalScrollPane(Labels.create("description", work.getDetails().getDescription().orElse("")), 12000, 40)
      );

      HBox hbox = Containers.hbox("episode-panel", vbox, poster);

      /*
       * The following four tweaks to the layout are needed to have the description box and poster play nice.
       *
       * The poster should take up as much space as possible, aspect ratio allowing, without "pushing" its parent
       * bigger (as that would resize the root of this control even).  The outer container that is returned therefore
       * gets its min and pref sizes forced to a lower arbirtrary values; min size to prevent the entire root to
       * become bigger; preferred size to prevent it stealing space from parent containers (like the primary left poster).
       *
       * Secondly, the description box gets its preferred width lowered, as Labels with long texts use a preferred
       * width that would fit their entire text instead.  Containers with such a Label are not smart enough to use
       * Label's content bias to find a more reasonable configuration by themselves.
       *
       * Finally, the description box is the only box allowed to take up left-over space.  Everything else will
       * get the exact space required, and no more.
       */

      HBox.setHgrow(vbox, Priority.ALWAYS);  // Description box can grow, poster however should just try and attain its preferred size (which can be huge).
      vbox.setPrefWidth(100);  // Limit pref width on the description box, so other content can be resized to their preferred sizes.
      hbox.setMinSize(100, 100);
      hbox.setPrefSize(100, 100);

      return hbox;
    }

    private StatusIndicator createMediaStatusIndicatorPane(Val<Double> percentage, Val<Double> missingFraction) {
      StatusIndicator indicator = new StatusIndicator();

      indicator.setAlignment(Pos.BOTTOM_RIGHT);
      indicator.getStyleClass().add("indicator-pane");
      indicator.value.bind(percentage.conditionOnShowing(indicator));
      indicator.missingFraction.bind(missingFraction.conditionOnShowing(indicator));

      return indicator;
    }
  }

  private static int toSeasonBarIndex(Work episode) {
    Sequence sequence = episode.getDetails().getSequence().orElseThrow();

    return sequence.getType() == Type.SPECIAL ? 0 : sequence.getType() == Type.EXTRA ? -1 : sequence.getSeasonNumber().orElse(-1);
  }

  private static String createSeasonEpisodeText(Work work) {
    int seasonNumber = toSeasonBarIndex(work);

    return seasonNumber == 0 ? "Special"
        : seasonNumber == -1 ? "Extra"
                             : "Season " + seasonNumber + ", Episode " + work.getDetails().getSequence().map(Sequence::getNumber).orElse(0);
  }

  private static StarRating createStarRating(Reception reception, double radius, double innerRadius) {
    StarRating starRating = new StarRating(radius, innerRadius, 5);

    if(reception != null) {
      starRating.setRating(reception.getRating() / 10);
    }
    else {
      starRating.setVisible(false);
      starRating.setManaged(false);
    }

    return starRating;
  }
}
