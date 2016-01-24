package pl.maxmati.po.ftp.server.gui;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import pl.maxmati.ftp.common.beans.Group;
import pl.maxmati.ftp.common.beans.User;
import pl.maxmati.po.ftp.server.UsersManager;
import pl.maxmati.po.ftp.server.database.ConnectionPool;
import pl.maxmati.po.ftp.server.database.dao.GroupsDAO;
import pl.maxmati.po.ftp.server.database.dao.UsersDAO;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Created by maxmati on 1/24/16
 */
public class Controller implements Initializable{
    @FXML private TextField groupAddName;
    @FXML private TableView<GroupData> groupTable;
    @FXML private TableColumn<GroupData, Integer> groupId;
    @FXML private TableColumn<GroupData, String> groupName;

    private final ObservableList<GroupData> groupData;

    @FXML private TextField userAddName;
    @FXML private PasswordField userAddPassword;
    @FXML private TableView<UserData> userTable;
    @FXML private TableColumn<UserData, String> userPassword;
    @FXML private TableColumn<UserData, String> userName;
    @FXML private TableColumn<UserData, Integer> userID;

    private final ObservableList<UserData> userData;

    private final ConnectionPool cp = new ConnectionPool();
    private final UsersDAO usersDAO = new UsersDAO(cp);
    private final GroupsDAO groupsDAO = new GroupsDAO(cp);


    private final UsersManager usersManager = new UsersManager(usersDAO);

    public Controller() {
        List<User> users = usersDAO.findUsers();
        List<UserData> usersList = users.stream().map(UserData::new).collect(Collectors.toList());
        userData = FXCollections.observableList(usersList);

        UserData.manager = usersManager;

        List<Group> groups = groupsDAO.findGroups();
        List<GroupData> groupsList = groups.stream().map(GroupData::new).collect(Collectors.toList());
        groupData = FXCollections.observableArrayList(groupsList);

        GroupData.dao = groupsDAO;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initUserTable();

        initGroupTable();
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

    public void addUser() {
        final String username = userAddName.getText();
        final String password = userAddPassword.getText();

        if(username.isEmpty() || password.isEmpty())
            return;

        User user = usersManager.createUser(username, password);
        userData.add(new UserData(user));
    }

    public void addGroup() {
        final String name = groupAddName.getText();

        if(name.isEmpty())
            return;

        Group group = new Group(name);
        groupsDAO.save(group);
        groupData.add(new GroupData(group));
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
    }
}
