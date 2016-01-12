package pl.maxmati.po.ftp.server;

import pl.maxmati.po.ftp.server.exceptions.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Collectors;

/**
 * Created by maxmati on 1/9/16
 */
public class LocalFilesystem implements Filesystem {
    private final PermissionManager permissionManager;

    public LocalFilesystem() {
        this.permissionManager = null;
    }

    public LocalFilesystem(PermissionManager permissionManager) {
        this.permissionManager = permissionManager;

    }

    @Override
    public String listFiles(Path directory){
        System.out.println("Listing files in directory " + directory);

        if(permissionManager != null && !permissionManager.haveReadPermission(directory))
            throw new PermissionDeniedException();

        try {
            return Files.list(directory)
                    .filter(this::isFileVisible)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.joining("\n", "", "\n"));
        } catch (IOException e) {
            throw new FilesystemException(e);
        }
    }

    @Override
    public boolean isValidDirectory(Path path) {
        return Files.isDirectory(path);
    }

    @Override
    public void createDir(Path path) {
        if(!parentIsDirectory(path))
            throw new NoSuchFileException(path.toString());

        if(Files.exists(path))
            throw new FileAlreadyExistsException(path.toString());


        if(permissionManager != null && !permissionManager.haveWritePermission(path.getParent()))
            throw new PermissionDeniedException();

        try {
            Files.createDirectory(path);

            if(permissionManager != null)
                permissionManager.addFileEntry(path);

        } catch (IOException e) {
            throw new FilesystemException(e);
        }
    }

    @Override
    public void remove(Path path, boolean directory) {
        if(permissionManager != null && !permissionManager.haveWritePermission(path.getParent()))
            throw new PermissionDeniedException();

        try {
            if(!Files.exists(path))
                throw new NoSuchFileException(path.toString());

            if(directory) {
                if (!Files.isDirectory(path))
                    throw new NotDirectoryException(path.toString());
                if (Files.list(path).count() != 0)
                    throw new DirectoryNotEmptyException(path.toString());
            } else {
                if (!Files.isRegularFile(path))
                    throw new NotRegularFileException(path.toString());
            }

            Files.delete(path);

            if(permissionManager != null){
                permissionManager.removeFileEntry(path);
            }
        } catch (IOException e) {
            throw new FilesystemException(e);
        }
    }

    @Override
    public InputStream getFile(Path path) {
        try {
            if(!Files.isReadable(path))
                throw new PermissionDeniedException();

            if(!Files.isRegularFile(path))
                throw new NotRegularFileException(path.toString());

            if(permissionManager != null && !permissionManager.haveReadPermission(path))
                throw new PermissionDeniedException();

            return Files.newInputStream(path);
        } catch (IOException e) {
            throw new FilesystemException(e);
        }
    }

    @Override
    public OutputStream storeFile(Path path, boolean append) {
        try {
            boolean shouldAddFilesEntry = false;

            if(Files.exists(path)){
                if(!Files.isWritable(path))
                    throw new PermissionDeniedException();

                if(!Files.isRegularFile(path))
                    throw new NotRegularFileException(path.toString());

                if(permissionManager != null && !permissionManager.haveWritePermission(path))
                    throw new PermissionDeniedException();
            }else {
                if(!parentIsDirectory(path))
                    throw new NoSuchFileException(path.toString());

                if(append)
                    throw new NoSuchFileException(path.toString());

                if(permissionManager != null && !permissionManager.haveWritePermission(path.getParent()))
                    throw new PermissionDeniedException();

                shouldAddFilesEntry = true;
            }


            OutputStream os;
            if(append)
                os = Files.newOutputStream(path, StandardOpenOption.APPEND);
            else
                os = Files.newOutputStream(path);

            if(shouldAddFilesEntry && permissionManager != null)
                permissionManager.addFileEntry(path);

            return os;

        } catch (IOException e) {
            throw new FilesystemException(e);
        }
    }

    private boolean parentIsDirectory(Path path) {
        return Files.isDirectory(path.getParent());
    }

    private boolean isFileVisible(Path path) {
        return permissionManager == null || permissionManager.isValid(path);
    }
}
