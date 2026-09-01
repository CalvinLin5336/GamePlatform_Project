package com.example.demo.modules.game.poker.model;

import com.example.demo.modules.game.poker.util.Sort;
public class Round {
	private int  round_number;		// 這是第幾輪
	private Card[] round_hand; 		// 本輪出牌
	private String hand_type;		// 牌型名稱
	private int hand_type_number=-1;	// 牌型編號 0=高牌 
	private long hand_strength;		// 牌型強度
//	private int duplicate_value; 	// 牌型特徵值
	// game1.getPlayer()[0].getRound()[1].getRound_hand()[i]
	// 牌型筆記
	/*
	 reference by Gemini
	 同花順 (Straight Flush)
	 鐵支 (Four of a Kind)
	 葫蘆 (Full House)
	 同花 (Flush)
	 順子 (Straight)
	 三條 (Three of a Kind)
	 兩對 (Two Pairs)
	 一對 (One Pair)
	 高牌 (High Card)
	*/
	// 判斷邏輯
	/*
	 duplicate_value	牌型特徵值
	 hand_strength		牌型大小
	 同花順	8+第二張牌點數(2 digit)+最大牌花色值(1 digit)
	 鐵支	7+鐵支1號代表點數(2 digit)
	 葫蘆	6+葫蘆1號代表點數(2 digit)
	 同花	5+由大到小的5個點數(共10digit)+花色(1 digit)
	 順子	4+第二張牌點數(2 digit)+最大牌花色值(1 digit)
	 三條	3+三條1號代表數(2 digit)
	 兩對	2+兩對1號代表數(2 digit)+兩對2號代表數(2digit)+高牌點數(2 dgit)
	 一對	1+一對
	 高牌	0+
	*/
//	Round(){}
	public Round(int round_number, int hand_number)
	{
		round_hand=new Card[hand_number];
		this.round_number=round_number;
		for(int i=0;i<hand_number;i++)
		{
			round_hand[i]=new Card();
		}
	}
	// 判斷牌型 top
	public void determine_hand()
	{
		Sort.sort_depends_on_point_increasing(round_hand);
		int duplicate_value=0;		// 牌型特徵值
		for(Card card:round_hand)	// 重複次數歸0
		{
			card.setDuplicate_number(0);
		}
		for(int i=0;i<round_hand.length;i++) // 計算牌型特徵值
		{
			for(int j=0;j<round_hand.length;j++)
			{
				if(i!=j)
				{
					if(round_hand[i].getPoint()==round_hand[j].getPoint())
					{
						duplicate_value++;
						round_hand[i].setDuplicate_number(round_hand[i].getDuplicate_number()+1);
//						System.out.println(round_hand[i].getName()+"\t"+round_hand[i].getDuplicate_number());	// 測試運行是否正常
					}
				}
			}
		}
		switch(duplicate_value)	// 根據牌型特徵值進行操作
		{
			case 0:
				hand_type="高牌";
				hand_type_number=0;
				break;
			case 2:
				hand_type="一對";
				hand_type_number=1;
				break;
			case 4:
				hand_type="兩對";
				hand_type_number=2;
				break;
			case 6:
				hand_type="三條";
				hand_type_number=3;
				break;
			case 8:
				hand_type="葫蘆";
				hand_type_number=6;
				break;
			case 12:
				hand_type="鐵支";
				hand_type_number=7;
				break;
			default:	// debug用
				hand_type="牌型特徵值異常";
				System.out.println("牌型特徵值異常");
		}
		if(hand_type_number==0)	// 如果前面判定是高牌，其中有可能是同花/順子，這邊接著判定
		{
			if(round_hand.length==5)	// 如果有五張牌才判定，因為三張牌的牌型沒有同花跟順子
			{
				// 判定同花
				boolean is_flush=true;	// 預設"是同花"，不是的再改成"不是同花"
				for(int i=0;i<round_hand.length-1;i++)
				{	
					if(round_hand[i].getSuits()!=round_hand[i+1].getSuits()) is_flush=false;	// 如果有牌跟下一張牌花色不同，就不是同花
				}
				if(is_flush)	// 如果是同花，設定相關變數
				{
					hand_type="同花";
					hand_type_number=5;
				}
				// 判定順子
				boolean is_straight=true;	// 預設"是順子"，不是的再改成"不是順子"
				if(round_hand[0].getPoint()==1 && round_hand[1].getPoint()==10)	// 先判定較特別的10JQKA，第一張耀是A，第二張要是10，才執行
				{
					for(int i=1;i<=3;i++)
					{
						if(round_hand[i].getPoint()+1!=round_hand[i+1].getPoint()) is_straight=false;	// 如果(第2~4張牌中)有牌跟下一張牌的點數不是連號，就不是順子
					}
				}
				else	// 除了10JQKA的情況
				{
					for(int i=0;i<4;i++)
					{
						if(round_hand[i].getPoint()+1!=round_hand[i+1].getPoint()) is_straight=false;	// 如果有牌跟下一張牌的點數不是連號，就不是順子
					}
				}
				if(is_straight)	// 如果是順子，設定相關變數
				{
					hand_type="順子";
					hand_type_number=4;
				}
				if(is_flush && is_straight)	// 是同花又是順子 那就是同花順
				{
					hand_type="同花順";
					hand_type_number=8;
				}
			}
		}
	}
	// 判斷牌型 end
	// 計算牌型強度 top
	public void determine_hand_strength() // 計算手牌強度
	{
		hand_strength=0;
		determine_hand();
		hand_strength=hand_type_number*(long)Math.pow(10, 11);
		switch(hand_type_number)
		{
			case 0:	// 高牌
				Sort.sort_depends_on_point_A14_decreasing(round_hand);
				for(int i=0;i<round_hand.length;i++)
				{
					hand_strength+=round_hand[i].getPoint_A14()*(long)Math.pow(10, 9-i*2);
				}
				hand_strength+=round_hand[0].getSuits_strength();
				break;
			case 1: // 一對
				Sort.sort_depends_on_suit_decreasing(round_hand);
				Sort.sort_depends_on_point_A14_decreasing(round_hand);
				Sort.sort_depends_on_duplicate_number_decreasing(round_hand);
				hand_strength+=round_hand[0].getPoint_A14()*(long)Math.pow(10, 9);
				for(int i=2;i<round_hand.length;i++)
				{
					hand_strength+=round_hand[i].getPoint_A14()*(long)Math.pow(10, 11-i*2);
				}
				hand_strength+=round_hand[0].getSuits_strength();
				break;
			case 2: // 兩對
				Sort.sort_depends_on_suit_decreasing(round_hand);
				Sort.sort_depends_on_point_A14_decreasing(round_hand);
				Sort.sort_depends_on_duplicate_number_decreasing(round_hand);
				hand_strength+=round_hand[0].getPoint_A14()*(long)Math.pow(10, 9);
				hand_strength+=round_hand[2].getPoint_A14()*(long)Math.pow(10, 7);
				hand_strength+=round_hand[4].getPoint_A14()*(long)Math.pow(10, 5);
				hand_strength+=round_hand[0].getSuits_strength();
				break;
			case 3: // 三條
				Sort.sort_depends_on_suit_decreasing(round_hand);
				Sort.sort_depends_on_point_A14_decreasing(round_hand);
				Sort.sort_depends_on_duplicate_number_decreasing(round_hand);
				hand_strength+=round_hand[0].getPoint_A14()*(long)Math.pow(10, 9);
				break;
			case 4: // 順子
				Sort.sort_depends_on_point_increasing(round_hand);
				if(round_hand[0].getPoint()==1 && round_hand[1].getPoint()==10)
				{
					hand_strength+=(round_hand[1].getPoint_A14()+1)*(long)Math.pow(10, 9);
					hand_strength+=round_hand[0].getSuits_strength();
				}
				else
				{
					hand_strength+=round_hand[1].getPoint_A14()*(long)Math.pow(10, 9);
					hand_strength+=round_hand[4].getSuits_strength();
				}
				break;
			case 5:	// 同花
				Sort.sort_depends_on_point_A14_decreasing(round_hand);
				for(int i=0;i<round_hand.length;i++)
				{
					hand_strength+=round_hand[i].getPoint_A14()*(long)Math.pow(10, 9-i*2);
				}
				hand_strength+=round_hand[0].getSuits_strength();
				break;
			case 6:	// 葫蘆
				Sort.sort_depends_on_suit_decreasing(round_hand);
				Sort.sort_depends_on_duplicate_number_decreasing(round_hand);
				hand_strength+=round_hand[0].getPoint_A14()*(long)Math.pow(10, 9);
				break;
			case 7:	// 鐵支
				Sort.sort_depends_on_suit_decreasing(round_hand);
				Sort.sort_depends_on_duplicate_number_decreasing(round_hand);
				hand_strength+=round_hand[0].getPoint_A14()*(long)Math.pow(10, 9);
				break;
			case 8:	// 同花順
				Sort.sort_depends_on_point_increasing(round_hand);
				if(round_hand[0].getPoint()==1 && round_hand[1].getPoint()==10)
					hand_strength+=(round_hand[1].getPoint_A14()+1)*(long)Math.pow(10, 9);
				else
					hand_strength+=round_hand[1].getPoint_A14()*(long)Math.pow(10, 9);
				hand_strength+=round_hand[0].getSuits_strength();
				break;
		}
	}
	// 計算牌型強度 end
	// 新增手牌
	// 
	
	public Card[] getRound_hand() {
		return round_hand;
	}
	public void setRound_hand(Card[] round_hand) {
		this.round_hand = round_hand;
	}
	public int getRound_number() {
		return round_number;
	}
	public void setRound_number(int round_number) {
		this.round_number = round_number;
	}
	public String getHand_type() {
		return hand_type;
	}
	public void setHand_type(String hand_type) {
		this.hand_type = hand_type;
	}
	public int getHand_type_number() {
		return hand_type_number;
	}
	public void setHand_type_number(int hand_type_number) {
		this.hand_type_number = hand_type_number;
	}
	public long getHand_strength() {
		return hand_strength;
	}
	public void setHand_strength(long hand_strength) {
		this.hand_strength = hand_strength;
	}
	public String show_round_hand()
	{
		String round_hand_names="";
		for(Card cards:round_hand)
			round_hand_names+=cards.getName()+" ";
		return round_hand_names; 
	}
	
}
