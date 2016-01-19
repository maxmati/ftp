package pl.maxmati.po.ftp.client.widgets.filesystemTree;

import java.nio.file.Path;

/**
 * Created by maxmati on 1/13/16
 */
public class FileEntry {
    private final Path path;
    private final boolean directory;
    private boolean populated;

    public FileEntry(Path path, boolean isDirectory) {
        this.path = path;
        this.directory = isDirectory;
    }

    @Override
    public String toString() {
        if(path.getFileName() != null)
            return path.getFileName().toString();
        else
            return "/";
    }

    public Path getPath() {
        return path;
    }

    public void setPopulated(boolean populated) {
        this.populated = populated;
    }

    public boolean isPopulated() {
        return populated;
    }

    public boolean isDirectory() {
        return directory;
    }
}
