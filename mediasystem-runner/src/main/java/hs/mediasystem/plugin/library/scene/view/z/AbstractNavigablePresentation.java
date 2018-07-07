package hs.mediasystem.plugin.library.scene.view.z;

public abstract class AbstractNavigablePresentation {

  public abstract void back();


  /* hierarchy

   library/presentation
   library/nodefactory

   library.placer.serie/placer
   -> depends on library
   -> depends on serie

   serie/presentation
   serie/nodefactory

   configuration: SeriePresentation belongs under LibraryPresentation

   coolLayout
   -> depends on library
   -> depends on serie

Relevant placers:

  @Inject private final List<Placer<Parent, ?>>

   */
}
