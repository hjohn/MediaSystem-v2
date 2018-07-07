package hs.mediasystem.ext.scanners;

import hs.mediasystem.ext.scanners.NameDecoder.DecodeResult;
import hs.mediasystem.ext.scanners.NameDecoder.Hint;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class NameDecoderTest {
  private final Hint[] hints;
  private final String input;
  private final String title;
  private final String alternativeTitle;
  private final String subtitle;
  private final String sequence;
  private final String imdb;
  private final Integer year;
  private final String extension;

  public NameDecoderTest(Hint[] hints, String input, String title, String alternativeTitle, String subtitle, String sequence, Integer year, String imdb, String extension) {
    this.hints = hints;
    this.input = input;
    this.title = title;
    this.alternativeTitle = alternativeTitle;
    this.subtitle = subtitle;
    this.sequence = sequence;
    this.year = year;
    this.imdb = imdb;
    this.extension = extension;
  }

  @Parameters
  public static Collection<Object[]> data() {
    Hint[] hintsMovie = new Hint[] {Hint.MOVIE};
    Hint[] hintsEpisode = new Hint[] {Hint.EPISODE};

    return Arrays.asList(new Object[][] {
      {hintsMovie, "A-team, The [2010, 1080p].mkv", "A-team, The", null, null, null, 2010, null, "mkv"},
      {hintsMovie, "Underworld  - 03 - Rise of the Lycans [2009, Action Fantasy Horror Thriller, 1080p].mkv", "Underworld", null, "Rise of the Lycans", "03", 2009, null, "mkv"},
      {hintsMovie, "District 9 [2009, Action SF Thriller, 1080p].mkv", "District 9", null, null, null, 2009, null, "mkv"},
      {hintsMovie, "Let me in.mkv", "Let me in", null, null, null, null, null, "mkv"},
      {hintsMovie, "Star Wars - 01.mkv", "Star Wars", null, null, "01", null, null, "mkv"},
      {hintsMovie, "Ace Ventura - When Nature Calls [1995, 720p].mkv", "Ace Ventura", null, "When Nature Calls", null, 1995, null, "mkv"},
      {hintsMovie, "Alice [(1461312), Fantasy, 720p].avi", "Alice", null, null, null, null, "1461312", "avi"},
      {hintsMovie, "King's Speech, The [2010, 1080p].mp4", "King's Speech, The", null, null, null, 2010, null, "mp4"},
      {hintsMovie, "Die Hard - 01 [1988, Action Crime Thriller, 1080p].mkv", "Die Hard", null, null, "01", 1988, null, "mkv"},
      {hintsMovie, "Die Hard - 04 - Live Free or Die Hard [2007, Action Crime Thriller, 1080p].mkv", "Die Hard", null, "Live Free or Die Hard", "04", 2007, null, "mkv"},
      {hintsMovie, "Batman - 06 - The Dark Knight [1080p].mkv", "Batman", null, "The Dark Knight", "06", null, null, "mkv"},
      {hintsMovie, "James Bond - 01 - Dr. No [1962].mkv", "James Bond", null, "Dr. No", "01", 1962, null, "mkv"},
      //{"James Bond - 04b - Never say never again [1983, 720p].mkv", "James Bond", null, "Never say never again", "04b", 1983, null, "mkv"},
      {hintsMovie, "James Bond - 20 - Die Another Day [2002 (246460), Action Adventure Thriller, 720p].tar.gz", "James Bond", null, "Die Another Day", "20", 2002, "246460", "tar.gz"},
      {hintsMovie, "Hauru no Ugoku Shiro (Howl's Moving Castle) [2004, Animation Adventure Fantasy Romance].mkv", "Hauru no Ugoku Shiro", "Howl's Moving Castle", null, null, 2004, null, "mkv"},
      {hintsMovie, "Alice (TV mini-series) [2009 (1461312), Fantasy, 720p].mkv", "Alice", "TV mini-series", null, null, 2009, "1461312", "mkv"},
      {hintsMovie, "Some Title - 33 Rabbits.mkv", "Some Title", null, "33 Rabbits", null, null, null, "mkv"},

      // Butchered names
      {hintsEpisode, "Birds.Of.Prey.(1x01).Final.Pilot.FTV.ShareReactor.mpg", "Birds Of Prey", null, "Final Pilot FTV ShareReactor", "1,01", null, null, "mpg"},
      {hintsEpisode, "Cleopatra 2525 - 1x05-06 - Home and rescue.mkv", "Cleopatra 2525", null, "Home and rescue", "1,05-06", null, null, "mkv"},
      {hintsEpisode, "Desperate Housewives S01E15 Impossible 720p h264-CtrlHD.mkv", "Desperate Housewives", null, "Impossible 720p h264-CtrlHD", "01,15", null, null, "mkv"},
      {hintsEpisode, "Dharma.And.Greg.-.2x18.-.See.Dharma.Run.Amok.english-japanese.[tvu.org.ru].mpg", "Dharma And Greg", null, "See Dharma Run Amok english-japanese", "2,18", null, null, "mpg"},
      {hintsEpisode, "Battlestar_Galactica_2003_-_1x01_-_Lowdown_(documentary).avi", "Battlestar Galactica 2003", null, "Lowdown (documentary)", "1,01", null, null, "avi"},
      {hintsEpisode, "Farscape - S01E08 - That Old Black Magic.avi", "Farscape", null, "That Old Black Magic", "01,08", null, null, "avi"},
      {hintsEpisode, "FireFly.S01E08.720p.HDTV.XViD-ANON.mkv", "FireFly", null, "720p HDTV XViD-ANON", "01,08", null, null, "mkv"},
      {hintsEpisode, "game.of.thrones.s01e10.720p.hdtv.x264-orenji.mkv", "game of thrones", null, "720p hdtv x264-orenji", "01,10", null, null, "mkv"},
      {hintsEpisode, "Greys.Anatomy.S01E09.720p.HDTV.x264-AJP69.mkv", "Greys Anatomy", null, "720p HDTV x264-AJP69", "01,09", null, null, "mkv"},
      {hintsEpisode, "HEROES - S03 E20 - COLD SNAP 720p DD5.1 x264 MMI.mkv", "HEROES", null, "COLD SNAP 720p DD5.1 x264 MMI", "03,20", null, null, "mkv"},
      {hintsEpisode, "Misfits_of_Science_#10-Grand_Theft_Bunny.imrah[sfcc].mkv", "Misfits of Science", null, "Grand Theft Bunny.imrah", ",10", null, null, "mkv"},
      {hintsEpisode, "Monk.S01E01-02.Mr.Monk.and.the.Candidate.720p.WEB-DL.H264.AAC20-myTV.mkv", "Monk", null, "Mr Monk and the Candidate 720p WEB-DL H264 AAC20-myTV", "01,01-02", null, null, "mkv"},
      {hintsEpisode, "Monk.S03E15.720p.AAC2.0.720p.WEB-DL-TB.mkv", "Monk", null, "720p AAC2 0 720p WEB-DL-TB", "03,15", null, null, "mkv"},
      {hintsEpisode, "Monty.Python.-2x11-.How.not.to.beseen.avi", "Monty Python", null, "How not to beseen", "2,11", null, null, "avi"},
      {hintsEpisode, "Police_Squad!_-_1x05_-_Rendezvous_At_Big_Gulch_(Terror_In_The_Neighborhood).avi", "Police Squad!", null, "Rendezvous At Big Gulch (Terror In The Neighborhood)", "1,05", null, null, "avi"},
      {hintsEpisode, "Babylon 5 [1x04] Infection.avi", "Babylon 5", null, "Infection", "1,04", null, null, "avi"},
      {hintsEpisode, "24 [S01 E03].avi", "24", null, null, "01,03", null, null, "avi"},
      {hintsEpisode, "24 - 6x09 - 2-3PM.mkv", "24", null, "2-3PM", "6,09", null, null, "mkv"},

      {hintsEpisode, "ttscc208-dot.mkv", "ttscc", null, "dot", "2,08", null, null, "mkv"},
      {hintsEpisode, "terminator.the.sarah.connor.chronicles.215.desert.cantos-sitv.mkv", "terminator the sarah connor chronicles", null, "desert cantos-sitv", "2,15", null, null, "mkv"},

      {hintsEpisode, "Stargate.Universe.S01E01-E03.Air.Extended.720p.Bluray.DTS.x264-DiRTY.mkv", "Stargate Universe", null, "Air Extended 720p Bluray DTS x264-DiRTY", "01,01-03", null, null, "mkv"},
      {hintsEpisode, "Heroes.S04.Bonus.Genetics.of.a.Scene.720p.BluRay.x264-EbP.mkv", "Heroes", null, "Bonus Genetics of a Scene 720p BluRay x264-EbP", "04", null, null, "mkv"},

      {hintsEpisode, "Gooische Vrouwen - 02x01_xvid.avi", "Gooische Vrouwen", null, "_xvid", "02,01", null, null, "avi"},

      {hintsEpisode, "Pillars.of.the.Earth.Part.1.720p.BluRay.X264-REWARD.mkv", "Pillars of the Earth", null, "720p BluRay X264-REWARD", ",1", null, null, "mkv"},
      {hintsEpisode, "Desperate.Housewives.S04E16E17.720p.HDTV.X264-DIMENSION.mkv", "Desperate Housewives", null, "720p HDTV X264-DIMENSION", "04,16-17", null, null, "mkv"}
    });
  }

  @Test
  public void shouldDecodeProperly() {
    DecodeResult result = new NameDecoder(hints).decode(input);

    Assert.assertEquals(title, result.getTitle());
    Assert.assertEquals(alternativeTitle, result.getAlternativeTitle());
    Assert.assertEquals(subtitle, result.getSubtitle());
    Assert.assertEquals(year, result.getReleaseYear());
    Assert.assertEquals(sequence, result.getSequence());
    Assert.assertEquals(imdb, result.getCode());
    Assert.assertEquals(extension, result.getExtension());
  }
}
