package com.example.demo.modules.game.tjpoker.model;

public class Player {
	private String name;
	private Card hand[]=new Card[15];
	private Round rounds[]=new Round[3];
	
	public Player()
	{
		super();
		for(int i=0;i<hand.length;i++)
		{
			hand[i]=new Card();
		}
		rounds[0]=new Round(1, 3);
		rounds[1]=new Round(2, 5);
		rounds[2]=new Round(3, 5);
		
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Card[] getHand() {
		return hand;
	}
	public void setHand(Card[] hand) {
		this.hand = hand;
	}
	public Round[] getRounds() {
		return rounds;
	}
	public void setRounds(Round[] rounds) {
		this.rounds = rounds;
	}
	
	

}
