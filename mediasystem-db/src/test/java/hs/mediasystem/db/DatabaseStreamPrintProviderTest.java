package hs.mediasystem.db;

import hs.mediasystem.db.streamids.DatabaseStreamIdStore;
import hs.mediasystem.db.uris.DatabaseUriStore;
import hs.mediasystem.scanner.api.StreamID;
import hs.mediasystem.scanner.api.StreamPrint;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseStreamPrintProviderTest {
  @TempDir static Path tempDir;

  @Mock DatabaseStreamIdStore idStore;
  @Mock DatabaseUriStore uriStore;
  @Mock MediaHash mediaHash;

  @InjectMocks DatabaseStreamPrintProvider provider;

  Path newFile = tempDir.resolve("dir/1");
  Path existingFile = tempDir.resolve("dir/2");
  StringURI existingFileUri = new StringURI(existingFile.toUri().toString());
  byte[] hash1 = new byte[] {1, 2, 3, 4};
  byte[] hash2 = new byte[] {2, 2, 3, 4};
  byte[] existingFileHash = new byte[] {5, 6, 7, 8};
  StreamPrint existingFileStreamPrint = new StreamPrint(new StreamID(101), 15L, 1201, existingFileHash);

  @BeforeEach
  void beforeEach() throws IOException {
    when(uriStore.findAll(any(), any())).thenReturn(new HashMap<>(Map.of(existingFile.toUri().toString(), existingFileStreamPrint.getId())));
    when(idStore.findAll(any(), any())).thenReturn(new HashMap<>(Map.of(existingFileStreamPrint.getId(), existingFileStreamPrint)));

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

    StreamPrint streamPrint = provider.get(new StringURI(newFile.toUri().toString()), size, lastModificationTime);

    assertEquals(lastModificationTime, streamPrint.getLastModificationTime());
    assertEquals(size, streamPrint.getSize());
    assertArrayEquals(hash1, streamPrint.getHash());
  }

  @Test
  void shouldFindExistingPrint() throws IOException {
    StreamPrint streamPrint = provider.get(new StringURI(existingFile.toUri().toString()), 15L, 1201);

    assertEquals(existingFileStreamPrint, streamPrint);
  }

  @Test
  void shouldNotFindExistingPrint() throws IOException {
    when(mediaHash.computeFileHash(newFile)).thenReturn(existingFileHash);
    when(idStore.findOrAdd(Long.valueOf(15), 1201L, existingFileHash)).thenReturn(102);

    StreamPrint streamPrint = provider.get(new StringURI(newFile.toUri().toString()), 15L, 1201);

    // Part of the new print should match, but not all (new id)
    assertEquals(existingFileStreamPrint.getSize(), streamPrint.getSize());
    assertArrayEquals(existingFileStreamPrint.getHash(), streamPrint.getHash());
    assertNotEquals(existingFileStreamPrint, streamPrint);
  }

  @Test
  void modifiedFileWithSameNameShouldGetNewID() throws IOException {
    long lastModificationTime = Files.getLastModifiedTime(newFile).toMillis();
    long size = Files.size(newFile);

    when(mediaHash.computeFileHash(newFile)).thenReturn(hash1);
    when(idStore.findOrAdd(size, lastModificationTime, hash1)).thenReturn(102);

    StreamPrint streamPrint = provider.get(new StringURI(newFile.toUri().toString()), size, lastModificationTime);

    assertEquals(new StreamID(102), streamPrint.getId());

    // Now modify file:
    Files.write(newFile, List.of("Hello World! And thank you!"));

    lastModificationTime = Files.getLastModifiedTime(newFile).toMillis();
    size = Files.size(newFile);

    when(mediaHash.computeFileHash(newFile)).thenReturn(hash2);
    when(idStore.findOrAdd(size, lastModificationTime, hash2)).thenReturn(103);

    StreamPrint streamPrintModified = provider.get(new StringURI(newFile.toUri().toString()), size, lastModificationTime);

    assertFalse(Arrays.equals(streamPrintModified.getHash(), streamPrint.getHash()));
    assertEquals(new StreamID(103), streamPrintModified.getId());
    assertNotEquals(streamPrintModified.getSize(), streamPrint.getSize());
    assertNotEquals(streamPrintModified.getLastModificationTime(), streamPrint.getLastModificationTime());
    assertNotEquals(streamPrintModified, streamPrint);
    assertNotEquals(streamPrintModified.getId(), streamPrint.getId());
  }

  @Test
  void modifiedFileWithSameNameAndContentShouldGetNewID() throws IOException {
    long lastModificationTime = Files.getLastModifiedTime(newFile).toMillis();
    long size = Files.size(newFile);

    when(mediaHash.computeFileHash(newFile)).thenReturn(hash1);
    when(idStore.findOrAdd(size, lastModificationTime, hash1)).thenReturn(102);

    StreamPrint streamPrint = provider.get(new StringURI(newFile.toUri().toString()), size, lastModificationTime);

    assertEquals(new StreamID(102), streamPrint.getId());

    // Now modify file:
    Files.write(newFile, List.of("Hello World!"));  // content same, size same, but date will have changed

    lastModificationTime = Files.getLastModifiedTime(newFile).toMillis();
    size = Files.size(newFile);

    when(mediaHash.computeFileHash(newFile)).thenReturn(hash1);
    when(idStore.findOrAdd(size, lastModificationTime, hash1)).thenReturn(103);

    StreamPrint streamPrintModified = provider.get(new StringURI(newFile.toUri().toString()), size, lastModificationTime);

    assertArrayEquals(streamPrintModified.getHash(), streamPrint.getHash());
    assertEquals(new StreamID(103), streamPrintModified.getId());
    assertEquals(streamPrintModified.getSize(), streamPrint.getSize());
    assertNotEquals(streamPrintModified.getLastModificationTime(), streamPrint.getLastModificationTime());
    assertNotEquals(streamPrintModified, streamPrint);
    assertNotEquals(streamPrintModified.getId(), streamPrint.getId());
  }
}
