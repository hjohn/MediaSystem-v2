package hs.mediasystem.plugin.library.scene.grid;

import hs.mediasystem.ui.api.domain.Work;

import java.util.List;

import javax.inject.Singleton;

@Singleton
public class FolderPresentationFactory extends GridViewPresentationFactory {

  public FolderPresentation create(List<Work> items, String settingPostfix, ViewOptions<Work, Work> viewOptions, Object contextItem) {
    return new FolderPresentation(settingPostfix, items, viewOptions, contextItem);
  }

  public class FolderPresentation extends GridViewPresentation<Work, Work> {
    public final String settingPostfix;
    public final ViewOptions<Work, Work> viewOptions;

    public FolderPresentation(String settingPostfix, List<Work> items, ViewOptions<Work, Work> viewOptions, Object contextItem) {
      super("Folder:" + settingPostfix, viewOptions, Work::getId);

      this.settingPostfix = settingPostfix;
      this.viewOptions = viewOptions;
      this.inputItems.set(items);  // TODO add refresh code
      this.rootContextItem.set(contextItem);
    }
  }
}
