package pl.maxmati.po.ftp.server;

import pl.maxmati.po.ftp.server.exceptions.FilesystemException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * Created by maxmati on 1/9/16
 */
public class Filesystem {
    public String listFiles(Path directory){
        System.out.println("Listing files in directory " + directory);
        try {
            return Files.list(directory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.joining("\n", "", "\n"));
        } catch (IOException e) {
            throw new FilesystemException(e);
        }
    }

    public boolean isValidDirectory(Path path) {
        return Files.isDirectory(path);
    }

    public boolean createDir(Path path) {
        if(!Files.exists(path.getParent()) || Files.exists(path))
            return false;

        try {
            Files.createDirectory(path);
            return true;
        } catch (IOException e) {
            throw new FilesystemException(e);
        }
    }

    public boolean remove(Path path, boolean directory) {
        try {
            if(directory)
                if(!Files.isDirectory(path) || Files.list(path).count() != 0)
                    return false;
            else
                if(!Files.isRegularFile(path))
                    return false;

            Files.delete(path);
            return true;
        } catch (IOException e) {
            throw new FilesystemException(e);
        }
    }
}
