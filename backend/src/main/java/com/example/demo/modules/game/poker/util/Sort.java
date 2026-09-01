package com.example.demo.modules.game.poker.util;

import com.example.demo.modules.game.poker.model.Card;
public class Sort
{
	public static void sort_depends_on_number (Card[] cards)	// 根據牌的編號進行升序排序
	{
		boolean is_sorted;
		Card card_tmp=new Card();
		do
		{
			is_sorted=true;
			for(int i=0;i<cards.length-1;i++)
			{
				if(cards[i].getNumber()>cards[i+1].getNumber())
				{
					card_tmp.copy(cards[i]);
					cards[i].copy(cards[i+1]);
					cards[i+1].copy(card_tmp);
					is_sorted=false;
				}
			}
		}while(!is_sorted);
	}
	public static void sort_depends_on_point_increasing (Card[] cards)	// 根據牌的點數進行升序排序
	{
		boolean is_sorted;
		Card card_tmp=new Card();
		do
		{
			is_sorted=true;
			for(int i=0;i<cards.length-1;i++)
			{
				if(cards[i].getPoint()>cards[i+1].getPoint())
				{
					card_tmp.copy(cards[i]);
					cards[i].copy(cards[i+1]);
					cards[i+1].copy(card_tmp);
					is_sorted=false;
				}
			}
		}while(!is_sorted);
	}
	public static void sort_depends_on_point_decreasing (Card[] cards)	// 根據牌的點數進行降序排序
	{
		boolean is_sorted;
		Card card_tmp=new Card();
		do
		{
			is_sorted=true;
			for(int i=0;i<cards.length-1;i++)
			{
				if(cards[i].getPoint()<cards[i+1].getPoint())
				{
					card_tmp.copy(cards[i]);
					cards[i].copy(cards[i+1]);
					cards[i+1].copy(card_tmp);
					is_sorted=false;
				}
			}
		}while(!is_sorted);
	}
	public static void sort_depends_on_point_A14_increasing (Card[] cards)	// 根據牌的點數(A=14)進行升序排序
	{
		boolean is_sorted;
		Card card_tmp=new Card();
		do
		{
			is_sorted=true;
			for(int i=0;i<cards.length-1;i++)
			{
				if(cards[i].getPoint_A14()>cards[i+1].getPoint_A14())
				{
					card_tmp.copy(cards[i]);
					cards[i].copy(cards[i+1]);
					cards[i+1].copy(card_tmp);
					is_sorted=false;
				}
			}
		}while(!is_sorted);
	}
	public static void sort_depends_on_point_A14_decreasing (Card[] cards)	// 根據牌的點數(A=14)進行降序排序
	{
		boolean is_sorted;
		Card card_tmp=new Card();
		do
		{
			is_sorted=true;
			for(int i=0;i<cards.length-1;i++)
			{
				if(cards[i].getPoint_A14()<cards[i+1].getPoint_A14())
				{
					card_tmp.copy(cards[i]);
					cards[i].copy(cards[i+1]);
					cards[i+1].copy(card_tmp);
					is_sorted=false;
				}
			}
		}while(!is_sorted);
	}
	public static void sort_depends_on_suit_decreasing (Card[] cards)	// 根據花色強度降序排序
	{
		boolean is_sorted;
		Card card_tmp=new Card();
		do
		{
			is_sorted=true;
			for(int i=0;i<cards.length-1;i++)
			{
				if(cards[i].getSuits_strength()<cards[i+1].getSuits_strength())
				{
					card_tmp.copy(cards[i]);
					cards[i].copy(cards[i+1]);
					cards[i+1].copy(card_tmp);
					is_sorted=false;
				}
			}
		}while(!is_sorted);
	}
	public static void sort_depends_on_duplicate_number_decreasing (Card[] cards)	// 根據重複次數降序排序
	{
//		System.out.println("start to sort_depends_on_duplicate_number_decreasing!");
		boolean is_sorted;
		Card card_tmp=new Card();
		do
		{
			is_sorted=true;
			for(int i=0;i<cards.length-1;i++)
			{
				if(cards[i].getDuplicate_number()<cards[i+1].getDuplicate_number())
				{
					card_tmp.copy(cards[i]);
					cards[i].copy(cards[i+1]);
					cards[i+1].copy(card_tmp);
					is_sorted=false;
//					System.out.println("sorting");
				}
			}
		}while(!is_sorted);
//		System.out.println("sorted!");
	}
}
 
