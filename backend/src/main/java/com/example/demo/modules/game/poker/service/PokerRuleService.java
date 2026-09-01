package com.example.demo.modules.game.poker.service;

import com.example.demo.modules.game.poker.model.Card;
import com.example.demo.modules.game.poker.model.HandResult;
import com.example.demo.modules.game.poker.model.Player;
import com.example.demo.modules.game.poker.model.RoundSlot;

public interface PokerRuleService {
    void resetPlayers(Player player[]);
    void assignCard(int choice[], Card card, RoundSlot slot);
    void clearAllChoices(Player player, int choice[]);
    void autoFill(Player player, int choice[]);
    void sortBySuit(Player player, int choice[]);
    void sortByPoint(Player player, int choice[]);
    Card[] cardsOf(Player player, int choice[], RoundSlot slot);
    String cardsText(Card cards[]);
    String validateChoice(Player player, int choice[]);
    HandResult evaluate(Card cards[]);
    String buildResultText(Player player[], int choice[][]);
    String buildRoundResultText(Player player[], int choice[][], RoundSlot slot);
    String buildFinalText(Player player[], int p1Wins, int p2Wins);
    int roundWinner(Player player[], int choice[][], RoundSlot slot);
    boolean isRedSuit(Card card);
}
