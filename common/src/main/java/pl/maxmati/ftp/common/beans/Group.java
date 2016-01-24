package pl.maxmati.ftp.common.beans;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by maxmati on 1/7/16
 */
public class Group {
    private Integer id = null;
    private String name;
    private List<User> members = new LinkedList<>();
    private boolean membersSynchronized = false;

    public Group(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public Group(String name) {
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if(name == null) throw new NullPointerException();
        this.name = name;
    }

    public List<User> getMembers() {
        return members;
    }

    public void setMembers(List<User> members) {
        this.members = members;
        membersSynchronized = false;
    }

    public void addMember(User user){
        members.add(user);
        membersSynchronized = false;
    }

    public void removeMember(User user){
        members.remove(user);
        membersSynchronized = false;
    }

    public void markMembersSynchronized(){
        membersSynchronized = true;
    }

    public boolean isMembersSynchronized() {
        return membersSynchronized;
    }
}
