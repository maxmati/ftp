package pl.maxmati.po.ftp.server.gui;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import pl.maxmati.ftp.common.beans.User;
import pl.maxmati.po.ftp.server.UsersManager;
import pl.maxmati.po.ftp.server.database.ConnectionPool;
import pl.maxmati.po.ftp.server.database.dao.UsersDAO;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Created by maxmati on 1/24/16
 */
public class Controller implements Initializable{
    @FXML private TextField userAddName;
    @FXML private PasswordField userAddPassword;
    @FXML private TableView<UserData> userTable;
    @FXML private TableColumn<UserData, String> userPassword;
    @FXML private TableColumn<UserData, String> userName;
    @FXML private TableColumn<UserData, Integer> userID;

    private final ConnectionPool cp = new ConnectionPool();
    private final UsersDAO usersDAO = new UsersDAO(cp);
    private final UsersManager usersManager = new UsersManager(usersDAO);


    private final ObservableList<UserData> data;

    public Controller() {
        List<User> users = usersDAO.findUsers();
        List<UserData> usersData = users.stream().map(UserData::new).collect(Collectors.toList());
        data = FXCollections.observableList(usersData);

        UserData.manager = usersManager;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
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


        userTable.setItems(data);
        userTable.setEditable(true);
    }

    public void addUser() {
        final String username = userAddName.getText();
        final String password = userAddPassword.getText();

        if(username.isEmpty() || password.isEmpty())
            return;

        User user = usersManager.createUser(username, password);
        data.add(new UserData(user));
    }

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
    }
}
