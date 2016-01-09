package pl.maxmati.po.ftp.server;

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
            throw new RuntimeException(e);
        }
    }
}
