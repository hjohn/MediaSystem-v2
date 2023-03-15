package hs.mediasystem.plugin.basictheme;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.PersonId;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.plugin.home.HomePresentation;
import hs.mediasystem.plugin.home.HomeScreenNodeFactory;
import hs.mediasystem.plugin.library.scene.base.LibraryNodeFactory;
import hs.mediasystem.plugin.library.scene.base.LibraryPresentation;
import hs.mediasystem.plugin.library.scene.grid.FolderPresentationFactory.FolderPresentation;
import hs.mediasystem.plugin.library.scene.grid.FolderSetup;
import hs.mediasystem.plugin.library.scene.grid.ProductionCollectionFactory;
import hs.mediasystem.plugin.library.scene.grid.contribution.ContributionsPresentationFactory;
import hs.mediasystem.plugin.library.scene.grid.contribution.ContributionsPresentationFactory.ContributionsPresentation;
import hs.mediasystem.plugin.library.scene.grid.generic.GenericCollectionSetup;
import hs.mediasystem.plugin.library.scene.grid.generic.GenericCollectionPresentationFactory.GenericCollectionPresentation;
import hs.mediasystem.plugin.library.scene.grid.contribution.ContributionsSetup;
import hs.mediasystem.plugin.library.scene.grid.participation.ParticipationsPresentationFactory;
import hs.mediasystem.plugin.library.scene.grid.participation.ParticipationsPresentationFactory.ParticipationsPresentation;
import hs.mediasystem.plugin.library.scene.grid.recommendation.RecommendationsPresentationFactory;
import hs.mediasystem.plugin.library.scene.grid.recommendation.RecommendationsSetup;
import hs.mediasystem.plugin.library.scene.grid.recommendation.RecommendationsPresentationFactory.RecommendationsPresentation;
import hs.mediasystem.plugin.library.scene.grid.participation.ParticipationsSetup;
import hs.mediasystem.plugin.library.scene.overview.ProductionOverviewNodeFactory;
import hs.mediasystem.plugin.library.scene.overview.ProductionPresentationFactory;
import hs.mediasystem.plugin.library.scene.overview.ProductionPresentationFactory.ProductionPresentation;
import hs.mediasystem.plugin.library.scene.overview.ProductionPresentationFactory.State;
import hs.mediasystem.plugin.playback.scene.PlaybackLayout;
import hs.mediasystem.plugin.playback.scene.PlaybackOverlayPresentation;
import hs.mediasystem.plugin.rootmenu.MenuPresentation;
import hs.mediasystem.plugin.rootmenu.RootMenuScenePlugin;
import hs.mediasystem.presentation.NodeFactory;
import hs.mediasystem.presentation.ParentPresentation;
import hs.mediasystem.presentation.Placer;
import hs.mediasystem.presentation.PlacerQualifier;
import hs.mediasystem.presentation.Presentation;
import hs.mediasystem.presentation.Theme;
import hs.mediasystem.runner.presentation.PresentationLoader;
import hs.mediasystem.runner.root.RootNodeFactory;
import hs.mediasystem.runner.root.RootPresentation;
import hs.mediasystem.runner.util.resource.ResourceManager;
import hs.mediasystem.ui.api.domain.Work;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javafx.event.Event;
import javafx.scene.Node;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.int4.dirk.api.InstanceResolver;
import org.int4.dirk.api.instantiation.InjectionException;
import org.int4.dirk.util.Annotations;

@Singleton
public class BasicTheme implements Theme {
  @Inject private InstanceResolver instanceResolver;
  @Inject private ProductionCollectionFactory productionCollectionFactory;
  @Inject private RecommendationsPresentationFactory recommendationsPresentationFactory;
  @Inject private ContributionsPresentationFactory contributionsPresentationFactory;
  @Inject private ProductionPresentationFactory productionPresentationFactory;
  @Inject private ParticipationsPresentationFactory personParticipationsPresentationFactory;

  @Override
  public boolean nestPresentation(ParentPresentation ancestor, Presentation descendant) {
    List<Class<? extends Presentation>> hierarchy = new ArrayList<>();

    for(Class<? extends Presentation> cls = descendant.getClass(); (cls = findParent(cls)) != ancestor.getClass();) {
      if(cls == null) {
        return false;  // given ancestor presentation can not be an ancestor for the given descendant
      }

      hierarchy.add(0, cls);
    }

    ParentPresentation presentation = ancestor;

    for(Class<? extends Presentation> parentClass : hierarchy) {
      presentation.childPresentation.set(instanceResolver.getInstance(parentClass));

      presentation = (ParentPresentation)presentation.childPresentation.get();
    }

    presentation.childPresentation.set(descendant);  // presentation is now the direct parent for descendant

    return true;
  }

  @Override
  public <P extends ParentPresentation, C extends Presentation> Node place(P parentPresentation, C childPresentation) {
    @SuppressWarnings("unchecked")
    Class<? extends NodeFactory<C>> childNodeFactoryClass = (Class<? extends NodeFactory<C>>)(Class<?>)findNodeFactory(childPresentation.getClass());

    if(parentPresentation != null) {
      Class<?> parentNodeFactoryClass = findNodeFactory(parentPresentation.getClass());

      try {
        Annotation descriptor = Annotations.of(
          PlacerQualifier.class,
          Map.of(
            "parent", parentNodeFactoryClass,
            "child", childNodeFactoryClass
          )
        );

        @SuppressWarnings("unchecked")
        Placer<P, C> instance = instanceResolver.getInstance(Placer.class, descriptor);

        return instance.place(parentPresentation, childPresentation);
      }
      catch(InjectionException e) {
        // Fall-through
      }
    }

    NodeFactory<C> nodeFactory = instanceResolver.getInstance(childNodeFactoryClass);

    return nodeFactory.create(childPresentation);
  }

  @Override
  public Optional<NavigationTarget> targetFor(Presentation presentation) {
    if(presentation instanceof GenericCollectionPresentation<?, ?> p) {
      return Optional.of(production(((Work)p.selectedItem.get()).getId()));
    }

    if(presentation instanceof ContributionsPresentation p) {
      return Optional.of(participation(p.selectedItem.get().person().getId()));
    }

    if(presentation instanceof ParticipationsPresentation p) {
      return Optional.of(production(p.selectedItem.get().getWork().getId()));
    }

    if(presentation instanceof RecommendationsPresentation p) {
      return Optional.of(production(p.selectedItem.get().getId()));
    }

    return Optional.empty();
  }

  @Override
  public List<NavigationTarget> targetsFor(Presentation presentation) {
    if(presentation instanceof ProductionPresentation p) {
      if(p.state.get() == State.OVERVIEW) {
        return create(p.root.get());
      }

      return create(p.selectedChild.get());
    }

    return List.of();
  }

  private List<NavigationTarget> create(Work work) {
    WorkId id = work.getId();

    if(id.getDataSource().getName().equals("TMDB")) {
      if(id.getType().isComponent() && id.getType().isPlayable()) {
        return List.of(contributions(id));
      }

      List<NavigationTarget> targets = new ArrayList<>();

      targets.add(contributions(id));
      targets.add(recommendations(id));

      work.getParent().filter(p -> p.type().equals(MediaType.COLLECTION))
        .ifPresent(p -> targets.add(collection(p.id())));

      return targets;
    }

    return List.of();
  }

  private NavigationTarget contributions(WorkId id) {
    return new SimpleNavigationTarget<>(contributionsPresentationFactory, factory -> factory.create(id));
  }

  private NavigationTarget recommendations(WorkId id) {
    return new SimpleNavigationTarget<>(recommendationsPresentationFactory, factory -> factory.create(id));
  }

  private NavigationTarget collection(WorkId id) {
    return new SimpleNavigationTarget<>(productionCollectionFactory, factory -> factory.create(id));
  }

  private NavigationTarget production(WorkId id) {
    return new SimpleNavigationTarget<>(productionPresentationFactory, factory -> factory.create(id));
  }

  private NavigationTarget participation(PersonId id) {
    return new SimpleNavigationTarget<>(personParticipationsPresentationFactory, factory -> factory.create(id));
  }

  private class SimpleNavigationTarget<F> implements NavigationTarget {
    private final F factory;
    private final Function<F, Presentation> call;

    SimpleNavigationTarget(F factory, Function<F, Presentation> call) {
      this.factory = factory;
      this.call = call;
    }

    @Override
    public String getLabel() {
      return ResourceManager.getText(factory.getClass(), "title");
    }

    @Override
    public void go(Event event) {
      PresentationLoader.navigate(event, () -> call.apply(factory));
    }
  }

  private static Class<? extends Presentation> findParent(Class<? extends Presentation> cls) {
    if(cls == GenericCollectionPresentation.class) {
      return LibraryPresentation.class;
    }
    if(cls == FolderPresentation.class) {
      return LibraryPresentation.class;
    }
    if(cls == ParticipationsPresentation.class) {
      return LibraryPresentation.class;
    }
    if(cls == ContributionsPresentation.class) {
      return LibraryPresentation.class;
    }
    if(cls == ProductionPresentation.class) {
      return LibraryPresentation.class;
    }
    if(cls == RecommendationsPresentation.class) {
      return LibraryPresentation.class;
    }
    if(cls == PlaybackOverlayPresentation.class) {
      return RootPresentation.class;
    }
    if(cls == LibraryPresentation.class) {
      return RootPresentation.class;
    }
    if(cls == MenuPresentation.class) {
      return RootPresentation.class;
    }
    if(cls == HomePresentation.class) {
      return RootPresentation.class;
    }

    return null;
  }

  @SuppressWarnings("unchecked")
  private static <P extends Presentation, T extends NodeFactory<P>> Class<T> findNodeFactory(Class<P> cls) {
    if(cls == GenericCollectionPresentation.class) {
      return (Class<T>)GenericCollectionSetup.class;
    }
    if(cls == FolderPresentation.class) {
      return (Class<T>)FolderSetup.class;
    }
    if(cls == LibraryPresentation.class) {
      return (Class<T>)LibraryNodeFactory.class;
    }
    if(cls == MenuPresentation.class) {
      return (Class<T>)RootMenuScenePlugin.class;
    }
    if(cls == HomePresentation.class) {
      return (Class<T>)HomeScreenNodeFactory.class;
    }
    if(cls == ParticipationsPresentation.class) {
      return (Class<T>)ParticipationsSetup.class;
    }
    if(cls == ContributionsPresentation.class) {
      return (Class<T>)ContributionsSetup.class;
    }
    if(cls == ProductionPresentation.class) {
      return (Class<T>)ProductionOverviewNodeFactory.class;
    }
    if(cls == RecommendationsPresentation.class) {
      return (Class<T>)RecommendationsSetup.class;
    }
    if(cls == PlaybackOverlayPresentation.class) {
      return (Class<T>)PlaybackLayout.class;
    }
    if(cls == RootPresentation.class) {
      return (Class<T>)RootNodeFactory.class;
    }

    throw new IllegalStateException(BasicTheme.class + " missing node factory for " + cls);
  }
}
