package hs.mediasystem.plugin.basictheme;

import hs.ddif.core.Injector;
import hs.ddif.core.NoSuchBeanException;
import hs.ddif.core.util.AnnotationDescriptor;
import hs.ddif.core.util.Value;
import hs.mediasystem.plugin.library.scene.LibraryNodeFactory;
import hs.mediasystem.plugin.library.scene.LibraryPresentation;
import hs.mediasystem.plugin.library.scene.view.CastAndCrewPresentation;
import hs.mediasystem.plugin.library.scene.view.CastAndCrewSetup;
import hs.mediasystem.plugin.library.scene.view.MovieCollectionSetup;
import hs.mediasystem.plugin.library.scene.view.PersonParticipationsPresentation;
import hs.mediasystem.plugin.library.scene.view.PersonParticipationsSetup;
import hs.mediasystem.plugin.library.scene.view.ProductionDetailPresentation;
import hs.mediasystem.plugin.library.scene.view.ProductionDetailSetup;
import hs.mediasystem.plugin.library.scene.view.SerieCollectionPresentation;
import hs.mediasystem.plugin.library.scene.view.SerieCollectionSetup;
import hs.mediasystem.plugin.library.scene.view.SerieEpisodesPresentation;
import hs.mediasystem.plugin.library.scene.view.SerieEpisodesSetup;
import hs.mediasystem.plugin.library.scene.view.SerieSeasonsPresentation;
import hs.mediasystem.plugin.library.scene.view.SerieSeasonsSetup;
import hs.mediasystem.plugin.movies.videolibbaroption.MovieCollectionPresentation;
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
import hs.mediasystem.runner.RootNodeFactory;
import hs.mediasystem.runner.RootPresentation;

import javafx.scene.Node;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BasicTheme implements Theme {
  @Inject private Injector injector;

  @Override
  public Class<? extends Presentation> findParent(Class<? extends Presentation> cls) {
    if(cls == MovieCollectionPresentation.class) {
      return LibraryPresentation.class;
    }
    if(cls == SerieCollectionPresentation.class) {
      return LibraryPresentation.class;
    }
    if(cls == ProductionDetailPresentation.class) {
      return LibraryPresentation.class;
    }
    if(cls == PersonParticipationsPresentation.class) {
      return LibraryPresentation.class;
    }
    if(cls == CastAndCrewPresentation.class) {
      return LibraryPresentation.class;
    }
    if(cls == SerieEpisodesPresentation.class) {
      return LibraryPresentation.class;
    }
    if(cls == SerieSeasonsPresentation.class) {
      return LibraryPresentation.class;
    }
    if(cls == LibraryPresentation.class) {
      return RootPresentation.class;
    }
    if(cls == MenuPresentation.class) {
      return RootPresentation.class;
    }
    if(cls == PlaybackOverlayPresentation.class) {
      return RootPresentation.class;
    }

    return null;
  }

  @SuppressWarnings("unchecked")
  private <P extends Presentation, T extends NodeFactory<P>> Class<T> findNodeFactory(Class<P> cls) {
    if(cls == MovieCollectionPresentation.class) {
      return (Class<T>)MovieCollectionSetup.class;
    }
    if(cls == SerieCollectionPresentation.class) {
      return (Class<T>)SerieCollectionSetup.class;
    }
    if(cls == LibraryPresentation.class) {
      return (Class<T>)LibraryNodeFactory.class;
    }
    if(cls == MenuPresentation.class) {
      return (Class<T>)RootMenuScenePlugin.class;
    }
    if(cls == ProductionDetailPresentation.class) {
      return (Class<T>)ProductionDetailSetup.class;
    }
    if(cls == PersonParticipationsPresentation.class) {
      return (Class<T>)PersonParticipationsSetup.class;
    }
    if(cls == CastAndCrewPresentation.class) {
      return (Class<T>)CastAndCrewSetup.class;
    }
    if(cls == SerieEpisodesPresentation.class) {
      return (Class<T>)SerieEpisodesSetup.class;
    }
    if(cls == SerieSeasonsPresentation.class) {
      return (Class<T>)SerieSeasonsSetup.class;
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
  public <P extends ParentPresentation, C extends Presentation> Placer<P, C> findPlacer(P parentPresentation, C childPresentation) {
    @SuppressWarnings("unchecked")
    Class<? extends NodeFactory<C>> childNodeFactoryClass = (Class<? extends NodeFactory<C>>)findNodeFactory(childPresentation.getClass());
    Class<?> parentNodeFactoryClass = findNodeFactory(parentPresentation.getClass());

    try {
      AnnotationDescriptor descriptor = AnnotationDescriptor.describe(
        PlacerQualifier.class,
        new Value("parent", parentNodeFactoryClass),
        new Value("child", childNodeFactoryClass)
      );

      @SuppressWarnings("unchecked")
      Placer<P, C> instance = injector.getInstance(Placer.class, descriptor);

      return instance;
    }
    catch(NoSuchBeanException e) {
      NodeFactory<C> nodeFactory = injector.getInstance(childNodeFactoryClass);

      return new Placer<>() {
        @Override
        public Node place(P parentPresentation, C presentation) {
          return nodeFactory.create(presentation);
        }
      };
    }
  }
}
