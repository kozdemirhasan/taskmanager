package com.taskmanager.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.taskmanager.entity.Friendship;
import com.taskmanager.entity.User;
import com.taskmanager.enums.FriendshipStatus;
import com.taskmanager.repository.FriendshipRepository;
import com.taskmanager.repository.UserRepository;

@Service
public class FriendshipService {

    @Autowired
    private FriendshipRepository friendshipRepository;
    
    @Autowired
    private UserRepository userRepository;

    public void sendFriendRequest(User sender, String receiverEmail) {
        User receiver = userRepository.findByEmail(receiverEmail);
        if (receiver == null) {
            throw new RuntimeException("Kullanıcı bulunamadı");
        }
        
        if (friendshipRepository.existsBySenderAndReceiverAndStatus(
                sender, receiver, FriendshipStatus.PENDING) ||
            friendshipRepository.existsBySenderAndReceiverAndStatus(
                receiver, sender, FriendshipStatus.PENDING)) {
            throw new RuntimeException("Zaten bekleyen bir arkadaşlık isteği var");
        }
        
        if (friendshipRepository.existsBySenderAndReceiverAndStatus(
                sender, receiver, FriendshipStatus.ACCEPTED) ||
            friendshipRepository.existsBySenderAndReceiverAndStatus(
                receiver, sender, FriendshipStatus.ACCEPTED)) {
            throw new RuntimeException("Bu kullanıcı zaten arkadaşınız");
        }

        Friendship friendship = new Friendship();
        friendship.setSender(sender);
        friendship.setReceiver(receiver);
        friendship.setStatus(FriendshipStatus.PENDING);
        
        friendshipRepository.save(friendship);
    }

    public void acceptFriendRequest(Long friendshipId, User receiver) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow(() -> new RuntimeException("Arkadaşlık isteği bulunamadı"));
            
        if (!friendship.getReceiver().equals(receiver)) {
            throw new RuntimeException("Bu isteği kabul etme yetkiniz yok");
        }
        
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(friendship);
    }

    public void rejectFriendRequest(Long friendshipId, User receiver) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow(() -> new RuntimeException("Arkadaşlık isteği bulunamadı"));
            
        if (!friendship.getReceiver().equals(receiver)) {
            throw new RuntimeException("Bu isteği reddetme yetkiniz yok");
        }
        
        friendship.setStatus(FriendshipStatus.REJECTED);
        friendshipRepository.save(friendship);
    }

    public List<User> getFriendList(User user) {
        List<Friendship> friendships = friendshipRepository
            .findBySenderOrReceiverAndStatus(user, FriendshipStatus.ACCEPTED);
            
        return friendships.stream()
            .filter(f -> f.getSender() != null && f.getReceiver() != null)
            .map(f -> {
                if (f.getSender().equals(user)) {
                    return f.getReceiver();
                } else {
                    return f.getSender();
                }
            })
            .collect(Collectors.toList());
    }

    public List<Friendship> getPendingRequests(User user) {
        List<Friendship> requests = friendshipRepository.findByReceiverAndStatus(user, FriendshipStatus.PENDING);
        return requests.stream()
            .filter(f -> f != null && f.getSender() != null)
            .collect(Collectors.toList());
    }
} 