package hs.mediasystem.plugin.library.scene.view;

import java.time.LocalDate;

import groovy.lang.Script;

public abstract class MediaSystemScript extends Script {

  public LocalDate date(String text) {
    return LocalDate.parse(text);
  }
/*
  public static void main(String[] args) {
    List<Predicate<MediaItem<MovieDescriptor>>> filters = new MovieCollectionSetup().getFilters();

    System.out.println(filters.get(0).test(new MediaItem(
      new MovieDescriptor(
        new Production(new ProductionIdentifier(DataSource.instance(Type.of("MOVIE"), "TMDB"), "12345"), "Something", "Bla", LocalDate.of(2080, 1, 1), null, null, null),
        "tagline",
        null,
        Collections.emptyList(),
        Collections.emptyList()
      ),
      Collections.emptySet()
    )));
  }

  protected List<Predicate<MediaItem<MovieDescriptor>>> getFilters() {
    CompilerConfiguration configuration = new CompilerConfiguration();

    configuration.setScriptBaseClass(MediaSystemScript.class.getName());

    GroovyShell shell = new GroovyShell(configuration);


    Script script = shell.parse("return production.date >= date(\"2012-01-01\")");



    return List.of(
      mi -> { script.setProperty("production", mi.getProduction()); return (boolean)script.run(); }
    );
  }
  */
}