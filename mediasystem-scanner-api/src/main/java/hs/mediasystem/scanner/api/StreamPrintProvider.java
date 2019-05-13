package hs.mediasystem.scanner.api;

import hs.mediasystem.util.StringURI;

import java.io.IOException;

/**
 * Provides {@link StreamPrint}s.<p>
 *
 * A {@link StreamPrint} is a unique signature of a file or directory.  For files,
 * this signature includes its size, modification time and hash.  If a file is renamed
 * but these values match, it is considered to be the same stream, and the same StreamPrint
 * will be returned.<p>
 *
 * Any other change to a file will result in it being assigned a new {@link StreamID}.<p>
 *
 * For a directory, the same signatures are gathered, apart from the size which is <code>null</code>
 * for directories.  When a directory is renamed, but a matching modification time and hash
 * can be found, its {@link StreamID} remains unchanged.  Similarly, when a directory is
 * modified (with new files for example) but its name remains unchanged, its StreamPrint is
 * updated but the {@link StreamID} is kept the same.<p>
 */
public interface StreamPrintProvider {
  StreamPrint get(StringURI uri, Long size, long lastModificationTime) throws IOException;
  StreamPrint get(StreamID streamId);
}
