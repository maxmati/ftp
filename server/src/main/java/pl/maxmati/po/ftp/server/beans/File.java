package pl.maxmati.po.ftp.server.beans;

import pl.maxmati.po.ftp.common.beans.Group;
import pl.maxmati.po.ftp.common.beans.User;

import java.util.Objects;

/**
 * Created by maxmati on 1/12/16
 */
public class File {
    private Integer id = null;
    private String filename = null;
    private User owner = null;
    private Group group = null;
    private boolean ownerCanRead = true;
    private boolean ownerCanWrite = true;
    private boolean groupCanRead = false;
    private boolean groupCanWrite = false;

    public File(int id, String filename, User owner, Group group, boolean ownerCanRead,
                boolean ownerCanWrite, boolean groupCanRead, boolean groupCanWrite) {
        this.id = id;
        this.filename = filename;
        this.owner = owner;
        this.group = group;
        this.ownerCanRead = ownerCanRead;
        this.ownerCanWrite = ownerCanWrite;
        this.groupCanRead = groupCanRead;
        this.groupCanWrite = groupCanWrite;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public boolean isOwnerCanRead() {
        return ownerCanRead;
    }

    public void setOwnerCanRead(boolean ownerCanRead) {
        this.ownerCanRead = ownerCanRead;
    }

    public boolean isOwnerCanWrite() {
        return ownerCanWrite;
    }

    public void setOwnerCanWrite(boolean ownerCanWrite) {
        this.ownerCanWrite = ownerCanWrite;
    }

    public boolean isGroupCanRead() {
        return groupCanRead;
    }

    public void setGroupCanRead(boolean groupCanRead) {
        this.groupCanRead = groupCanRead;
    }

    public boolean isGroupCanWrite() {
        return groupCanWrite;
    }

    public void setGroupCanWrite(boolean groupCanWrite) {
        this.groupCanWrite = groupCanWrite;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof File)) return false;
        File file = (File) o;
        return ownerCanRead == file.ownerCanRead &&
                ownerCanWrite == file.ownerCanWrite &&
                groupCanRead == file.groupCanRead &&
                groupCanWrite == file.groupCanWrite &&
                Objects.equals(id, file.id) &&
                Objects.equals(filename, file.filename) &&
                Objects.equals(owner, file.owner) &&
                Objects.equals(group, file.group);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, filename, owner, group, ownerCanRead, ownerCanWrite, groupCanRead, groupCanWrite);
    }
}
