package hs.mediasystem.db.base;

import hs.mediasystem.db.contentprints.ContentPrintDatabase;
import hs.mediasystem.db.contentprints.ContentPrintRecord;
import hs.mediasystem.db.services.domain.ContentPrint;
import hs.mediasystem.db.uris.UriDatabase;
import hs.mediasystem.domain.stream.ContentID;
import hs.mediasystem.util.MediaHash;
import hs.mediasystem.util.PostConstructCaller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

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
  byte[] hash1 = new byte[] {1, 2, 3, 4};
  byte[] hash2 = new byte[] {2, 2, 3, 4};
  byte[] existingFileHash = new byte[] {5, 6, 7, 8};
  ContentPrint existingFileContentPrint = new ContentPrint(new ContentID(101), 15L, Instant.ofEpochMilli(1201), existingFileHash, Instant.ofEpochMilli(15001));

  @BeforeEach
  void beforeEach() throws IOException {
    when(uriStore.findAll(any(), any())).thenReturn(new HashMap<>(Map.of(existingFile.toUri().toString(), existingFileContentPrint.getId())));
    doAnswer(invocation -> {
      Consumer<ContentPrintRecord> consumer = invocation.getArgument(0);
      consumer.accept(new ContentPrintRecord(
        existingFileContentPrint.getId().asInt(),
        existingFileHash,
        15L,
        1201,
        null,
        15001
      ));
      return null;
    }).when(idStore).forEach(any());

    PostConstructCaller.call(provider);

    Files.createDirectory(tempDir.resolve("dir"));
    Files.write(newFile, List.of("Hello World!"));
    Files.write(existingFile, List.of("Hello Planet!"));
    Files.setLastModifiedTime(newFile, FileTime.fromMillis(0));  // Ensure that any modification results in a later modification time
  }

  @AfterEach
  void afterEach() throws IOException {
    try(Stream<Path> s = Files.walk(tempDir)) {
      s.skip(1)
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
    }
  }

  @Test
  void shouldCreateNewPrint() throws IOException {
    Instant lastModificationTime = Files.getLastModifiedTime(newFile).toInstant();
    long size = Files.size(newFile);

    when(mediaHash.computeFileHash(newFile)).thenReturn(hash1);
    when(idStore.findOrAdd(size, lastModificationTime, hash1)).thenReturn(createRecord(101, size, lastModificationTime, hash1));

    ContentPrint contentPrint = provider.get(newFile.toUri(), size, lastModificationTime);

    assertEquals(lastModificationTime, contentPrint.getLastModificationTime());
    assertEquals(size, contentPrint.getSize());
    assertArrayEquals(hash1, contentPrint.getHash());
  }

  @Test
  void shouldFindExistingPrint() throws IOException {
    ContentPrint contentPrint = provider.get(existingFile.toUri(), 15L, Instant.ofEpochMilli(1201));

    assertEquals(existingFileContentPrint, contentPrint);
  }

  @Test
  void shouldNotFindExistingPrint() throws IOException {
    when(mediaHash.computeFileHash(newFile)).thenReturn(existingFileHash);
    when(idStore.findOrAdd(Long.valueOf(15), Instant.ofEpochMilli(1201), existingFileHash)).thenReturn(createRecord(102, 15L, Instant.ofEpochMilli(1201), existingFileHash));

    ContentPrint contentPrint = provider.get(newFile.toUri(), 15L, Instant.ofEpochMilli(1201));

    // Part of the new print should match, but not all (new id)
    assertEquals(existingFileContentPrint.getSize(), contentPrint.getSize());
    assertArrayEquals(existingFileContentPrint.getHash(), contentPrint.getHash());
    assertNotEquals(existingFileContentPrint, contentPrint);
  }

  @Test
  void modifiedFileWithSameNameShouldGetNewID() throws IOException {
    Instant lastModificationTime = Files.getLastModifiedTime(newFile).toInstant();
    long size = Files.size(newFile);

    when(mediaHash.computeFileHash(newFile)).thenReturn(hash1);
    when(idStore.findOrAdd(size, Instant.ofEpochMilli(lastModificationTime.toEpochMilli()), hash1)).thenReturn(createRecord(102, size, lastModificationTime, hash1));

    ContentPrint contentPrint = provider.get(newFile.toUri(), size, lastModificationTime);

    assertEquals(new ContentID(102), contentPrint.getId());

    // Now modify file:
    Files.write(newFile, List.of("Hello World! And thank you!"));

    lastModificationTime = Files.getLastModifiedTime(newFile).toInstant();
    size = Files.size(newFile);

    when(mediaHash.computeFileHash(newFile)).thenReturn(hash2);
    when(idStore.findOrAdd(size, Instant.ofEpochMilli(lastModificationTime.toEpochMilli()), hash2)).thenReturn(createRecord(103, size, lastModificationTime, hash2));

    ContentPrint contentPrintModified = provider.get(newFile.toUri(), size, lastModificationTime);

    assertFalse(Arrays.equals(contentPrintModified.getHash(), contentPrint.getHash()));
    assertEquals(new ContentID(103), contentPrintModified.getId());
    assertNotEquals(contentPrintModified.getSize(), contentPrint.getSize());
    assertNotEquals(contentPrintModified.getLastModificationTime(), contentPrint.getLastModificationTime());
    assertNotEquals(contentPrintModified, contentPrint);
    assertNotEquals(contentPrintModified.getId(), contentPrint.getId());
  }

  @Test
  void modifiedFileWithSameNameAndContentShouldGetNewID() throws IOException {
    Instant lastModificationTime = Files.getLastModifiedTime(newFile).toInstant();
    long size = Files.size(newFile);

    when(mediaHash.computeFileHash(newFile)).thenReturn(hash1);
    when(idStore.findOrAdd(size, Instant.ofEpochMilli(lastModificationTime.toEpochMilli()), hash1)).thenReturn(createRecord(102, size, lastModificationTime, hash1));

    ContentPrint contentPrint = provider.get(newFile.toUri(), size, lastModificationTime);

    assertEquals(new ContentID(102), contentPrint.getId());

    // Now modify file:
    Files.write(newFile, List.of("Hello World!"));  // content same, size same, but date will have changed

    lastModificationTime = Files.getLastModifiedTime(newFile).toInstant();
    size = Files.size(newFile);

    when(mediaHash.computeFileHash(newFile)).thenReturn(hash1);
    when(idStore.findOrAdd(size, Instant.ofEpochMilli(lastModificationTime.toEpochMilli()), hash1)).thenReturn(createRecord(103, size, lastModificationTime, hash1));

    ContentPrint contentPrintModified = provider.get(newFile.toUri(), size, lastModificationTime);

    assertArrayEquals(contentPrintModified.getHash(), contentPrint.getHash());
    assertEquals(new ContentID(103), contentPrintModified.getId());
    assertEquals(contentPrintModified.getSize(), contentPrint.getSize());
    assertNotEquals(contentPrintModified.getLastModificationTime(), contentPrint.getLastModificationTime());
    assertNotEquals(contentPrintModified, contentPrint);
    assertNotEquals(contentPrintModified.getId(), contentPrint.getId());
  }

  private static ContentPrintRecord createRecord(int id, Long size, Instant lastModificationTime, byte[] hash) {
    return new ContentPrintRecord(
      id,
      hash,
      size,
      lastModificationTime.toEpochMilli(),
      null,
      lastModificationTime.toEpochMilli()
    );
  }
}
