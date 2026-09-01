package com.example.demo.modules.game.poker.util;

import com.example.demo.modules.game.poker.model.Card;
import com.example.demo.modules.game.poker.model.Player;
import com.example.demo.modules.game.poker.model.Round;

public class GameTool {
	
	// 自動選較強的牌組，依序找第3輪5張、第2輪5張、第1輪3張
	public static void auto_choose_best(Player player, int choice[])
	{
		clear_choice(choice);
		clear_rounds(player);
		
		Card hand[]=copy_cards(player.getHand());
		boolean used[]=new boolean[hand.length];
		
		Card round3[]=find_best_cards(hand, used, 5);
		mark_used(hand, used, round3);
		
		Card round2[]=find_best_cards(hand, used, 5);
		mark_used(hand, used, round2);
		
		Card round1[]=find_best_cards(hand, used, 3);
		mark_used(hand, used, round1);
		
		put_round(player, choice, 0, round1);
		put_round(player, choice, 1, round2);
		put_round(player, choice, 2, round3);
	}
	
	private static void clear_choice(int choice[])
	{
		for(int i=0;i<choice.length;i++)
		{
			choice[i]=0;
		}
	}
	
	private static void clear_rounds(Player player)
	{
		for(int i=0;i<player.getRounds().length;i++)
		{
			for(int j=0;j<player.getRounds()[i].getRound_hand().length;j++)
			{
				player.getRounds()[i].getRound_hand()[j].copy(new Card());
			}
		}
	}
	
	private static Card[] copy_cards(Card cards[])
	{
		Card copy[]=new Card[cards.length];
		for(int i=0;i<cards.length;i++)
		{
			copy[i]=new Card();
			copy[i].copy(cards[i]);
		}
		return copy;
	}
	
	private static Card[] find_best_cards(Card hand[], boolean used[], int count)
	{
		Card current[]=new Card[count];
		Card best[]=new Card[count];
		long best_score[]=new long[] {-1};
		find_best_cards(hand, used, count, 0, 0, current, best, best_score);
		return best;
	}
	
	private static void find_best_cards(Card hand[], boolean used[], int count, int start, int depth,
			Card current[], Card best[], long best_score[])
	{
		if(depth==count)
		{
			long score=score(current, count);
			if(score>best_score[0])
			{
				best_score[0]=score;
				for(int i=0;i<count;i++)
				{
					best[i]=new Card();
					best[i].copy(current[i]);
				}
			}
			return;
		}
		
		for(int i=start;i<hand.length;i++)
		{
			if(!used[i])
			{
				current[depth]=hand[i];
				find_best_cards(hand, used, count, i+1, depth+1, current, best, best_score);
			}
		}
	}
	
	private static long score(Card cards[], int count)
	{
		Round round;
		if(count==3)
		{
			round=new Round(1, 3);
		}
		else
		{
			round=new Round(2, 5);
		}
		
		for(int i=0;i<count;i++)
		{
			round.getRound_hand()[i].copy(cards[i]);
		}
		round.determine_hand_strength();
		return round.getHand_strength();
	}
	
	private static void mark_used(Card hand[], boolean used[], Card selected[])
	{
		for(Card selected_card:selected)
		{
			for(int i=0;i<hand.length;i++)
			{
				if(!used[i] && hand[i].getNumber()==selected_card.getNumber())
				{
					used[i]=true;
					break;
				}
			}
		}
	}
	
	private static void put_round(Player player, int choice[], int round_index, Card cards[])
	{
		for(int i=0;i<cards.length;i++)
		{
			player.getRounds()[round_index].getRound_hand()[i].copy(cards[i]);
			choice[cards[i].getNumber()]=round_index+1;
		}
	}
}
