package com.example.demo.modules.game.tjpoker.model;
public class Card{
	private String name="";				// 牌的名稱 ♠Q
	private String suits="";			// 花色		♠
	private int suits_strength=-1;		// 花色大小
	private int point=-1;				// 牌的點數 1~13
	private int point_A14=-1;			// 牌的點數(A視為14，比其他牌大) 2~14
	private int number=-1;				// 牌在新牌中的位置 0=黑桃A 51=梅花K
	private int player=-1;				// 在哪個玩家手上1=P1 2=P2 0=deck
	private int position=-1;			// 在牌堆中的所處位置 0=牌堆頂 51=牌堆底
	private int duplicate_number=0;		// 重複次數 0=在所處牌型中沒有其他跟自己點數相同的牌 3=有其他三張一樣點數的牌，也就是鐵支
	
	
	
	public Card()
	{
	}
	public Card(String suits, int point, int number) {
//		super();
		this.suits = suits;			
		this.point = point;
		this.number = number;
		if (point==1) point_A14=14;
		else point_A14=point;
		if(point==1) name=suits+"A";
		else if(point==11)	name=suits+"J";
		else if(point==12)	name=suits+"Q";
		else if(point==13)	name=suits+"K";
		else if(2<=point&&point<=10)	name=suits+point;
		switch(suits)
		{
			case "♠":
				suits_strength=3;
				break;
			case "♡":
				suits_strength=2;
				break;
			case "♢":
				suits_strength=1;
				break;
			case "♣":
				suits_strength=0;
				break;
			default:
				suits_strength=-2;
				System.out.println("Error，suits_strength");
		}
	}
	public void copy(Card card)
	{
		this.name=card.getName();
		this.suits=card.getSuits();
		this.suits_strength=card.getSuits_strength();
		this.point=card.getPoint();
		this.point_A14=card.getPoint_A14();
		this.number=card.getNumber();
		this.player=card.getPlayer();
		this.position=card.getPosition();
		this.duplicate_number=card.getDuplicate_number();
	}
	// get/set
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getSuits() {
		return suits;
	}
	public void setSuits(String suits) {
		this.suits = suits;
	}
	
	public int getPoint() {
		return point;
	}
	public void setPoint(int point) {
		this.point = point;
	}
	public int getNumber() {
		return number;
	}
	public void setNumber(int number) {
		this.number = number;
	}
	public int getPlayer() {
		return player;
	}
	public void setPlayer(int player) {
		this.player = player;
	}
	public int getPosition() {
		return position;
	}
	public void setPosition(int position) {
		this.position = position;
	}
	public int getSuits_strength() {
		return suits_strength;
	}
	public void setSuits_strength(int suits_strength) {
		this.suits_strength = suits_strength;
	}
	public int getPoint_A14() {
		return point_A14;
	}
	public void setPoint_A14(int point_A14) {
		this.point_A14 = point_A14;
	}
	public int getDuplicate_number() {
		return duplicate_number;
	}
	public void setDuplicate_number(int duplicate_number) {
		this.duplicate_number = duplicate_number;
	}
}
