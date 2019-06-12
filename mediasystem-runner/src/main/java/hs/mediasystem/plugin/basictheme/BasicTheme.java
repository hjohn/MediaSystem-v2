package hs.mediasystem.plugin.basictheme;

import hs.ddif.core.Injector;
import hs.ddif.core.NoSuchBeanException;
import hs.ddif.core.util.AnnotationDescriptor;
import hs.ddif.core.util.Value;
import hs.mediasystem.plugin.library.scene.base.LibraryNodeFactory;
import hs.mediasystem.plugin.library.scene.base.LibraryPresentation;
import hs.mediasystem.plugin.library.scene.serie.ProductionOverviewNodeFactory;
import hs.mediasystem.plugin.library.scene.serie.ProductionPresentation;
import hs.mediasystem.plugin.library.scene.view.CastAndCrewPresentation;
import hs.mediasystem.plugin.library.scene.view.CastAndCrewSetup;
import hs.mediasystem.plugin.library.scene.view.GenericCollectionPresentation;
import hs.mediasystem.plugin.library.scene.view.GenericCollectionSetup;
import hs.mediasystem.plugin.library.scene.view.PersonParticipationsPresentation;
import hs.mediasystem.plugin.library.scene.view.PersonParticipationsSetup;
import hs.mediasystem.plugin.library.scene.view.ProductionCollectionPresentation;
import hs.mediasystem.plugin.library.scene.view.ProductionCollectionSetup;
import hs.mediasystem.plugin.library.scene.view.RecommendationsPresentation;
import hs.mediasystem.plugin.library.scene.view.RecommendationsSetup;
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
  @Inject private Injector injector;

  @Override
  public Class<? extends Presentation> findParent(Class<? extends Presentation> cls) {
    if(cls == GenericCollectionPresentation.class) {
      return LibraryPresentation.class;
    }
    if(cls == PersonParticipationsPresentation.class) {
      return LibraryPresentation.class;
    }
    if(cls == CastAndCrewPresentation.class) {
      return LibraryPresentation.class;
    }
    if(cls == ProductionPresentation.class) {
      return LibraryPresentation.class;
    }
    if(cls == RecommendationsPresentation.class) {
      return LibraryPresentation.class;
    }
    if(cls == ProductionCollectionPresentation.class) {
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
    if(cls == LibraryPresentation.class) {
      return (Class<T>)LibraryNodeFactory.class;
    }
    if(cls == MenuPresentation.class) {
      return (Class<T>)RootMenuScenePlugin.class;
    }
    if(cls == PersonParticipationsPresentation.class) {
      return (Class<T>)PersonParticipationsSetup.class;
    }
    if(cls == CastAndCrewPresentation.class) {
      return (Class<T>)CastAndCrewSetup.class;
    }
    if(cls == ProductionPresentation.class) {
      return (Class<T>)ProductionOverviewNodeFactory.class;
    }
    if(cls == RecommendationsPresentation.class) {
      return (Class<T>)RecommendationsSetup.class;
    }
    if(cls == ProductionCollectionPresentation.class) {
      return (Class<T>)ProductionCollectionSetup.class;
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
    return injector.getInstance(presentationClass);
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
        Placer<P, C> instance = injector.getInstance(Placer.class, descriptor);

        return instance;
      }
      catch(NoSuchBeanException e) {
        // Fall-through
      }
    }

    NodeFactory<C> nodeFactory = injector.getInstance(childNodeFactoryClass);

    return new Placer<>() {
      @Override
      public Node place(P parentPresentation, C presentation) {
        return nodeFactory.create(presentation);
      }
    };
  }
}
