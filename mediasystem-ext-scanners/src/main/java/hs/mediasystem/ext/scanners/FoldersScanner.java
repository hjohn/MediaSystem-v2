package hs.mediasystem.ext.scanners;

import hs.mediasystem.domain.stream.MediaType;
import hs.mediasystem.domain.stream.StreamID;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Attribute;
import hs.mediasystem.ext.basicmediatypes.domain.stream.ContentPrint;
import hs.mediasystem.ext.basicmediatypes.domain.stream.ContentPrintProvider;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Scanner;
import hs.mediasystem.ext.basicmediatypes.domain.stream.Streamable;
import hs.mediasystem.ext.scanners.NameDecoder.DecodeResult;
import hs.mediasystem.ext.scanners.NameDecoder.Mode;
import hs.mediasystem.util.Attributes;
import hs.mediasystem.util.StringURI;
import hs.mediasystem.util.checked.CheckedStream;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FoldersScanner implements Scanner {
  private static final NameDecoder FILE_NAME_DECODER = new NameDecoder(Mode.FILE);
  private static final MediaType FOLDER = MediaType.of("FOLDER");
  private static final MediaType FILE = MediaType.of("FILE");

  @Inject private ContentPrintProvider contentPrintProvider;

  @Override
  public List<Streamable> scan(Path root, int importSourceId) throws IOException {
    return scan(root, importSourceId, null);
  }

  private List<Streamable> scan(Path root, int importSourceId, StreamID parentId) throws IOException {
    return CheckedStream.of(Files.walk(root, 1, FileVisitOption.FOLLOW_LINKS), IOException.class)
        .filter(path -> !path.equals(root))
        .filter(path -> Files.isDirectory(path) || Constants.VIDEOS.matcher(path.getFileName().toString()).matches())
        .map(p -> toStreamables(p, importSourceId, parentId))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private List<Streamable> toStreamables(Path path, int importSourceId, StreamID parentId) throws IOException {
    String fileName = path.getFileName().toString();
    DecodeResult result = FILE_NAME_DECODER.decode(fileName);
    StringURI uri = new StringURI(path.toUri());
    Attributes attributes = Attributes.of(
      Attribute.TITLE, result.getTitle(),
      Attribute.SUBTITLE, result.getSubtitle(),
      Attribute.DESCRIPTION, result.getAlternativeTitle(),
      Attribute.YEAR, result.getReleaseYear()
    );
    List<Streamable> results = new ArrayList<>();

    if(Files.isDirectory(path)) {
      ContentPrint contentPrint = contentPrintProvider.get(uri, null, Files.getLastModifiedTime(path).toMillis());
      StreamID id = new StreamID(importSourceId, contentPrint.getId(), fileName);

      List<Streamable> list = scan(path, importSourceId, id);

      if(!list.isEmpty()) {
        results.add(new Streamable(FOLDER, uri, id, parentId, attributes));
        results.addAll(list);
      }
    }
    else if(Files.isRegularFile(path)) {
      ContentPrint contentPrint = contentPrintProvider.get(uri, Files.size(path), Files.getLastModifiedTime(path).toMillis());
      StreamID id = new StreamID(importSourceId, contentPrint.getId(), fileName);

      return List.of(new Streamable(FILE, uri, id, parentId, attributes));
    }

    return results;
  }
}
