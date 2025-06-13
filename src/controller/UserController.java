package controller;

import dao.UserDAO;
import entity.users.SystemUser;

import java.util.List;

public class UserController {
    private UserDAO userDAO;

    public UserController() {
        this.userDAO = new UserDAO();
    }

    /**
     * Get all users from the database
     * @return List of all SystemUser objects
     */
    public List<SystemUser> getAllUsers() {
        return userDAO.getAllUsers();
    }
}
