package pl.maxmati.po.ftp.server.gui;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import pl.maxmati.ftp.common.beans.Group;
import pl.maxmati.ftp.common.beans.User;
import pl.maxmati.po.ftp.server.Config;
import pl.maxmati.po.ftp.server.UsersManager;
import pl.maxmati.po.ftp.server.database.ConnectionPool;
import pl.maxmati.po.ftp.server.database.dao.GroupsDAO;
import pl.maxmati.po.ftp.server.database.dao.UsersDAO;
import pl.maxmati.po.ftp.server.exceptions.DatabaseException;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Created by maxmati on 1/24/16
 */
public class Controller implements Initializable{
    @FXML private TextField ip1;
    @FXML private TextField ip2;
    @FXML private TextField ip3;
    @FXML private TextField ip4;

    @FXML private TextField databaseUrl;
    @FXML private TextField databaseLogin;
    @FXML private PasswordField databasePass;

    @FXML private TitledPane groupsPane;
    @FXML private TextField groupAddName;
    @FXML private TableView<GroupData> groupTable;
    @FXML private TableColumn<GroupData, Integer> groupId;
    @FXML private TableColumn<GroupData, String> groupName;

    private final ObservableList<GroupData> groupData = FXCollections.observableArrayList();

    @FXML private TitledPane usersPane;
    @FXML private TextField userAddName;
    @FXML private PasswordField userAddPassword;
    @FXML private TableView<UserData> userTable;
    @FXML private TableColumn<UserData, String> userPassword;
    @FXML private TableColumn<UserData, String> userName;
    @FXML private TableColumn<UserData, Integer> userID;

    private final ObservableList<UserData> userData = FXCollections.observableArrayList();

    @FXML private TitledPane usersGroupsPane;
    @FXML private ComboBox<UserData> userGroupAddUser;
    @FXML private ComboBox<GroupData> userGroupAddGroup;
    @FXML private TableView<UserGroupData> userGroupTable;
    @FXML private TableColumn<UserGroupData, UserData> userGroupUser;
    @FXML private TableColumn<UserGroupData, GroupData> userGroupGroup;

    private final ObservableList<UserGroupData> usersGroupsData = FXCollections.observableArrayList();

    private final ConnectionPool cp = new ConnectionPool();
    private final UsersDAO usersDAO = new UsersDAO(cp);


    private final GroupsDAO groupsDAO = new GroupsDAO(cp);
    private final UsersManager usersManager = new UsersManager(usersDAO);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadDataFromDB();

        initSettings();
        initUserTable();
        initGroupTable();
        initUserGroupTable();
    }

    public void addUser() {
        final String username = userAddName.getText();
        final String password = userAddPassword.getText();

        if(username.isEmpty() || password.isEmpty())
            return;


        User user = usersManager.createUser(username, password);
        final UserData userData = new UserData(user);
        this.userData.add(userData);

        Group group = new Group(username);
        groupsDAO.save(group);
        group.addMember(user);
        groupsDAO.saveMembers(group);
        final GroupData groupData = new GroupData(group);
        this.groupData.add(groupData);

        usersGroupsData.add(new UserGroupData(userData, groupData));

        userAddName.clear();
        userAddPassword.clear();
    }

    public void addGroup() {
        final String name = groupAddName.getText();

        if(name.isEmpty())
            return;

        Group group = new Group(name);
        groupsDAO.save(group);
        groupData.add(new GroupData(group));

        groupAddName.clear();
    }

    public void addUserGroup() {
        final GroupData selectedGroup = groupData.get(userGroupAddGroup.getSelectionModel().getSelectedIndex());
        final UserData selectedUser = userData.get(userGroupAddUser.getSelectionModel().getSelectedIndex());
        groupsDAO.addUserToGroup(selectedUser.getId(), selectedGroup.getId());
        usersGroupsData.add(new UserGroupData(selectedUser, selectedGroup));
        userGroupAddGroup.getSelectionModel().clearSelection();
        userGroupAddUser.getSelectionModel().clearSelection();
    }

    public void saveConfig() {
        String DBUrl = databaseUrl.getText();
        String DBUser = databaseLogin.getText();
        String DBPass = databasePass.getText();

        String ip1 = this.ip1.getText();
        String ip2 = this.ip2.getText();
        String ip3 = this.ip3.getText();
        String ip4 = this.ip4.getText();

        if(DBUrl.isEmpty() || DBUser.isEmpty() || DBPass.isEmpty() ||
                ip1.isEmpty() || ip2.isEmpty() || ip3.isEmpty() || ip4.isEmpty())
            return;

        Config config = Config.getInstance();
        config.setDBUrl(DBUrl);
        config.setDBUser(DBUser);
        config.setDBPass(DBPass);
        config.setIp1(Integer.valueOf(ip1));
        config.setIp2(Integer.valueOf(ip2));
        config.setIp3(Integer.valueOf(ip3));
        config.setIp4(Integer.valueOf(ip4));
        try {
            config.store();
        } catch (IOException e) {
            e.printStackTrace();
        }

        cp.clear();

        loadDataFromDB();
    }

    private void loadGroups() {
        try {

            List<Group> groups = groupsDAO.findGroups();
            List<GroupData> groupsList = groups.stream().map(GroupData::new).collect(Collectors.toList());
            groupData.addAll(groupsList);

            GroupData.dao = groupsDAO;
            groupsPane.setDisable(false);
        } catch (DatabaseException e){
            groupData.clear();
            groupsPane.setDisable(true);
        }
    }

    private void loadUsers() {
        try {
            List<User> users = usersDAO.findUsers();
            List<UserData> usersList = users.stream().map(UserData::new).collect(Collectors.toList());
            userData.addAll(usersList);

            UserData.manager = usersManager;
            usersPane.setDisable(false);
        } catch (DatabaseException e){
            userData.clear();
            usersPane.setDisable(true);
        }
    }

    private void loadUsersGroups() {
        try {
            List<Group> groups = groupsDAO.findGroups();
            groups.forEach(groupsDAO::fetchMembers);

            groups.forEach(group ->
                    group.getMembers().forEach(user ->
                            usersGroupsData.add(new UserGroupData(new UserData(user), new GroupData(group)))));

            usersGroupsPane.setDisable(false);
        } catch (DatabaseException e){
            usersGroupsData.clear();
            usersGroupsPane.setDisable(true);
        }
    }

    private void initSettings() {
        Config config = Config.getInstance();
        databaseUrl.setText(config.getDBUrl());
        databaseLogin.setText(config.getDBUser());
        databasePass.setText(config.getDBPass());
        ip1.setText(String.valueOf(config.getIP1()));
        ip2.setText(String.valueOf(config.getIP2()));
        ip3.setText(String.valueOf(config.getIP3()));
        ip4.setText(String.valueOf(config.getIP4()));
    }

    private void initGroupTable() {
        groupId.setCellValueFactory(new PropertyValueFactory<GroupData, Integer>("id"));
        groupName.setCellFactory(TextFieldTableCell.forTableColumn());
        groupName.setCellValueFactory(new PropertyValueFactory<GroupData, String>("name"));
        groupName.setOnEditCommit(event ->
                        event.getTableView().getItems().get(
                                event.getTablePosition().getRow()
                        ).setName(event.getNewValue())
        );

        MenuItem groupDelete = new MenuItem("Delete");
        groupDelete.setOnAction(event -> {
            GroupData item = groupData.get(groupTable.getSelectionModel().getSelectedIndex());
            if(item != null){
                item.delete();
                groupData.remove(item);
            }
        });

        groupTable.setItems(groupData);
        groupTable.setEditable(true);
        groupTable.setContextMenu(new ContextMenu(groupDelete));
    }

    private void initUserTable() {
        userID.setCellValueFactory(new PropertyValueFactory<UserData, Integer>("id"));

        userName.setCellFactory(TextFieldTableCell.forTableColumn());
        userName.setCellValueFactory(new PropertyValueFactory<UserData, String>("username"));
        userName.setOnEditCommit(event ->
                event.getTableView().getItems().get(
                    event.getTablePosition().getRow()
                ).setUsername(event.getNewValue())
        );

        userPassword.setCellFactory(TextFieldTableCell.forTableColumn());
        userPassword.setCellValueFactory(new PropertyValueFactory<UserData, String>("password"));
        userPassword.setOnEditCommit(event ->
                        event.getTableView().getItems().get(
                                event.getTablePosition().getRow()
                        ).setPassword(event.getNewValue())
        );

        MenuItem userDelete = new MenuItem("Delete");
        userDelete.setOnAction(event -> {
            UserData item = userData.get(userTable.getSelectionModel().getSelectedIndex());
            if(item != null){
                item.delete();
                userData.remove(item);
            }
        });

        userTable.setContextMenu(new ContextMenu(userDelete));
        userTable.setItems(userData);
        userTable.setEditable(true);
    }

    private void initUserGroupTable() {
        userGroupUser.setCellFactory(ComboBoxTableCell.forTableColumn(userData));
        userGroupUser.setCellValueFactory(new PropertyValueFactory<UserGroupData, UserData>("user"));
        userGroupUser.setOnEditCommit(event -> {
            UserGroupData data = event.getTableView().getItems().get(
                    event.getTablePosition().getRow()
            );

            groupsDAO.removeUserFromGroup(event.getOldValue().getId(), data.getGroup().getId());
            groupsDAO.addUserToGroup(event.getNewValue().getId(), data.getGroup().getId());
        });

        userGroupGroup.setCellFactory(ComboBoxTableCell.forTableColumn(groupData));
        userGroupGroup.setCellValueFactory(new PropertyValueFactory<UserGroupData, GroupData>("group"));
        userGroupGroup.setOnEditCommit(event -> {
            UserGroupData data = event.getTableView().getItems().get(
                    event.getTablePosition().getRow()
            );

            groupsDAO.removeUserFromGroup(data.getUser().getId(), event.getOldValue().getId());
            groupsDAO.addUserToGroup(data.getUser().getId(), event.getNewValue().getId());
        });

        MenuItem userGroupDelete = new MenuItem("Delete");
        userGroupDelete.setOnAction(event -> {
            UserGroupData item = usersGroupsData.get(userGroupTable.getSelectionModel().getSelectedIndex());
            if(item != null){
                groupsDAO.removeUserFromGroup(item.getUser().getId(), item.getGroup().getId());
                usersGroupsData.remove(item);
            }
        });

        userGroupTable.setContextMenu(new ContextMenu(userGroupDelete));
        userGroupTable.setEditable(true);
        userGroupTable.setItems(usersGroupsData);


        userGroupAddGroup.setItems(groupData);
        userGroupAddUser.setItems(userData);
    }

    private void loadDataFromDB() {
        loadUsers();
        loadGroups();
        loadUsersGroups();
    }

    @SuppressWarnings("WeakerAccess")
    public static class GroupData {
        public static GroupsDAO dao;
        private final SimpleIntegerProperty id;
        private final SimpleStringProperty name;
        private final Group group;

        public GroupData(Group group) {
            this.group = group;
            id = new SimpleIntegerProperty(group.getId());
            name = new SimpleStringProperty(group.getName());
        }

        public int getId() {
            return id.get();
        }


        public void setId(int id) {
            this.id.set(id);
        }

        public String getName() {
            return name.get();
        }

        public void setName(String name) {
            this.name.set(name);
            group.setName(name);
            dao.save(group);
        }

        public void delete() {
            dao.removeById(group.getId());
        }

        @Override
        public String toString() {
            return name.get();
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class UserData {
        private static UsersManager manager;
        private final SimpleIntegerProperty id;
        private final SimpleStringProperty username;
        private final SimpleStringProperty password;
        private final User user;

        public UserData(User user) {
            this.user = user;
            id = new SimpleIntegerProperty(user.getId());
            username = new SimpleStringProperty(user.getUsername());
            password = new SimpleStringProperty(user.getPassword());
        }

        public int getId() {
            return id.get();
        }

        public void setId(int id) {
            this.id.set(id);
        }

        public String getUsername() {
            return username.get();
        }

        public void setUsername(String username) {
            this.username.set(username);
            manager.changeName(user, username);
        }

        public String getPassword() {
            return password.get();
        }

        public void setPassword(String password) {
            this.password.set(password);
            manager.changePassword(user, password);
        }

        public void delete() {
            manager.delete(user);
        }

        @Override
        public String toString() {
            return username.get();
        }
    }

    @SuppressWarnings("WeakerAccess")
    public class UserGroupData {
        SimpleObjectProperty<UserData> user;
        SimpleObjectProperty<GroupData> group;

        public UserGroupData(UserData user, GroupData group) {
            this.user = new SimpleObjectProperty<>(user);
            this.group = new SimpleObjectProperty<>(group);
        }

        public UserData getUser() {
            return user.get();
        }

        public void setUser(UserData user) {
            this.user.set(user);
        }

        public GroupData getGroup() {
            return group.get();
        }

        public void setGroup(GroupData group) {
            this.group.set(group);
        }
    }
}
