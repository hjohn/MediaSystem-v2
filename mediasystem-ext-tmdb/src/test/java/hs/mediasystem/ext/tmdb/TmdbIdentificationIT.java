package hs.mediasystem.ext.tmdb;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ClasspathFileSource;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import hs.mediasystem.api.datasource.domain.Release;
import hs.mediasystem.api.discovery.Attribute;
import hs.mediasystem.api.discovery.Discovery;
import hs.mediasystem.db.base.DatabaseContentPrintProvider;
import hs.mediasystem.db.base.DatabaseResponseCache;
import hs.mediasystem.db.core.DiscoveryController;
import hs.mediasystem.db.core.IdentificationStore;
import hs.mediasystem.db.core.ImportSource;
import hs.mediasystem.db.core.ResourceService;
import hs.mediasystem.db.core.domain.ContentPrint;
import hs.mediasystem.db.core.domain.Resource;
import hs.mediasystem.db.core.domain.StreamTags;
import hs.mediasystem.db.extract.StreamDescriptorService;
import hs.mediasystem.db.util.InjectorExtension;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Match.Type;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.util.Attributes;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.int4.dirk.annotations.Produces;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(InjectorExtension.class)  // TODO this probably should not live in db project
public class TmdbIdentificationIT {
  private static final WireMockServer WIRE_MOCK_SERVER = new WireMockServer(
    WireMockConfiguration.options()
      .dynamicPort()
      .fileSource(new ClasspathFileSource("wiremock"))
      .notifier(new ConsoleNotifier(true))
  );

  static {
    WIRE_MOCK_SERVER.start();

    WIRE_MOCK_SERVER.stubFor(WireMock.get(WireMock.urlPathEqualTo("/3/configuration"))
      .willReturn(WireMock.aResponse().withStatus(200).withBody("""
        {
          "images": {
            "base_url": "http://image.tmdb.org/t/p/",
            "secure_base_url": "https://image.tmdb.org/t/p/"
          }
        }
      """))
    );

    WIRE_MOCK_SERVER.stubFor(WireMock.get(WireMock.urlPathEqualTo("/3/search/tv"))
      .withQueryParam("query", WireMock.equalTo("Charmed"))
      .willReturn(WireMock.aResponse().withStatus(200).withBody("""
        {
          "results": [
            {
              "backdrop_path": "/ueZFcwAUvkjyAB9beaiqJyg0M8H.jpg",
              "first_air_date": "1998-10-07",
              "genre_ids": [
                35,
                18,
                9648,
                10765
              ],
              "id": 1981,
              "name": "Charmed",
              "origin_country": [
                "US"
              ],
              "original_language": "en",
              "original_name": "Charmed",
              "overview": "Three sisters ...",
              "popularity": 195.889,
              "poster_path": "/z4bPJ1BWU2EtV69NII2GVvsugQ2.jpg",
              "vote_average": 8.2,
              "vote_count": 1866
            }
          ]
        }
      """))
    );

    WIRE_MOCK_SERVER.stubFor(WireMock.get(WireMock.urlPathEqualTo("/3/tv/1981"))
      .withQueryParam("append_to_response", WireMock.equalTo("keywords,content_ratings"))
      .willReturn(WireMock.aResponse().withStatus(200).withBody("""
        {
          "adult": false,
          "backdrop_path": "/ueZFcwAUvkjyAB9beaiqJyg0M8H.jpg",
          "created_by": [
            {
              "id": 1213587,
              "credit_id": "52571e50760ee3776a1d3bb2",
              "name": "Constance M. Burge",
              "gender": 1,
              "profile_path": null
            }
          ],
          "episode_run_time": [
            42
          ],
          "first_air_date": "1998-10-07",
          "genres": [
            {
              "id": 35,
              "name": "Comedy"
            },
            {
              "id": 18,
              "name": "Drama"
            },
            {
              "id": 9648,
              "name": "Mystery"
            },
            {
              "id": 10765,
              "name": "Sci-Fi & Fantasy"
            }
          ],
          "homepage": "",
          "id": 1981,
          "in_production": false,
          "languages": [
            "en"
          ],
          "last_air_date": "2006-05-21",
          "last_episode_to_air": {
            "air_date": "2006-05-21",
            "episode_number": 22,
            "id": 136107,
            "name": "Forever Charmed",
            "overview": "Piper & Leo, with help from Coop's ring, must travel back in time to change the events that lead to the deaths of Piper's sisters. Meanwhile, Billie uses her power of Projection to do the same thing; many familiar faces gather to help Piper in her time of need.",
            "production_code": "",
            "runtime": 42,
            "season_number": 8,
            "show_id": 1981,
            "still_path": "/c6yFeneGs47m5MO6M14J1dO1UCp.jpg",
            "vote_average": 8.2,
            "vote_count": 4
          },
          "name": "Charmed",
          "next_episode_to_air": null,
          "networks": [
            {
              "id": 21,
              "name": "The WB",
              "logo_path": "/9GlDHjQj9c2dkfARCR3zlH87R66.png",
              "origin_country": "US"
            }
          ],
          "number_of_episodes": 178,
          "number_of_seasons": 8,
          "origin_country": [
            "US"
          ],
          "original_language": "en",
          "original_name": "Charmed",
          "overview": "Three sisters (Prue, Piper and Phoebe) reunite and unlock their powers to become the Charmed Ones, the most powerful good witches of all time, whose prophesied destiny is to protect innocent lives from evil beings such as demons and warlocks. Each sister possesses unique magical powers that grow and evolve, while they attempt to maintain normal lives in modern day San Francisco. ",
          "popularity": 138.561,
          "poster_path": "/z4bPJ1BWU2EtV69NII2GVvsugQ2.jpg",
          "production_companies": [
            {
              "id": 1081,
              "logo_path": "/19kn4jVvpc3sAL3YpZNb3elhSMl.png",
              "name": "CBS Studios",
              "origin_country": "US"
            },
            {
              "id": 9223,
              "logo_path": "/of4mmVt6egYaO9oERJbuUxMOTkj.png",
              "name": "Paramount Television Studios",
              "origin_country": "US"
            },
            {
              "id": 15620,
              "logo_path": null,
              "name": "Spelling Television",
              "origin_country": ""
            },
            {
              "id": 70909,
              "logo_path": "/9YyYvnzyDmpe03DvYAZfdnROmwr.png",
              "name": "Worldvision Enterprises",
              "origin_country": "US"
            }
          ],
          "production_countries": [
            {
              "iso_3166_1": "US",
              "name": "United States of America"
            }
          ],
          "seasons": [
            {
              "air_date": null,
              "episode_count": 14,
              "id": 5887,
              "name": "Specials",
              "overview": "",
              "poster_path": "/77ZrLU67OkP6Krhu7uIYpoh0XbX.jpg",
              "season_number": 0
            },
            {
              "air_date": "1998-10-07",
              "episode_count": 22,
              "id": 5879,
              "name": "Season 1",
              "overview": "The three Halliwell sisters discover that they are descendents of a line of female witches. Each has a special ability (stopping time, moving objects, seeing the future), and they can also combine their abilities into the \\"Power of Three\\" to fight demons, warlocks, and other evils.",
              "poster_path": "/xg0IMNo8ljyOgqr9732ZUr3bRkB.jpg",
              "season_number": 1
            },
            {
              "air_date": "1999-09-30",
              "episode_count": 22,
              "id": 5886,
              "name": "Season 2",
              "overview": "The sisters continue to balance their personal lives with their magic as they begin to gain more control over their powers. The sisters also find themselves traveling to both the past and the future throughout the season, in addition to facing their own family history when they are forced to destroy the demon that was responsible for their mother's death.",
              "poster_path": "/dQNTM7vbm4IOfln0YEXMbo47poH.jpg",
              "season_number": 2
            },
            {
              "air_date": "2000-10-05",
              "episode_count": 22,
              "id": 5881,
              "name": "Season 3",
              "overview": "The third season of Charmed sees many dramatic changes in the sisters' lives. As Cole/Belthazor moves his way into The Charmed Ones' lives, he becomes conflicted between his duty toward The Triad and his love for Phoebe.",
              "poster_path": "/kNNHvSM8QxR5XJ8rbyML25OsLAB.jpg",
              "season_number": 3
            },
            {
              "air_date": "2001-10-04",
              "episode_count": 22,
              "id": 5882,
              "name": "Season 4",
              "overview": "Season 4 brings big changes in the lives of the Halliwell sisters. As they face the loss of a sister, Paige Matthews arrives - half sister to the Halliwells. As Piper and Phoebe cope with the death of their sister, they learn to accept Paige and teach her the ways of witchcraft all while perfecting the use of their own growing powers.",
              "poster_path": "/6NA6OO4DbFsdViMppWPpTWlSpRA.jpg",
              "season_number": 4
            },
            {
              "air_date": "2002-09-22",
              "episode_count": 23,
              "id": 5880,
              "name": "Season 5",
              "overview": "Season 5 ties up loose ends and brings in new characters. Piper deals with being a Charmed One while being pregnant. After the birth, and learning that she is the mother of a very important and powerful child, Piper must then face various demonic forces who intend to either kill or capture her son.",
              "poster_path": "/deGGnjH9rcgMWrjHR2zljt3EYP.jpg",
              "season_number": 5
            },
            {
              "air_date": "2003-09-28",
              "episode_count": 23,
              "id": 5883,
              "name": "Season 6",
              "overview": "The sixth season focuses on new characters as well as The Charmed Ones. Leo leaves on a quest making life hard for Piper as a single mother to Wyatt. Chris travels back in time and we learn about his true connection to the Halliwells.",
              "poster_path": "/rZBj9FcUV4dl9wt0Zl076TXmnyw.jpg",
              "season_number": 6
            },
            {
              "air_date": "2004-09-12",
              "episode_count": 22,
              "id": 5884,
              "name": "Season 7",
              "overview": "The sisters question themselves after Gideon's betrayal. Phoebe is such an emotional wreck that her boss lets her take a break and hires a ghost writer -- a man with whom she takes awhile to warm up to but eventually develops feelings for. Finally, darkness surrounds the sisters in the form Zankou, a demon who had been locked away by The Source.",
              "poster_path": "/uVZX3dinMZPLdf4fkBHoJRnk540.jpg",
              "season_number": 7
            },
            {
              "air_date": "2005-09-25",
              "episode_count": 22,
              "id": 5885,
              "name": "Season 8",
              "overview": "The sisters have to live under false identities to be free of demons. Each sister remains unhappy and eventually decides to return to their Charmed lives. Paige, however, begins helping a young witch named Billie, who eventually discovers the sisters' magical secrets but agrees to keep them under wraps in exchange for witchcraft training.",
              "poster_path": "/vhtEqM3SaWbTsMzSGoLgs8TVnb1.jpg",
              "season_number": 8
            }
          ],
          "spoken_languages": [
            {
              "english_name": "English",
              "iso_639_1": "en",
              "name": "English"
            }
          ],
          "status": "Ended",
          "tagline": "",
          "type": "Scripted",
          "vote_average": 8.217,
          "vote_count": 1868,
          "keywords": {
            "results": [
              {
                "name": "witch",
                "id": 616
              },
              {
                "name": "sibling relationship",
                "id": 380
              },
              {
                "name": "magic",
                "id": 2343
              },
              {
                "name": "warrior woman",
                "id": 3389
              },
              {
                "name": "female protagonist",
                "id": 11322
              },
              {
                "name": "demon",
                "id": 15001
              },
              {
                "name": "succubus",
                "id": 161173
              }
            ]
          },
          "content_ratings": {
            "results": [
              {
                "iso_3166_1": "DE",
                "rating": "12"
              },
              {
                "iso_3166_1": "US",
                "rating": "TV-14"
              },
              {
                "iso_3166_1": "GB",
                "rating": "15"
              },
              {
                "iso_3166_1": "ES",
                "rating": "16"
              }
            ]
          }
        }
      """))
    );

    try {
      WIRE_MOCK_SERVER.stubFor(WireMock.get(WireMock.urlPathEqualTo("/3/tv/1981"))
        .withQueryParam("append_to_response", WireMock.equalTo("season/0,season/1,season/2,season/3,season/4,season/5,season/6,season/7,season/8"))
        .willReturn(WireMock.aResponse().withStatus(200).withBodyFile("tmdb-tv-1981-season[0-8].json"))
      );

      WIRE_MOCK_SERVER.stubFor(WireMock.get(WireMock.urlPathEqualTo("/3/search/movie"))
        .withQueryParam("query", WireMock.equalTo("Terminator"))
        .willReturn(WireMock.aResponse().withStatus(200).withBodyFile("tmdb-search-movie-terminator.json"))
      );

      WIRE_MOCK_SERVER.stubFor(WireMock.get(WireMock.urlPathEqualTo("/3/movie/218"))
        .willReturn(WireMock.aResponse().withStatus(200).withBodyFile("tmdb-movie-218.json"))
      );
    }
    catch(Exception e) {
      throw new IllegalStateException(e);
    }

    WIRE_MOCK_SERVER.stubFor(WireMock.get("/3/find/").willReturn(WireMock.ok().withBody("{}")));
  }

  private static final ImportSource TMDB_IMPORT_SOURCE = new ImportSource(
    (root, registry) -> {},
    URI.create("/"),
    Optional.of("TMDB"),
    new StreamTags(Set.of())
  );

  @Produces private static final DatabaseResponseCache RESPONSE_CACHE = mock(DatabaseResponseCache.class);
  @Produces @Named("ext.tmdb.host") private static final String TMDB_HOST = "http://localhost:" + WIRE_MOCK_SERVER.port() + "/";
  @Produces private static final IdentificationStore IDENTIFICATION_STORE = mock(IdentificationStore.class);
  @Produces private static final StreamDescriptorService STREAM_DESCRIPTOR_SERVICE = mock(StreamDescriptorService.class);
  @Produces private static final DatabaseContentPrintProvider CONTENT_PRINT_PROVIDER = mock(DatabaseContentPrintProvider.class);
  @Produces private static final Collection<ImportSource> IMPORT_SOURCES = List.of(TMDB_IMPORT_SOURCE);

  @Inject private ResourceService resourceService;
  @Inject private DiscoveryController discoveryController;

  @Inject TmdbIdentificationProvider tmdbIdentificationProvider;  // Loads it and injects it in DiscoveryController

  @Test
  void test() throws IOException {
    when(CONTENT_PRINT_PROVIDER.get(any(), any(), any())).thenReturn(mock(ContentPrint.class));

    Discovery serieDiscovery = new Discovery(MediaType.SERIE, URI.create("/Series/Charmed"), Attributes.of("title", "Charmed"), Instant.now(), null);

    discoveryController.registerDiscovery(
      TMDB_IMPORT_SOURCE,
      URI.create("/"),
      List.of(
        serieDiscovery
      )
    );

    await().untilAsserted(() -> {
      Resource resource = resourceService.find(URI.create("/Series/Charmed")).orElseThrow(() -> new AssertionError());

      assertThat(resource.match().type()).isEqualTo(Type.NAME);
      assertThat(resource.match().accuracy()).isEqualTo(1.0f);

      Release firstRelease = resource.releases().getFirst();

      assertThat(firstRelease.getId()).isEqualTo(new WorkId(DataSource.instance("TMDB"), MediaType.SERIE, "1981"));
      assertThat(firstRelease.getDetails().getTitle()).isEqualTo("Charmed");
      assertThat(firstRelease.getDetails().getDate()).contains(LocalDate.of(1998, 10, 7));
    });

    discoveryController.registerDiscovery(
      TMDB_IMPORT_SOURCE,
      URI.create("/Series/Charmed"),
      List.of(
        new Discovery(MediaType.EPISODE, URI.create("/Series/Charmed/Ep1x01.mkv"), Attributes.of("title", "Charmed", "sequence", "1,01", "childType", "EPISODE"), Instant.now(), 12345L),
        new Discovery(MediaType.EPISODE, URI.create("/Series/Charmed/Ep1x02-03.mkv"), Attributes.of("title", "Charmed", "sequence", "1,02-03", "childType", "EPISODE"), Instant.now(), 12346L)
      )
    );

    await().untilAsserted(() -> {
      Resource resource = resourceService.find(URI.create("/Series/Charmed/Ep1x01.mkv")).orElseThrow(() -> new AssertionError());

      assertThat(resource.match().type()).isEqualTo(Type.DERIVED);
      assertThat(resource.match().accuracy()).isEqualTo(1.0f);

      Release firstRelease = resource.releases().getFirst();

      assertThat(firstRelease.getId()).isEqualTo(new WorkId(DataSource.instance("TMDB"), MediaType.EPISODE, "1981/1/1"));
      assertThat(firstRelease.getDetails().getTitle()).isEqualTo("Something Wicca This Way Comes");
      assertThat(firstRelease.getDetails().getDate()).contains(LocalDate.of(1998, 10, 7));
    });

    await().untilAsserted(() -> {
      Resource resource = resourceService.find(URI.create("/Series/Charmed/Ep1x02-03.mkv")).orElseThrow(() -> new AssertionError());

      assertThat(resource.match().type()).isEqualTo(Type.DERIVED);
      assertThat(resource.match().accuracy()).isEqualTo(1.0f);

      Release firstRelease = resource.releases().getFirst();

      assertThat(firstRelease.getId()).isEqualTo(new WorkId(DataSource.instance("TMDB"), MediaType.EPISODE, "1981/1/2"));
      assertThat(firstRelease.getDetails().getTitle()).isEqualTo("I've Got You Under My Skin");
      assertThat(firstRelease.getDetails().getDate()).contains(LocalDate.of(1998, 10, 14));

      Release secondRelease = resource.releases().get(1);

      assertThat(secondRelease.getId()).isEqualTo(new WorkId(DataSource.instance("TMDB"), MediaType.EPISODE, "1981/1/3"));
      assertThat(secondRelease.getDetails().getTitle()).isEqualTo("Thank You for Not Morphing");
      assertThat(secondRelease.getDetails().getDate()).contains(LocalDate.of(1998, 10, 21));
    });

    discoveryController.registerDiscovery(
      TMDB_IMPORT_SOURCE,
      URI.create("/"),
      List.of(
        new Discovery(MediaType.MOVIE, URI.create("/Movies/Terminator.avi"), Attributes.of(Attribute.TITLE, "Terminator", Attribute.YEAR, "1984"), Instant.now(), 30000L)
      )
    );

    await().untilAsserted(() -> {
      Resource resource = resourceService.find(URI.create("/Movies/Terminator.avi")).orElseThrow(() -> new AssertionError());

      assertThat(resource.match().type()).isEqualTo(Type.NAME_AND_RELEASE_DATE);
      assertThat(resource.match().accuracy()).isEqualTo(0.8052575f);

      Release firstRelease = resource.releases().getFirst();

      assertThat(firstRelease.getId()).isEqualTo(new WorkId(DataSource.instance("TMDB"), MediaType.MOVIE, "218"));
      assertThat(firstRelease.getDetails().getTitle()).isEqualTo("The Terminator");
      assertThat(firstRelease.getDetails().getDate()).contains(LocalDate.of(1984, 10, 26));
    });
  }
}
