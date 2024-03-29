package pl.maxmati.po.ftp.common.filesystem;

import pl.maxmati.po.ftp.common.PermissionManager;
import pl.maxmati.po.ftp.common.exceptions.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by maxmati on 1/9/16
 */
public class LocalFilesystem implements Filesystem {
    private final PermissionManager permissionManager;
    private Path cwd;
    private final Path chroot;

    public LocalFilesystem(Path cwd) {
        this.permissionManager = null;
        this.cwd = cwd;
        this.chroot = Paths.get("/");
    }

    public LocalFilesystem(PermissionManager permissionManager, Path cwd, Path chroot) {
        this.permissionManager = permissionManager;
        this.cwd = cwd;
        this.chroot = chroot;
    }

    @Override
    public String listFilesName(Path path){
        return listFiles(path).parallelStream()
                .map(Path::getFileName)
                .map(Path::toString)
                .collect(Collectors.joining("\n", "", "\n"));
    }

    @Override
    public List<Path> listFiles(Path path){
        path = resolveIfRelative(path);
        path = prependRoot(path);

        System.out.println("Listing files in directory " + path);

        if(permissionManager != null && !permissionManager.haveReadPermission(path))
            throw new PermissionDeniedException();

        try {
            return Files.list(path).parallel()
                    .filter(this::isFileVisible)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new FilesystemException(e);
        }
    }

    private Path prependRoot(Path path) {
        return chroot.resolve(Paths.get("/").relativize(path));
    }

    @Override
    public boolean isDirectory(Path path) {
        path = resolveIfRelative(path);
        path = prependRoot(path);

        return Files.isDirectory(path);
    }

    @Override
    public void createDir(Path path) {
        path = resolveIfRelative(path);
        path = prependRoot(path);

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
        path = resolveIfRelative(path);
        path = prependRoot(path);

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
        path = resolveIfRelative(path);
        path = prependRoot(path);

        try {
            if(!Files.exists(path))
                throw new NoSuchFileException(path.toString());

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
        path = resolveIfRelative(path);
        path = prependRoot(path);

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

    @Override
    public void changeDirectory(Path path) {
        path = resolveIfRelative(path);
        if(isDirectory(path)){
            System.out.println("Changing working directory to: " + path);
            cwd = path;
        } else {
            throw new NoSuchFileException(path.toString());
        }
    }

    @Override
    public Path getCWD() {
        return cwd;
    }

    @Override
    public String getID() {
        return "local";
    }


    @Override
    public void setPermissions(Path path, boolean userCanRead, boolean userCanWrite, boolean groupCanRead, boolean groupCanWrite) {
        if(permissionManager == null) return;
        path = resolveIfRelative(path);
        path = prependRoot(path);

        if(!permissionManager.isOwner(path))
            throw new PermissionDeniedException();

        permissionManager.setPermissions(path, userCanRead, userCanWrite, groupCanRead, groupCanWrite);
    }

    private Path resolveIfRelative(Path path) {
        if(!path.isAbsolute())
            path = cwd.resolve(path);
        return path;
    }

    private boolean parentIsDirectory(Path path) {
        return Files.isDirectory(path.getParent());
    }

    private boolean isFileVisible(Path path) {
        return permissionManager == null || permissionManager.isValid(path);
    }
}
