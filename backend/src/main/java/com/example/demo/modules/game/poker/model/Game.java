package com.example.demo.modules.game.poker.model;
public class Game {
	private Card[] deck=new Card[52]; 		// 生成牌堆
	private Player[] player=new Player[2];	// 生成玩家陣列
	private int number=0;					// 這是第幾局
	private static int counter=0;			// 總共玩了幾局
	private static int game_index=0;		// 這局遊戲的編號
	private static Round rounds_tmp[]=new Round[3];	// 暫存玩家的選牌
	private final static String RULE=""
			+ "規則:\n"
			+ "\t從15張手牌中選3/5/5張作為第1/2/3輪的出牌(兩張棄牌)，然後依序比牌型大小\n"
			+ "\t五張牌牌型:\t同花順>鐵支>葫蘆>同花>順子>三條>兩對>一對>高牌\n"
			+ "\t三張牌牌型:\t三條>一對>高牌\n"
			+ "遊戲流程:\n"
			+ "\t遊戲開始階段>玩家一準備階段>玩家一選擇手牌階段>玩家一預覽出牌階段>\n"
			+ "\t玩家二準備階段>玩家二選擇手牌階段>玩家二預覽出牌階段>\n"
			+ "\t準備公布結果階段>結果公布階段";
	private static int stage=0;	// 遊戲階段/流程
	//
	// 建構式
	public Game()
	{
		// 打開一副新的撲克牌
		String suits;
		for (int i=0;i<4;i++)
		{
			for(int j=0;j<13;j++)
			{
				switch(i)
				{
					case 0:
						suits ="♠";
						break;
					case 1:
						suits ="♡";
						break;
					case 2:
						suits ="♢";
						break;
					case 3:
						suits ="♣";
						break;
					default:
						suits="error";
				}
				deck[i*13+j]=new Card(suits, j+1, i*13+j);
			}
			
		}
		// 招待2名玩家入座
		player[0]=new Player();
		player[1]=new Player();
		player[0].setName("玩家一");
		player[1].setName("玩家二");
		
		game_index=counter;	// 設定本局遊戲的編號
		counter++;			// 總局數+1
		number=counter; 	// 當前局數=總局數
		
		shuffle_02();		// 洗牌(方法2)
		deal();				// 發牌
		
		// 選牌暫存初始化
		rounds_tmp[0]=new Round(1, 3);
		rounds_tmp[1]=new Round(2, 5);
		rounds_tmp[2]=new Round(3, 5);
	}
	// 洗牌(法1，已廢棄) top
	// 估計有約10的-13次方左右的機率，會出現洗牌不完全均勻的狀況
	// 存在有兩個隨機數恰好相等而互相不會交換位置，因而整副牌會有兩張相鄰的牌少交換一次
	// 大於兩個重複的情況也有，估算下來整體約10的-13次方的機率
	void shuffle_01()
	{
		// 生成0~51的均勻隨機數列
		// 生成原始隨機數列
		double random_array[]=new double[52];
		for(int i=0;i<52;i++)
		{
			random_array[i]=Math.random();
		}
//		// 生成0~51，隨機排序數列 // 不需要了
//		int sorted_array[]=new int[52];
//		for(int i=0;i<52;i++)
//		{
//			sorted_array[i]=i;
//		}		
		double random_array_tmp;		// 原始隨機數列的暫存用變數
//		int sorted_array_tmp;			// 隨機排序數列的暫存變數
		Card card_tmp=new Card();		// 卡片暫存
		boolean is_sorted; 	// 是否排序完成
		// 排序
		do
		{
			is_sorted=true;
			for(int i=0;i<51;i++) // 選擇的數與下個數比較決定要不要交換，由大至小
			{
				if (random_array[i]<random_array[i+1])
				{
					random_array_tmp=random_array[i]; 	// 暫存原始隨機數列第i個
//					sorted_array_tmp=sorted_array[i];
//					card_tmp=deck[i];					// 暫存牌堆中編號i的牌 // 卡片給卡片屬於址傳址
					card_tmp.copy(deck[i]);				// 暫存牌堆中編號i的牌
					random_array[i]=random_array[i+1];	// 原始隨機數列中把i+1的值給i
//					sorted_array[i]=sorted_array[i+1];
//					deck[i]=deck[i+1];					// 牌堆中編號i+1的牌的資料給編號i的牌 // 卡片給卡片屬於址傳址
					deck[i].copy(deck[i+1]);
					random_array[i+1]=random_array_tmp;	// 把暫存的數字給原始隨機數列中的第i+1個
//					sorted_array[i+1]=sorted_array_tmp;
//					deck[i+1]=card_tmp;					// 把暫存的牌給編號i+1的牌 // 卡片給卡片屬於址傳址
					deck[i+1].copy(card_tmp); 			// 把暫存的牌的資料給牌堆中編號i+1的牌
					// 完成一次交換
					is_sorted=false;
				}
			}
		}while(!is_sorted);
		
		// 把均勻隨機排序好的數列內的值給牌堆裡的牌
		for(int i=0;i<52;i++)
		{
//			deck[i].setPosition(sorted_array[i]); // 不需要了
			deck[i].setPosition(i);
		}
	}
	// 洗牌(法1) end
	// 洗牌(法2) top
	void shuffle_02()
	{
		int random_number;	// 隨機0~51-i的整數
		int step;			// 剩餘步數
		int current;		// 當前位置
		int random_array[]=new int[52];	// 隨機數陣列，0~51的整數順序隨機
		for(int i=0;i<random_array.length;i++) random_array[i]=-1; // 隨機數陣列內全給定初始值-1
		boolean is_placed;	// 第i個數字是否放置完成
		// 開始生成隨機數陣列
		for(int i=0;i<deck.length;i++)
		{
			current=0;			// 將當前位置重置到0
			is_placed=false;	// 數字i尚未放置完成
			random_number=(int)(Math.random()*(52-i));	// 生成隨機數0~51-i
			step=random_number;	// 把得到的隨機數當作步數
			while(!is_placed)	// 如果數字i還沒放好就繼續做
			{
				if(random_array[current]==-1)	// 如果隨機數陣列中當前位置裡是-1，說明這裡還是空的可以放置數字i
				{
					if(step==0)	// 步數為0(腳下的空間又是空的)說明抵達終點
					{
						random_array[current]=i;	// 放下手中的數字，丟進腳下的空間
						is_placed=true;				// 數字i放置完成
					}
					else if(step>0)					// 步數還沒用完
					{
						current+=1;					// 往前走一步
						step-=1;					// 剩餘步數-1
					}
					else
						System.out.println("Error: shuffle_02-step");	// 步數異常
				}
				else current+=1;			// 往前走一步(剩餘步數不減)
			}
		}
		// 隨機數陣列完成
		Card card_tmp=new Card();	// 暫存卡片
		int random_array_tmp;		// 隨機數列暫存數
		boolean is_sorted=false;
		while(!is_sorted)
		{
			is_sorted=true;
			for(int i=0;i<deck.length-1;i++)	// i=0~50
			{
				if(random_array[i]>random_array[i+1])	// 隨機數列中i位置內的數字比下一個大
				{
					// 就跟下一個做交換。同時，牌堆相應位置的卡片也同步作交換
					random_array_tmp=random_array[i];
					card_tmp.copy(deck[i]);
					random_array[i]=random_array[i+1];
					deck[i].copy(deck[i+1]);
					random_array[i+1]=random_array_tmp;
					deck[i+1].copy(card_tmp);
					is_sorted=false;
				}
			}
		}
		for(int i=0;i<deck.length;i++)
		{
			deck[i].setPosition(i);	// 設置卡牌當前位置
		}
	}
	// 洗牌(法2) end
	// 發牌 top
	public void  deal()
	{
		for(int i=0;i<30;i++)
		{
			if(i==i>>1<<1) // i是偶數
			{
				player[0].getHand()[i/2].copy(deck[i]);
			}
			else if(i!=i>>1<<1)
			{
				player[1].getHand()[i/2].copy(deck[i]);
			}
			else
				System.out.println("Error!!!");
		}
	}
	// 發牌 end
	// 階段提示訊息 top
	public String stage_message(int stage)
	{
		switch(stage)
		{
			case 0:
				return "遊戲開始階段\n"+
						"請按開始遊戲按鈕\n";
			case 1:
				return "遊戲準備階段\n"+
						player[0].getName()+"準備選牌\n"+
						"請"+player[1].getName()+"暫時迴避\n"+
						player[0].getName()+"準備好後請按確認鍵\n";
			case 2:
				return "選擇手牌階段\n"+
						"三個回合分別需要選擇3/5/5張牌\n"+
						"手牌選擇完畢請按確認鍵\n";
			case 3:
				return "預覽出牌階段\n"+
						player[0].getName()+"的出牌:\n"+
						"第一回合:\n"+
						"\t"+rounds_tmp[0].getHand_type()+"\n"+
						"\t"+rounds_tmp[0].show_round_hand()+"\n"+
						"第二回合:\n"+
						"\t"+rounds_tmp[1].getHand_type()+"\n"+
						"\t"+rounds_tmp[1].show_round_hand()+"\n"+
						"第三回合:\n"+
						"\t"+rounds_tmp[2].getHand_type()+"\n"+
						"\t"+rounds_tmp[2].show_round_hand()+"\n"+
						"顯示的牌若無問題請按確認鍵\n"+
						"否則請按取消以重新選牌\n";
			case 4:
				return "換人準備階段\n"+
						player[0].getName()+"選牌完畢\n"+
						"請"+player[1].getName()+"準備好後按下確認鍵\n";
			case 5:
				return "選擇手牌階段\n"+
						"三個回合分別需要選擇3/5/5張牌\n"+
						"手牌選擇完畢請按確認鍵\n";
			case 6:
				return "預覽出牌階段\n"+
						player[1].getName()+"的出牌:\n"+
						"第一回合:\n"+
						"\t"+rounds_tmp[0].getHand_type()+"\n"+
						"\t"+rounds_tmp[0].show_round_hand()+"\n"+
						"第二回合:\n"+
						"\t"+rounds_tmp[1].getHand_type()+"\n"+
						"\t"+rounds_tmp[1].show_round_hand()+"\n"+
						"第三回合:\n"+
						"\t"+rounds_tmp[2].getHand_type()+"\n"+
						"\t"+rounds_tmp[2].show_round_hand()+"\n"+
						"顯示的牌若無問題請按確認鍵\n"+
						"否則請按取消以重新選牌\n";
			case 7:
				return "準備公布結果階段\n"+
						"請兩位玩家都準備好後按下確認鍵以觀看遊戲結果\n";
			case 8:
				return "結果公布階段\n"+
						"勝者:\t"+player[determine_winner()].getName()+"\n"+
						player[0].getName()+"\t\t\t"+player[1].getName()+"\n"+
						"第一回合:    "+player[determine_round_winner(0)].getName()+"勝\n"+
						player[0].getRounds()[0].getHand_type()+"\t\t\t"+player[1].getRounds()[0].getHand_type()+"\n"+
						player[0].getRounds()[0].show_round_hand()+"\t\t"+player[1].getRounds()[0].show_round_hand()+"\n"+
						"第二回合:    "+player[determine_round_winner(1)].getName()+"勝\n"+
						player[0].getRounds()[1].getHand_type()+"\t\t\t"+player[1].getRounds()[1].getHand_type()+"\n"+
						player[0].getRounds()[1].show_round_hand()+"\t"+player[1].getRounds()[1].show_round_hand()+"\n"+
						"第三回合:    "+player[determine_round_winner(2)].getName()+"勝\n"+
						player[0].getRounds()[2].getHand_type()+"\t\t\t"+player[1].getRounds()[2].getHand_type()+"\n"+
						player[0].getRounds()[2].show_round_hand()+"\t"+player[1].getRounds()[2].show_round_hand()+"\n"+
						"";
			default:
				return "遊戲階段異常";
		}
	}
	// 階段提示訊息 end
	// 計算勝負 top
	public int determine_winner()
	{
		int player_0_wins=0;
		int player_1_wins=0;
		for(int i=0;i<player[0].getRounds().length;i++)
		{
			if(player[0].getRounds()[i].getHand_strength()>player[1].getRounds()[i].getHand_strength())
				player_0_wins+=1;
			else if(player[0].getRounds()[i].getHand_strength()<player[1].getRounds()[i].getHand_strength())
				player_1_wins+=1;
			else
				System.out.println("Error! determine_winner wins");
		}
		if(player_0_wins>player_1_wins)
			return 0;
		else if(player_0_wins<player_1_wins)
			return 1;
		else return -1;
	}
	// 計算勝負 end

	// 計算單回合勝負 top
	public int determine_round_winner(int round)
	{
		if(player[0].getRounds()[round].getHand_strength()>player[1].getRounds()[round].getHand_strength())
			return 0;
		else if(player[0].getRounds()[round].getHand_strength()<player[1].getRounds()[round].getHand_strength())
			return 1;
		else
		{
			System.out.println("Error determine_round_winner");
			return -1;
		}
	}
	// 計算單回合勝負 end
	
	// set/get
	public Card[] getDeck() {
		return deck;
	}
	public void setDeck(Card[] deck) {
		this.deck = deck;
	}
	public Player[] getPlayer() {
		return player;
	}
	public void setPlayer(Player[] player) {
		this.player = player;
	}
	public int getNumber() {
		return number;
	}
	public void setNumber(int number) {
		this.number = number;
	}
	public static int getCounter() {
		return counter;
	}
	public static void setCounter(int counter) {
		Game.counter = counter;
	}
	public static String getRULE() {
		return RULE;
	}
	public static int getGame_index() {
		return game_index;
	}
	public static void setGame_index(int game_index) {
		Game.game_index = game_index;
	}
	public static Round[] getRounds_tmp() {
		return rounds_tmp;
	}
	public static void setRounds_tmp(Round[] rounds_tmp) {
		Game.rounds_tmp = rounds_tmp;
	}
	public static int getStage() {
		return stage;
	}
	public static void setStage(int stage) {
		Game.stage = stage;
	}
	
	
}
