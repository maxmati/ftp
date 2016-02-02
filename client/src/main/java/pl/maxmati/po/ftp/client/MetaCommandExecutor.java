package pl.maxmati.po.ftp.client;

import pl.maxmati.ftp.common.filesystem.LocalFilesystem;
import pl.maxmati.po.ftp.client.filesystem.FTPFilesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;

/**
 * Created by maxmati on 2/2/16
 */
public class MetaCommandExecutor {

    private final LocalFilesystem localFilesystem;
    private final FTPFilesystem ftpFilesystem;

    public MetaCommandExecutor(LocalFilesystem localFilesystem, FTPFilesystem ftpFilesystem) {
        this.localFilesystem = localFilesystem;
        this.ftpFilesystem = ftpFilesystem;
    }

    public void executeCommand(String command) {
        System.out.println("Executing meta command: " + command);
        final String[] tokens = command.split(" ");
        if(tokens.length != 3)
            return;

        OutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            switch (tokens[0].toLowerCase()) {
                case "get":
                    inputStream = ftpFilesystem.getFile(Paths.get(tokens[1]));
                    outputStream = localFilesystem.storeFile(Paths.get(tokens[2]), false);
                    break;
                case "put":
                    inputStream = localFilesystem.getFile(Paths.get(tokens[1]));
                    outputStream = ftpFilesystem.storeFile(Paths.get(tokens[2]), false);
                    break;
            }
            copyFile(inputStream, outputStream);
            inputStream = null;
            outputStream = null;
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
                if( outputStream != null)
                    outputStream.close();
            }catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    private void copyFile(InputStream inputStream, OutputStream outputStream) {
        int n;
        byte[] buffer = new byte[1024];
        try {
            while ((n = inputStream.read(buffer)) > -1) {
                outputStream.write(buffer, 0, n);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }
}
