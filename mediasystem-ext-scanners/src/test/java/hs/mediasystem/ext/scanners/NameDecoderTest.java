package hs.mediasystem.ext.scanners;

import hs.mediasystem.ext.scanners.NameDecoder.DecodeResult;
import hs.mediasystem.ext.scanners.NameDecoder.Mode;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NameDecoderTest {

  @ParameterizedTest
  @MethodSource("argumentsProvider")
  public void shouldDecodeProperly(Mode mode, String input, String expectedTitle, String expectedAltTitle, String expectedSubtitle, String expectedSequence, Integer expectedYear, String expectedImdb, String expectedExtension) {
    DecodeResult result = new NameDecoder(mode).decode(input);

    assertEquals(expectedTitle, result.getTitle(), "Title for '" + input + "'");
    assertEquals(expectedSequence, result.getSequence(), "Sequence for '" + input + "'");
    assertEquals(expectedAltTitle, result.getAlternativeTitle(), "Alternative Title for '" + input + "'");
    assertEquals(expectedSubtitle, result.getSubtitle(), "Subtitle for '" + input + "'");
    assertEquals(expectedYear, result.getReleaseYear());
    assertEquals(expectedImdb, result.getCode());
    assertEquals(expectedExtension, result.getExtension(), "Extension for '" + input + "'");
  }

  static Stream<Arguments> argumentsProvider() {
    return Stream.of(
      //           Mode          Input                                                                Title                   Alternative Title       Subtitle                 Seq   Year  IMDB       Extension
      Arguments.of(Mode.MOVIE,   "A-team, The [2010, 1080p].mkv",                                     "A-team, The",          null,                   null,                    null, 2010, null,      "mkv"),
      Arguments.of(Mode.MOVIE,   "Underworld  - 03 - Rise of the Lycans [2009, Action, 1080p].mkv",   "Underworld",           null,                   "Rise of the Lycans",    "03", 2009, null,      "mkv"),
      Arguments.of(Mode.MOVIE,   "District 9 [2009, Action SF Thriller, 1080p].mkv",                  "District 9",           null,                   null,                    null, 2009, null,      "mkv"),
      Arguments.of(Mode.MOVIE,   "Let me in.mkv",                                                     "Let me in",            null,                   null,                    null, null, null,      "mkv"),
      Arguments.of(Mode.MOVIE,   "Star Wars - 01.mkv",                                                "Star Wars",            null,                   null,                    "01", null, null,      "mkv"),
      Arguments.of(Mode.MOVIE,   "Ace Ventura - When Nature Calls [1995, 720p].mkv",                  "Ace Ventura",          null,                   "When Nature Calls",     null, 1995, null,      "mkv"),
      Arguments.of(Mode.MOVIE,   "Alice [(1461312), Fantasy, 720p].avi",                              "Alice",                null,                   null,                    null, null, "1461312", "avi"),
      Arguments.of(Mode.MOVIE,   "King's Speech, The [2010, 1080p].mp4",                              "King's Speech, The",   null,                   null,                    null, 2010, null,      "mp4"),
      Arguments.of(Mode.MOVIE,   "Die Hard - 01 [1988, Action Crime Thriller, 1080p].mkv",            "Die Hard",             null,                   null,                    "01", 1988, null,      "mkv"),
      Arguments.of(Mode.MOVIE,   "Die Hard - 04 - Live Free or Die Hard [2007, Action, 1080p].mkv",   "Die Hard",             null,                   "Live Free or Die Hard", "04", 2007, null,      "mkv"),
      Arguments.of(Mode.MOVIE,   "Batman - 06 - The Dark Knight [1080p].mkv",                         "Batman",               null,                   "The Dark Knight",       "06", null, null,      "mkv"),
      Arguments.of(Mode.MOVIE,   "James Bond - 01 - Dr. No [1962].mkv",                               "James Bond",           null,                   "Dr. No",                "01", 1962, null,      "mkv"),
      Arguments.of(Mode.MOVIE,   "James Bond - 20 - Die Another Day [2002 (246460), 720p].tar.gz",    "James Bond",           null,                   "Die Another Day",       "20", 2002, "246460",  "tar.gz"),
      Arguments.of(Mode.MOVIE,   "Hauru no Ugoku Shiro (Howl's Moving Castle) [2004, Animation].mkv", "Hauru no Ugoku Shiro", "Howl's Moving Castle", null,                    null, 2004, null,      "mkv"),
      Arguments.of(Mode.MOVIE,   "Alice (TV mini-series) [2009 (1461312), Fantasy, 720p].mkv",        "Alice",                "TV mini-series",       null,                    null, 2009, "1461312", "mkv"),
      Arguments.of(Mode.MOVIE,   "Some Title - 33 Rabbits.mkv",                                       "Some Title",           null,                   "33 Rabbits",            null, null, null,      "mkv"),

      //           Mode          Input                                                                Title                   Alt   Subtitle                            Seq         Year  IMDB  Extension
      Arguments.of(Mode.EPISODE, "Birds.Of.Prey.(1x01).Final.Pilot.FTV.ShareReactor.mpg",             "Birds Of Prey",        null, "Final Pilot FTV ShareReactor",     "1,01",     null, null, "mpg"),
      Arguments.of(Mode.EPISODE, "Cleopatra 2525 - 1x05-06 - Home and rescue.mkv",                    "Cleopatra 2525",       null, "Home and rescue",                  "1,05-06",  null, null, "mkv"),
      Arguments.of(Mode.EPISODE, "Desperate Housewives S01E15 Impossible 720p h264-CtrlHD.mkv",       "Desperate Housewives", null, "Impossible 720p h264-CtrlHD",      "01,15",    null, null, "mkv"),
      Arguments.of(Mode.EPISODE, "Dharma.And.Greg.-.2x18.-.See.Dharma.Run.Amok.[tvu.org.ru].mpg",     "Dharma And Greg",      null, "See Dharma Run Amok [tvu org ru]", "2,18",     null, null, "mpg"),
      Arguments.of(Mode.EPISODE, "Battlestar_Gala_2003_-_1x01_-_Lowdown_(documentary).avi",           "Battlestar Gala 2003", null, "Lowdown (documentary)",            "1,01",     null, null, "avi"),
      Arguments.of(Mode.EPISODE, "Farscape - S01E08 - That Old Black Magic.avi",                      "Farscape",             null, "That Old Black Magic",             "01,08",    null, null, "avi"),
      Arguments.of(Mode.EPISODE, "FireFly.S01E08.720p.HDTV.XViD-ANON.mkv",                            "FireFly",              null, "720p HDTV XViD-ANON",              "01,08",    null, null, "mkv"),
      Arguments.of(Mode.EPISODE, "game.of.thrones.s01e10.720p.hdtv.x264-orenji.mkv",                  "game of thrones",      null, "720p hdtv x264-orenji",            "01,10",    null, null, "mkv"),
      Arguments.of(Mode.EPISODE, "Greys.Anatomy.S01E09.720p.HDTV.x264-AJP69.mkv",                     "Greys Anatomy",        null, "720p HDTV x264-AJP69",             "01,09",    null, null, "mkv"),
      Arguments.of(Mode.EPISODE, "HEROES - S03 E20 - COLD SNAP 720p DD5.1 x264 MMI.mkv",              "HEROES",               null, "COLD SNAP 720p DD5.1 x264 MMI",    "03,20",    null, null, "mkv"),
      Arguments.of(Mode.EPISODE, "Misfits_of_Science_#10-Grand_Theft_Bunny.imrah[sfcc].mkv",          "Misfits of Science",   null, "Grand Theft Bunny.imrah[sfcc]",    ",10",      null, null, "mkv"),
      Arguments.of(Mode.EPISODE, "Monk.S01E01-02.Mr.Monk.and.the.Candidate.720p.mkv",                 "Monk",                 null, "Mr Monk and the Candidate 720p",   "01,01-02", null, null, "mkv"),
      Arguments.of(Mode.EPISODE, "Monk.S03E15.720p.AAC2.0.720p.WEB-DL-TB.mkv",                        "Monk",                 null, "720p AAC2 0 720p WEB-DL-TB",       "03,15",    null, null, "mkv"),
      Arguments.of(Mode.EPISODE, "Monty.Python.-2x11-.How.not.to.beseen.avi",                         "Monty Python",         null, "How not to beseen",                "2,11",     null, null, "avi"),
      Arguments.of(Mode.EPISODE, "Police_Squad!_-_1x05_-_Rendezvous_At_Big_Gulch_(Terror).avi",       "Police Squad!",        null, "Rendezvous At Big Gulch (Terror)", "1,05",     null, null, "avi"),
      Arguments.of(Mode.EPISODE, "Babylon 5 [1x04] Infection.avi",                                    "Babylon 5",            null, "Infection",                        "1,04",     null, null, "avi"),
      Arguments.of(Mode.EPISODE, "24 [S01 E03].avi",                                                  "24",                   null, null,                               "01,03",    null, null, "avi"),
      Arguments.of(Mode.EPISODE, "24 - 6x09 - 2-3PM.mkv",                                             "24",                   null, "2-3PM",                            "6,09",     null, null, "mkv"),
      Arguments.of(Mode.EPISODE, "ttscc208-dot.mkv",                                                  "ttscc",                null, "dot",                              "2,08",     null, null, "mkv"),
      Arguments.of(Mode.EPISODE, "terminator.the.s.c.c.215.desert.mkv",                               "terminator the s c c", null, "desert",                           "2,15",     null, null, "mkv"),
      Arguments.of(Mode.EPISODE, "Stargate.Universe.S01E01-E03.Air.Extended.720p.DTS.x264-DiRTY.mkv", "Stargate Universe",    null, "Air Extended 720p DTS x264-DiRTY", "01,01-03", null, null, "mkv"),
      Arguments.of(Mode.EPISODE, "Gooische Vrouwen - 02x01_xvid.avi",                                 "Gooische Vrouwen",     null, "_xvid",                            "02,01",    null, null, "avi"),
      Arguments.of(Mode.EPISODE, "Pillars.of.the.Earth.Part.1.720p.BluRay.X264-REWARD.mkv",           "Pillars of the Earth", null, "720p BluRay X264-REWARD",          ",1",       null, null, "mkv"),
      Arguments.of(Mode.EPISODE, "Desperate.Housewives.S04E16E17.720p.HDTV.X264-DIMENSION.mkv",       "Desperate Housewives", null, "720p HDTV X264-DIMENSION",         "04,16-17", null, null, "mkv"),
      Arguments.of(Mode.EPISODE, "Doctor Who - 4X12 - The Stolen Earth.avi",                          "Doctor Who",           null, "The Stolen Earth",                 "4,12",     null, null, "avi"),
      Arguments.of(Mode.EPISODE, "KILL la KILL - 01 (720p) [914B637B].mkv",                           "KILL la KILL",         null, "(720p) [914B637B]",                ",01",      null, null, "mkv"),
      Arguments.of(Mode.EPISODE, "KILL la KILL - 10 (720p) [8D12E723].mkv",                           "KILL la KILL",         null, "(720p) [8D12E723]",                ",10",      null, null, "mkv"),  // E723 in hash can be interpreted as 7x23
      Arguments.of(Mode.EPISODE, "Star Trek Voyager - 501 - Night [Absolon].mkv",                     "Star Trek Voyager",    null, "Night [Absolon]",                  "5,01",     null, null, "mkv"),
      Arguments.of(Mode.EPISODE, "Star.Trek.Enterprise.S03E21.E2.720p.BluRay.x264-Green.mkv",         "Star Trek Enterprise", null, "E2 720p BluRay x264-Green",        "03,21",    null, null, "mkv"),
      Arguments.of(Mode.EPISODE, "The.Expanse.S01E09.E10.Critical.Mass.Leviathan.Wakes.mkv",          "The Expanse",          null, "Critical Mass Leviathan Wakes",    "01,09-10", null, null, "mkv"),
      Arguments.of(Mode.EPISODE, "Battlestar Gala 1978 - 1x01-03 Saga Of A Star World 2.avi",         "Battlestar Gala 1978", null, "Saga Of A Star World 2",           "1,01-03",  null, null, "avi"),
      Arguments.of(Mode.EPISODE, "S01E01 The Man Trap.avi",                                           "",                     null, "The Man Trap",                     "01,01",    null, null, "avi"),  // No serie name (for title)
      Arguments.of(Mode.EPISODE, "Horizon.S2016E12.Sports.Doping.Winning.At.Any.Cost.mkv",            "Horizon",              null, "Sports Doping Winning At Any Cost","2016,12",  null, null, "mkv"),  // Year as season number

      /*
       * Specials Parsing
       */

      //           Mode          Input                                                                Title                                   Alt   Subtitle Seq   Year  IMDB  Extension
      Arguments.of(Mode.EPISODE, "Heroes.S04.Bonus.Genetics.of.a.Scene.mkv",                          "Heroes S04 Bonus Genetics of a Scene", null, null,    null, null, null, "mkv"),
      Arguments.of(Mode.EPISODE, "Test S2.mkv",                                                       "Test S2",                              null, null,    null, null, null, "mkv"),

      //           Mode          Input                                                                Title     Alt   Subtitle                     Seq   Year  IMDB  Extension
      Arguments.of(Mode.SPECIAL, "Heroes.S04.Bonus.Genetics.of.a.Scene.mkv",                          "Heroes", null, "Bonus Genetics of a Scene", "04", null, null, "mkv"),
      Arguments.of(Mode.SPECIAL, "Test S2.mkv",                                                       "Test",   null, null,                        "2",  null, null, "mkv"),

      /*
       * Serie Folder Name Parsing
       */

      //           Mode          Input                                                                Title        Alternative Title       Subtitle                 Seq   Year  IMDB       Extension
      Arguments.of(Mode.SERIE,   "Spartacus - Blood and Sand [2001]",                                 "Spartacus", null,                   "Blood and Sand",        null, 2001, null,      ""),
      Arguments.of(Mode.SERIE,   "Spartacus (original) [2001]",                                       "Spartacus", "original",             null,                    null, 2001, null,      ""),

      /*
       * Simple parsing
       */

      Arguments.of(Mode.SIMPLE, "Spartacus (original) - Blood and Sand [2001].mkv",                 "Spartacus (original) - Blood and Sand [2001]", null, null, null, null, null, "mkv")
    );
  }
}
