package com.example.demo.modules.lobby.repository;

import com.example.demo.modules.lobby.entity.Room;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomRepository extends JpaRepository<Room, String> {
    // 繼承 JpaRepository 後，我們就直接擁有了 save(), findById(), findAll() 等方法！
	List<Room> findByStatus(String status);
}