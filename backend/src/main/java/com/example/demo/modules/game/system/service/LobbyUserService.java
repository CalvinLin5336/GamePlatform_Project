package com.example.demo.modules.game.system.service;

import com.example.demo.modules.game.system.dto.LobbyUserView;

public interface LobbyUserService {
    LobbyUserView findLobbyUser(Long userId);
}
