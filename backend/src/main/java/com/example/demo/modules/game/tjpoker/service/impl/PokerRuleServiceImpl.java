package com.example.demo.modules.game.tjpoker.service.impl;

import com.example.demo.modules.game.tjpoker.model.Card;
import com.example.demo.modules.game.tjpoker.model.Player;
import com.example.demo.modules.game.tjpoker.model.Round;
import com.example.demo.modules.game.tjpoker.util.GameTool;
import com.example.demo.modules.game.tjpoker.util.Sort;
import com.example.demo.modules.game.tjpoker.model.HandResult;
import com.example.demo.modules.game.tjpoker.model.RoundSlot;
import com.example.demo.modules.game.tjpoker.service.PokerRuleService;

import org.springframework.stereotype.Service;

@Service
public class PokerRuleServiceImpl implements PokerRuleService {
    // 新局 top
    public void resetPlayers(Player player[]) {
        Card deck[] = createDeck();
        shuffle(deck);

        player[0].setName("玩家");
        player[1].setName("電腦");
        for (int i = 0; i < 15; i++) {
            player[0].getHand()[i].copy(deck[i * 2]);
            player[1].getHand()[i].copy(deck[i * 2 + 1]);
        }
        Sort.sort_depends_on_number(player[0].getHand());
        Sort.sort_depends_on_number(player[1].getHand());
    }
    // 新局 end

    // 建立牌堆 top
    private Card[] createDeck() {
        Card deck[] = new Card[52];
        String suits[] = { "♠", "♡", "♢", "♣" };
        for (int i = 0; i < suits.length; i++) {
            for (int point = 1; point <= 13; point++) {
                deck[i * 13 + point - 1] = new Card(suits[i], point, i * 13 + point - 1);
            }
        }
        return deck;
    }
    // 建立牌堆 end

    // 洗牌 top
    private void shuffle(Card deck[]) {
        for (int i = 0; i < deck.length; i++) {
            int randomIndex = (int) (Math.random() * deck.length);
            Card tmp = deck[i];
            deck[i] = deck[randomIndex];
            deck[randomIndex] = tmp;
        }
    }
    // 洗牌 end

    // 選牌 top
    public void assignCard(int choice[], Card card, RoundSlot slot) {
        choice[card.getNumber()] = slot.getIndex();
    }
    // 選牌 end

    // 清除選牌 top
    public void clearAllChoices(Player player, int choice[]) {
        for (int i = 0; i < choice.length; i++) {
            choice[i] = RoundSlot.DISCARD.getIndex();
        }
    }
    // 清除選牌 end

    // 電腦選牌 top
    public void autoFill(Player player, int choice[]) {
        GameTool.auto_choose_best(player, choice);
    }
    // 電腦選牌 end

    // 切換排序 top
    public void sortBySuit(Player player, int choice[]) {
        Sort.sort_depends_on_number(player.getHand());
    }

    public void sortByPoint(Player player, int choice[]) {
        Sort.sort_depends_on_point_increasing(player.getHand());
    }
    // 切換排序 end

    // 取得某輪手牌 top
    public Card[] cardsOf(Player player, int choice[], RoundSlot slot) {
        int count = 0;
        for (int i = 0; i < player.getHand().length; i++) {
            if (choice[player.getHand()[i].getNumber()] == slot.getIndex()) {
                count++;
            }
        }

        Card cards[] = new Card[count];
        int index = 0;
        for (int i = 0; i < player.getHand().length; i++) {
            if (choice[player.getHand()[i].getNumber()] == slot.getIndex()) {
                cards[index] = new Card();
                cards[index].copy(player.getHand()[i]);
                index++;
            }
        }
        sortHighToLow(cards);
        return cards;
    }
    // 取得某輪手牌 end

    // 顯示牌名 top
    public String cardsText(Card cards[]) {
        if (cards.length == 0) return "尚未選牌";
        String text = "";
        for (int i = 0; i < cards.length; i++) {
            if (i > 0) text += "  ";
            text += cards[i].getName();
        }
        return text;
    }
    // 顯示牌名 end

    // 檢查選牌 top
    public String validateChoice(Player player, int choice[]) {
        for (int i = 0; i < RoundSlot.values().length; i++) {
            RoundSlot slot = RoundSlot.values()[i];
            int count = cardsOf(player, choice, slot).length;
            if (count != slot.getNeed()) {
                return slot.getLabel() + " 需要 " + slot.getNeed() + " 張，目前是 " + count + " 張。";
            }
        }
        return null;
    }
    // 檢查選牌 end

    // 牌型判斷 top
    // 使用原本Round判斷牌型與強度
    public HandResult evaluate(Card cards[]) {
        Player tempPlayer = new Player();
        Round round;
        if (cards.length == 3) {
            round = tempPlayer.getRounds()[0];
        } else {
            round = tempPlayer.getRounds()[1];
        }

        for (int i = 0; i < cards.length; i++) {
            round.getRound_hand()[i].copy(cards[i]);
        }
        round.determine_hand_strength();
        return new HandResult(round.getHand_type(), round.getHand_strength());
    }
    // 牌型判斷 end

    // 結果文字 top
    public String buildResultText(Player player[], int choice[][]) {
        String text = "";
        int p1Wins = 0;
        int p2Wins = 0;
        RoundSlot rounds[] = { RoundSlot.ROUND_1, RoundSlot.ROUND_2, RoundSlot.ROUND_3 };

        for (int i = 0; i < rounds.length; i++) {
            RoundSlot slot = rounds[i];
            Card p1Cards[] = cardsOf(player[0], choice[0], slot);
            Card p2Cards[] = cardsOf(player[1], choice[1], slot);
            HandResult p1 = evaluate(p1Cards);
            HandResult p2 = evaluate(p2Cards);
            int compare = p1.compare(p2);

            text += "\n" + slot.getLabel() + "\n";
            text += player[0].getName() + ": " + cardsText(p1Cards) + "  " + p1.getName() + "\n";
            text += player[1].getName() + ": " + cardsText(p2Cards) + "  " + p2.getName() + "\n";

            if (compare > 0) {
                p1Wins++;
                text += "勝方: " + player[0].getName() + "\n";
            } else if (compare < 0) {
                p2Wins++;
                text += "勝方: " + player[1].getName() + "\n";
            } else {
                text += "本輪平手\n";
            }
        }

        text += "\n總結: ";
        if (p1Wins > p2Wins) text += player[0].getName() + " 獲勝";
        else if (p2Wins > p1Wins) text += player[1].getName() + " 獲勝";
        else text += "平手";
        return text;
    }
    // 結果文字 end

    // 單輪結果文字 top
    public String buildRoundResultText(Player player[], int choice[][], RoundSlot slot) {
        Card p1Cards[] = cardsOf(player[0], choice[0], slot);
        Card p2Cards[] = cardsOf(player[1], choice[1], slot);
        HandResult p1 = evaluate(p1Cards);
        HandResult p2 = evaluate(p2Cards);
        int winner = roundWinner(player, choice, slot);

        String text = "\n" + slot.getLabel() + "\n";
        text += player[0].getName() + ": " + cardsText(p1Cards) + "  " + p1.getName() + "\n";
        text += player[1].getName() + ": " + cardsText(p2Cards) + "  " + p2.getName() + "\n";

        if(winner==1) text += "勝方: " + player[0].getName() + "\n";
        if(winner==2) text += "勝方: " + player[1].getName() + "\n";
        return text;
    }
    // 單輪結果文字 end

    // 總結文字 top
    public String buildFinalText(Player player[], int p1Wins, int p2Wins) {
        String text = "\n總結: ";
        if(p1Wins>p2Wins) text += player[0].getName() + " 獲勝";
        else if(p2Wins>p1Wins) text += player[1].getName() + " 獲勝";
        else text += "勝負計算異常";
        return text;
    }
    // 總結文字 end

    // 單輪勝負 top
    public int roundWinner(Player player[], int choice[][], RoundSlot slot) {
        Card p1Cards[] = cardsOf(player[0], choice[0], slot);
        Card p2Cards[] = cardsOf(player[1], choice[1], slot);
        HandResult p1 = evaluate(p1Cards);
        HandResult p2 = evaluate(p2Cards);
        int compare = p1.compare(p2);

        if(compare>0) return 1;
        if(compare<0) return 2;
        throw new IllegalStateException("牌力相同，勝負計算異常");
    }
    // 單輪勝負 end

    // 判斷是否為紅牌(愛心/方塊) top
    public boolean isRedSuit(Card card) {
        if(card.getSuits()=="♡") return true;
        if(card.getSuits()=="♢") return true;
        return false;
    }
    // 判斷是否為紅牌(愛心/方塊) end

    // 根據牌的點數(A=14)進行降序排序 top
    private void sortHighToLow(Card cards[]) {
        Sort.sort_depends_on_suit_decreasing(cards);
        Sort.sort_depends_on_point_A14_decreasing(cards);
    }
    // 根據牌的點數(A=14)進行降序排序 end
}
