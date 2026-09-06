package com.example.demo.modules.game.turing.service.impl;

import com.example.demo.modules.game.turing.model.TuringUser;
import com.example.demo.modules.game.turing.model.GameConfig;
import com.example.demo.modules.game.turing.repository.TuringUserRepository;
import com.example.demo.modules.game.turing.repository.GameConfigRepository;
import com.example.demo.modules.game.turing.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service // 👈 關鍵！加上這行，Spring 才能找到它
public class UserServiceImpl implements UserService {

    @Autowired
    private TuringUserRepository turingUserRepository;

    @Autowired
    private GameConfigRepository gameConfigRepository;

    @Override
    public TuringUser login(String username, String password) {
        TuringUser user = turingUserRepository.findByUsername(username.trim())
                .orElseThrow(() -> new RuntimeException("該帳號不存在！")); // 可換回你的 GameException

        if (user.isBlocked()) throw new RuntimeException("您的帳號已被管理員封鎖！");
        if (!user.getPassword().equals(password)) throw new RuntimeException("密碼輸入錯誤，請重新確認！");

        return user;
    }

    @Override
    public TuringUser register(String username, String password) {
        String trimmedUser = username.trim();
        if (turingUserRepository.findByUsername(trimmedUser).isPresent()) {
            throw new RuntimeException("該帳號已存在！請選擇其他名稱註冊。");
        }

        TuringUser newUser = new TuringUser();
        newUser.setUsername(trimmedUser);
        newUser.setPassword(password);
        newUser.setRole("USER");
        newUser.setTokens(10); 
        newUser.setBlocked(false);

        return turingUserRepository.save(newUser);
    }

    @Override
    public void createUser(TuringUser user) { turingUserRepository.save(user); }

    @Override
    public void updateUser(TuringUser user) { turingUserRepository.save(user); }

    @Override
    public void deleteUser(int userId) { turingUserRepository.deleteById(userId); }

    @Override
    public List<TuringUser> getAllUsers() { return turingUserRepository.findAll(); }

    @Override
    public void adjustUserTokens(int userId, int tokens) {
        turingUserRepository.updateTokens(userId, tokens);
    }

    @Override
    public void toggleUserBlockStatus(int userId, boolean blocked) {
        turingUserRepository.updateBlockedStatus(userId, blocked);
    }

    @Override
    public List<TuringUser> getLeaderboard() {
        return turingUserRepository.findTop10ByBlockedFalseOrderByTokensDesc();
    }

    @Override
    public Map<String, String> getGameConfigs() {
        return new HashMap<>();
    }

    @Override
    public int rewardTokensIfEligible(int userId, int roundsUsed) {
        return rewardTokensIfEligible(userId, roundsUsed, 'B');
    }

    @Transactional
    @Override
    public int rewardTokensIfEligible(int userId, int roundsUsed, char difficultyChar) {
        int limit = 5;
        int award = 10;
        
        if (difficultyChar == 'A') { limit = 3; award = 5; }
        else if (difficultyChar == 'C') { limit = 7; award = 20; }

        Optional<GameConfig> optConfig = gameConfigRepository.findById(difficultyChar);
        if (optConfig.isPresent()) {
            GameConfig config = optConfig.get();
            if (config.getSpeedBonusThreshold() > 0) limit = config.getSpeedBonusThreshold();
            if (config.getSpeedBonusAmount() > 0) award = config.getSpeedBonusAmount();
        }

        if (roundsUsed <= limit) {
            Optional<TuringUser> optUser = turingUserRepository.findById(userId);
            if (optUser.isPresent()) {
                TuringUser u = optUser.get();
                int updatedTokens = u.getTokens() + award;
                u.setTokens(updatedTokens);
                turingUserRepository.save(u);
                return award;
            }
        }
        return 0; 
    }
}