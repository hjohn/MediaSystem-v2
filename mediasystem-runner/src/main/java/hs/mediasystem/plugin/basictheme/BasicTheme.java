package hs.mediasystem.plugin.basictheme;

import hs.ddif.core.inject.instantiator.BeanResolutionException;
import hs.ddif.core.inject.instantiator.Instantiator;
import hs.ddif.core.util.AnnotationDescriptor;
import hs.ddif.core.util.Value;
import hs.mediasystem.plugin.home.HomePresentation;
import hs.mediasystem.plugin.home.HomeScreenNodeFactory;
import hs.mediasystem.plugin.library.scene.base.LibraryNodeFactory;
import hs.mediasystem.plugin.library.scene.base.LibraryPresentation;
import hs.mediasystem.plugin.library.scene.grid.FolderPresentationFactory.FolderPresentation;
import hs.mediasystem.plugin.library.scene.grid.FolderSetup;
import hs.mediasystem.plugin.library.scene.grid.GenericCollectionPresentationFactory.GenericCollectionPresentation;
import hs.mediasystem.plugin.library.scene.grid.GenericCollectionSetup;
import hs.mediasystem.plugin.library.scene.grid.RecommendationsPresentationFactory.RecommendationsPresentation;
import hs.mediasystem.plugin.library.scene.grid.RecommendationsSetup;
import hs.mediasystem.plugin.library.scene.grid.contribution.ContributionsPresentationFactory.ContributionsPresentation;
import hs.mediasystem.plugin.library.scene.grid.contribution.ContributionsSetup;
import hs.mediasystem.plugin.library.scene.grid.participation.ParticipationsPresentationFactory.ParticipationsPresentation;
import hs.mediasystem.plugin.library.scene.grid.participation.ParticipationsSetup;
import hs.mediasystem.plugin.library.scene.overview.ProductionOverviewNodeFactory;
import hs.mediasystem.plugin.library.scene.overview.ProductionPresentationFactory.ProductionPresentation;
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
import hs.mediasystem.runner.root.RootNodeFactory;
import hs.mediasystem.runner.root.RootPresentation;

import javafx.scene.Node;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BasicTheme implements Theme {
  @Inject private Instantiator instantiator;

  @Override
  public Class<? extends Presentation> findParent(Class<? extends Presentation> cls) {
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
    if(cls == PlaybackOverlayPresentation.class) {
      return RootPresentation.class;
    }

    return null;
  }

  @SuppressWarnings("unchecked")
  private <P extends Presentation, T extends NodeFactory<P>> Class<T> findNodeFactory(Class<P> cls) {
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

    throw new IllegalStateException(this + " missing node factory for " + cls);
  }

  @Override
  public <P extends Presentation> P createPresentation(Class<P> presentationClass) {
    try {
      return instantiator.getInstance(presentationClass);
    }
    catch(BeanResolutionException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public <P extends ParentPresentation, C extends Presentation> Placer<P, C> findPlacer(P parentPresentation, C childPresentation) {
    @SuppressWarnings("unchecked")
    Class<? extends NodeFactory<C>> childNodeFactoryClass = (Class<? extends NodeFactory<C>>)(Class<?>)findNodeFactory(childPresentation.getClass());

    if(parentPresentation != null) {
      Class<?> parentNodeFactoryClass = findNodeFactory(parentPresentation.getClass());

      try {
        AnnotationDescriptor descriptor = AnnotationDescriptor.describe(
          PlacerQualifier.class,
          new Value("parent", parentNodeFactoryClass),
          new Value("child", childNodeFactoryClass)
        );

        @SuppressWarnings("unchecked")
        Placer<P, C> instance = instantiator.getInstance(Placer.class, descriptor);

        return instance;
      }
      catch(BeanResolutionException e) {
        // Fall-through
      }
    }

    try {
      NodeFactory<C> nodeFactory = instantiator.getInstance(childNodeFactoryClass);

      return new Placer<>() {
        @Override
        public Node place(P parentPresentation, C presentation) {
          return nodeFactory.create(presentation);
        }
      };
    }
    catch(BeanResolutionException e) {
      throw new IllegalStateException(e);
    }
  }
}
