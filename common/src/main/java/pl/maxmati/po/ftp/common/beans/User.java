package pl.maxmati.po.ftp.common.beans;

import java.util.Objects;

/**
 * Created by maxmati on 1/6/16
 */
@SuppressWarnings("unused")
public class User {
    private Integer id = null;
    private String username = null;
    private String password = null;
    private String salt = null;


    public User(String username, String password, String salt) {
        this(null, username, password, salt);
    }

    public User(Integer id, String username, String password, String salt) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.salt = salt;
    }

    public User(String username) {
        this.username = username;
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if(username == null) throw new NullPointerException();
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if(password == null) throw new NullPointerException();
        this.password = password;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        if(salt == null) throw new NullPointerException();
        this.salt = salt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) &&
                Objects.equals(username, user.username) &&
                Objects.equals(password, user.password) &&
                Objects.equals(salt, user.salt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, password, salt);
    }
}
