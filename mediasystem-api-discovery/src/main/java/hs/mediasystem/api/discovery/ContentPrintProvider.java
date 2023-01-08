package hs.mediasystem.api.discovery;

import hs.mediasystem.domain.stream.ContentID;

import java.io.IOException;
import java.net.URI;

/**
 * Provides {@link ContentPrint}s.<p>
 *
 * A {@link ContentPrint} is a unique signature of a file or directory.  For files,
 * this signature includes its size, modification time and hash.  If a file is renamed
 * but these values match, it is considered to be the same stream, and the same ContentPrint
 * will be returned.<p>
 *
 * Any other change to a file will result in it being assigned a new {@link ContentID}.<p>
 *
 * For a directory, the same signatures are gathered, apart from the size which is <code>null</code>
 * for directories.  When a directory is renamed, but a matching modification time and hash
 * can be found, its {@link ContentID} remains unchanged.  Similarly, when a directory is
 * modified (with new files for example) but its name remains unchanged, its ContentPrint is
 * updated but the {@link ContentID} is kept the same.<p>
 */
public interface ContentPrintProvider {
  ContentPrint get(URI uri, Long size, long lastModificationTime) throws IOException;
  ContentPrint get(ContentID contentId);
}
