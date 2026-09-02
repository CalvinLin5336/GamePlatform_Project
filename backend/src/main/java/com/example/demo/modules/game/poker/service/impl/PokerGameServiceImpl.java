package com.example.demo.modules.game.poker.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.modules.game.poker.dto.CardView;
import com.example.demo.modules.game.poker.dto.GameView;
import com.example.demo.modules.game.poker.dto.JoinResult;
import com.example.demo.modules.game.poker.dto.PreviewView;
import com.example.demo.modules.game.poker.dto.ResultView;
import com.example.demo.modules.game.poker.exception.GameException;
import com.example.demo.modules.game.poker.model.Card;
import com.example.demo.modules.game.poker.model.Game;
import com.example.demo.modules.game.poker.model.GameRoom;
import com.example.demo.modules.game.poker.model.HandResult;
import com.example.demo.modules.game.poker.model.Player;
import com.example.demo.modules.game.poker.model.RoundResult;
import com.example.demo.modules.game.poker.model.RoundSlot;
import com.example.demo.modules.game.poker.service.PokerGameService;
import com.example.demo.modules.game.poker.service.PokerRuleService;
import com.example.demo.modules.game.poker.util.GameTool;

@Service
public class PokerGameServiceImpl implements PokerGameService {
    private static final long ROUND_RESULT_DURATION_MILLIS=3000;
    private Map<String, GameRoom> rooms = new HashMap<String, GameRoom>();
    @Autowired
    private PokerRuleService pokerRuleService;

    @Override
    public synchronized JoinResult join(
            String roomId,
            String mode,
            Long userId,
            String playerName) {
        if(roomId==null || roomId.isBlank()) {
            throw new GameException("INVALID_ROOM", "缺少房間編號");
        }
        roomId=roomId.trim();
        if(!GameRoom.MODE_PLAYER.equals(mode) && !GameRoom.MODE_COMPUTER.equals(mode)) {
            throw new GameException("INVALID_MODE", "模式必須是 PLAYER 或 COMPUTER");
        }
        if(userId==null || userId<=0) {
            throw new GameException("INVALID_USER", "缺少玩家編號");
        }
        GameRoom room=rooms.get(roomId);
        if(room==null) {
            room=new GameRoom(roomId, mode);
            rooms.put(roomId, room);
        }
        if(!room.getMode().equals(mode)) throw new GameException("MODE_MISMATCH", "此房間已使用其他遊戲模式");

        int seat=findOrAssignSeat(room, userId);

        String token=UUID.randomUUID().toString();
        room.getUserIds()[seat]=userId;
        room.getSeatTokens()[seat]=token;
        room.getConnected()[seat]=true;
        if(playerName!=null && !playerName.trim().equals("")) {
            room.getPlayers()[seat].setName(playerName.trim());
        }
        if(GameRoom.MODE_COMPUTER.equals(mode)) {
            room.getPlayers()[1].setName("電腦");
        }
        if(GameRoom.MODE_COMPUTER.equals(mode)) room.getConnected()[1]=true;

        if(GameRoom.STATUS_WAITING.equals(room.getStatus())) {
            if(GameRoom.MODE_COMPUTER.equals(mode)
                    || (room.getConnected()[0] && room.getConnected()[1])) deal(room);
        }
        return new JoinResult(roomId, token, seat+1, view(roomId, token));
    }

    private int findOrAssignSeat(GameRoom room, Long userId) {
        for(int i=0;i<room.getUserIds().length;i++) {
            if(userId.equals(room.getUserIds()[i])) return i;
        }
        if(GameRoom.MODE_COMPUTER.equals(room.getMode())) {
            if(room.getUserIds()[0]!=null) {
                throw new GameException("ROOM_FULL", "此電腦對戰房間已有玩家");
            }
            return 0;
        }
        boolean seat1Empty=room.getUserIds()[0]==null;
        boolean seat2Empty=room.getUserIds()[1]==null;
        if(!seat1Empty && !seat2Empty) {
            throw new GameException("ROOM_FULL", "此房間已有兩位玩家");
        }
        if(seat1Empty && seat2Empty) return Math.random()<0.5 ? 0 : 1;
        return seat1Empty ? 0 : 1;
    }

    @Override
    public synchronized void leave(String roomId, String token) {
        GameRoom room=requireRoom(roomId);
        int seat=requireSeat(room, token);
        room.getConnected()[seat]=false;
        room.getSeatTokens()[seat]=null;
    }

    @Override
    public synchronized GameView view(String roomId, String token) {
        GameRoom room=requireRoom(roomId);
        advanceRoundIfReady(room);
        int seat=requireSeat(room, token);
        Player player=room.getPlayers()[seat];
        int choice[]=room.getChoices()[seat];

        List<CardView> hand=new ArrayList<CardView>();
        for(Card card:player.getHand()) {
            CardView cardView=new CardView(card.getNumber(), card.getSuits(), card.getPoint(),
                    card.getName(), choice[card.getNumber()]);
            hand.add(cardView);
        }

        List<ResultView> resultViews=new ArrayList<ResultView>();
        for(RoundResult result:room.getResults()) {
            List<String> player1Cards=cardNames(result.getPlayer1Cards());
            List<String> player2Cards=cardNames(result.getPlayer2Cards());
            ResultView resultView=new ResultView(result.getRound(), player1Cards, result.getPlayer1Type(),
                    player2Cards, result.getPlayer2Type(), result.getWinner());
            resultViews.add(resultView);
        }

        long remaining=0;
        if(room.getRoundResultEndsAt()!=null) {
            remaining=Math.max(0, room.getRoundResultEndsAt()-System.currentTimeMillis());
        }
        return new GameView(room.getId(), room.getMode(), room.getStatus(),
                room.getCurrentRound(), seat+1, room.getConnected()[0], room.getConnected()[1],
                room.getRoundConfirmed()[0], room.getRoundConfirmed()[1], hand,
                preview(player, choice), resultViews, room.getWinner(),
                room.getPlayers()[0].getName(), room.getPlayers()[1].getName(), remaining);
    }

    @Override
    public synchronized GameView select(String roomId, String token, Map<Integer, Integer> choices) {
        GameRoom room=requirePlaying(roomId);
        int seat=requireSeat(room, token);
        Player player=room.getPlayers()[seat];
        int playerChoice[]=room.getChoices()[seat];

        if(room.getRoundConfirmed()[seat]) {
            throw new GameException("ALREADY_CONFIRMED", "本輪已確認，不能再修改");
        }
        if(choices==null) throw new GameException("INVALID_SELECTION", "沒有收到選牌資料");

        Map<Integer, Integer> clean=new HashMap<Integer, Integer>();
        for(Card card:player.getHand()) {
            Integer received=choices.get(card.getNumber());
            int slot=received==null ? 0 : received;
            if(slot<0 || slot>3) throw new GameException("INVALID_SLOT", "選牌區域錯誤");

            int oldSlot=playerChoice[card.getNumber()];
            if(oldSlot>0 && oldSlot<room.getCurrentRound() && slot!=oldSlot) {
                throw new GameException("ROUND_LOCKED", "前一輪的牌不能修改");
            }
            if(slot>0 && slot!=room.getCurrentRound() && slot!=oldSlot) {
                throw new GameException("INVALID_SLOT", "目前只能選擇第 "+room.getCurrentRound()+" 輪");
            }
            clean.put(card.getNumber(), slot);
        }
        for(Card card:player.getHand()) {
            playerChoice[card.getNumber()]=clean.get(card.getNumber());
        }
        return view(roomId, token);
    }

    @Override
    public synchronized GameView confirm(String roomId, String token) {
        GameRoom room=requirePlaying(roomId);
        int seat=requireSeat(room, token);
        validateRound(room.getPlayers()[seat], room.getChoices()[seat], room.getCurrentRound());
        room.getRoundConfirmed()[seat]=true;

        if(GameRoom.MODE_COMPUTER.equals(room.getMode())) room.getRoundConfirmed()[1]=true;
        if(room.getRoundConfirmed()[0] && room.getRoundConfirmed()[1]) resolveCurrentRound(room);
        return view(roomId, token);
    }

    @Override
    public synchronized GameView nextRound(String roomId, String token) {
        GameRoom room=requireRoom(roomId);
        requireSeat(room, token);
        advanceRoundIfReady(room);
        if(GameRoom.STATUS_PLAYING.equals(room.getStatus())) return view(roomId, token);
        if(GameRoom.STATUS_ROUND_RESULT.equals(room.getStatus())) {
            throw new GameException("RESULT_PAUSE", "結果展示倒數尚未結束");
        }
        throw new GameException("NOT_ROUND_RESULT", "目前不能進入下一輪");
    }

    @Override
    public synchronized GameView autoSelect(String roomId, String token) {
        GameRoom room=requirePlaying(roomId);
        int seat=requireSeat(room, token);
        if(room.getRoundConfirmed()[seat]) throw new GameException("ALREADY_CONFIRMED", "本輪已確認，不能再修改");
        GameTool.auto_choose_current_round(
                room.getPlayers()[seat], room.getChoices()[seat], room.getCurrentRound());
        return view(roomId, token);
    }

    @Override
    public synchronized boolean tokenIsValid(String roomId, String token) {
        try {
            requireSeat(requireRoom(roomId), token);
            return true;
        }
        catch(GameException e) {
            return false;
        }
    }

    private void deal(GameRoom room) {
        String player1Name=room.getPlayers()[0].getName();
        String player2Name=room.getPlayers()[1].getName();
        room.setGame(new Game());
        room.getPlayers()[0].setName(player1Name);
        room.getPlayers()[1].setName(player2Name);
        for(int i=0;i<room.getChoices().length;i++) Arrays.fill(room.getChoices()[i], 0);
        room.setResults(new ArrayList<RoundResult>());
        room.setWinner(null);
        room.setRoundResultEndsAt(null);
        room.setCurrentRound(1);
        room.getRoundConfirmed()[0]=false;
        room.getRoundConfirmed()[1]=false;
        room.setStatus(GameRoom.STATUS_PLAYING);
        if(GameRoom.MODE_COMPUTER.equals(room.getMode())) {
            GameTool.auto_choose_best(room.getPlayers()[1], room.getChoices()[1]);
        }
    }

    private void validateRound(Player player, int choice[], int round) {
        RoundSlot slot=RoundSlot.fromIndex(round);
        int count=pokerRuleService.cardsOf(player, choice, slot).length;
        if(count!=slot.getNeed()) {
            throw new GameException("INVALID_SELECTION", slot.getLabel()+" 需要選擇 "+slot.getNeed()+" 張牌");
        }
    }

    private Map<Integer, PreviewView> preview(Player player, int choice[]) {
        Map<Integer, PreviewView> answer=new LinkedHashMap<Integer, PreviewView>();
        for(int i=1;i<=3;i++) {
            RoundSlot slot=RoundSlot.fromIndex(i);
            Card cards[]=pokerRuleService.cardsOf(player, choice, slot);
            String type=null;
            if(cards.length==slot.getNeed()) type=pokerRuleService.evaluate(cards).getName();
            answer.put(i, new PreviewView(cards.length, slot.getNeed(), type));
        }
        return answer;
    }

    private void resolveCurrentRound(GameRoom room) {
        RoundSlot slot=RoundSlot.fromIndex(room.getCurrentRound());
        Card player1Array[]=pokerRuleService.cardsOf(room.getPlayers()[0], room.getChoices()[0], slot);
        Card player2Array[]=pokerRuleService.cardsOf(room.getPlayers()[1], room.getChoices()[1], slot);
        HandResult player1Result=pokerRuleService.evaluate(player1Array);
        HandResult player2Result=pokerRuleService.evaluate(player2Array);
        int compare=player1Result.compare(player2Result);
        int winner=0;
        if(compare>0) winner=1;
        if(compare<0) winner=2;
        if(compare==0) throw new GameException("RESULT_ERROR", "牌力相同，勝負計算異常");

        List<Card> player1Cards=new ArrayList<Card>(Arrays.asList(player1Array));
        List<Card> player2Cards=new ArrayList<Card>(Arrays.asList(player2Array));
        RoundResult result=new RoundResult(room.getCurrentRound(), player1Cards, player1Result.getName(),
                player2Cards, player2Result.getName(), winner);
        room.getResults().add(result);

        if(room.getCurrentRound()<3) {
            room.setStatus(GameRoom.STATUS_ROUND_RESULT);
            room.setRoundResultEndsAt(System.currentTimeMillis()+ROUND_RESULT_DURATION_MILLIS);
        }
        else finishGame(room);
    }

    private void finishGame(GameRoom room) {
        int player1Wins=0;
        int player2Wins=0;
        for(RoundResult result:room.getResults()) {
            if(result.getWinner()==1) player1Wins++;
            if(result.getWinner()==2) player2Wins++;
        }
        if(player1Wins>player2Wins) room.setWinner(1);
        else if(player2Wins>player1Wins) room.setWinner(2);
        else throw new GameException("RESULT_ERROR", "勝場相同，整局勝負計算異常");
        room.setRoundResultEndsAt(null);
        room.setStatus(GameRoom.STATUS_FINISHED);
    }

    private void advanceRoundIfReady(GameRoom room) {
        if(!GameRoom.STATUS_ROUND_RESULT.equals(room.getStatus())) return;
        if(room.getRoundResultEndsAt()==null
                || System.currentTimeMillis()<room.getRoundResultEndsAt()) return;
        room.setCurrentRound(room.getCurrentRound()+1);
        room.getRoundConfirmed()[0]=false;
        room.getRoundConfirmed()[1]=false;
        room.setRoundResultEndsAt(null);
        room.setStatus(GameRoom.STATUS_PLAYING);
    }

    private List<String> cardNames(List<Card> cards) {
        List<String> names=new ArrayList<String>();
        for(Card card:cards) names.add(card.getName());
        return names;
    }

    private GameRoom requireRoom(String roomId) {
        GameRoom room=rooms.get(roomId);
        if(room==null) throw new GameException("ROOM_NOT_FOUND", "找不到房間");
        return room;
    }

    private GameRoom requirePlaying(String roomId) {
        GameRoom room=requireRoom(roomId);
        advanceRoundIfReady(room);
        if(!GameRoom.STATUS_PLAYING.equals(room.getStatus())) {
            throw new GameException("NOT_PLAYING", "目前不是選牌階段");
        }
        return room;
    }

    private int requireSeat(GameRoom room, String token) {
        for(int i=0;i<2;i++) {
            if(token!=null && token.equals(room.getSeatTokens()[i])) return i;
        }
        throw new GameException("INVALID_PLAYER", "玩家連線已失效，請重新加入");
    }
}
