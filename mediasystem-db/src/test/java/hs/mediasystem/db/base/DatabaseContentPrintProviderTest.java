package hs.mediasystem.db.base;

import hs.mediasystem.db.contentprints.ContentPrintDatabase;
import hs.mediasystem.db.contentprints.ContentPrintRecord;
import hs.mediasystem.db.uris.UriDatabase;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.ext.basicmediatypes.domain.stream.ContentPrint;
import hs.mediasystem.util.MediaHash;
import hs.mediasystem.util.PostConstructCaller;
import hs.mediasystem.util.StringURI;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseContentPrintProviderTest {
  @TempDir static Path tempDir;

  @Mock ContentPrintDatabase idStore;
  @Mock UriDatabase uriStore;
  @Mock MediaHash mediaHash;

  @InjectMocks DatabaseContentPrintProvider provider;

  Path newFile = tempDir.resolve("dir/1");
  Path existingFile = tempDir.resolve("dir/2");
  StringURI existingFileUri = new StringURI(existingFile.toUri().toString());
  byte[] hash1 = new byte[] {1, 2, 3, 4};
  byte[] hash2 = new byte[] {2, 2, 3, 4};
  byte[] existingFileHash = new byte[] {5, 6, 7, 8};
  ContentPrint existingFileContentPrint = new ContentPrint(new ContentID(101), 15L, 1201, existingFileHash);

  @BeforeEach
  void beforeEach() throws IOException {
    when(uriStore.findAll(any(), any())).thenReturn(new HashMap<>(Map.of(existingFile.toUri().toString(), existingFileContentPrint.getId())));
    doAnswer(invocation -> {
      Consumer<ContentPrintRecord> consumer = invocation.getArgument(0);
      consumer.accept(new ContentPrintRecord() {{
        setId(existingFileContentPrint.getId().asInt());
        setHash(existingFileHash);
        setSize(15L);
        setLastModificationTime(1201);
      }});
      return null;
    }).when(idStore).forEach(any());

    PostConstructCaller.call(provider);

    Files.createDirectory(tempDir.resolve("dir"));
    Files.write(newFile, List.of("Hello World!"));
    Files.write(existingFile, List.of("Hello Planet!"));
  }

  @AfterEach
  void afterEach() throws IOException {
    Files.walk(tempDir)
      .skip(1)
      .sorted(Comparator.reverseOrder())
      .map(Path::toFile)
      .forEach(File::delete);
  }

  @Test
  void shouldCreateNewPrint() throws IOException {
    when(mediaHash.computeFileHash(newFile)).thenReturn(hash1);

    long lastModificationTime = Files.getLastModifiedTime(newFile).toMillis();
    long size = Files.size(newFile);

    ContentPrint contentPrint = provider.get(new StringURI(newFile.toUri().toString()), size, lastModificationTime);

    assertEquals(lastModificationTime, contentPrint.getLastModificationTime());
    assertEquals(size, contentPrint.getSize());
    assertArrayEquals(hash1, contentPrint.getHash());
  }

  @Test
  void shouldFindExistingPrint() throws IOException {
    ContentPrint contentPrint = provider.get(new StringURI(existingFile.toUri().toString()), 15L, 1201);

    assertEquals(existingFileContentPrint, contentPrint);
  }

  @Test
  void shouldNotFindExistingPrint() throws IOException {
    when(mediaHash.computeFileHash(newFile)).thenReturn(existingFileHash);
    when(idStore.findOrAdd(Long.valueOf(15), 1201L, existingFileHash)).thenReturn(102);

    ContentPrint contentPrint = provider.get(new StringURI(newFile.toUri().toString()), 15L, 1201);

    // Part of the new print should match, but not all (new id)
    assertEquals(existingFileContentPrint.getSize(), contentPrint.getSize());
    assertArrayEquals(existingFileContentPrint.getHash(), contentPrint.getHash());
    assertNotEquals(existingFileContentPrint, contentPrint);
  }

  @Test
  void modifiedFileWithSameNameShouldGetNewID() throws IOException {
    long lastModificationTime = Files.getLastModifiedTime(newFile).toMillis();
    long size = Files.size(newFile);

    when(mediaHash.computeFileHash(newFile)).thenReturn(hash1);
    when(idStore.findOrAdd(size, lastModificationTime, hash1)).thenReturn(102);

    ContentPrint contentPrint = provider.get(new StringURI(newFile.toUri().toString()), size, lastModificationTime);

    assertEquals(new ContentID(102), contentPrint.getId());

    // Now modify file:
    Files.write(newFile, List.of("Hello World! And thank you!"));

    lastModificationTime = Files.getLastModifiedTime(newFile).toMillis();
    size = Files.size(newFile);

    when(mediaHash.computeFileHash(newFile)).thenReturn(hash2);
    when(idStore.findOrAdd(size, lastModificationTime, hash2)).thenReturn(103);

    ContentPrint contentPrintModified = provider.get(new StringURI(newFile.toUri().toString()), size, lastModificationTime);

    assertFalse(Arrays.equals(contentPrintModified.getHash(), contentPrint.getHash()));
    assertEquals(new ContentID(103), contentPrintModified.getId());
    assertNotEquals(contentPrintModified.getSize(), contentPrint.getSize());
    assertNotEquals(contentPrintModified.getLastModificationTime(), contentPrint.getLastModificationTime());
    assertNotEquals(contentPrintModified, contentPrint);
    assertNotEquals(contentPrintModified.getId(), contentPrint.getId());
  }

  @Test
  void modifiedFileWithSameNameAndContentShouldGetNewID() throws IOException {
    long lastModificationTime = Files.getLastModifiedTime(newFile).toMillis();
    long size = Files.size(newFile);

    when(mediaHash.computeFileHash(newFile)).thenReturn(hash1);
    when(idStore.findOrAdd(size, lastModificationTime, hash1)).thenReturn(102);

    ContentPrint contentPrint = provider.get(new StringURI(newFile.toUri().toString()), size, lastModificationTime);

    assertEquals(new ContentID(102), contentPrint.getId());

    // Now modify file:
    Files.write(newFile, List.of("Hello World!"));  // content same, size same, but date will have changed

    lastModificationTime = Files.getLastModifiedTime(newFile).toMillis();
    size = Files.size(newFile);

    when(mediaHash.computeFileHash(newFile)).thenReturn(hash1);
    when(idStore.findOrAdd(size, lastModificationTime, hash1)).thenReturn(103);

    ContentPrint contentPrintModified = provider.get(new StringURI(newFile.toUri().toString()), size, lastModificationTime);

    assertArrayEquals(contentPrintModified.getHash(), contentPrint.getHash());
    assertEquals(new ContentID(103), contentPrintModified.getId());
    assertEquals(contentPrintModified.getSize(), contentPrint.getSize());
    assertNotEquals(contentPrintModified.getLastModificationTime(), contentPrint.getLastModificationTime());
    assertNotEquals(contentPrintModified, contentPrint);
    assertNotEquals(contentPrintModified.getId(), contentPrint.getId());
  }
}
