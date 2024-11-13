package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User getUserById(Long id) {
        if (userRepository.findById(id).isPresent())
            return userRepository.findById(id).get();
        else
            throw new RuntimeException("User not found");
    }

    public void deleteUserById(Long id) {
        userRepository.deleteById(id);
    }

    public void updateUser(User user) {
        User existingUser = getUserById(user.getId_user());

        if (user.getName() != null) {
            existingUser.setName(user.getName());
        }
        if (user.getSalary() != null) {
            existingUser.setSalary(user.getSalary());
        }
        if (user.getPhone() != null) {
            existingUser.setPhone(user.getPhone());
        }
        if (user.getAge() != null) {
            existingUser.setAge(user.getAge());
        }
        if (user.getEmail() != null) {
            existingUser.setEmail(user.getEmail());
        }
        if (user.getPassword() != null) {
            existingUser.setPassword(user.getPassword());
        }

        userRepository.saveAndFlush(existingUser);
    }

    public boolean userExist(Long id) {
        return userRepository.existsById(id);
    }

    public void addUser(User user) {
        userRepository.save(user);
    }

}
