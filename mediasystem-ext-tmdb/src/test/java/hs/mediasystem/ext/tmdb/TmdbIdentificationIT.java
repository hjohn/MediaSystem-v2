package hs.mediasystem.ext.tmdb;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ClasspathFileSource;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import hs.mediasystem.api.discovery.Attribute;
import hs.mediasystem.api.discovery.ContentPrint;
import hs.mediasystem.api.discovery.Discovery;
import hs.mediasystem.db.DatabaseResponseCache;
import hs.mediasystem.db.InjectorExtension;
import hs.mediasystem.db.core.DiscoverEvent;
import hs.mediasystem.db.core.IdentificationEvent;
import hs.mediasystem.db.core.IdentifierService;
import hs.mediasystem.db.core.StreamTags;
import hs.mediasystem.db.core.StreamableEvent;
import hs.mediasystem.db.core.StreamableService;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.work.DataSource;
import hs.mediasystem.domain.work.Match.Type;
import hs.mediasystem.domain.work.WorkId;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.events.InMemoryEventStore;
import hs.mediasystem.util.events.SynchronousEventStream;
import hs.mediasystem.util.events.store.EventStore;
import hs.mediasystem.util.events.streams.EventStream;

import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.int4.dirk.annotations.Produces;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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

  private static ContentPrint contentPrint(int id) {
    return new ContentPrint(new ContentID(id), null, 1000L, new byte[] {1, 2, 3}, Instant.ofEpochSecond(1000));
  }

  @Produces private static final DatabaseResponseCache RESPONSE_CACHE = mock(DatabaseResponseCache.class);
  @Produces @Named("ext.tmdb.host") private static final String TMDB_HOST = "http://localhost:" + WIRE_MOCK_SERVER.port() + "/";
  @Produces private static final EventStore<StreamableEvent> EVENT_STORE = new InMemoryEventStore<>(StreamableEvent.class);
  @Produces private static final EventStore<IdentificationEvent> IDENTIFICATION_EVENT_STORE = new InMemoryEventStore<>(IdentificationEvent.class);

  @Produces private static final EventStream<DiscoverEvent> DISCOVER_EVENTS = new SynchronousEventStream<>();

  private static final URI ROOT = Path.of("/").toUri();

  @Inject private IdentifierService identifierService;

  @Inject StreamableService streamableService;
  @Inject TmdbIdentificationService tmdbIdentificationService;

  @Test
  void test() throws InterruptedException {
    BlockingQueue<IdentificationEvent> events = new LinkedBlockingQueue<>();

    identifierService.identificationEvents().plain().subscribe(events::add);

    DISCOVER_EVENTS.push(new DiscoverEvent(ROOT.resolve("/Series/"), Optional.of("TMDB"), new StreamTags(Set.of("cartoon")), List.of(
      new Discovery(MediaType.SERIE, ROOT.resolve("/Series/Charmed"), Attributes.of("title", "Charmed"), Optional.empty(), contentPrint(1))
    )));

    {
      IdentificationEvent event = events.poll(10, TimeUnit.SECONDS);

      assertThat(event.match().type()).isEqualTo(Type.NAME);
      assertThat(event.match().accuracy()).isEqualTo(1.0f);

      assertThat(event.releases().get(0).getId()).isEqualTo(new WorkId(DataSource.instance("TMDB"), MediaType.SERIE, "1981"));
      assertThat(event.releases().get(0).getDetails().getTitle()).isEqualTo("Charmed");
      assertThat(event.releases().get(0).getDetails().getDate()).contains(LocalDate.of(1998, 10, 7));
    }

    DISCOVER_EVENTS.push(new DiscoverEvent(ROOT.resolve("/Series/Charmed/"), Optional.of("TMDB"), new StreamTags(Set.of("cartoon")), List.of(
      new Discovery(MediaType.EPISODE, ROOT.resolve("/Series/Charmed/Ep1x01.mkv"), Attributes.of("title", "Charmed", "sequence", "1,01", "childType", "EPISODE"), Optional.of(ROOT.resolve("/Series/Charmed")), contentPrint(2)),
      new Discovery(MediaType.EPISODE, ROOT.resolve("/Series/Charmed/Ep1x02-03.mkv"), Attributes.of("title", "Charmed", "sequence", "1,02-03", "childType", "EPISODE"), Optional.of(ROOT.resolve("/Series/Charmed")), contentPrint(2))
    )));

    {
      IdentificationEvent event = events.poll(10, TimeUnit.SECONDS);

      assertThat(event.match().type()).isEqualTo(Type.DERIVED);
      assertThat(event.match().accuracy()).isEqualTo(1.0f);

      assertThat(event.releases().get(0).getId()).isEqualTo(new WorkId(DataSource.instance("TMDB"), MediaType.EPISODE, "1981/1/1"));
      assertThat(event.releases().get(0).getDetails().getTitle()).isEqualTo("Something Wicca This Way Comes");
      assertThat(event.releases().get(0).getDetails().getDate()).contains(LocalDate.of(1998, 10, 7));
    }

    {
      IdentificationEvent event = events.poll(10, TimeUnit.SECONDS);

      assertThat(event.match().type()).isEqualTo(Type.DERIVED);
      assertThat(event.match().accuracy()).isEqualTo(1.0f);

      assertThat(event.releases().get(0).getId()).isEqualTo(new WorkId(DataSource.instance("TMDB"), MediaType.EPISODE, "1981/1/2"));
      assertThat(event.releases().get(0).getDetails().getTitle()).isEqualTo("I've Got You Under My Skin");
      assertThat(event.releases().get(0).getDetails().getDate()).contains(LocalDate.of(1998, 10, 14));

      assertThat(event.releases().get(1).getId()).isEqualTo(new WorkId(DataSource.instance("TMDB"), MediaType.EPISODE, "1981/1/3"));
      assertThat(event.releases().get(1).getDetails().getTitle()).isEqualTo("Thank You for Not Morphing");
      assertThat(event.releases().get(1).getDetails().getDate()).contains(LocalDate.of(1998, 10, 21));
    }

    DISCOVER_EVENTS.push(new DiscoverEvent(ROOT.resolve("/Movies/"), Optional.of("TMDB"), new StreamTags(Set.of("movies")), List.of(
      new Discovery(MediaType.MOVIE, ROOT.resolve("/Movies/Terminator.avi"), Attributes.of(Attribute.TITLE, "Terminator", Attribute.YEAR, "1984"), Optional.empty(), contentPrint(3))
    )));

    {
      IdentificationEvent event = events.poll(10, TimeUnit.SECONDS);

      assertThat(event.match().type()).isEqualTo(Type.NAME_AND_RELEASE_DATE);
      assertThat(event.match().accuracy()).isEqualTo(0.79420984f);

      assertThat(event.releases().get(0).getId()).isEqualTo(new WorkId(DataSource.instance("TMDB"), MediaType.MOVIE, "218"));
      assertThat(event.releases().get(0).getDetails().getTitle()).isEqualTo("The Terminator");
      assertThat(event.releases().get(0).getDetails().getDate()).contains(LocalDate.of(1984, 10, 26));
    }
  }
}
