package lets_play.service;

import lets_play.exception.ResourceNotFoundException;
import lets_play.model.User;
import lets_play.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(String id) {
        return userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable")); 
    }

    public User updateUser(String id, User updatedUser) {
        User user = getUserById(id);
        user.setName(updatedUser.getName());
        user.setEmail(updatedUser.getEmail());
        return userRepository.save(user);
    }

    public void deleteUser(String id) {
        getUserById(id);
        userRepository.deleteById(id);
    }
}