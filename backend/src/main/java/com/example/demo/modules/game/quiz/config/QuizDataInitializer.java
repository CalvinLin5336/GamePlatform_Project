package com.example.demo.modules.game.quiz.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.demo.modules.game.quiz.model.Option;
import com.example.demo.modules.game.quiz.model.Player;
import com.example.demo.modules.game.quiz.model.Question;
import com.example.demo.modules.game.quiz.repository.PlayerRepository;
import com.example.demo.modules.game.quiz.repository.QuestionRepository;


/*
 * 問答遊戲(Quiz)初始化資料建立器
 * 實作 CommandLineRunner，在Spring Boot 啟動時自動檢查並寫入預設玩家與20題試卷題庫
 * */
@Component
public class QuizDataInitializer implements CommandLineRunner {
	
	@Autowired
	private PlayerRepository playerRepository;
	@Autowired
	private QuestionRepository questionRepository;
	
	@Override
	public void run(String... args) throws Exception {
		// TODO Auto-generated method stub
		
		//1.初始化排行榜預設玩家資料(滿分 100為基準)
		createPlayerIfMissing("PlayerOne", 100);
        createPlayerIfMissing("QuizMaster", 90);
        createPlayerIfMissing("LuckyGuy", 75);
        
        //2. 強制清空就題目(依賴 Question 實體上的 Cascade 設置級聯刪除 Option)
        questionRepository.deleteAll();
        
        //3. 強制重新寫入 77題試卷題庫(含詳細解說與選項)
        initQuestionsIfEmpty(); 
		
	}
	/*
	 * 檢查特定玩家是否存在，若不存在才建立(確保重啟不重複寫入) 
	 * */
	private void createPlayerIfMissing(String username,int highScore) {
		// TODO Auto-generated method stub
		if(playerRepository.findByUsername(username).isPresent()) {
			return;
		}
		
		Player player = new Player();
		player.setUsername(username);
		player.setHighScore(highScore);
		playerRepository.save(player);		
	}
	/*
	 * 若資料庫沒有題目則建立 SQL 題庫內容
	 */
	private void initQuestionsIfEmpty() {
		/*
		if (questionRepository.count() > 0) {
			return;
		}*/

		// Q1
		createQuestion(
			"<p>How many of thes compile?</p><p><br></p><p>18:Comparator&lt;String&gt; c1=(j,k)-&gt;0;</p><p>19:Comparator&lt;String&gt; c2=(String j,String k)-&gt;0;</p><p>20:Comparator&lt;String&gt; c3=(var j,String k)-&gt;0 ;</p><p>21:Comparator&lt;String&gt; c4=(var j,k)-&gt;0 ;</p><p>22:Comparator&lt;String&gt; c5=(var j, var k)-&gt;0;</p>",
			"<p>說明:</p><p>考卷ch1-ex1</p><p>這些有多少個可以編譯？</p><p>口訣:</p><p>Lambda 聯想【學生排隊穿制服】，String是制服、var是運動服，沒寫是便服，服飾部統一就會出錯</p><p>說明:Lambda 參數型別全寫、全省var:不能有人穿便服，有人穿制服</p><p>20:Comparator&lt;String&gt; c3=(var j,String k)-&gt;0 //一邊var，另一也要var</p><p>21:Comparator&lt;String&gt; c4=(var j,k)-&gt;0 //一邊省略，另一邊也要</p>",
			30,
			new String[]{"<p>A.0</p>", "<p>B.1</p>", "<p>C.2</p>", "<p>D.3</p>", "<p>E.4</p>", "<p>F.5</p>"},
			3
		);

		// Q2
		createQuestion(
			"<p>What is the output of the following application?</p><p><br></p><p>public class Airplane{</p><p>\tstatic int start=2;</p><p>\tfinal int end;</p><p>\tpublic Airplane(int x)</p><p>\t{</p><p>\t\tx=4;</p><p>\t\tend=x;</p><p>\t}</p><p><br></p><p>\tpublic void fly(int distance)</p><p>\t{</p><p>\t\tSystem.out.print(end-start + \" \");</p><p>\t\tSystem.out.print(distance);</p><p>\t}</p><p>}</p><p><br></p><p>public class ex2{</p><p>\tpublic static void main(String[] args){</p><p>\t\tnew Airplane(10).fly(5);</p><p>\t}</p><p>}</p>",
			"<p>說明:</p><p>考卷ch1-ex2</p><p>以下應用程式的輸出是什麼？</p><p>建構子參數X先被重新賦值為4，因此final 實例變數end 的值是4，而非創時傳入的10。靜態變數start為2，fly(5)先輸出end-start，也就是2，再輸出distance的值5</p><p>口訣:</p><p>X為快遞員，原本拿著【10】進門，進門後包裹換成【4】，最後交給end。</p><p>var只能住在方法裡，出生時一定要有值，var看右邊決定型別，決定後就不能變</p>",
			30,
			new String[]{"<p>A.2 5</p>", "<p>B.8 5</p>", "<p>C.6 5</p>", "<p>D.The code does not compile.</p>", "<p>E.None of the adove</p>"},
			0
		);

		// Q3
		createQuestion(
			"<p>Given the code fragment:</p><p><br></p><p>var i=10;</p><p>var j=5;</p><p>i+=(j*5+i)/j-2;</p><p>System.out.println(i);</p><p><br></p><p>What is the result?</p>",
			"<p>說明</p><p>考卷ch1-ex3</p><p>1.分步驟計算括號與右邊數值，計算括號內的最內層: j * 5 + i → ( 5 * 5 +10 = 25 + 10 = 35) 接著除以 j : 35 / j → 35/5=7 最後減去2 → 7-2 = 5</p><p>2.加回原來的i: 將算出來的結果5 加回原本的i (也就是10) 新 i = 10 + 5 → 得到15</p><p>公式:</p><p>i+=(j*5+i)/j-2; —&gt; i= 10+(5*5+i)/5-2 —&gt;10+7-2</p><p>口訣:</p><p>【加等於是先算再加，右邊算到底，左邊再加回去】</p>",
			15,
			new String[]{"<p>A)5</p>", "<p>B)11</p>", "<p>C)21</p>", "<p>D)23</p>", "<p>E)15</p>"},
			4
		);

		// Q4
		createQuestion(
			"<p>Given:</p><p>public class Tester{</p><p>\t\tpublic static void main(String[] args) {</p><p>\t\tStringBuilder sb = new StringBuilder(5); </p><p>\t\tsb.append(\"HOWDY\"); </p><p>\t\tsb.insert(0,' '); </p><p>\t\tsb.replace(3,5,\"LL\");</p><p>\t\tsb.insert(6,\"COW\"); </p><p>\t\tsb.delete(2,7);</p><p>\t\tSystem.out.println(sb.length());</p><p><br></p><p>\t}</p><p>}</p><p>what is the result?</p>",
			"<p>說明:</p><p>卷ch1-ex4</p><p>最後sb = “ HOW” 共四個位元長度</p><p><br></p><p>StringBuilder sb = new StringBuilder(5); </p><p>//建構一個初始容量為零的字串產生器，初始容量由capacity參數指定。</p><p>\t\t</p><p>\t\tsb.append(\"HOWDY\"); //HOWDY</p><p>\t\tsb.insert(0,' ');   // HOWDY，0的部分加空格</p><p>\t\tsb.replace(3,5,\"LL\");// HOLLY，3與5 換成LL</p><p>\t\tsb.insert(6,\"COW\"); // HOLLYCOW,6的部分換成COW</p><p>\t\tsb.delete(2,7);// HOW,去除小於7大於等於2</p><p>\t\tSystem.out.println(sb.length());</p><p>聯想:</p><p>把第二個數字想成【門口警衛】；第一個索引可以進去，第二個索引站在門口</p><p>口訣:</p><p>【前面進門，後面不進門】</p><p><br></p>",
			30,
			new String[]{"<p>A)5</p>", "<p>B)3</p>", "<p>C)An exception is thrown at runtime.</p>", "<p>D)4</p>"},
			3
		);

		// Q5
		createQuestion(
			"<p>Given:</p><p><br></p><p>public class StrBldr{</p><p>\tstatic StringBuilder sb1= new StringBuilder(\"yo \"); </p><p>\tStringBuilder sb2 = new StringBuilder(\"hi \");</p><p>\tpublic static void main(String[] args){</p><p>\tsb1 = sb1.append(new StrBldr().foo(new StringBuilder(\"hey\"))); </p><p>\t\tSystem.out.println(sb1);</p><p>\t\t}</p><p>\tStringBuilder foo(StringBuilder s){</p><p>\t\tSystem.out.print(s+\" oh \"+sb2);</p><p>\t\treturn new StringBuilder(\"ey\");</p><p>\t}</p><p>}</p><p><br></p><p>What is the result?</p>",
			"<p>說明:</p><p>卷ch1-ex5</p><p>下面程序輸出會是甚麼?</p><p>答案: D)hey oh hi yo ey</p><p>註解:</p><p>public class StrBldr{</p><p>\t//StringBuilder 建構一個初始容量為 16 個字元的空字串產生器</p><p>\tstatic StringBuilder sb1= new StringBuilder(\"yo \");</p><p>\t//第四 new StrBldr()\t </p><p>\tStringBuilder sb2 = new StringBuilder(\"hi \");</p><p>\t//第三 +sb2</p><p>\tpublic static void main(String[] args)</p><p>\t{</p><p>\t\tsb1 = sb1.append(new StrBldr().foo(new StringBuilder(\"hey\"))); \t\t</p><p>\t\t//第一 new StringBuilder(\"hey\")\t</p><p>\t\tSystem.out.println(sb1);</p><p>\t}</p><p>\tStringBuilder foo(StringBuilder s){</p><p>\t\tSystem.out.print(s+\" oh \"+sb2);//第二\ts+\" oh \"\t</p><p>\t\treturn new StringBuilder(\"ey\");//第五\tnew StringBuilder(\"ey\") </p><p>\t}</p><p>} </p><p><br></p><p>//hey oh hi yo ey</p><p><br></p><p>聯想:</p><p>如同禮物包裝，最裡面最先做好→ 再放入外層→最後印出</p><p>sb1（草莓）想要加上（append）內餡，但內餡（函式）現在還是空盒子，必須先打開。開盒子（執行函式）的過程中順便把廚房給炸了（先執行了裡面的 print），盒子打開發現裡面的藍莓(”ey”)；最後草莓才加上藍莓，變成草莓藍莓蛋糕(”yoey”)</p><p>口訣:</p><p>【括號最深先執行，從裡拆到外】</p>",
			30,
			new String[]{"<p>A)oh hi hey</p>", "<p>B)hey oh hi</p>", "<p>C)A compile time error occurs.</p>", "<p>D)hey oh hi yo ey</p>", "<p>E)yo ey</p>", "<p>F)hey oh hi ey</p>"},
			3
		);

		// Q6
		createQuestion(
			"<p>Given:</p><p><br></p><p>public class Test{</p><p>\tpublic void process(byte v){ </p><p>\t\tSystem.out.println(\"Byte value \"+ v);</p><p>\t}</p><p>\tpublic void process(short v){ </p><p>\t\tSystem.out.println(\"Short value\"+v);</p><p>\t}</p><p>\tpublic void process(Object v) </p><p>\t{</p><p>\t\tSystem.out.println(\"Object value\"+v);</p><p>\t}</p><p><br></p><p>\tpublic static void main(String[] args) {</p><p>\t\t byte x=12;</p><p>\t\t short y=13;\t</p><p>\t   new Test().process(x+y); //line1</p><p>\t}</p><p>}</p><p><br></p><p>What is the output?</p>",
			"<p>說明:</p><p>卷ch1-ex6</p><p>下面程序輸出會是什麼?</p><p>A)Object value 25</p><p>程式註解:</p><p>public class ex6{</p><p>\tpublic void process(byte v){ //int 塞不進去，類型太小</p><p>\t\tSystem.out.println(\"Byte value \"+ v);</p><p>\t}</p><p>\tpublic void process(short v){ //int 塞不進去，類型太小</p><p>\t\tSystem.out.println(\"Short value\"+v);</p><p>\t}</p><p>\tpublic void process(Object v) //所有的父類別，可塞</p><p>\t{</p><p>\t\tSystem.out.println(\"Object value\"+v);</p><p>\t}</p><p><br></p><p>\tpublic static void main(String[] args) {</p><p>\t\tbyte x=12;</p><p>\t\tshort y=13;</p><p>\t\t//promotion</p><p>\t\t//int z = x+y; //x與 y被強制轉為INT</p><p>\t\tnew ex6().process(x+y);</p><p>\t}</p><p>}</p><p>口訣:</p><p>【小三一算，全進int】</p><p>諧音技法:小三碰到加減乘除，就【進】級，【進】聯想到int，</p><p><br></p>",
			30,
			new String[]{"<p>A)Object value 25</p>", "<p>B)Byte value 25</p>", "<p>C)Short value 25</p>", "<p>D)The compilation fails due to an error in line 1</p>"},
			0
		);

		// Q7 (複選)
		createQuestionWithFlags(
			"<p>Variables declared as which of the following are never permitted in a switch statement?(Choose two)</p>",
			"<p>說明:</p><p>卷ch2-ex1</p><p>哪一個在宣告變數時是不允許的，跟switch搭配?</p><p>只有int、var、String、char 能搭配switch</p><p>口訣:</p><p>【Switch想成想型分類機，只能接受容易分格內容】</p><p>小整數、字串、enum</p><p>不接受:long、float、double、boolean、Object諧音口訣</p><p>【小字枚進門，長浮雙布物退貨】</p><p>小=byte、Short、char、int</p><p>字=String</p><p>枚=enum</p><p>=⇒進門</p><p>長=long</p><p>浮-float</p><p>雙=double</p><p>布-boolean</p><p>物=Object</p><p>=⇒退貨</p>",
			30,
			new String[]{"<p>A.var</p>", "<p>B.double</p>", "<p>C.int</p>", "<p>D.String</p>", "<p>E.char</p>", "<p>F.Object</p>"},
			new boolean[]{false, true, false, false, false, true}
		);

		// Q8
		createQuestion(
			"<p>What is the output of the following application?</p><p><br></p><p>package planning;</p><p>  public class ThePlan{</p><p>    var plan=1;  </p><p>    plan= plan++ + -- plan;</p><p>    if(plan==1){</p><p>        System.out.print(\"Plan A\");</p><p>    }else{</p><p>     if(plan==2) System.out.print(\"Plan B\");</p><p>    }else System.out.print(\"Plan C\");}</p><p>  }</p>",
			"<p>說明:</p><p>卷ch2-ex2</p><p>這個應用程式後輸出甚麼內容?</p><p>D:編譯失敗、無法編譯</p><p><br></p><p>1.var在java11只能在 main才能宣告</p><p>2.類別本體中直接寫了plan=plan+++—plan一類的可執行語句，唯其不在方法、建構子或初始化方塊中，故予發不合法</p><p>口訣:</p><p>【var是臨時工，只能待在方法中】</p>",
			30,
			new String[]{"<p>A.Plan A</p>", "<p>B.Plan B</p>", "<p>C.Plan C</p>", "<p>D.The class does not compile</p>", "<p>E.None of the adove</p>"},
			3
		);

		// Q9
		createQuestion(
			"<p>Given:</p><p><br></p><p>int i=10;</p><p>do{</p><p>    for(int j=i/2;j&gt;0;j--){</p><p>        System.out.print(j+\" \");</p><p>    }</p><p>    i-=2;</p><p>}while(i&gt;0);</p><p><br></p><p>What is the result?</p>",
			"<p>說明:</p><p>卷ch2-ex3</p><p>do while 至少執行一次，每輪for迴圈從i/2開始遞減到1，隨後i再減2，因次打印:5~1,4~1,3~1,2~1,1</p><p>口訣:</p><p>【do先做再問】</p><p>【巢狀迴圈，外圈定起點，內圈跑到底；】</p><p>【十的一半五、八的一半四、六三、四二、二一】</p><p>外圈減2，內圈從一半倒數</p>",
			30,
			new String[]{"<p>A)5 4 3 2 1</p>", "<p>B)nothing</p>", "<p>C)5</p>", "<p>D)5 4 3 2 1 4 3 2 1 3 2 1 2 1 1</p>"},
			3
		);

		// Q10
		createQuestion(
			"<p>Which statement about the Elephant propram is correct?</p><p><br></p><p>package stampede;</p><p>interface Long{\t\t\t\t\t\t\t\t\t</p><p>\tNumber length();</p><p>}</p><p><br></p><p>public class Elephant{</p><p>\tpublic class Trunk implements Long{ \t\t</p><p>\t\tpublic Number length(){ return 6;}//k1</p><p>\t}</p><p><br></p><p>\tpublic class MyTrunk extends Trunk{      \t//k2</p><p>\t\tpublic Integer length(){ return 9;} \t//k3 </p><p>\t}</p><p>\tpublic static void charage(){</p><p>\t\tSystem.out.print(new MyTrunk().length()); </p><p>\t}</p><p><br></p><p>\tpublic static void main(String[] cute){</p><p>\t \tnew Elephant().charge();\t\t\t\t//k4 </p><p>\t}</p><p>}</p>",
			"<p>說明:</p><p>卷ch3-ex1</p><p>底下的敘述，有關Elephant propram 何者是正確的?</p><p>程式碼說明:</p><p>package stampede;</p><p>interface Long{\t\t\t\t\t\t\t\t\t//Long 是個介面</p><p>\tNumber length();</p><p>}</p><p><br></p><p>public class Elephant{</p><p>\tpublic class Trunk implements Long{ \t\t</p><p>\t//內部類別Trunk 去實作這個Long</p><p>\t\tpublic Number length(){ return 6;} \t\t//k1</p><p>\t}</p><p><br></p><p>\tpublic class MyTrunk extends Trunk{      \t//k2</p><p>\t\tpublic Integer length(){ return 9;} //k3\t</p><p>\t\t//k3 有繼承關係，Interger有覆蓋Number 可以</p><p>\t}</p><p>\tpublic static void charage(){</p><p>\t\tSystem.out.print(new MyTrunk().length()); </p><p>\t\t//static 內部的也要static</p><p>\t}</p><p><br></p><p>\tpublic static void main(String[] cute){</p><p>\t \tnew Elephant().charge();//k4\t\t\t\t</p><p>\t \t//k4 呼叫static 透過類別，不是</p><p>\t}</p><p>}</p><p><br></p><p>A.It compiles nad prints 6 //錯誤!輸出是9</p><p>B.The code does not compile because of like k1 //錯誤! k1沒問題</p><p>C.The code does not compile because of like k2 //錯誤! k2沒問題</p><p>D.The code does not compile because of like k3 //錯誤! k3沒問題</p><p>E.The code does not compile because of like k4 </p><p>//錯誤! k4的問題是在前面new MyTrunk().length()就造成</p><p>F.None of the above //上面找不到完整的敘述(以上說法都不正確)</p><p><br></p><p>介面方法回傳Number,Trunk用Number實作﹑MyTrunk覆寫時使用Interger，屬於協變回型別，是合法的靜態charge方法也可以通過對象呼叫，所以程式碼本身可以編譯並出輸出9</p><p>口訣:</p><p>【this 、super搶第一、只能有一個第一】</p><p>把非static內部類別想成【大象的鼻子】。象鼻不能憑空出現，必須先有大象。</p><p>=⇒【非static內部類別， 要先找外部主人；更短、象鼻不能離開大象】</p>",
			30,
			new String[]{"<p>A.It compiles nad prints 6</p>", "<p>B.The code does not compile because of like k1 </p>", "<p>C.The code does not compile because of like k2 </p>", "<p>D.The code does not compile because of like k3 </p>", "<p>E.The code does not compile because of like k4 </p>", "<p>F.None of the above</p>"},
			5
		);

		// Q11
		createQuestion(
			"<p>What is the output of the following application?</p><p><br></p><p>package sports;</p><p>abstract class Ball{ </p><p>\tprotected final int size; </p><p>\tpublic Ball(int size){</p><p>\t\tthis.size = size;</p><p>\t}</p><p>}</p><p><br></p><p>interface Equipment{} </p><p><br></p><p>public class SoccerBall extends Ball implements Equipment{ </p><p>\tpublic SoccerBall(){</p><p>\t\tsuper(5); </p><p>\t}</p><p>\tpublic Ball get(){ return this;}</p><p>\tpublic static void main(String[] passes){</p><p>\t\tvar equipment=(Equipment)(Ball)new SoccerBall().get(); </p><p>\t\tSystem.out.print(((SoccerBall).equipment).size); \t</p><p>\t}</p><p>}</p>",
			"<p>卷ch3-ex2</p><p>解說:</p><p>以下的數據輸出哪一個是對的?</p><p>程式說明:</p><p>package sports;</p><p>abstract class Ball{ //抽象類別</p><p>\tprotected final int size; //沒有初始直，但建構式有給值</p><p>\tpublic Ball(int size){</p><p>\t\tthis.size = size;</p><p>\t}</p><p>}</p><p><br></p><p>interface Equipment{} //再一個介面</p><p><br></p><p>public class SoccerBall extends Ball implements Equipment{ </p><p>//SoccerBall 繼承 Ball 實作 Equipment</p><p>\tpublic SoccerBall(){</p><p>\t\tsuper(5); //執行super 上面的public Ball(int size)</p><p>\t}</p><p>\tpublic Ball get(){ return this;}</p><p>\t</p><p>\tpublic static void main(String[] passes){</p><p>\t\tvar equipment=(Equipment)(Ball)new SoccerBall().get(); //有繼承關係都OK</p><p>\t\tSystem.out.print(((SoccerBall).equipment).size); </p><p>\t\t//編譯時會先判斷Equipment有沒有size名稱，但有轉型成SoccerBall </p><p>\t\t//就可以找到.size, size的值就是5</p><p>\t}</p><p>}</p><p><br></p><p>SoccerBall建構子呼叫super(5)，把父類別受保護的final欄位size初始化為5，對象先向上轉型為Ball，再轉型為Equipment最後向下轉回SoccerBall，實際對象始終是SoccerBall，因此讀取size 得到 5</p><p>口訣:</p><p>【轉型只換名牌、不換真身；真身是足球，轉回足球就能取出size】</p>",
			30,
			new String[]{"<p>A.5</p>", "<p>B.The code dose not compile due to an invlid cast</p>", "<p>C.The code does not compile for a different reason</p>", "<p>D.The code compiles but throws a ClassCastException at runtime</p>"},
			0
		);

		// Q12
		createQuestion(
			"<p>Given:</p><p><br></p><p>public class GameObject{</p><p>\tpublic Object[] move(int x,int y){</p><p>\t\tSystem.out.println(\"Move GameObject\");</p><p>\t\treturn new Integer[]{x+10,y+10};</p><p>\t}</p><p>}</p><p><br></p><p>and</p><p><br></p><p>public class Avatar extends GameObject{  </p><p>\tpublic Objectp[] move(Number x,Number y){  </p><p>\t\tSystem.out.println(\"Move Character\");</p><p>\t\treturn super.move(x.intValue(),y.intValue()); </p><p>\t}</p><p><br></p><p>\tpublic static void main(String... args)</p><p>\t{</p><p>\t\tvar charactor = new Avatar(); </p><p>\t\tcharacter.move(10.0,10.0); </p><p>\t\tcharacter.move(10,10);</p><p>\t}</p><p>}</p><p><br></p><p>What is the result?</p>",
			"<p>卷ch3-ex3</p><p>說明:</p><p>下面應用程序的輸出結果是甚麼?</p><p>程式說明:</p><p>public class GameObject{</p><p>\tpublic Object[] move(int x,int y){</p><p>\t</p><p>\t\tSystem.out.println(\"Move GameObject\"); // *2</p><p>\t\t</p><p>\t\treturn new Integer[]{x+10,y+10};</p><p>\t}</p><p>}</p><p><br></p><p>//and</p><p><br></p><p>public class Avatar extends GameObject{  //Avatar 繼承自 GameObject</p><p>\tpublic Objectp[] move(Number x,Number y){  </p><p>\t\t// over loading Number 底下有Int 與 double 可以接受</p><p>\t\t</p><p>\t\tSystem.out.println(\"Move Character\");  </p><p>\t\t</p><p>\t\treturn super.move(x.intValue(),y.intValue()); //整數</p><p>\t}</p><p><br></p><p>\tpublic static void main(String... args)</p><p>\t{</p><p>\t\tvar charactor = new Avatar(); </p><p>\t\t//新增這個子類別物件</p><p>\t\tcharacter.move(10.0,10.0); </p><p>\t\t//做兩個動作，先執行 \"Move Charactor\"，再執行 \"Move GameObject\"</p><p>\t\tcharacter.move(10,10);</p><p>\t\t//int 只取move(int x, int y) 執行 \"Move GameObject\"</p><p>\t}</p><p>}</p><p><br></p><p>move(Number x,Number y)與父類別的move(int x,int y)參數不同，所以是多載而不是覆寫；傳入character.move(10.0,10.0); 時只能匹配move(Number x,Number y)版本，該方法內部呼叫父類別 int 版本，傳入整數 10 時，編輯器優先選擇更精確的的(int x,int y)的版本，</p><p>名稱一樣、參數一樣，才叫做Override</p><p>記憶點:</p><p>把多載想成【找尺寸適合的鞋子】，編譯時會優先找最吻合的方法，不會繞遠路</p><p>口訣</p><p>【多載找最合身:原型優先，裝箱退後】、【有int就選int，不必先裝成Integer再升Number】</p>",
			30,
			new String[]{"<p>A)Move Character </p><p>\tMove GameObject </p><p>\tMove GameObject </p>", "<p>B)Move GameObject </p><p>\tMove GameObject</p>", "<p>C)Move GameObject </p><p>\tMove Character </p><p>\tMove GameObject</p>", "<p>D)Move GameObject</p>"},
			0
		);

		// Q13
		createQuestion(
			"<p>public class Menu{</p><p>\tenum Machine{</p><p>\t   AUTO(\"Truck\"),MEDICAL(\"Scanner\"); </p><p>\t   private String type;</p><p>\t   private Machine(String type){</p><p>\t\t\tthis.type=type;</p><p>\t   }\t   </p><p>\t   private void setType(String type){</p><p>\t\t\tthis.type=type;   //line 1</p><p>\t   }</p><p>\t   private String getType(){</p><p>\t\t\treturn type;</p><p>\t   }</p><p>\t}</p><p><br></p><p>\tpublic static void main(String[] args){</p><p>\t\tMachine.AUTO.setType(\"Sedan\");  //line 2</p><p><br></p><p>\t\tfor(Machine p:Machine.value())</p><p>\t\t{</p><p>\t\t\tSystem.out.println(p+\": \"+p.getType()); //line3</p><p>\t\t}</p><p>\t}</p><p>}</p><p><br></p><p>What is the result?</p>",
			"<p>卷ch3-ex4</p><p>說明: 下面應用程序的輸出結果是甚麼?</p><p>答案:</p><p>F)AUTO:Sedan</p><p>\tMEDICAL:Scanner</p><p>程式說明:</p><p>public class Menu{</p><p>\tenum Machine{//enum 裡面，用 Machine 宣告一個物件叫AUTO  與 MEDICAL 物件</p><p>\t   AUTO(\"Truck\"),MEDICAL(\"Scanner\"); // 第一呼叫\t   </p><p>\t   private String type;</p><p>\t   private Machine(String type){</p><p>\t\t\tthis.type=type;</p><p>\t   }\t   </p><p>\t   private void setType(String type){</p><p>\t\t\tthis.type=type;   //line 1</p><p>\t   }</p><p>\t   private String getType(){</p><p>\t\t\treturn type;</p><p>\t   }</p><p>\t}</p><p><br></p><p>\tpublic static void main(String[] args){</p><p>\t\tMachine.AUTO.setType(\"Sedan\");  //line 2  --&gt;第二呼叫AUTO更改</p><p><br></p><p>\t\tfor(Machine p:Machine.value())</p><p>\t\t{</p><p>\t\t\tSystem.out.println(p+\": \"+p.getType()); //line3</p><p>\t\t}</p><p>\t}</p><p>}</p><p><br></p><p>列舉常量本質上是Machine的固定實例，但實例中的普通欄位仍可以改變</p><p>AUTO 的type 被setType改成Sedan ；MEDICAL 保持 Scanner，只能放大權限，不能縮小</p><p>記憶:</p><p>enum常數想成【固定兩台機器】。AUTO是一台固定機器；MEDICAL是另一台，機器數固定，機器上的標籤仍可更換，原本AUTO →Truck執行setType()後: AUTO→ “Sedan”、MEDICAL沒改，所以仍是MEDICAL→”Scanner”。</p><p>口訣:</p><p>【Enum人數固定，身上的資料未必固定】、【常數不能換人，欄位可以換衣服】</p>",
			30,
			new String[]{"<p>A)The compilation fails due to an error on line 3.</p>", "<p>B)The compilation due to an error on line 2.</p>", "<p>C)AUTO:Truck</p><p>\tMEDICAL:Scanner</p>", "<p>D)An exception is throw at run time</p>", "<p>E)The compilation fails due to an error on line 1</p>", "<p>F)AUTO:Sedan</p><p>\tMEDICAL:Scanner</p>"},
			5
		);

		// Q14
		createQuestion(
			"<p>Given:</p><p><br></p><p>class Scope{</p><p>\tstatic int myint=666;</p><p>\tpublic static void main(String[] args){</p><p>\t\tint myint=myint;  \t\t</p><p>\t\tSystem.out.println(myint);</p><p>\t}</p><p>}</p><p><br></p><p>Which is true?</p>",
			"<p>卷ch3-ex5</p><p>關於下面便量作用代碼，哪項說法正確?</p><p>答案:</p><p>D)The code does not compile successfully</p><p>程式碼編譯失敗</p><p>程式說明:</p><p>//變數的生命週期</p><p>class Scope{</p><p>\tstatic int myint=666; //static 的變數，要存取要class 提供</p><p>\tpublic static void main(String[] args){</p><p>\t\tint myint=myint;  </p><p>\t\t//矛盾，在main裡 變數宣告要給初始值，</p><p>\t\t//但 myint 宣告後又指向自己，根本沒【值】一定編譯失敗</p><p>\t\tSystem.out.println(myint);</p><p>\t}</p><p>}</p><p><br></p><p>區域變數int myint = myint; 中，右側名稱會被解析為正在宣告區域變數，而不是同名靜態欄位，該區域變數尚未初始化，因而出現”變數尚未初始化”的編譯錯誤。</p><p>記憶:</p><p>某人要用自己現在的錢，給剛出生的自己發薪水，自相矛盾</p><p>口訣:</p><p>【同名就近找，新人未領值】、【右邊先找最近的myint；不會自動回頭找static】</p>",
			30,
			new String[]{"<p>A)Code compiles but throws a runtime exception when run.</p>", "<p>B)It prints 666</p>", "<p>C)The code compiles and runs successfully but with a wrong answer(i.e.,a bug)</p>", "<p>D)The code does not compile successfully</p>"},
			3
		);

		// Q15
		createQuestion(
			"<p>Given:</p><p><br></p><p>package test.t1;</p><p>public class A{</p><p>\tpublic int x=42;</p><p>\tprotected A(){} //line 1</p><p>}</p><p><br></p><p>and</p><p><br></p><p>package test.t2;</p><p>import test.t1.*;</p><p><br></p><p>public class B extends A{</p><p>\tint x= 17; //line2</p><p>\tpublic B(){super();} //line 3\t</p><p>}</p><p><br></p><p>and</p><p><br></p><p>package test;</p><p>import test.t1.*;</p><p>import test.t2.*;</p><p>public class Tester{</p><p>\tpublic static void main(String[] args)</p><p>\t{</p><p>\t\tA obj= new B();\t\t             //line4\t</p><p>\t\tSystem.out.println(obj.x);     //line5</p><p>\t}</p><p>}</p><p><br></p><p>What is the result?</p>",
			"<p>卷ch3-ex6</p><p>說明: 程式運行結果如何? </p><p>答案: G)42 </p><p>程式說明:</p><p>package test.t1</p><p>public class A{</p><p>\tpublic int x=42;</p><p>\tprotected A(){} //line 1</p><p>}</p><p><br></p><p>package test.t2;</p><p>import test.t1.*;</p><p><br></p><p>public class B extends A{</p><p>\tint x= 17; //line2</p><p>\tpublic B(){super();} //line 3\t</p><p>}</p><p><br></p><p>package test;</p><p>import test.t1.*;</p><p>import test.t2.*;</p><p>public class Tester{</p><p>\tpublic static void main(String[] args)</p><p>\t{</p><p>\t\tA obj= new B();\t//line4 \t\t\t</p><p>\t\t//obj 變數的宣告型態是 A（父類別）。</p><p>\t\t//obj 變數的實際型態（實體）是 B（子類別）。</p><p>\t\t</p><p>\t\tSystem.out.println(obj.x);  //line5 </p><p>\t\t//關鍵是X值是誰</p><p>\t\t//因為 x 是一個屬性（欄位），</p><p>\t\t//Java 編譯器在處理屬性存取時，</p><p>\t\t//只看物件的宣告型態。因為 obj 被宣告為 A 類型，</p><p>\t\t//所以它抓取的就是類別 A 裡面的 x</p><p>\t}</p><p><br></p><p>\t//A宣告 故抓42</p><p>}</p><p><br></p><p>重點</p><p>方法的覆寫（Override）： 如果子類別改寫了父類別的方法，在執行期（Runtime）會根據實際 new 出來的物件來決定呼叫誰（動態繫結）。</p><p>屬性的隱藏（Hiding）： 當子類別宣告了與父類別同名的屬性時，子類別只是「隱藏」了父類別的屬性，兩者同時存在於記憶體中。</p><p>此時，要存取哪一個屬性，完全是看【<strong>變數宣告的型態】</strong>（編譯期決定）。</p><p>記憶:</p><p>B可以在建構子中呼叫父類別的A的protected建構子，obj的編譯實行別是A，所以obj.x訪問的是A.x，故輸出是42</p><p>口訣:</p><p>【抽象可以蓋房子(Constructor)，不能住人(new)】</p>",
			30,
			new String[]{"<p>A)The compilation fails due to an error in line 4</p>", "<p>B)17</p>", "<p>C)The compilation fails due to an error in line 2</p>", "<p>D)The compilation fails due to an error in line 3</p>", "<p>E)The compilation fails due to an error in line 1</p>", "<p>F)The compilation fails due to an error in line 5</p>", "<p>G)42</p>"},
			6
		);

		// Q16 (複選)
		createQuestionWithFlags(
			"<p>Given:</p><p><br></p><p>public class DNASynth{\t</p><p>\tint aCount;</p><p>\tint tCount;</p><p>\tint cCount;</p><p>\tint gCount;</p><p>\t</p><p>\tDNASynth(int aCount, int tCount, int c, int g){</p><p>\t\t//line 1</p><p>\t}</p><p>\tint setCCount(int c){</p><p>\t\treturn c;</p><p>\t}</p><p>\tvoid setGCount(int gCount){</p><p>\t\tthis.gCount = gCount;</p><p>\t}</p><p>}</p><p><br></p><p>Which two lines of code when inserted in line 1 correctly modifies instance variables?</p>",
			"<p>卷ch3-ex7</p><p>在構造方法的line1插入哪兩行代碼，可以正確修改實例變量?(兩個選項)</p><p><br></p><p>//Java作用域(Scope)、this關鍵字與方法調用</p><p>lass DNASynth{</p><p>\t//4個成員變數(Instance Variables)</p><p>\tint aCount;</p><p>\tint tCount;</p><p>\tint cCount;</p><p>\tint gCount;</p><p><br></p><p>\t// 構造函數(Constructor)</p><p>\tDNASynth(int aCount, int tCount, int c, int g){</p><p>\t\t//line 1</p><p>\t}</p><p>\tint setCCount(int c){</p><p>\t\treturn c;</p><p>\t}</p><p>\tvoid setGCount(int gCount){</p><p>\t\tthis.gCount = gCount;</p><p>\t}</p><p>}</p><p><br></p><p>為何BE能成功修改會員變數(Instance Variables)</p><p>B)cCount=setCCount(c)</p><p>變數名稱不重疊(無shadowing):</p><p>在構造函數中，成員變數名稱是cCount，而傳進來的參數名稱是c，兩者名字不同!</p><p>指派機制:</p><p>調用setCCount(c)時，將參數c傳入該方法，方法直接返回c的值。接著將這個傳回值賦予成員變數cCount</p><p>結果:cCount 成功被賦予傳進來的c值 ；延伸思考，直接寫cCount = c，也能成功修改成員變數。</p><p><br></p><p>E)setGCount(g)</p><p>方法內部的this處理了命名衝突:</p><p><br></p><p>java:</p><p>void setGCount(int gCount){</p><p>\tthis.gCount = gCount; </p><p>\t// this.gCount 代表成員變數，gCount代表傳入 setGCount 的區域參數</p><p>}</p><p>調用過程:</p><p>line 1 寫下 setGCount(g)時，構造函數把參數g傳給了 setGCount 方法。</p><p>setGCount 方法內部使用了 this.gCount=gCount; 明確的將傳進來的數值存入成員變數gCount中</p><p>結果:成員變數gCount成功被設定</p><p><br></p><p>cCount = setCCount(c)的方法回傳的c賦值給實例欄位cCount，setGCount(g)通過this.gCount修改實例欄位，關鍵是區分餐數名，區域變數名與對象欄位</p><p>介面欄位: 公、靜、常。介面方法:公 、抽。</p><p>記憶:</p><p>兩個送貨方式，第一種是快遞送出，再由建構子收貨: cCount=setCCount(c)；第二種是快遞直接送到家裡:setGCount(g)因為其內部方法已經有: this.gCount = gCount;</p><p>口訣:</p><p>【return 要接貨(cCount = setCCount(c))、this已送到家。(setGCount(g))】</p>",
			30,
			new String[]{"<p>A)tCount=tCount;</p>", "<p>B)cCount = setCCount(c);</p>", "<p>C)setCCount(c)=cCount;</p>", "<p>D)aCount = aCount;</p>", "<p>E)setGCount(g);</p>"},
			new boolean[]{false, true, false, false, true}
		);

		// Q17
		createQuestion(
			"<p>Given</p><p><br></p><p>public class Price{</p><p>    private final double value;</p><p><br></p><p>    public Price(String value) {</p><p>        this(Double.parseDouble(value));</p><p>    }</p><p><br></p><p>    public Price(double value) {</p><p>        this.value = value;</p><p>    }    </p><p>    public Price() {}</p><p>    public double getValue() {return value;}</p><p>    public static void main(String[] args) {</p><p>        Price p1 = new Price(\"1.99\");</p><p>        Price p2 = new Price(2.99);</p><p>        Price p3 = new Price();       </p><p>        System.out.println(p1.getValue() + \",\" + p2.getValue() + \",\" + p3.getValue());</p><p>    }</p><p>}</p><p><br></p><p>What is the result?</p>",
			"<p>卷ch3-ex8 以上程式碼運行結果為何?</p><p>C)The compilation fails //編譯錯誤</p><p><br></p><p>value被宣告為final且未被給予初始值</p><p>java規則中，所有未賦值的final 實例變數，必需在物件建構完成前被賦值一次，且每個建構子都必須確保被初始化</p><p>public Price(){} →出錯點，這裡即沒有賦值給value，也沒透過this(…)委派其他建構子，故編譯錯誤</p><p>口訣:</p><p>【宣告final空留白，每個建構都要拜】</p><p>(宣告了blank final 每個建構子都必需給其賦值或呼叫this(…)，全都不能漏)</p>",
			30,
			new String[]{"<p>A)1.99,2.99,00</p>", "<p>B)1.99,2.99</p>", "<p>C)The compilation fails</p>", "<p>D)1.99,2.99,0</p>"},
			2
		);

		// Q18 (複選)
		createQuestionWithFlags(
			"<p>Given:</p><p><br></p><p>public interface Builder{</p><p>\tpublic A build(String str);</p><p>}</p><p><br></p><p>and</p><p><br></p><p>public class BuilderImpl implements Builder{</p><p>\t@Override</p><p>\tpublic B Build(String str){</p><p>\t\treturn new B(str);</p><p>\t}</p><p>}</p><p><br></p><p>Assuming that this code compiles correctly,which three statements are true?</p>",
			"<p>卷ch3-ex9 假設碼能被正確編譯，以下哪三項說碼正確?</p><p> </p><p>實作介面方法時，允許使用共變回傳型別，因此B必需是A的子類別，方法本體直接new B(str)所以B不能是abstract，同時B要繼承A，因此A不能是final</p><p>D)B is a subtype of A (B必需是A的子類別)</p><p>E)B cannot be abstract (B不能是抽象類別)</p><p>F)A cannot be final (A不能是final類別)</p><p>口訣:</p><p>【回傳可縮小、實體不可抽、被繼不能終】</p><blockquote>回傳可縮小(共變回傳):覆寫時回傳型態只能更具體或相同，不能變大(父類別)</blockquote><blockquote>實體不可抽(new 關鍵字):只要看到 new X()，X絕對不能是abstract</blockquote><blockquote>被繼不能終(繼承限制):當一個類別被別人當父類別實，它絕對不能是final</blockquote><p><br></p>",
			30,
			new String[]{"<p>A)A cannot be abstract.</p>", "<p>B)A is a subtype of B.</p>", "<p>C)B cannot be final.</p>", "<p>D)B is a subtype of A.</p>", "<p>E)B cannot be abstract.</p>", "<p>F)A cannot be final.</p>"},
			new boolean[]{false, false, false, true, true, true}
		);

		// Q19 (複選)
		createQuestionWithFlags(
			"<p>Given</p><p><br></p><p>public class Foo{</p><p>\tpublic void foo(Collection arg){</p><p>\t\tSystem.out.println(\"Bonjour le monde\");</p><p>\t}</p><p>}</p><p><br></p><p>and</p><p><br></p><p>public class Bar extends Foo{</p><p>\tpublic void foo(Collection arg){</p><p>\t\tSystem.out.println(\"Hello world\");</p><p>\t}</p><p>\tpublic void foo(List arg){</p><p>\t\tSystem.out.println(\"Hello Mundol!\");</p><p>\t}</p><p>}</p><p><br></p><p>and</p><p><br></p><p>Foo f1 = new Foo();</p><p>Foo f2 = new Bar();</p><p>Bar b1 = new Bar();</p><p>List&lt;String&gt; li=new ArrayList&lt;&gt;();</p><p><br></p><p>Which three are correct?</p>",
			"<p>卷ch3-ex10 以下哪三項說法是正確的? </p><p><br></p><p>多載在編譯時期根據參考型別與參數選擇，覆寫在執行期根據實際對象選擇，</p><p>b1是Bar 型別，List多載最具體，f2的編譯期型別是Foo，只能看到foo(Collection)執行時Bar的覆寫，f1則˙執行Foo的版本</p><p><br></p><p>D)b1.foo(li) prints Hello Mundo!</p><p>實際物件是List，會精準匹配到foo(List)這個方法簽名</p><p><br></p><p>E)f2.foo(li) prints Hello world</p><p>執行Bar 複寫後的foo(Collection)，印出\"Hello world”</p><p><br></p><p>G)f1.foo(li) prints Bonjour le monde!</p><p>實際物件是Foo，執行Foo的foo(Collection),印出Bonjour le monde!</p><p><br></p><p>口訣:</p><p>【多載看左，覆寫看右】</p><p>參數挑選(Overload):看左邊變數宣告型別，在編譯期【由左決定方法簽名】</p><p>實作執行(Override):看右邊實際物件型別，在執行期【由右決定最後結果】</p>",
			30,
			new String[]{"<p>A)f2.foo(li) prints Bonjour le monde</p>", "<p>B)f1.foo(li) prints Hello Mundo!</p>", "<p>C)f2.foo(li) prints Hello Mundo!</p>", "<p>D)b1.foo(li) prints Hello Mundo!</p>", "<p>E)f2.foo(li) prints Hello world</p>", "<p>F)b1.foo(li) prints Hello world</p>", "<p>G)f1.foo(li) prints Bonjour le monde!</p>", "<p>H)f1.foo(li) prints Hello world</p>", "<p>I)b1.foo(li) prints Bonjour le monde!</p>"},
			new boolean[]{false, false, false, true, true, false, true, false, false}
		);

		// Q20
		createQuestion(
			"<p>Given</p><p><br></p><p>public class Test{</p><p>\tpublic static void main(String[] args) {</p><p>\t\tAnotherClass ac=new AnotherClass(); </p><p>\t\tSomeClass sc= new AnotherClass(); </p><p>\t\tac=sc;</p><p>\t\tsc.methodA();</p><p>\t\tac.methodA();</p><p>\t}</p><p>}</p><p><br></p><p>class SomeClass{</p><p>\tpublic void methodA{</p><p>\t\tSystem.out.println(\"SomeClass#methodA()\");</p><p>\t}</p><p>}</p><p>class AnotherClass extends SomeClass{</p><p>\tpublic void methodA(){</p><p>\t\tSystem.out.println(\"AnotherClass#methodA\");</p><p>\t}</p><p>}</p><p><br></p><p>What is the Result?</p>",
			"<p>卷ch3-ex11 以上程式輸出結果為何?</p><p>答案D) 編譯失敗</p><p><br></p><p>class ch3ex11{</p><p>\tpublic static void main(String[] args) {</p><p>\t\tAnotherClass ac=new AnotherClass(); </p><p>\t\tSomeClass sc= new AnotherClass(); //（合法的向上轉型）</p><p>\t\t//ac=sc; </p><p>\t\t//【型別向下轉型(Downcasting)】陷阱</p><p>\t\t/*</p><p>\t\tac 宣告型別子類別 AnotherClass</p><p>\t\tsc 宣告型別父類別 SomeClass</p><p>\t\t「父類別變數 sc」直接放進「子類別變數 ac」裡面</p><p>\t\tjava不允許隱式向下轉型（Implicit Downcasting），</p><p>\t\tac=sc;\t\t此編譯器會直接報錯</p><p>\t\t*/</p><p>\t\t//修正</p><p>\t\tac = (AnotherClass) sc; </p><p>\t\t//加上強制轉型，編譯就能順利通過</p><p>\t\tsc.methodA();</p><p>\t\tac.methodA();</p><p>\t}</p><p>}</p><p>//java 編譯器的編譯時期，只看變數【宣告的型別】(Referenc Type)】</p><p>//不理會記憶體裡實際放的是何</p><p>class SomeClass{</p><p>\tpublic void methodA{</p><p>\t\tSystem.out.println(\"SomeClass#methodA()\");</p><p>\t}</p><p>}</p><p>class AnotherClass extends SomeClass{</p><p>\tpublic void methodA(){</p><p>\t\tSystem.out.println(\"AnotherClass#methodA\");</p><p>\t}</p><p>}</p><p><br></p><p>ac的型別是AnotherClass，sc的型別是SomeClass，雖然sc實際參考AnotherClass對象，但不能不經強制轉型，就把父類別參考賦予子類別型別變數，因此ac = sc 編譯失敗</p><p>記憶:</p><p>sc 牽著一隻【AnotherClass 狗】，但牠的證件只寫著【someClass 動物】，沒有證件檢查前，不能直接將牠放進只接收【AnotherClass 狗】的房間</p><p>口訣</p><p>【真身是子類別也沒用，指派先看右邊的證件】</p><p>要寫成: ac = (AnotherClass) sc; 才可通過編譯，且必須保證真身確實是 AnotherClass</p>",
			30,
			new String[]{"<p>A) A ClassCastException is thrown at runtime.</p>", "<p>B) SomeClass#methodA()</p><p>AnotherClass#methodA()</p>", "<p>C) AnotherClass#methodA()</p><p>AnotherClass#methodA()</p>", "<p>D) The compilation fails</p>", "<p>E) AnotherClass#methodA()</p><p>SomeClass#methodA()</p>", "<p>F) SomeClass#methodA()</p><p>SomeClass#methodA()</p>"},
			3
		);

		// Q21
		createQuestion(
			"<p>Given:</p><p><br></p><p>interface AbilityA{</p><p>\tdefault void action(){</p><p>\t\tSystem.out.println(\"a action\");</p><p>\t}</p><p>}</p><p><br></p><p>and</p><p><br></p><p>interface AbilityB{</p><p>\tvoid action();</p><p>}</p><p><br></p><p>and</p><p><br></p><p>public class Test implements AbilityA,AbilityB{ //line 1</p><p>\tpublic void action(){</p><p>\t\tSystem.out.println(\"ab action\");</p><p>\t}</p><p>\tpublic static void main(S[] args){</p><p>\t\tAbilityB x = new Test(); //line 2</p><p>\t\tx.action();</p><p>\t}</p><p>}</p><p><br></p><p>What is the result?</p>",
			"<p>卷ch3-ex12 以上程式執行結果為何?</p><p>答案:E)ab action</p><p><br></p><p>預設方法衝突:當繼承/實作的介面間有同名方法時，只要子類別手動覆寫方法，編譯就會通過</p><p><br></p><p>多型呼叫:不管前面宣告的型別是介面還是父類別，只要方法被覆寫過，執行期 永遠執行實際物件(new 出來的那個) 的實作</p><p>AbilityA提供default action，AbilityB宣告抽象action，實作類別Test自己覆寫action，解決了繼承衝突，通過AbilityB參考呼叫時仍會發生動態綁定，執行Testaction；方法看右邊，欄位看左邊。</p><p><br></p><p>記憶:</p><p>兩位長輩對action() 有不同說法: A已經提供預設作法；B只要求必須有做法，Test 自己站出來宣布最終版本。</p><p><br></p><p>口訣:</p><p>【介面意見打架，實作類自己作答】</p>",
			30,
			new String[]{"<p>A) The compilation fails on line 1</p>", "<p>B) An exception is thrown at run time</p>", "<p>C) The compilation fails on line 2</p>", "<p>D) a action</p>", "<p>E) ab action</p>"},
			4
		);

		// Q22
		createQuestion(
			"<p>Given the enum declaration:</p><p><br></p><p>1.enum Alphabet{</p><p>2. A,B,C</p><p>3.</p><p>4.}</p><p>Example this code:</p><p>\tSystem.out.println(Alphabet.getFirstLetter());</p><p>What code shold be written at line 3 to make this print A?</p>",
			"<p>卷ch3-ex13 在枚舉第3行寫入哪段代碼才能使實例輸出A?</p><p><br></p><p>A)static String getFirstLetter(){ return A.toString();}</p><p>定義靜態方法getFirstLetter()並返回A.toString()</p><p>只有被標記為static(靜態)方法才能直接透過類別名稱來呼叫A 是一個合法物件reference</p><p>呼叫A.toString()會直接傳回該枚舉常數的名詞字串\"A”傳回型態String，符合列印目標</p><p><br></p><p>記憶:</p><p>Alphabet 班級要派出第一位同學，第一位就是A，因為方法是透過班級名稱直接叫，所以方法也必須是static</p><p><br></p><p>口訣:</p><p>【類別名稱直接叫，方法就要static；第一個字母直接回A】</p>",
			30,
			new String[]{"<p>A) static String getFirstLetter(){ return A.toString(); }</p>", "<p>B) static String getFirstLetter(){ return Alphabet.values().toString();}</p>", "<p>C) String getFirstLetter(){ return A.toString();}</p>", "<p>D) final String getFirstLetter(){ return A.toString();}</p>"},
			0
		);

		// Q23 (複選)
		createQuestionWithFlags(
			"<p>Given:</p><p><br></p><p>public interface ExampleInterface{}</p><p><br></p><p>Which two statements are valid to be written in this interface?</p>",
			"<p>卷ch3-ex14 在介面 ExampleInterface中，哪兩個方法聲明是合法?'</p><p>A)public String method();</p><blockquote>正確，此為標準的抽象方法宣告(Adstrct Method)，方法預設是public abstrcact，</blockquote><blockquote>即使只寫public string method();完全合乎語法</blockquote><p>G)public abstract void methodB();</p><blockquote>正確:這是顯式寫出 abstract 修飾符號的抽象方法宣告</blockquote><p><br></p><p>B)public void methodF(){</p><p>\tSystem.out.println(\"F\");</p><p>}</p><p>//錯誤! 介面中的普通抽象方法不能有方法體{...}，只有default或static</p><p>C)public int x;</p><p>//錯誤! 介面中的變數未給予初始值，</p><p>D)final void methodE()</p><p>//錯誤! 介面的抽象方法是為了給實作類別去覆寫，final禁止覆寫，衝突</p><p>E)final void methodG(){</p><p>\tSystem.out.println(\"G\");</p><p>}</p><p>//錯誤! 介面不允許使用final來修飾方法</p><p>F)private abstrcact void methodC();</p><p>//錯誤! 抽象方法必須給子類別實作，因此不能是 private（private 與 abstract 不能同時存在）</p><p><br></p><p>口訣:</p><p>【變數必初(值)，方法無體；預設公抽(public abstract)，絕不修終(不加final)】</p><p>1.變數必初 : 介面的變數都是常數(public static final)，必須給初值</p><p>2.方法無體 : 一般抽象方法只有宣告，每有跨號{}的方法體</p><p>3.預設公抽 : 介面裡的方法預設就是public與 abstract(寫不寫都依樣)</p><p>4.絕不修終 : 介面方法不能加上final(修飾終結)，因為介面本意就是等著被覆寫</p>",
			30,
			new String[]{"<p>A)public String method();</p>", "<p>B)public void methodF(){</p><p>\tSystem.out.println(\"F\");</p><p>\t}</p>", "<p>C)public int x;</p>", "<p>D)final void methodE()</p>", "<p>E)final void methodG(){</p><p>\tSystem.out.println(\"G\");</p><p>\t}</p>", "<p>F)private abstrcact void methodC();</p>", "<p>G)public abstract void methodB();</p>"},
			new boolean[]{true, false, false, false, false, false, true}
		);

		// Q24
		createQuestion(
			"<p>Given:</p><p><br></p><p>public class Person{</p><p>\tprivate String name;</p><p>\tprivate Person child;</p><p>\tpublic Person(String name,Person child)</p><p>\t{</p><p>\t\tthis(name);</p><p>\t\tthis.child = child;</p><p>\t}</p><p>\tpublic Person(String name){</p><p>\t\tthis.name = name;</p><p>\t}</p><p>\tpublic String toString(){</p><p>\t \treturn name+\"\"+child;\t </p><p>\t}</p><p>}</p><p><br></p><p>and</p><p><br></p><p>public class Tester{</p><p>\tpublic static Person createPeople(){</p><p>\t\tPerson jane = new Person(\"Jane\");\t</p><p>\t\tPerson john = new Person(\"John\",jane);\t\t</p><p>\t\treturn jane;</p><p>\t}</p><p><br></p><p>\tpublic static Person createPerson(Person person){\t\t</p><p>\t\tperson = new Person(\"Jack\",person);\t\t</p><p>\t\treturn person;</p><p>\t}</p><p>\tpublic static void main(String[] args)</p><p>\t{</p><p>\t\tPerson person=createPeople();</p><p>\t\t//line 1</p><p>\t\tperson=createPerson(person);\t\t</p><p>\t\t//line 2</p><p>\t\tString name=person.toString();</p><p>\t\tSystem.out.println(name);</p><p>\t}</p><p>}</p><p><br></p><p>Which statement is true?</p>",
			"<p>卷ch3-ex15 關於對象引用與垃圾回收哪項說法正確?</p><p>C)The memory allocated for jane object can be reused in line 2.</p><p>為 jane 物件分配的記憶體可以在第 2 行中重複使用。</p><p><br></p><p>public class Person{</p><p>\tprivate String name;</p><p>\tprivate Person child;</p><p>\tpublic Person(String name,Person child)</p><p>\t{</p><p>\t\tthis(name);</p><p>\t\tthis.child = child;</p><p>\t}</p><p>\tpublic Person(String name){</p><p>\t\tthis.name = name;</p><p>\t}</p><p>\tpublic String toString(){</p><p>\t \treturn name+\"\"+child;\t </p><p>\t}</p><p>}</p><p><br></p><p>and</p><p><br></p><p>public class Tester{</p><p>\tpublic static Person createPeople(){</p><p>\t\tPerson jane = new Person(\"Jane\");\t</p><p>\t\tPerson john = new Person(\"John\",jane);\t\t</p><p>\t\treturn jane;</p><p>\t}</p><p><br></p><p>\tpublic static Person createPerson(Person person){\t\t</p><p>\t\tperson = new Person(\"Jack\",person);\t\t</p><p>\t\treturn person;</p><p>\t}</p><p>\tpublic static void main(String[] args)</p><p>\t{</p><p>\t\tPerson person=createPeople();</p><p>\t\t//line 1</p><p>\t\tperson=createPerson(person);\t\t</p><p>\t\t//line 2</p><p>\t\tString name=person.toString();</p><p>\t\tSystem.out.println(name);</p><p>\t}</p><p>}</p><p><br></p><p>答案選C的關鍵在於:jane物件在line 2 之後已經「無法被任何變數存取到」</p><p>(Unreachable)，因此它占用的記憶體可以被回收/重複使用(Reused)</p><p>createPeople回傳jane，john只是區域變數，方法結束後不再可被參考到，caretePerson創建Jack，並讓Jack.child指向原來的Jane；當line2 執行後，person指向Jack，Jack仍被Jack.child參考，所以嚴格說此時Jane仍可以被參考到。</p><p><br></p><p>記憶:</p><p>垃圾回收題不要看變數名稱是否消失，要畫【繩子】。變數或物件欄位→是否還有繩子連到該物件?沒有任何繩子連到該物件?沒有任何繩子能從有效參考走到物件時，才有資格被回收。</p><p><br></p><p>口訣:</p><p>【GC不看名字，看還有沒有繩子】、【沒人連得到，才有回收資格】</p>",
			30,
			new String[]{"<p>A) The memory allocated for John object can be reused in line 1.</p>", "<p>B) The memory allocated for Jack object can be reused in line 2.</p>", "<p>C) The memory allocated for Jane object can be reused in line 2.</p>", "<p>D) The memory allocated for Jane object can be reused in line 1.</p>"},
			2
		);

		// Q25
		createQuestion(
			"<p>Given:</p><p><br></p><p>public interface A{</p><p>    public Iterable a();</p><p>}</p><p><br></p><p>public interface B extends A{</p><p>    public Collection a();</p><p>}</p><p><br></p><p>public interface C extends A{</p><p>    public Path a();</p><p>}</p><p><br></p><p>public interface D extends B,C{</p><p><br></p><p>}</p><p><br></p><p>why does D cause a compilation error?</p>",
			"<p>卷ch3-ex16 為什麼接口D會導致編譯錯誤?</p><p>答案:C)D inherits a() from B and c but the return types are incompatible. //D同時從B和C繼承a()，但返回類型不兼容</p><p><br></p><p>/*</p><p>Java 的傳回型態相容性與協便傳回型態機制</p><p><br></p><p>介面D同時繼承了B和C，但B和C的a()方法回傳了</p><p>兩個完全互不相容的類別，導致編輯器不知D裡面</p><p>的 a()到底回傳何。</p><p>*/</p><p><br></p><p>Given:</p><p><br></p><p>public interface A{</p><p>    public Iterable a();</p><p>}</p><p>//A定義了Iterable a()</p><p><br></p><p>public interface B extends A{</p><p>    public Collection a();</p><p>}</p><p>//Collection 是 Iterable的子介面--&gt; 協變傳回型態</p><p><br></p><p>public interface C extends A{</p><p>    public Path a();</p><p>}</p><p>//C extends A 將 a() 回傳型態改為Path，Path 也實作Iterable&lt;Path&gt;</p><p>//Path型態也是Iterable的子類別</p><p><br></p><p>public interface D extends B,C{</p><p><br></p><p>}</p><p>//D同時繼承B與C，同時拿到兩個同名的a()方法</p><p><br></p><p>//Java 允許介面多重繼承同名方法，前提必須能找到一個統一且相容的回傳型態</p><p>//但Collection 與 Path 兩者沒有繼承關係，編譯器無法同時滿足B與C傳回型態，</p><p>//故編譯錯誤</p><p><br></p><p>B.a()回傳Collection，C.a()回傳Path，兩者都可以作為A.a()回傳Iterable協作覆寫，但Collection與Path之間沒有子類型關係，D無法繼承出一個唯一兼容的方法。</p><p><br></p><p>記憶:</p><p>A說:【送一個迭代物件就好。】 B說:【我要送Collection。】 C說:【我要送Path。】 B與C雖然各自都可以符合A的大方向，但Collection與Path 彼此不是父子關係。D同時繼承後，不知道最終應承諾哪一種。</p><p><br></p><p>口訣</p><p>【回傳可以變小，但兩個小孩必須能排出父子；若兩個回傳型別互不相容: Collection ↔ Path】</p>",
			30,
			new String[]{"<p>A) D does not define any method.</p>", "<p>B) D inherits a() only from c.</p>", "<p>C) D inherits a() from B and c but the return types are incompatible.</p>", "<p>D) D extends more thean one interface</p>"},
			2
		);

		// Q26
		createQuestion(
			"<p>Given the code fragment:</p><p><br></p><p><br></p><p>8.public class Test{</p><p>9.    private final int x=1;</p><p>10.    static final int y;</p><p>11.    public Test(){</p><p>12.        System.out.print(x);</p><p>13.        System.out.print(y);</p><p>14.    }</p><p>15.    public static void main(String args[]){</p><p>16.        new Test();</p><p>17.    }</p><p>18.}</p><p><br></p><p>what is the result?</p>",
			"<p>卷ch3-ex17 給定以下程式碼片段：結果是什麼？</p><p><br></p><p>A)The compilation falls at line 13</p><p>//編譯結果在第13行出錯</p><p>//static final 變數在類別載入時，必須完成初始化，</p><p>//程式碼的y完全沒有賦予初值，編譯直接報錯</p><p><br></p><p>Static final 欄位 y 沒有宣告處或靜態初始化中賦值，建構子中讀取 y 時，變譯器發現她尚未完成確定賦值，因此無法編譯</p><p><br></p><p>記憶:</p><p>static final 是【全班唯一、永久固定的位置】。既然只有一個公用座位，就應在類別啟動階段安排好，不能等某個物件出生後再說。</p><p><br></p><p>口訣:</p><p>【static final是公產，static階段就要填滿】</p><p><br></p>",
			30,
			new String[]{"<p>A) The compilation falls at line 13</p>", "<p>B) The complation falls at line 9</p>", "<p>D) 1</p>", "<p>E) 10</p>"},
			0
		);

		// Q27
		createQuestion(
			"<p>Given:</p><p><br></p><p>1.interface Pastry{</p><p>2.    void getIngredients();</p><p>3.}</p><p>4.abstract class Cookie implements Pastry{}</p><p>5.</p><p>6.class ChocolateCookie implements Cookie{</p><p>7.    public void getIngredients(){}</p><p>8.}</p><p>9.class CoconutChoolateCookie extends ChocolateCookie{</p><p>10.    void getIngredients(int x){}</p><p>11.}</p><p><br></p><p>what is the result?</p>",
			"<p>卷ch3-ex18 關於下面 Pasty，Cookie 和 Chocolate the Cookie 的代碼哪項正確?</p><p><br></p><p>D)The compilation falls due to an error in line 6. </p><p>//編譯失敗，原因是第6行出現錯誤，問題出在implements 與 </p><p>//extends 的關鍵字使用錯誤 </p><p>//Cookie 是一個抽象類別(abstract class)，它不是介面 //ChocolateCookie 是一個類別，當一個類別要繼承另一個類別時， </p><p>//必須使用extends 關鍵字 </p><p>//程式碼中使用了 implements Cookie 邊議會提是錯誤</p><p><br></p><p>Cookie 式類別不是介面，因此ChocolateCookie 應該使用extendsCookie，而不能implements Cooke，抽象類別可以暫時不時做介面，但具體子類別必須實作。</p><p><br></p><p>記憶:</p><p>extends、 implements 想乘兩種插頭；類別接類別 →extends ；類別接介面 →implements介面接介面 → extends；題目拿【介面插頭impements】去接一個class 、插頭不合。</p><p><br></p><p>口訣:</p><p>【類接類用extends；類接介面 implements。】</p>",
			30,
			new String[]{"<p>A) The compilation falls due to an error in line 10.</p>", "<p>B) The compilation falls due to an error in line 9.</p>", "<p>C) The compilation falls due to an error in line 4.</p>", "<p>D) The compilation falls due to an error in line 6.</p>", "<p>E) The compilation succeeds</p>", "<p>F) The compilation falls due to an error in line 7.</p>", "<p>G) The compilation falls due to an error in line 2.</p>"},
			3
		);

		// Q28
		createQuestion(
			"<p>Given the following application, which specific type of exception will be printed in the stack trace at runtime?</p><p><br></p><p>package carnival;</p><p>public class WhackAnException{</p><p>   public static void main(String... hammer)</p><p>   {</p><p>        try{</p><p>           throw new ClassCastException();  </p><p>        }catch(IllegalArgumentException e){           </p><p>           throw new IlleaglArgumentException();</p><p>        }catch(RuntimeException e){</p><p>            throw new NullPointerException();   </p><p>        }finally{</p><p>            throw new RuntimeException();</p><p>        }</p><p>   }</p><p>}</p>",
			"<p>卷ch4-ex1 給定以下應用程序，運行時堆疊追蹤中將列印哪種特定類型的異常？</p><p><br></p><p>答案:</p><p>D)RuntimeException </p><p>//異常被覆蓋</p><p>//在java中，finally區塊內拋出新異常，會直接【覆蓋】(Mask)並取代之前try或</p><p>//catch中未處理的舊異常，原本NullpointerException 被拋棄</p><p>//最後在finall中拋出 java.lang.RuntimeException</p><p><br></p><p>package carnival;</p><p>public class WhackAnException{</p><p>   public static void main(String... hammer)</p><p>   {</p><p>        try{</p><p>           throw new ClassCastException();</p><p>           //拋出異常，執行 throw new ClassCastException，</p><p>           //拋出一個ClassCastException</p><p>        }catch(IllegalArgumentException e){</p><p>           //捕獲IllegalArgumentException 不匹配</p><p>           throw new IlleaglArgumentException();</p><p>        }catch(RuntimeException e){</p><p>            //捕獲RuntimeException，因ClassCastException </p><p>            // 是 RuntimeException子類別，成功捕獲</p><p>            throw new NullPointerException();</p><p>            //拋出一個NullpointerException</p><p>        }finally{</p><p>            throw new RuntimeException();</p><p>            //進入finally區塊，執行throw new RuntimeException();</p><p>        }</p><p>   }</p><p>}</p><p><br></p><p>try 中拋出的 ClassCastException 會被RuntimeException 的catch捕捉並改變拋出 NullPointerException，但finaly 無論如何都會執行，並再次拋出RuntimeException，finally拋出例外覆蓋之前正在傳播的的例外</p><p>記憶:</p><p>畫面前面的人都在喊例外，最後finally拿麥克風大喊，蓋過所有人的聲音。</p><p>口訣:</p><p>【finally最後喊，最後喊得算】、【finally 再丟例外、前例外直接被蓋】</p>",
			60,
			new String[]{"<p>A. ClassCast Exception</p>", "<p>B. IllegalArgumentException</p>", "<p>C. NullPointerExcetion</p>", "<p>D.RuntimeException</p>", "<p>E.The code does not compile.</p>", "<p>F.None of the above</p>"},
			3
		);

		// Q29
		createQuestion(
			"<p>Given the following application，what is the name of the class printed at line e1?</p><p><br></p><p>package canyon;</p><p>final class FallenException extends Exception{}</p><p>final class HikingGear implements AutoCloseable{</p><p>    @Override public void close() throws Exception{</p><p>        throw new FallenException();</p><p>    }</p><p>}</p><p><br></p><p>public class Cliff{</p><p>    public final void climb() throws Exception{</p><p>        try(HikingGear gear=new HikingGear()){</p><p>            throw new RuntimeException();            </p><p>        }</p><p>    }</p><p>    public static void main(String... rocks){</p><p>        try{</p><p>            new Cliff().climb();</p><p>        }catch(Throwable t){</p><p>            System.out.print(t); //e1</p><p>        }</p><p>    }</p><p>}</p>",
			"<p>卷ch4-ex2 給定以下應用程序，第 e1 行列印的類別名稱是什麼？</p><p><br></p><p>B. java.long.RuntimeException</p><p>try 主踢先拋RuntimeException，隨後資源close() 又拋FallenException，try-with-resources會保留主體例外做為主例外，把關閉資源產生的例外放入suppressed exceptions，所以接打印 t 時顯示 RuntimeException</p><p><br></p><p>記憶:</p><p>主角先跌倒，關門的人後來也跌倒；新聞標題仍報導【主角跌倒】，關門者跌到只放在附註。</p><p><br></p><p>口訣:</p><p>【主體先丟當主角，close後丟當配角。】</p><p>【配角】就是: suppressed exception</p><p><br></p>",
			30,
			new String[]{"<p>A. canyon.fallenException</p>", "<p>B. java.lang.RuntimeException</p>", "<p>C. The code dose not compile.</p>", "<p>D. The code compile，but the answer cannot be determained until runtime.</p>", "<p>E. None of the above</p>"},
			1
		);

		// Q30
		createQuestion(
			"<p>Given:</p><p><br></p><p>import java.io.FileNotFoundException;</p><p>import java.io.IOException;</p><p><br></p><p>public class Tester{</p><p>    public static void main(String[] args)</p><p>    {    </p><p>        try{</p><p>            doA();</p><p>        }//line 1</p><p>    }</p><p>    private static void doA() throws Exception,IndexOutOfBoundsException{</p><p>        if(false){</p><p>            throw new FileNotFoundException();</p><p>        }else{</p><p>            throw new IndexOutOfBoundsException();</p><p>        }</p><p>    }</p><p>}</p><p><br></p><p>What must be added in line 1 to compile this class?</p>",
			"<p>卷ch4-ex3 要編譯這個類，需要在line 1添加什麼?</p><p><br></p><p>C) catch(Exception e){} </p><p>//IndexOutOfBoundsException 是 RuntimeExcpeiton </p><p>//是不受檢例外，編譯器不強制處理， </p><p>//doA()宣告了 throws Exception </p><p>//在line1 補上 catch(Exception e){}才能通過編譯</p><p><br></p><p>doA()宣告throws Exception，因此呼叫方法必須捕捉Exception或繼續宣告拋出，IndexOutOfBoundxException是非受檢例外，不強制處理，但Exception是受檢例外。</p><p><br></p><p>記憶:</p><p>受檢例外像一張掛號信。收到後不能假裝沒看到，必須:自己簽收處理或轉寄給上一層</p><p><br></p><p>口訣:</p><p>【Checked 要交代: 不是catch，就是throws。】—&gt; 【受檢不放生】</p><p><br></p><p><br></p>",
			30,
			new String[]{"<p>A) catch(FileNotFoundException | Exception e){}</p>", "<p>B) catch(FileNotFoundException e){}</p><p>\tcatch(IndexOutOfBoundsException e){}</p>", "<p>C) catch(Exception e){}</p>", "<p>D) catch(IndexOutOfBoundsException e){}</p><p>\tcatch(FileNotFoundException e){}</p>", "<p>E) catch(FileNotFoundException | IndexOutBoundException e){}</p>"},
			2
		);

		// Q31
		createQuestion(
			"<p>Given</p><p><br></p><p>char[] characters=new char[100];</p><p>try(FileReader reader=new FileReader(\"file_to_path\");){</p><p>        //line 1</p><p>            System.out.println(String.valueOf(characters));</p><p>}catch(IOException e){</p><p>        e.printStackTrace();</p><p>} </p><p><br></p><p>You want to read data through the reader object. </p><p><br></p><p>Which statment inserted on line1 will accomplish this?</p>",
			"<p>卷ch4-ex4 你想透過讀取器物件讀取資料；</p><p>在第1行插入哪一語句可以達到此目的?</p><p><br></p><p>答案:C)reader.read(characters);</p><p><br></p><p>FileReader.read(char[])會嘗試把字元投入給定陣列，並回傳實際讀取數量或-1，try-with-resources會在結束時自動關閉reader</p><p>reader是讀書的人，characters是裝字的袋子，reader.read(袋子)就是把字放入袋子。</p><p><br></p><p>記憶:</p><p>【誰讀? reader； 讀到哪? characters。直接組成: reader.read(characters); 】</p>",
			30,
			new String[]{"<p>A) reader.readLine()</p>", "<p>B) characters = reader.read()</p>", "<p>C) reader.read(characters);</p>", "<p>D) charaters.read();</p>"},
			2
		);

		// Q32
		createQuestion(
			"<p>Given:</p><p><br></p><p>public class ExSuper extends Exception{</p><p>    private final int eCode;</p><p>    public ExSuper(int eCode,Throwable cause){</p><p>        super(cause);</p><p>        this.eCode=eCode;</p><p>    }</p><p><br></p><p>    public ExSuper(int eCode,String msg,Throwable cause){</p><p>        super(msg,cause);</p><p>        this.eCode=eCode;</p><p>    }</p><p><br></p><p>    public String getMessage(){</p><p>        return this.eCode + \":\"+         </p><p>        super.getMessage()+\"_\"+</p><p>        this.getCause().getMessage();        </p><p>    }</p><p>}</p><p><br></p><p>public class ExSub extends ExSuper{</p><p>    public ExSub(int eCode,String msg,Throwable cause)</p><p>    {</p><p>        super(eCode,msg,cause);</p><p>    }</p><p>}</p><p><br></p><p>and the code fragment:</p><p><br></p><p>try{</p><p>    String param1=\"oracle\";</p><p>    if(param1.equalslgnoreCase(\"oracle\")){        </p><p>        throw new ExSub(9001,\"APPLICATION ERROR-9001\",new </p><p>        FileNotFoundException(\"MyFile.txt\"));</p><p>    }    </p><p>    throw new ExSuper(9001,new FileNotFoundException(\"MyFile.txt\")); // Line1</p><p>}</p><p>catch(ExSuper ex)</p><p>{</p><p>    System.out.println(ex.getMessage());</p><p>}</p><p><br></p><p>What is the result?</p>",
			"<p>卷ch4-ex5 上述程式碼執行結果為何?</p><p><br></p><p>答案:C)9001:APPLICATION ERROR-9001-MyFile.txt</p><p>//把這三部分用包含的分隔符（冒號 : 與 連字號 -）串接起來：</p><p>//9001 + : + APPLICATION ERROR-9001 + - + MyFile.txt</p><p><br></p><p>//多型(Polymorphism)、例外繼承及字串組裝邏輯</p><p>public class ExSuper extends Exception{</p><p>    private final int eCode;</p><p>    public ExSuper(int eCode,Throwable cause){</p><p>        super(cause);</p><p>        this.eCode=eCode;</p><p>    }</p><p><br></p><p>    public ExSuper(int eCode,String msg,Throwable cause){</p><p>        super(msg,cause);</p><p>        this.eCode=eCode;</p><p>    }</p><p><br></p><p>    public String getMessage(){</p><p>        return this.eCode + \":\"+ </p><p>        //1.傳入錯誤碼 9001</p><p>        super.getMessage()+\"_\"+</p><p>        //2.呼叫父類別 Exception 訊息，</p><p>        //傳入的msg參數: \"APPLICATION ERROR-9001\"</p><p>        this.getCause().getMessage();</p><p>        //3.cause 是 FileNotFoundException(\"MyFile.txt\")，</p><p>        //訊息為\"MyFile.txt\"</p><p>    }</p><p>}</p><p><br></p><p>public class ExSub extends ExSuper{</p><p>    //ExSub 是 ExSuper的子類別，會被 catch(ExSuper ex)捕捉</p><p>    public ExSub(int eCode,String msg,Throwable cause)</p><p>    {</p><p>        super(eCode,msg,cause);</p><p>    }</p><p>}</p><p><br></p><p>and the code fragment:</p><p><br></p><p>try{</p><p>    String param1=\"oracle\";</p><p>    if(param1.equalslgnoreCase(\"oracle\")){</p><p>        //if 結果為true</p><p>        throw new ExSub(9001,\"APPLICATION ERROR-9001\",new </p><p>        FileNotFoundException(\"MyFile.txt\"));</p><p>    }</p><p>    //程式發射(throw)，ExSub例外，後方的Line1 完全部會被執行</p><p>    throw new ExSuper(9001,new FileNotFoundException(\"MyFile.txt\")); // Line1</p><p>}</p><p>catch(ExSuper ex)</p><p>{</p><p>    System.out.println(ex.getMessage());</p><p>    //ex.getMessage()呼叫在ExSuper 被覆蓋(Override)的 getMessage()方法</p><p>}</p><p><br></p><p>參數等於oracle，所以拋出ExSub，ExSub呼叫父類別含消息和cause的建構子，覆寫的getMessage()把錯誤碼，父類別消息和cause的消息拼接起來</p><p><br></p><p>記憶:</p><p>製作一張錯誤標籤 : 錯誤碼 : 主訊息-原因訊息</p><p><br></p><p>口訣:</p><p>【自訂訊息別用猜，照著return 從左拚到右】</p><p>本題專用: 碼 、 冒號 、主訊息、橫線、 原因。</p>",
			30,
			new String[]{"<p>A) Compilations fails at Line 1;</p>", "<p>B) 9001: java.io.FileNotFoundException:MyFile.txt-MyFile.txt</p>", "<p>C) 9001: APPLICATION ERROR-9001-MyFile.txt</p>", "<p>D) 9001: APPLICATION ERROR-9001-MyFile.txt</p><p>\t9001: java.io.FileNotFoundException: MyFile.txt-MyFile.txt</p>"},
			2
		);

		// Q33
		createQuestion(
			"<p>Given:</p><p><br></p><p>public class Option{</p><p>\tpublic static void main(String[] args){</p><p>\t\tSystem.out.println(\"Ans:\"+convert(\"a\").get());</p><p>\t}</p><p>\t</p><p>\tprivate static Optional&lt;Integer&gt;convert(String s){</p><p>\t\ttry{</p><p>\t\t\treturn Optional.of(Integer.parselnt(s));</p><p>\t\t}catch(Exception e){</p><p>\t\t\treturn Optional.empty();\t\t\t</p><p>\t\t}</p><p>\t}</p><p>}</p><p><br></p><p>What is the result?</p>",
			"<p>卷ch4-ex6 下面程序的運行結果是甚麼?</p><p><br></p><p>C) A java.util.NoSuchFilementException is thrown at run time 運行時拋出了一個 java.util.NoSuchFilementException 異常</p><p><br></p><p>public class Option{</p><p>\tpublic static void main(String[] args){</p><p>\t\tSystem.out.println(\"Ans:\"+convert(\"a\").get());</p><p>\t}</p><p>\t//進入convert方法，執行Integer.parseInt(\"a\");</p><p>\t//因字串\"a\"不是數字，會拋出NumberFormatException </p><p><br></p><p>\tprivate static Optional&lt;Integer&gt;convert(String s){</p><p>\t\ttry{</p><p>\t\t\treturn Optional.of(Integer.parselnt(s));</p><p>\t\t}catch(Exception e){</p><p>\t\t\treturn Optional.empty();</p><p>\t\t\t//捕捉到例外後，執行return Opeional.empty();</p><p>\t\t\t//回傳是一個空的Optional物件</p><p>\t\t}</p><p>\t}</p><p>}</p><p>//執行.get()</p><p>//Java 的Optional.get()方法規定，若Optional物件是空</p><p>//呼叫.get()會直接拋出 NoSuchFilementException</p><p><br></p><p>記憶:</p><p>Optional 像一個便當盒。empty()=空便當盒 、get()=硬把飯拿出來空盒硬拿，當然找不道東西。</p><p><br></p><p>口訣:</p><p>【空盒不能硬 get，硬 get 就 NoSuchElement】</p><p>諧音: NoSuchElement = 沒有這個東西</p>",
			30,
			new String[]{"<p>A) Ans:</p>", "<p>B) Ans:a</p>", "<p>C) A java.util.NoSuchFilementException is thrown at run time</p>", "<p>D) The compilation fails</p>"},
			2
		);

		// Q34
		createQuestion(
			"<p>Given:</p><p><br></p><p>import java.io.*;</p><p><br></p><p>public class Tester{</p><p>\tpublic static void main(String[] args) </p><p>\t{</p><p>\t\ttry{</p><p>\t\t\tdoA();</p><p>\t\t\tdoB();</p><p>\t\t}catch(IOException e){</p><p>\t\t\tSystem.out.print(\"C\");</p><p>\t\t\treturn;</p><p>\t\t}finally{</p><p>\t\t\tSystem.out.print(\"d\");</p><p>\t\t}</p><p>\t\tSystem.out.print(\"f\");</p><p>\t}</p><p>\tprivate static void doA(){</p><p>\t\tSystem.out.print(\"a\");</p><p>\t\tif(false){</p><p>\t\t\tthrow new IndexOutOfBoundsException();</p><p>\t\t}</p><p>\t}</p><p>\tprivate static void doB() throws FileNotFoundException{</p><p>\t\tSystem.out.print(\"b\");</p><p>\t\tif(true)</p><p>\t\t{</p><p>\t\t\tthrow new FileNotFoundException();</p><p>\t\t}</p><p>\t}\t</p><p>}</p><p><br></p><p>What is the result?</p>",
			"<p>卷 ch4-ex7 上述程式執行結果為何?</p><p>D) abcd</p><p><br></p><p>doA輸出 a； doB先輸出 b再拋 FileNotFoundException，catch(IOException) 輸出 c，並執行 return，但 return 之前finally 仍執行輸出 d；finally 後面的 f 就不再執行</p><p><br></p><p>記憶:</p><p>有人準備從公司下班: return = 準備離開finally = 離開前一定要打卡，打卡完才真正來開。</p><p><br></p><p>口訣:</p><p>【return 只是想走，finally 打卡後才能走】</p>",
			30,
			new String[]{"<p>A) The compilation fails.</p>", "<p>B) adf</p>", "<p>C) abc</p>", "<p>D) abcd</p>", "<p>E) abdf</p>"},
			3
		);

		// Q35
		createQuestion(
			"<p>Given:</p><p><br></p><p>public class Test{</p><p>\t\tprivate int num=1;</p><p>\t\tprivate int div=0;</p><p><br></p><p>\t\tpublic void divide(){</p><p>\t\t\ttry{</p><p>\t\t\t\tnum=num/div;</p><p>\t\t\t\tSystem.out.print(\"Exception\");</p><p>\t\t\t}</p><p>\t\t\tcatch(ArithmeticException ae){num=100;}</p><p>\t\t\tcatch(Exception e){num=200;}</p><p>\t\t\tfinally{num=300;}</p><p>\t\t\tSystem.out.print(num);</p><p>\t}</p><p>\tpublic static void main(String[] args) {</p><p>\t\tch4ex8 test = new Test();</p><p>\t\ttest.divide();</p><p>\t}</p><p>}</p>",
			"<p>卷ch4-ex8 上述程式執行結果為何?</p><p>C)300</p><p><br></p><p>1/0 拋出ArthmeticException，進入第一個catch把num設為100，隨後finally必定執行，把num改為300，最後打印300</p><p><br></p><p>記憶:</p><p>Catch 像醫院掛號；找到第一個對症醫生就停止，不會每一科都看；最後finally像總務人員，又把數值改成300。</p><p><br></p><p>口訣:</p><p>【catch 中一個就停、finally最後改變，或:先變100，最後被300蓋過】</p>",
			30,
			new String[]{"<p>A) 200</p>", "<p>B) 100</p>", "<p>C) 300</p>", "<p>D) Exception</p>"},
			2
		);

		// Q36
		createQuestion(
			"<p>Given:</p><p><br></p><p>public class Test{</p><p>\tprivate int sum;</p><p>\tpublic int compute(){</p><p>\tint x=0;</p><p>\twhile(x&lt;3){</p><p>\t\tsum+=++x;\t\t</p><p>\t}</p><p>\t\treturn sum/4; </p><p>\t}</p><p>\tpublic static void main(String[] args) {</p><p>\t\tTest t = new Test();</p><p>\t\tint sum = t.compute();</p><p>\t\tsum = t.compute();</p><p>\t\tSystem.out.print(sum);</p><p>\t}</p><p>}</p><p><br></p><p>What is the output?</p>",
			"<p>卷ch4-ex9 上述程式執行結果為何?</p><p>ans: B)3</p><p>實列變數sum的預設值與生命週期:</p><p>private int sum；是類別的欄位(實例變數)，當new Test()</p><p>建立物件時，sum的預設值為0。</p><p>最重要的一點:sum屬於物件t，不會因為compute()方法執行</p><p>結束而被清空。第二次呼叫t.compute()時，sum會繼續使用</p><p>上次留下來的值。</p><p>前置遞增 ++x:</p><p>++x 是【先將x加1，再拿加完後的值進行計算/累加】</p><p><br></p><p>程式碼註解:</p><p>class ch4ex9{</p><p>\tprivate int sum;</p><p>\tpublic int compute(){</p><p>\tint x=0;</p><p>\twhile(x&lt;3){</p><p>\t\tsum+=++x;</p><p>\t\t//第一圈 sum+=1 -&gt;t.sum =&gt; 6+1=7</p><p>\t\t//第二圈 sum+=2         =&gt; 7+2=9</p><p>\t\t//第三圈 sum+=3         =&gt; 9+3=12</p><p>\t}</p><p>\t\treturn sum/4; //12/4 =3</p><p>\t}</p><p>\tpublic static void main(String[] args) {</p><p>\t\tch4ex9 t = new ch4ex9();</p><p>\t\tint sum = t.compute(); </p><p>\t\t//第一次 x累加到 6，sum回傳1</p><p>\t\tsum = t.compute();</p><p>\t\t//第二次 x累加到 12 呼叫回傳3，賦值給main裡的區域變數sum</p><p>\t\tSystem.out.print(sum);</p><p>\t}</p><p>}</p><p><br></p><p>步驟拆解:</p><p>++x (先加後用)</p><p>。先將 x 的值加1，此時 x 變為 1。</p><p>sum + =1</p><p>。等同於 sum = sum + 1</p><p>。將 sum 原本的值加上 1 後再存回sum</p><p>解果:</p><p>。 x 的值: 1</p><p>。 sum 的值: 原值 + 1</p><p>第一次compute把實例欄位 sum 從 0 累加 1+2+3 = 6，回傳整數除法6/4 = 1，第二次繼續在同一欄位上累加到12，回傳12 / 4 ，最後打印第二次回傳3</p><p><br></p><p>記憶:</p><p>畫面x像每天歸零的計步器；實例欄位sum像銀行存款，第二天仍保留。</p><p><br></p><p>口訣:</p><p>【區域變數每次重生，實例欄位繼續累積】</p><p>本專題:第一次存6得1，第二次存12得3</p>",
			30,
			new String[]{"<p>A) 6</p>", "<p>B) 3</p>", "<p>C) An exception is thrown at runtime</p>", "<p>D) 9</p>"},
			1
		);

		// Q37
		createQuestion(
			"<p>Which of the following fills the blank so this code compiles?</p><p><br></p><p>public static void getExceptions(Collection&lt;__&gt;coll){</p><p>\tcoll.add(new RuntimeException());</p><p>\tcoll.add(new Exception());</p><p>}</p>",
			"<p>卷ch5_ex1 說明:</p><p>下列哪個選項可以填入空白處，讓這個程式碼能夠編譯通過?</p><p>C) ? super Exception</p><p><br></p><p>方法需要向集合中加入Exception和RuntimeException，使用Collection&lt;?super Exception表示元素類型別是Exception或其父類別，因此這兩個對象都可安全加入。</p><p><br></p><p>口訣:</p><p>【要往裡面塞，用super】→【塞資料找叔伯】</p><p><br></p><p>補充: (PESC 原則):</p><p>Producer Extends (從集合讀取資料用 extends) </p><p>Consumer Super (往集合寫入資料用 super)</p><p>在本題是【接收消費資料】，因為程式執行了 coll.add(...) 進行寫入，所以必需選擇 ? super Exception</p>",
			30,
			new String[]{"<p>A.?</p>", "<p>B.? extends Exception</p>", "<p>C.? super Exception</p>", "<p>D. None of the above</p>"},
			2
		);

		// Q38
		createQuestion(
			"<p>What does the following output?</p><p><br></p><p>18.List&lt;String&gt; list = List.of(</p><p>19.\t\"Mary\",\"had\",\"a\",\"little\",\"lamb\");</p><p>20.\t\tSet&lt;String&gt; set = new HashSet&lt;&gt;(list);</p><p>21. set.addAll(list);</p><p>22. for(String sheep:set)</p><p>23.\t\t if(sheep.length()&gt;1)</p><p>24.\t\t\t    set.remove(sheep);</p><p>25. System.out.println(set);</p>",
			"<p>卷ch5_ex2 說明:</p><p><br></p><p>以下程式碼的輸出康結果是何?</p><p>E) The code throws an exception at runtime</p><p>//程式碼在運行拋出ConcurrentModificationException</p><p><br></p><p>for-each 迴圈本質:語法糖for(String sheep : set)在背後其實用Iterator(迭代器)來逐一讀取元素</p><p><br></p><p>結構修改檢查:呼叫 <span style=\"color: rgb(207, 81, 72);\">set.remove(sheep)</span>直接從 hashSet移除元素時，改變set 的內部結構</p><p><br></p><p>觸發保護機制:疊代器在下一次嘗試讀取下一個元素(hasNext()或 next())時，迭代器檢測到集合被併行修改，於是會立刻拋出 <span style=\"color: rgb(207, 81, 72);\">java.util.ConcurrentModificationException</span>例外。</p><p><br></p><p>口訣:</p><p>【走訪時，直接刪，肯定崩(拋出例外)；要安全，Iterator / removelf 最穩定】</p><p><br></p><p>補充:</p><p>若要安全移除長度大於1的元素，應修改為</p><p>set.removeIf(sheep → sheep.length() &gt; 1 );</p><p><br></p>",
			30,
			new String[]{"<p>A. [a.lamb,had,Mary,little]</p>", "<p>B. [a]</p>", "<p>C. [a,a]</p>", "<p>D. The code does not compile</p>", "<p>E. The code throws exception at runtime</p>"},
			4
		);

		// Q39
		createQuestion(
			"<p>Given:</p><p><br></p><p>ArrayList&lt;Integer&gt; a1 = new ArrayList&lt;&gt;();</p><p>a1.add(1);</p><p>a1.add(2);</p><p>a1.add(3);</p><p><br></p><p>Iterator&lt;Integer&gt; itr = a1.iterator();</p><p>while(itr.hasNext()){</p><p>\tif(itr.next()==2){ </p><p>\t\t\t\ta1.remove(2);</p><p>\t\t\tSystem.out.print(itr.next());</p><p>\t\t\t}</p><p>}</p><p><br></p><p>What is the result?</p>",
			"<p>卷ch5_ex3 說明:</p><p>下面Iterator代碼會產生甚麼結果?</p><p>C) A Concurrent Modification Exception is thrown at run time</p><p>//運行時拋出【Concurrent Modification Exception】</p><p><br></p><p>。 a1.remove(2) 踩雷: 呼叫的是集合自己的方法( a1.remove，次處是刪除index 2元素) 這會改變 ArrayList 內部的修改器( modCount++)</p><p><br></p><p>。 迭代器檢測衝突: 下一行立即執行 itr.next()，而每次呼叫 next() 時會檢查 modCount == expectedModCount。發現兩者不一致，立即引爆Fail-Fast機制拋出例外</p><p><br></p><p>解析:</p><p>import java.util.*;</p><p><br></p><p>class ch5ex3{</p><p>\tpublic static void main(String[] args) {</p><p>\t\tArrayList&lt;Integer&gt; a1 = new ArrayList&lt;&gt;();</p><p>\t\ta1.add(1);</p><p>\t\ta1.add(2);</p><p>\t\ta1.add(3);</p><p><br></p><p>\t\tIterator&lt;Integer&gt; itr = a1.iterator();</p><p>\t\twhile(itr.hasNext()){</p><p>\t\t\tif(itr.next()==2){ </p><p>\t\t\t\t//第一圈 itr.net()取得1 !=2 下一圈</p><p>\t\t\t\t//第二圈 itr.net()取得2 條件成立</p><p>\t\t\t\ta1.remove(2);</p><p>\t\t\t\t//執行a1.remove(2) 直接叫a1 把索引2元素刪掉</p><p>\t\t\t\t//a1 內部變數 modCount 被增加</p><p>\t\t\t\tSystem.out.print(itr.next());</p><p>\t\t\t\t//執行itr.next():Iterator 被要求下一個元素</p><p>\t\t\t\t//檢查內部預期次數與a1的modCount 不一致，</p><p>\t\t\t\t//Iterator 認定被非法修改，為防止未定義</p><p>\t\t\t\t//立即拋出 CouncurrentModificationException!</p><p>\t\t\t}</p><p>\t\t}</p><p>\t}</p><p>}</p><p>//答案:Exception in thread \"main\" java.util.ConcurrentModificationException</p><p>/*</p><p>//集合與疊代器(Iterator)觀念</p><p><br></p><p>口訣:</p><p>【迭代走訪中，別用List刪；偷動Collection，下次next()必翻車!】</p><p><br></p><p>對比:</p><p>集合直接刪(錯誤) : a1.remove(…) → 拋出 ConcurrentModificationException</p><p>迭代器安全刪(正確): itr.remove() → 安全刪除當前元素，不拋例外</p><p><br></p>",
			30,
			new String[]{"<p>A) 1 2 followed by an exception</p>", "<p>B) 1 2 3 followed by an exception</p>", "<p>C) A ConcurrentModificationException iS thrown at run time</p>", "<p>D) 1 2 4 5</p>"},
			2
		);

		// Q40
		createQuestion(
			"<p>Givent:</p><p><br></p><p>import java.util.ArrayList;</p><p>import java.util.Arrays;</p><p>public class NewMain{</p><p>\tpublic static void main(String[] args){</p><p>\t\tString[] catNames = {\"abyssinian\",\"oxicat\",\"korat\",\"laprm\",\"bengal\",\"sphynx\"};</p><p><br></p><p>\t\tvar cats = new ArrayList&lt;&gt;(Arrays.asList(catNames));</p><p>\t\tcats.sort((var a,var b)-&gt;-a.compareTo(b));</p><p>\t\tcats.forEach(System.out::println);</p><p>\t}</p><p>}</p><p><br></p><p>What is result?</p>",
			"<p>卷ch5_ex4 說明:</p><p>下面排序程序的訊息結果是甚麼?</p><p>B) </p><p>sphynx </p><p>oxicat </p><p>laperm </p><p>korat </p><p>bengal </p><p>abyssinian</p><p><br></p><p>經過-a.compareTo(b)倒序處置後，會由Z到A輸出，結果如下；</p><p>import java.util.ArrayList;</p><p>import java.util.Arrays;</p><p><br></p><p>class ch5ex4{</p><p>\tpublic static void main(String[] args) {</p><p>\t\tString[] catNames = {\"abyssinian\",\"oxicat\",\"korat\",\"laprm\",\"bengal\",\"sphynx\"};</p><p><br></p><p>\t\tvar cats = new ArrayList&lt;&gt;(Arrays.asList(catNames));</p><p>\t\tcats.sort((var a,var b)-&gt;-a.compareTo(b)); //關鍵程式碼</p><p>\t\t//compareTo(b)預設字母排序，依照字典順序A-Z進行升幕比較</p><p>\t\t//加入負號 - (反轉排序)，變成降幕</p><p>\t\tcats.forEach(System.out::println);</p><p>\t}</p><p>}</p><p><br></p><p>比較器回傳-a.compareTo(b)，相當於把自然排序結果取反，因此按降序排列，forEach(System.out.println)，每個元素單獨占一行</p><p><br></p><p>記憶:</p><p>a.compareTo(b)原本像電扶梯往上: A → Z 前面加負號，就像按下反轉開關: Z → A</p><p><br></p><p>口訣:</p><p>【compareTo 正常升幕，前面負號整隊反轉。】、【比較前加負，順序就倒數】</p>",
			30,
			new String[]{"<p>A)nothing</p>", "<p>B) sphynx</p><p>oxicat</p><p>laperm</p><p>korat</p><p>bengal</p><p>abyssinian</p>", "<p>C) abyssinian</p><p>oxlcat</p><p>korat</p><p>laperm</p><p>bengal</p><p>sphynx</p>", "<p>D) abyssinian</p><p>bengal</p><p>korat</p><p>laperm</p><p>OXICat</p><p>sphynx</p>"},
			1
		);

		// Q41
		createQuestion(
			"<p>What is possible output of the following application?</p><p><br></p><p>import java.url.*;</p><p>import java.util.stream.*;</p><p>public class Thermometer{</p><p>\tprivate String feelsLike;</p><p>\tprivate double temp;</p><p>\t@Override public String toString(){return feelsLike;}</p><p>\t//Constructor/Getters/Setters Omitted</p><p><br></p><p>\tpublic static void main(String... season){</p><p>\t\tvar readings = List.of(new Thermometer(\"HOT!\",72),</p><p>\t\t\tnew Thermometer(\"Too Cold\",0),</p><p>\t\t\tnew Thermometer(\"Just right!\"72));</p><p>\t\treadings</p><p>\t\t\t.parallelStream()    //k1</p><p>\t\t\t.collect(Collectors.groupingByConcurrent(</p><p>\t\t\t\tThermometer::getTemp))  //k2</p><p>\t\t\t.forEach(System.out::println); //k3\t\t\t</p><p>\t}</p><p>}</p>",
			"<p>卷ch6_ex1 說明:</p><p>以下應用程式可能的輸出結果是甚麼?</p><p>public static void main(String... season){</p><p>\t\tvar readings = List.of(new Thermometer(\"HOT!\",72),</p><p>\t\t\tnew Thermometer(\"Too Cold\",0),</p><p>\t\t\tnew Thermometer(\"Just right!\"72));</p><p>\t\treadings</p><p>\t\t\t.parallelStream()    //k1</p><p>\t\t\t.collect(Collectors.groupingByConcurrent(</p><p>\t\t\t\tThermometer::getTemp))  //k2</p><p>\t\t\t.forEach(System.out::println); //k3</p><p>\t\t\t//k3行:對ConcurrentMap呼叫</p><p>\t\t\t//.forEach(System.out::println)</p><p>\t\t\t//傳遞只有一個參數的方法參考(System.out.println)</p><p>\t\t\t//給需要兩個參數Map.forEach，型態不符合，因此</p><p>\t\t\t//在k3行會發生編譯失敗</p><p>}</p><p><br></p><p>答案:E) The code does not compile because of line k3</p><p>//由於第k3行，程式碼無法編譯；型態不匹配(k3行): Map.forEach()需要BiConsumer(Key,Value)， 但 System.out.println 只能接收單一參數，導致k3無法通過編譯。</p><p>groupingByConcurrent(…)的collect結果是一個map，而map.forEach需要BiConsumer&lt;K,V&gt;，System.out.println只匹配單參數Consumer，不能為map.forEach的參數</p><p><br></p><p>記憶:</p><p>題目抓字看到: collect(groupingByConcurrent(…)) 要立即想到結果不是Stream，而是:</p><p>ConcurrentMap&lt;K, List&lt;T&gt;&gt; ，後面呼叫的是: Map.forEach(…)</p><p><br></p><p>口訣:</p><p>【分組收完變Map，Map的forEach要key、Value 要兩個人】</p>",
			30,
			new String[]{"<p>A) {0.0=[Cold], 72.0=[Hot!,Just right!]}</p>", "<p>B) {0.0=[Cold!], 72.0=[Just right!], 72.0=[HOT!]}</p>", "<p>C) The code dose not compile because of line k1</p>", "<p>D) The code dose not compile because of line k2</p>", "<p>E) The code dose not compile because of line k3</p>", "<p>F) None of the above</p>"},
			4
		);

		// Q42
		createQuestion(
			"<p>What is the output of the following application?</p><p><br></p><p>package lot;</p><p>import java.util.function.*;</p><p><br></p><p>public class Warehouse{</p><p>\tprivate int quantity= 40;</p><p>\tprivate final BooleanSupplier stock;</p><p>\t{</p><p>\t\tstock=()-&gt;quantity&gt;0;</p><p>\t}</p><p><br></p><p>\tpublic void checkInventory(){</p><p>\t\tif(stock.get())</p><p>\t\t\tSystem.out.print(\"Plenty\");</p><p>\t\telse{</p><p>\t\t\tSystem.out.print(\"On Backorder!\");</p><p>\t\t}</p><p>\t}</p><p><br></p><p>\tpublic static void main(String... widget){</p><p>\t\tfinal Warehouse w13=new Warehouse();</p><p>\t\tw13.checkInventory();</p><p>\t}</p><p>}</p>",
			"<p>卷ch6-ex2 說明:</p><p>下列應用程式的輸出是甚麼?</p><p>C) The code does not compile because of the checkInventory() method.</p><p>//由於checkInventory()方法問題，程式碼無法編譯；</p><p><br></p><p>checkInventory()中使用stock.<span style=\"color: rgb(207, 81, 72);\">get()</span>，但BooleanSupplier 正確名稱應為stock.<span style=\"color: rgb(207, 81, 72);\">getAsBoolean()</span> 因呼叫不存在的方法，故checkInventory()這一行方生編譯錯誤</p><p><br></p><p>記憶:</p><p>普通Supplier 拿的是【包裝好的Boolean變單】: get()BooleanSupplier直接拿【原味boolean】: getAsBoolean()</p><p><br></p><p>口訣: </p><p>【BooleanSupplier 不是 get，要getAsBoolean】</p><p><br></p><p>專門處理基本型別的介面，方法名稱通常帶As型別: BooleanSupplier → getAsBoolean()IntSupplier → getAsInt()LongSupplier → getAsLong()DoubleSupplier → getAsDouble()</p><p>正確寫法 if(stock.getAsBoolean()){ System.out.print(”Plenty!”)}</p>",
			30,
			new String[]{"<p>A. Plenty</p>", "<p>B. On Backorder!</p>", "<p>C. The code does not compile because of the checkInventory() method.</p>", "<p>D. The code does not compile for a different reason</p>"},
			2
		);

		// Q43
		createQuestion(
			"<p>Which code fragment represents a valid Comparator implementation?</p>",
			"<p>卷ch6-ex3 說明:</p><p>哪個程式碼片段代表有效的Compator 實作?</p><p>D)new Comparator&lt;String&gt;(){</p><p>\t public int compare(String str1,String str2){</p><p>\t\t return str1.compareTo(str2); </p><p>\t } </p><p> };</p><p><br></p><p>完全符合Comparator規範</p><p>1.使用匿名內部實作Comparator&lt;String&gt;介面</p><p>2.正確覆寫方法，public int compare(String str1,String str2)</p><p>3,方法名稱、參數型別與數量、回傳型別(int)完全符合規範</p><p><br></p><p>分析:</p><p>A)new Comparator&lt;String&gt;(){</p><p>\tpublic int compareTo(String str1,String str2){</p><p>\t\treturn str1.compareTo(str2);</p><p>\t}</p><p>};</p><p>//方法名稱錯誤，Comparator 介面定義方法叫compare，</p><p>//而不是compareTo</p><p><br></p><p>B)public class Comps implements Comparator{</p><p>\tpublic boolean compare(Object obj1,Object obj2){</p><p>\t\treturn obj1.equals(obj2);</p><p>\t}</p><p>}</p><p>//回傳型別錯誤，compare方法回傳型態必須是int</p><p><br></p><p>C)public class Comps implements Comparator{</p><p>\tpublic int compare(String str1,String str2){</p><p>\t\treturn str1.length()-str2.length();</p><p>\t}</p><p>}</p><p>//Comparator 要加上泛型 -&gt; Comparator&lt;String&gt; 與</p><p>//public 上面要加上 @Override 才能用String</p><p>//未正確覆寫(Override)方法，此處寫String參數會變多載(Overload)</p><p>//而非覆寫，導致編譯失敗</p><p><br></p><p>Comparator&lt;String&gt; 要求實作 public int compare(String, String) D 的參數與回傳型別完全匹配，並回傳字元串比較結果</p><p><br></p><p>口訣:</p><p>【Comparator: 兩個進來，一個int 出去。也就是: 參數:String、 String回傳:int 方法名:compare】、</p><p>【諧音: compare要 倆倆比較，最後給整數裁判分數】</p><p>三秒核對表看到匿名內部類別時檢查:</p><p>。方法是不是 compare?</p><p>。是否有兩個T參數?</p><p>。回傳型別是不是int?</p><p>。權限是否為public?</p><p>四項都符合才合法</p><p><br></p>",
			30,
			new String[]{
				"<p>A)new Comparator&lt;String&gt;(){ </p><p>\tpublic int compareTo(String str1,String str2){</p><p>\treturn str1.compareTo(str2); </p><p>\t} </p><p>};</p>",
				"<p>B)public class Comps implements Comparator{</p><p>  public boolean compare(Object obj1,Object obj2){</p><p>\t return obj1.equals(obj2);</p><p>  }</p><p>};</p>",
				"<p>C)public class Comps implements Comparator{</p><p>  public int compare(String str1,String str2){</p><p>    return str1.length()-str2.length(); </p><p>    } </p><p>};</p>",
				"<p>D)new Comparator&lt;String&gt;(){</p><p>  public int compare(String str1,String str2){</p><p>  return str1.compareTo(str2); </p><p> } </p><p>};</p>"
			},
			3
		);

		// Q44
		createQuestion(
			"<p>Given:</p><p><br></p><p>var fruits = List.of(\"apple\",\"orange\",\"banana\",\"lemon\");</p><p>Optional&lt;String&gt; result=fruits.stream().filter(f-&gt;f.contains(\"n\")).findAny(); //line1</p><p><br></p><p>System.out.println(result.get());</p><p><br></p><p>You replace the on line 1 to use parallelStream.</p><p>Which one is correct?</p>",
			"<p>卷ch6-ex4 說明:</p><p>您需要將第一行中的程式碼替換為 parallelStream</p><p>哪一項是正確的?</p><p><br></p><p>D) The code may produce a different result</p><p>//這段程式碼可成產生不同結果</p><blockquote>條件符合元素不只有一個，表單中含有字母n的字串有三個</blockquote><blockquote>findAny()不保證順序:(多線程)串流會被拆分多個小任務，依照CPU處理時間，findAny()優先返回該結果</blockquote><blockquote>在平行處理排程中，由JVM與作業係同決定，哪一個線程先搶答不固定</blockquote><p>findAny在併行中允許回傳任意匹配元素，套件含有n的字元有orange、banana、lemon，因此結果不保證，一般是回傳第一個，但規範不要求findAny保持訊續。</p><p><br></p><p>記憶:</p><p>findFirst 是按照排隊順序叫第一位；findAny是【誰先舉手就是誰】</p><p><br></p><p>口訣:</p><p>【findAny不挑第一，平行時誰先到就選誰】、【findFirst()確認第一，重順序；finAny隨便一個，平行可能換人】</p><p><br></p><p>循序Stream中，常看起來取得第一個符合者，但題目改為paralleStream(平行流)後，可能由不同執行緒先找到不同的元素</p><p><br></p>",
			30,
			new String[]{"<p>A)The compiation false.</p>", "<p>B)The code will produce the same result</p>", "<p>C) A NoSuchElementException is thrown at run time</p>", "<p>D) The code may produce a different result</p>"},
			3
		);

		// Q45
		createQuestion(
			"<p>Given the code fragment:</p><p><br></p><p>1. var list = List.of(1,2,3,4,5,6,7,8,9,10);</p><p>2. UnaryOperator&lt;Integer&gt; u= i-&gt;i*2;</p><p>3. list.replaceAll(u);</p><p><br></p><p>Which can replace line 2?</p>",
			"<p>卷ch6-ex5 說明:</p><p>哪一行可替換第2行?</p><p>C) UnaryOperator&lt;Integer&gt; u=(var i) → (i * 2);</p><p>Lambda 參數型別與括號規則;</p><p>。當Lambda參數沒有標註型別時，單一參數可省略括號: i -&gt; i*2</p><p>。當使用var 獲明確指定型別時，必須加上圓括號()。</p><p>。C)的寫法(var i)-&gt;(i * 2)符合語法規範，(i*2)加不加都不影響結果</p><p><br></p><p>簡記:</p><p>要Lambda 參數用了var 或寫出具體型別，就一定要加小括號()。因此C)題的(var i)</p><p> 符合語法規範</p><p><br></p><p>口訣:</p><p>【Lambda用var，括號不能省】、【諧音: var要進括號家，不能一個在外趴】</p>",
			30,
			new String[]{"<p>A) UnaryOperator&lt;Integer&gt; u= var i→{return i*2;}</p>", "<p>B) UnaryOperator&lt;Integer&gt; u=i →{return i*2;}</p>", "<p>C) UnaryOperator&lt;Integer&gt; u=(var i) → (i * 2);</p>", "<p>D) UnaryOperator&lt;Integer&gt; u=(int i) → i*2;</p>"},
			2
		);

		// Q46 (複選)
		createQuestionWithFlags(
			"<p>Which two are valid statements?</p>",
			"<p>卷ch6-ex6 說明:</p><p>哪兩個說法是有效的？</p><p>B)BiPredicate&lt;<strong>Integer,Integer</strong>&gt;test=(Integer x,final Integer y)-&gt;(x.equals(y));</p><p>//原因：兩個參數都明確指定了 顯式型別 Integer，樣式一致。其中給參數加上修飾子 final 是完全合法的。</p><p>D)BiPredicate&lt;<strong>Integer,Integer</strong>&gt;test=(<strong>var</strong> x,<strong>final va</strong>r y)-&gt;(x.equals(y));</p><p>//原因：兩個參數都使用了 var，樣式一致。在 Java 11+ 中，var 允許我們在 Lambda 參數上記述 final 或註解（Annotations）。</p><p><br></p><p> 只要考察java Lamba表達式的參照與法規範(包含java11 引入的var關鍵字規則)</p><p><br></p><p>Lambda 參數黃金規則:</p><p>Lambda 表達式宣告多個參數時，所有參數的類宣傳樣式須完全一致，不可混用!</p><p>1.同為顯示型別(Explicit Types):列如(Integer x, Integer y)</p><p>2.同為var 型別(Implicit/Var Types):列如(var x,var y)(java11+)</p><p>3.同為隱式型別(Implicit Type，省略型別):列如(x,y)</p><p><br></p><p>口訣:</p><p>「要嘛全顯示、要嘛全Var、要嘛全省略；修飾子(如final)只能搭配顯示或var使用。」</p>",
			30,
			new String[]{
				"<p>A)BiPredicate&lt;Integer,Integer&gt;test=(final var x,y)-&gt;(x.equals(y));</p>",
				"<p>B)BiPredicate&lt;Integer,Integer&gt;test=(Integer x,final Integer y)-&gt;(x.equals(y));</p>",
				"<p>C)BiPredicate&lt;Integer,Integer&gt;test=(final Integer var x,var y)-&gt;(x.equals(y));</p>",
				"<p>D)BiPredicate&lt;Integer,Integer&gt;test=(var x,final var y)-&gt;(x.equals(y));</p>",
				"<p>E)BiPredicate&lt;Ingeger,Integer&gt;test=(Integer var x, final var y)-&gt;(x.equals(y));</p>"
			},
			new boolean[]{false, true, false, true, false}
		);

		// Q47
		createQuestion(
			"<p>Why would choose to use a peek operation insted of a forEach operation on a Stream?</p>",
			"<p>卷ch6-ex7 說明:</p><p>為何會選擇對stream 使用peek 操作而非forEach操作?</p><p>A)to process the current item and return a stream</p><p>//處理當前元素後仍返回一個stream</p><p><br></p><p>分析:</p><p>//重點在區分java stream 中【中間操作】與【終端襙作】差異</p><p><br></p><p>A)to process the current item and return a stream(正確)</p><p>//這正是peek的定義，執行傳入的動作並回傳Stream，讓你能在流水線中檢觀察資料</p><p><br></p><p>B)to process the current item and return void(X)</p><p>//這是forEach的行為</p><p><br></p><p>C)to remove an item from the beginning of the stream(X)</p><p>//Stream的peek操作不會移除任何元素</p><p><br></p><p>D)to remove an item form the end of the stream(X)</p><p>//同上，peek不會改變Stream內的元素個數或結構</p><p><br></p><p>peek 與 forEach雖然都對stream中的每個元素執行某個動作(通常傳入Consumer)，但它們的管道鍊(Pipeline)中的定位完全不同</p><p><br></p><p>1.peek 是「中間操作（Intermediate Operation）」</p><p>作用:主要用於除錯或處理過程中觀察/印出元素狀態</p><p>特性：它會處理當前元素，並將 Stream 繼續往下傳遞，讓你可以繼續串接其他 Stream 操作（如 filter()、map()、collect() 等）。</p><p><br></p><p>2.forEach 是「終端操作（Terminal Operation）」</p><p>作用:表示Stream流水線的終點</p><p>特性：它會消耗並關閉 Stream，執行完後不能再串接任何 Stream 方法。</p><p><br></p><p>記憶:</p><p>peek(沿途查看/紀錄): 像流水線上的檢查員，他會把產品拿起來看一下、做個紀錄(列入列印Logo)，然後把產品放回傳送帶送走，讓其繼續流向下一站</p><p>forEach(終點打包): 像流水線最後的裝箱打包員，他把產品拿走裝進箱子，流水線到這裡就正式結束，後面不能再接任何工序</p><p><br></p><p>口訣:</p><p>【Peek偷看繼續流，Each到站void帶走】</p><p>。Peek 只是偷看一下(除錯)，回傳Stream繼續流</p><p>。forEach是終點站，回傳void全部帶走/結束</p>",
			30,
			new String[]{"<p>A)to process the current item and return a stream</p>", "<p>B)to process the current item and return void</p>", "<p>C)to remove an item from the beginning of the stream</p>", "<p>D)to remove an item form the end of the stream</p>"},
			0
		);

		// Q48
		createQuestion(
			"<p>Given the content from line.txt</p><p> C</p><p> C++</p><p> Java</p><p> GO</p><p> Kotlin</p><p><br></p><p>and</p><p><br></p><p>String fileName = \"lines.txt\";</p><p>List&lt;String&gt; list = new ArrayList&lt;&gt;();</p><p>try (Stream&lt;String&gt; stream = Files.lines(Paths.get(fileName))) {\t</p><p>    list = stream</p><p>        .filter(line -&gt; !line.equalsIgnoreCase(\"JAVA\"))\t\t</p><p>        .map(String::toUpperCase)\t\t</p><p>        .collect(Collectors.toList());\t\t</p><p>\t\t} catch (IOException e) {</p><p>    </p><p>\t\t}</p><p>list.forEach(System.out::println);</p><p><br></p><p>What is the Result?</p>",
			"<p>卷ch6-ex8 說明:</p><p>讀取Lines.txt後，輸出程序是甚麼?</p><p>C)C</p><p>  C++</p><p>  GO</p><p>  KOTLIN</p><p><br></p><p>解析:</p><p>String fileName = \"lines.txt\";</p><p>List&lt;String&gt; list = new ArrayList&lt;&gt;();</p><p>try (Stream&lt;String&gt; stream = Files.lines(Paths.get(fileName))) {</p><p>\t/*</p><p>\tFiles.lines(Paths.get(fileName))</p><p>\t讀取line.txt檔案，將每一行作為Stream&lt;String&gt;元素處理，此Stream中元素依序為</p><p>\t「\"C\",\"C++\",\"JAVA\",\"GO\",\"Kotlin\"」\t</p><p>\t*/\t</p><p>    list = stream</p><p>        .filter(line -&gt; !line.equalsIgnoreCase(\"JAVA\"))</p><p>\t\t//過濾掉不符合條件的元素</p><p>        .map(String::toUpperCase)</p><p>\t\t//將留下來的每個字串都轉為大寫</p><p>        .collect(Collectors.toList());</p><p>\t\t//將處理完的Stream收集轉回一個List&lt;String&gt;集合</p><p>} catch (IOException e) {</p><p>    e.printStackTrace();</p><p>}</p><p>list.forEach(System.out::println);</p><p><br></p><p>filter排除忽略大小寫等於JAVA的行，其餘字串轉換為大寫並收集到列表，最後forEach按列表順序輸出</p><p><br></p><p>口訣:</p><p>【filter決定留不留，map決定變什麼】、【驚嘆號趕走Java，其餘都大寫】</p>",
			30,
			new String[]{"<p>A) C</p><p>C++</p><p>Go</p><p>Kotlin</p>", "<p>B) JAVA</p>", "<p>C) C</p><p>C++</p><p>GO</p><p>KOTLIN</p>", "<p>D) C</p><p>C++</p><p>JAVA</p><p>GO</p><p>KOTLIN</p>"},
			2
		);

		// Q49 (複選)
		createQuestionWithFlags(
			"<p>Given:</p><p><br></p><p>public class Employee{</p><p>\tprivate String name;</p><p>\tprivate String neighborhood;</p><p>\tprivate int salary;</p><p>\t//Constructors and setter and getter methods go here</p><p>}</p><p><br></p><p>and the code fragment:</p><p><br></p><p>List&lt;Employee&gt;roster=new ArrayList&lt;&gt;();</p><p>Predicate&lt;Employee&gt;p=e-&gt;e.getSalary()&gt;30;</p><p>Function&lt;Employee,Optional&lt;String&gt;&gt;f=</p><p> e-&gt;Optional.ofNullable(e.getNeighborhood());</p><p><br></p><p>Which teo objcet group all employee with a salary greater than 30 by neighborhood?</p>",
			"<p>卷ch6-ex9 說明:</p><p>按社區(nighborhood)進行分組(group by)，並且只收集薪水大於30的員工?</p><p>答案:A) Map&lt;<span style=\"color: rgb(207, 81, 72);\">Optional&lt;String&gt;</span>,<span style=\"color: rgb(80, 148, 110);\">List&lt;Empoloyee&gt;</span>&gt;r4=roster.stream().collect(Collectors.groupingBy(<span style=\"color: rgb(207, 81, 72);\">f</span>, <span style=\"color: rgb(80, 148, 110);\">Collectors.filtering(</span><span style=\"color: rgb(207, 81, 72);\">p</span><span style=\"color: rgb(80, 148, 110);\">,Collectors.toList())</span>));</p><p>// 1.Key:使用Function f 處理</p><p>// 2.Value:只保留薪水&gt;30的員工</p><p>//雖然Key變成了Option&lt;String&gt;(能避免null問題)，但依然成功將員工依據f的結果分組</p><p>//<span style=\"color: rgb(80, 148, 110);\">並透過Collectors.filtering(p,...)過濾薪水&gt;30的員工\t</span></p><p><br></p><p>E) Map&lt;<strong>String</strong>,<strong>List&lt;Employee</strong>&gt;&gt;r1=roster.stream()</p><p>.collect(Collectors.groupingBy(<strong>Employee::getNeighborhood</strong>,</p><p><strong>Collectors.filtering(p,Collectors.toList())</strong>));</p><p>//<span style=\"color: rgb(207, 81, 72);\">1.Key:直接回傳String</span></p><p>//<span style=\"color: rgb(80, 148, 110);\">2.Value:只保留薪水&gt;30的員工</span></p><p>// <span style=\"color: rgb(80, 148, 110);\">使用Collectors.filtering(p,...)正確過濾薪水&gt;30的員工</span></p><p><br></p><p>兩種寫法都使用groupingBy，並把filtering(p,tolist())作為下游收集器，A的關鍵是Optional&lt;String&gt;可表示缺失，E的關鍵是String，但若neighborthood為null，groupingBy可能拋出NullPointerExption，實際使用時要注意。</p><p><br></p><p>補充:</p><p>需要Stream API 提供 Collectors.groupingBy搭配 downstream collector(下游收集器)</p><p>1.Collectors.groupingBy(分類函式，下游收集器)</p><blockquote>第一個參數:指定要用何當作Map的Key(這裡就是neighborhood)</blockquote><blockquote>第二個參數:指定對分出來的每一個群組(Value)要做何處理</blockquote><p>2.Collectors.filtering(條件斷言，下游收集器)</p><blockquote>先用Predicate過濾元素(這裡用p過濾薪水大於30)</blockquote><blockquote>過濾後再將符合條件的元素收集進List</blockquote><p><br></p><p>口訣:</p><p>【groupingBy 先分區，filtering再查薪水】、【外面分組、裡面過濾】</p><p>A與E的差異在；A的Map key:Optional&lt;String&gt; 因為分類函式f回傳Optional，可包住可能為null的neighborhood</p>",
			30,
			new String[]{
				"<p>A) Map&lt;Optional&lt;String&gt;,List&lt;Employee&gt;&gt;r4=roster.stream()</p><p>.collect(Collectors.groupingBy(f,Collectors.filtering(p,Collectors.toList())));</p>",
				"<p>B) Map&lt;Optional&lt;String&gt;,List&lt;Employee&gt;&gt;r2=roster.stream().filter(p)</p><p>.collect(Collectors.groupingBy(f,Employee::getNeighborhood));</p>",
				"<p>C) Map&lt;Optional&lt;String&gt;,List&lt;Employee&gt;&gt;r5=roster.stream()</p><p>.collect(Collectors.groupingBy(Employee::getNeighborhood,</p><p>Collectors.filtering(p,Collectors.toList())));</p>",
				"<p>D) Map&lt;Optional&lt;String&gt;,List&lt;Employee&gt;&gt;r3=roster.stream()</p><p>.filter(p).collect(Collectors.groupingBy(p));</p>",
				"<p>E) </p><p>Map&lt;String,List&lt;Employee&gt;&gt;r1=roster.stream()</p><p>.collect(Collectors.groupingBy(Employee::getNeighborhood,</p><p>Collectors.filtering(p,Collectors.toList())));</p>"
			},
			new boolean[]{true, false, false, false, true}
		);

		// Q50
		createQuestion(
			"<p>Given the code fragment</p><p><br></p><p>public class Main{</p><p>\tpublic static void main(String[] args)</p><p>\t{\t</p><p>\t\tList&lt;String&gt;fruits=List.of(\"banana\",\"orange\",\"apple\",\"lemon\");</p><p>\t\tStream&lt;String&gt;s1=fruist.stream();</p><p>\t\tStream&lt;String&gt;s2=s1.peek(i-&gt;Syste.out.print(i+\" \"));</p><p>\t\tSystme.out.println(\"--------\");\t\t</p><p>\t\tStream&lt;String&gt;s3=s2.sorted();</p><p>\t\tStream&lt;String&gt;s4=s3.peek(i-&gt;System.out.print(i+\"\"));</p><p>\t\tSystem.out.println(\"--------\");\t\t</p><p>\t\tString strFruits=s4.collect(Collectors.joining(\",\"));\t</p><p>\t}</p><p>}</p><p><br></p><p>What is the output?</p>",
			"<p>卷ch6-ex10 說明:</p><p>下列包含peek和sorted的代碼輸入是什麼?</p><p>A)-------- \t&lt;--第一個 println</p><p>  -------- \t&lt;--第二個 println</p><p><span style=\"color: rgb(44, 44, 43);\">banana orange apple lemon </span><span style=\"color: rgb(207, 81, 72);\">&lt;--第一個peek的輸出</span><span style=\"color: rgb(44, 44, 43);\"> apple banana lemon orange </span><span style=\"color: rgb(207, 81, 72);\">&lt;--第二個peek的輸出</span></p><p><br></p><p>觀念:</p><p>1. Stream操作不會立即執行:</p><p>\tpeek()和sorted()都是中間操作(Intermediate Operations)。在沒有呼叫終端操作</p><p>\t(Terminal Operation例如 collect())之前，Stream完全部會開始處理</p><p>2. collect()引發全套計算</p><p>\t程式碼中 s4.collect(Collectors.joining(\",\"))是終端操作，</p><p>\t此時整個Stream管道才會被啟動</p><p><br></p><p><br></p><p>分析:</p><p>public class Main{</p><p>\tpublic static void main(String[] args){</p><p>\t</p><p>\t\tList&lt;String&gt;fruits=List.of(\"banana\",\"orange\",\"apple\",\"lemon\");</p><p>\t\tStream&lt;String&gt;s1=fruist.stream();</p><p>\t\t</p><p>\t\tStream&lt;String&gt;s2=s1.peek(i-&gt;Syste.out.print(i+\" \"));</p><p>\t\tSystme.out.println(\"--------\");</p><p>\t\t//只印出--------，s2只是定義操作，此時peek根本還沒執行</p><p>\t\tStream&lt;String&gt;s3=s2.sorted();</p><p>\t\tStream&lt;String&gt;s4=s3.peek(i-&gt;System.out.print(i+\"\"));</p><p>\t\tSystem.out.println(\"--------\");</p><p>\t\t//再次印出--------，s3與s4都只是中間操作，peek依樣尚未執行</p><p>\t\tString strFruits=s4.collect(Collectors.joining(\",\"));</p><p>\t\t</p><p>\t\t//觸發終端操作collect()，Stream開始真正從頭執行</p><p>\t\t//執行第一個peek s2，Stream 必須讀取所有元素才能進行排序（sorted），因此會先印出原始清單：</p><p>\t\t//banana orange apple lemon</p><p>\t\t//執行排序sorted() </p><p>\t\t//將集合排序為：\"apple\", \"banana\", \"lemon\", \"orange\"。</p><p>\t\t//執行第二個 peek（來自 s4）：處理排序後的元素，依序印出</p><p>\t\t//apple banana lemon orange</p><p>\t}</p><p><br></p><p>}</p><p>Stream操作具有惰性，創建s2，s3，s4時都不會立即執行，兩個println分隔線會先被打印，collect開始消費流後，排序前的peek按原順序觀察元素，排序後的peek按字典順序觀察元素</p><p><br></p><p>記憶:</p><p>Stream像洗衣機預約: peek安排檢查衣服、sorted安排分類，最後collect按下啟動鍵；在按下啟動前，所有的安排都不執行。</p><p><br></p><p>口訣:</p><p>【Stream只安排工作表，終端操作才正式開工】</p>",
			30,
			new String[]{
				"<p>A)--------</p><p>--------</p><p>banana orange apple lemon apple banana lemon orange</p>",
				"<p>B)banana orange apple lemon</p><p>--------</p><p>apple banana lemon orange</p><p>--------</p>",
				"<p>C)--------</p><p>banana orange apple lemon</p><p>--------</p><p>apple banana lemon orange</p>",
				"<p>D)--------</p><p>--------</p>",
				"<p>E)banana orange apple lemon apple banana lemon orange</p><p>--------</p><p>--------</p>"
			},
			0
		);

		// Q51 (複選)
		createQuestionWithFlags(
			"<p>What statement are true about mandated java.base?(Choose two)</p>",
			"<p>卷ch7-ex1 說明:</p><p>關於requires<span style=\"color: rgb(207, 81, 72);\"> mandated java.base</span>，下列哪些敘述是正確的？ （選兩項）</p><p>C)This output is expected when running the <span style=\"color: rgb(207, 81, 72);\">jdeps</span> command</p><p>//執行 jdeps 指令時，預期會輸出此結果。</p><blockquote>jdeps 是Java提供依賴關係分析工具(Java Dependency Analysis Tool)。</blockquote><blockquote>當你針對任何Java模組.class檔執行jdeps分析依賴十，它會輸出該模組使用哪些模組</blockquote><blockquote>(註:選項A與B的指令語法寫錯了，正確的參數名稱分別是--list-modules與 --show-module-resolution，只帶一個雙連字號，因此A、B都是無效指令)。</blockquote><p>E)All modules will <span style=\"color: rgb(207, 81, 72);\">include</span> this in the <span style=\"color: rgb(207, 81, 72);\">output</span></p><p>//所有模組的輸出都將包含此內容</p><blockquote>Java模組系統，java.base是所有模組的基礎(包含基本的java.lang、java.util等類別)</blockquote><blockquote>所有Java模組都會隱式(Implicityly)或顯示地依賴java.base。</blockquote><blockquote>對任何模組執行jdeps進行依賴分系，輸出結果中一定都會包含對java.base的依賴</blockquote><p>核心概念在於java.base 是Java模組系統(JPMS)的最底層核心模組，jdeps是專門用來分析模組與類別依賴關係工具</p><p><br></p><p>總結:</p><p>jdeps用分析模組依賴關係，因所有模組都依賴java.base，所以任何模組執行jdeps時，輸出結果一定都會出現java.base。</p><p><br></p><p>口訣:</p><p>【Mandated 找 CE (See)，jdeps全 (All)都有】</p><p>。 C (jdeps): 只有依賴分析工具jdeps 在分析模組依賴時，會明確標註隱式強制依賴 requires mandated java.base; 。</p><p>。 E (All modules) : 所有的 Java模組(All)都隱式繼承並依賴 java.base，無一例外。</p><p><br></p><p>速記:</p><p>。 C = Check 依賴 (jdeps)</p><p>。 E= Every /E 全部 (All modules)</p><p>。 看到 Mandated java.base → 直接選 C、E</p><p><br></p>",
			30,
			new String[]{
				"<p>A.This output is expected when running the java --list--modules command.</p>",
				"<p>B.This output is expected when running the java --show--module--resolution command</p>",
				"<p>C.This output is expected when running the jdeps command</p>",
				"<p>D.This output is expected when running the jmod command</p>",
				"<p>E.All modules will include this in the output</p>",
				"<p>F.Some modules will include this in the output</p>"
			},
			new boolean[]{false, false, true, false, true, false}
		);

		// Q52
		createQuestion(
			"<p>what is the name of a file that declares a module?</p>",
			"<p>卷ch7-ex2 說明:</p><p>聲明模組的檔案叫什麼?</p><p>F) module-info.java</p><p>java9 引入【java平台模組系統(JPMS)】起，官方規定，每個模組根目錄下，必須有一個名為module-info.java 的檔案，用來聲明與定義該模組的相關資訊。 </p><p><br></p><p>口訣:</p><p>【模組聲明全寫(module)，自帶信息(info)加中槓】</p><p>。全寫不縮寫 : 必須是完整 module，不是縮寫 mod。</p><p>。信息(info) : 模組的描述檔固定使用 -info 結尾</p><p>。連字號(-) : 中間用短橫線連接，固定檔名為 module-info.java。</p><p><br></p><p>速記:</p><p>。 看到 declares a module → 找 F ( Full name : module-info.java ) 。</p>",
			30,
			new String[]{"<p>A. mod.java</p>", "<p>B. mod-data.java</p>", "<p>C. mod-info.java</p>", "<p>D. module.java</p>", "<p>E. module-data.java</p>", "<p>F. module-info.java</p>"},
			5
		);

		// Q53
		createQuestion(
			"<p>Suppose you hava a module that contains a class with a call to exports(ChocolateLab.class).Which part of the module service contains this class?</p>",
			"<p>卷ch7-ex3 說明:</p><p>假設你有一個模組，其中包含一個呼叫了 'exports(Chocolate Lab.class)’的類別。這個類別位於模組服務的哪個部分？</p><p>E)None of the adove</p><p>//以上皆非</p><p><br></p><p>exports 是 module-info.java中的模塊指令，不是普通java方法，也不能以export(class)的方式在類別中呼叫，因此題不屬合法的模塊服務宣告</p><p><br></p><p>記憶:</p><p>海關開放的是一整個貨櫃區: package不是只拿某一件商品:ChocolateLab.class出去</p><p><br></p><p>口訣:</p><p>【exports開放整個包，不單獨出口一個class】、【模組出口看package，不看.class。】</p>",
			30,
			new String[]{"<p>A.Consumer</p>", "<p>B.Service locator</p>", "<p>C.Service provider</p>", "<p>D.Service provider interface</p>", "<p>E.None of the above</p>"},
			4
		);

		// Q54
		createQuestion(
			"<p>How many to these keywords can be used in a module-info.java file:close，export，import，require，and uses?</p>",
			"<p>卷ch7-ex4 說明:</p><p>在 module-info.java 檔案中，可以使用下列哪些關鍵字：close、export、import、require 和 use<span style=\"color: rgb(207, 81, 72);\">s</span>？</p><p>B. One</p><p>此題中，只有use視可用在module-info.java中的合法關鍵字。</p><p><br></p><p>這題是單複數陷阱(export vs export<span style=\"color: rgb(207, 81, 72);\">s</span> 、 require vs require<span style=\"color: rgb(207, 81, 72);\">s</span>)，而close，export，import，require都不是模塊描述符合的指令，所以只有uses 這一個符合</p><p><br></p><p>口訣:</p><p>【模塊指定愛加s，沒有s多半是假貨】</p>",
			30,
			new String[]{"<p>A. None</p>", "<p>B. One</p>", "<p>C. Two</p>", "<p>D. Three</p>", "<p>E. Four</p>", "<p>F. Five</p>"},
			1
		);

		// Q55
		createQuestion(
			"<p>Which module defaults the foundational APIs of the Java SE Platform?</p>",
			"<p>卷ch7-ex5 說明:</p><p>哪個模組預設使用 Java SE 平台的基礎 API?</p><p>A)java.lang //是java 的Package(包) 並非模組</p><p><strong>B)java.base //正確</strong></p><p>C)java.object //java 完全沒有此模組</p><p>D)java.se //此為 超級模組(Aggregator Module) 期可以引出完整的Java SE API集合，但不是提供基礎核心API的預設模組</p><p>ans:</p><p>B)java.base 是基礎模組(Base Module)</p><p><br></p><p>java 9 引入的模組系統(JPMS)中，java.long，java.util，java.io等核心的API位於java.base ，所有命名模塊都會隱式讀取它，其為整個java SE 平台中最核心，最基礎的模組</p><p><br></p><p>記憶:</p><p>把Java DE 想成衣棟大樓 : java.base = 一樓大廳與地基其他模組 = 樓上的不同部門沒有javabase，其他模組也難以運作。</p><p><br></p><p>口訣:</p><p>【Java 的基本盤，就是java.base。】</p>",
			30,
			new String[]{"<p>A) java.laong</p>", "<p>B) java.base</p>", "<p>C) java.object</p>", "<p>D) java.se</p>"},
			1
		);

		// Q56
		createQuestion(
			"<p>What is the output of the following application?</p><p><br></p><p>import java.util.*;</p><p><br></p><p>public class SearchList&lt;T&gt;{</p><p>\tprivate List&lt;T&gt; data;</p><p>\tprivate boolean foundMatch=false;</p><p>\tpublic SearchList(List&lt;T&gt;list){</p><p>\t\tthis.data=list;</p><p>\t}</p><p>\tpublic void exists(T v,int start, int end){</p><p>\t\tif(end-start==0){}</p><p>\t\telse if(end-start==1){</p><p>\t\t\tfoundMatch=foundMatch||v.equals(data.get(start));</p><p>\t\t}else{</p><p>\t\t\tfinal int middle=start+(end-start)/2;</p><p>\t\t\tnew Thread(()-&gt;exists(v,start,middle)).run();</p><p>\t\t\tnew Thread(()-&gt;exists(v,middle,end)).run();</p><p>\t\t}</p><p><br></p><p>\t}</p><p><br></p><p>\tpublic static void main(String[] a)throws Exception{</p><p>\t\tList&lt;Integer&gt;data=List.of(1,2,3,4,5,6);</p><p>\t\tSearchList&lt;Integer&gt; t = new SearchList&lt;Integer&gt;(data);</p><p>\t\tt.exists(5,0,data.size());\t\t</p><p>\t\tSystem.out.print(t.foundMatch);\t</p><p>\t}</p><p>\t</p><p>}</p>",
			"<p>卷ch8-ex1 說明:</p><p>以下應用程式的輸出什麼?</p><p>A)true</p><p><br></p><p>分析:</p><p>//此題為二分搜尋法(Binary Search)與遞迴思維</p><p>import java.util.*;</p><p><br></p><p>public class SearchList&lt;T&gt;{</p><p>\tprivate List&lt;T&gt; data;</p><p>\tprivate boolean foundMatch=false;</p><p>\tpublic SearchList(List&lt;T&gt;list){</p><p>\t\tthis.data=list;</p><p>\t}</p><p>\tpublic void exists(T v,int start, int end){</p><p>\t\tif(end-start==0){}</p><p>\t\telse if(end-start==1){</p><p>\t\t\tfoundMatch=foundMatch||v.equals(data.get(start));</p><p>\t\t}else{</p><p>\t\t\tfinal int middle=start+(end-start)/2;</p><p>\t\t\tnew Thread(()-&gt;exists(v,start,middle)).run();</p><p>\t\t\tnew Thread(()-&gt;exists(v,middle,end)).run();</p><p><br></p><p>\t\t\t  //單執行續同步執行(陷阱)</p><p>\t\t    //程式碼中寫 new Thread(...).run();</p><p>\t\t    //呼叫的是.run()而非.start()!</p><p>\t\t    //.run()不會啟動新的執行續，而是在當前的主執行續中直接同步執行該方法</p><p>\t\t    //因此exists()執行完畢後，t.fondMatch已經被主執行續設為true，</p><p>\t\t    //接續印出true</p><p>\t\t}</p><p>\t}</p><p>\tpublic static void main(String[] a)throws Exception{</p><p>\t\tList&lt;Integer&gt;data=List.of(1,2,3,4,5,6);</p><p>\t\tSearchList&lt;Integer&gt; t = new SearchList&lt;Integer&gt;(data);</p><p>\t\tt.exists(5,0,data.size());</p><p>\t\t//搜尋目標 v=5 範圍從start=0 到 end 6</p><p>\t\tSystem.out.print(t.foundMatch);\t</p><p>\t}</p><p>}</p><p><br></p><p>程式碼呼的是Thread.run()，而不是start()，所以並沒有創建並執行，而是在當前執行序同步遞歸，最終會檢查到元素5，並把fountMath設為true</p><p><br></p><p>記憶:</p><p>你請兩個分身幫忙找東西，但只是喊 : run結果根本沒有叫出分身，而是自己依序做完兩份工作</p><p><br></p><p>口訣:</p><p>【run是自己跑，start才叫分身跑】</p>",
			30,
			new String[]{"<p>A. true</p>", "<p>B. false</p>", "<p>C. The code dose not compile</p>", "<p>D. The result is unknown until runtime</p>", "<p>E. An exception is thrown</p>", "<p>F. Noe of the above</p>"},
			0
		);

		// Q57 (複選)
		createQuestionWithFlags(
			"<p>Which of the following methods is not available on an ExecutorService instance?(Choose two.)</p>",
			"<p>卷ch8-ex2 說明:</p><p>下列哪些方法在 ExcelorService 實例上不可用？（選兩個）</p><p>execute 只接收Runnable，Callable應通過submi或invoke方法提交，ExecutorService也沒有exit()，結束服務通常用shutdown()或shutdown()</p><p><br></p><p>A為什麼錯 ? <strong>ex</strong>ecute(Callable) </p><p>//不可用:execute 僅接收Runnable</p><p>說明:在ExecutorService(及其父介面 Executor)中，execute()方法只解收Running 參數(即 execute(Runnable))</p><p><br></p><p>D為什麼錯 ? <strong>ex</strong>it() </p><p>//不可用: 無此方法，關閉應使用shutdown</p><p>說明:ExecutorService完全沒有 exit()這個方法</p><p><br></p><p>補充:</p><p>B.shutdownNow() 可用:用於立即停止執行器</p><p>C.submit(Runnable) 可用: 提交Runnable並傳Future&lt;?&gt;</p><p>E.submit(Callable) 可用:提交Callable並回傳Future&lt;T&gt;</p><p>F.execute(地Runnable) 可用:繼承自Executor介面</p><p><br></p><p>記憶:</p><p>execute()是單純叫員工去做事，不期待帶結果回來，因此接 Runnable。submit()是正式交件，可以收回Future，因此Runnable 或 Callable都能交</p><p><br></p><p>口訣:</p><p>【ex跑(Runnable) 不Call， sub 兩者通，停機shut，停機shut，ep 根本沒exit】</p><p><br></p><p>。execute只能跑(Run): execute()來自父介面 Executor，只接受 Runnable，沒有 execute(Callable) (即A錯誤)。</p><p>。 submit兩者通 : submit()支援 Runnable 與 Callable，並會回傳 Future (C、E都合法)</p><p>。 關閉用 shut: 關閉線呈池用 shutdown() 或 shutdownNow() (B合法)。</p><p>。 沒有 exit : Java 執行緒池完全沒有 exit() 方法，結束 JVM使用 System.exit() (即 D 錯誤)</p>",
			30,
			new String[]{"<p>A.execute(Callable) </p>", "<p>B.shutdownNow() </p>", "<p>C.submit(Runnable)</p>", "<p>D.exit()</p>", "<p>E.submit(Callable)\t</p>", "<p>F.execute(Runnable)</p>"},
			new boolean[]{true, false, false, true, false, false}
		);

		// Q58
		createQuestion(
			"<p>var c = new CopyOnWriteArrayList&lt;&gt;(List.of(\"1\",\"2\",\"3\",\"4\"));</p><p>Runnable r=()-&gt;{</p><p>\t\ttry{</p><p>\t\t\tThread.sleep(150);</p><p>\t\t}</p><p>\t\tcatch(InterruptedException e){</p><p>\t\t\tSystem.out.println(e);</p><p>\t\t}</p><p>\t\tc.set(3,\"four\");</p><p>\t\tSystem.out.print(c+\"\");</p><p>\t\t};</p><p>Thread t = new Thread(r);</p><p>t.start();</p><p>for(var s:c)</p><p>{</p><p>\t\tSystem.out.print(s+\"\");\t\t\t</p><p>\t\tThread.sleep(100);\t</p><p>}</p><p><br></p><p>What is the output?</p>",
			"<p>卷ch8-ex3 說明:</p><p>CopyOnWriteArrayList範例的輸出是甚麼 ?</p><p><br></p><p>A)12[1,2,3,four]34 </p><p>//12[裡面包1,2,3,four]34</p><p><br></p><p>此只要記住2個核心考點</p><p>1.原理考點:CopyOnWrite的【快照(Snapshot)】</p><blockquote>for-each 迴圈開始時，就已經鎖定當下版本(\"1\",\"2\",\"3\",\"4\")。</blockquote><blockquote>即使子執行緒把元素改為\"four\"，主執行續的迴圈完全不受影響，依然會把舊的\"3\"，\"4\"印完</blockquote><p>2.時間週考點:150ms剛好插在中間</p><blockquote>主執行緒:每印出一字補睡100ms</blockquote><blockquote>0ms:印出1(開始睡到100ms)</blockquote><blockquote>100ms:印出2(開始睡到200ms)</blockquote><blockquote>子執行緒:睡150ms後動作</blockquote><blockquote>150ms:改值並印出集合<a href=\"%E5%9B%A0%E7%82%BA150ms%E4%BB%8B%E6%96%BC100-200ms%E4%B9%8B%E9%96%93%EF%BC%8C%E5%89%9B%E5%A5%BD%E5%8D%B0%E5%9C%A82%E7%9A%84%E5%BE%8C%E9%9D%A2\" rel=\"noopener noreferrer\" target=\"_blank\">1,2,3,four</a></blockquote><blockquote>主執行緒:</blockquote><blockquote>200ms: 印出3</blockquote><blockquote>300ms: 印出4</blockquote><p><br></p><p>速記口訣:</p><p>【迴圈走舊版、印出看時間】</p><p>主執行緒走舊快照(必印1234)，子執行緒在150ms中間插隊([1,2,3,four])</p>",
			30,
			new String[]{"<p>A) 1 2 [1,2,3,four]3 4</p>", "<p>B) 1 2 [1,2,3,4] 3 four</p>", "<p>C) 1 2 [1,2,3,4] 3 4</p>", "<p>D) 1 2 [1,2,3,four] 3 four</p>"},
			0
		);

		// Q59
		createQuestion(
			"<p>Given</p><p><br></p><p>public interface Worker{</p><p>\tpublic void doProcess();</p><p>}</p><p><br></p><p>and</p><p><br></p><p>public class Hard Worker implements Worker{</p><p>\tpublic void doProcess(){</p><p>\t\tSystem.out.println(\"doing things\");</p><p>\t}</p><p>}</p><p><br></p><p>and</p><p><br></p><p>public class Cheater implements Worker{</p><p>\tpublic void doProcess(){}</p><p>}</p><p><br></p><p>and</p><p><br></p><p>public class Main&lt;T extends Worker&gt;extends Thread {  //Line 1</p><p>\tprivate List&lt;T&gt; processes = new ArrayList&lt;&gt;();   //Line 2</p><p>\tpublic voild addProcess(HardWorker w){\t\t\t //Line 3    </p><p>\t\tprocesses.add(w);\t</p><p>\t}</p><p><br></p><p>\tpublic void run(){</p><p>\t\tprocesses.forEach((p)-&gt;p.doProcess());</p><p>\t}</p><p>}</p><p><br></p><p>Whate needs to change to make these classes compile and still handle all types of interface Worker?</p>",
			"<p>卷ch8-ex4 說明:</p><p>需要做哪些修改才能使這些類別能夠編譯通過，並且仍然能夠處理所有類型的 Worker 介面？</p><p>答案: B)Replace Line 3 with public void addProcess(T w){</p><p>//把第3行改為public void addProcess(T w)</p><p><br></p><p>分析:</p><p>public class Main&lt;T extends Worker&gt;extends Thread {  //Line 1</p><p>\t//這裡宣告泛型T，代表Main 可以處理任何實作了Work類別(如HardWork與Cheater)</p><p><br></p><p><br></p><p>\tprivate List&lt;T&gt; processes = new ArrayList&lt;&gt;();   //Line 2</p><p>\tpublic voild addProcess(HardWorker w){\t\t\t //Line 3</p><p>    //public void addProcess(HardWorker w)這裡死板地把參數硬寫成 HardWorker。</p><p>    //Line 3 改成 public void addProcess(T w)，參數型態跟著泛型 T 走，就能靈活處理所有 Worker 類別了。</p><p>\t\t</p><p>\t\tprocesses.add(w);\t</p><p>\t}</p><p><br></p><p>\tpublic void run(){</p><p>\t\tprocesses.forEach((p)-&gt;p.doProcess());</p><p>\t}</p><p>}</p><p>Main&lt;T extends Worker&gt;的列表是元素型別T，addProcess若固定指定接收HardWorke，就不能接收Cheater或其他Worker子類別，改為T後，傳入值與processes的元素型別一致</p><p><br></p><p>記憶:</p><p>畫面箱子標示: T今天可能代表HardWorker，明天也可能代表Cheater。入口不能永久只讓HardWorker近來，否則箱子改成Cheater時就會出錯。</p><p><br></p><p>考試口訣與觀念:</p><p>【類別用T定義，方法就要用T來收】</p><p><br></p><p>邏輯:</p><p>1 看Line 1: 既然類別已經定義了通用代號T(&lt;T extends Work&gt;)</p><p>2.看Line 3: 方法參數就不能寫死特定類別(HardWorker)，否則泛型就白設了</p><p>3.選答案: 將參數改為 T w，讓它跟著T的型態動態變更</p><p><br></p>",
			30,
			new String[]{
				"<p>A) Replace Line 1 with public class Main&lt;T extends HardWorker&gt; extends Thread{</p>",
				"<p>B) Replace Line 3 with public void addProcess(T w){</p>",
				"<p>C) Replace Line 3 with public void addProcess(Worker w){</p>",
				"<p>D) Replace Line 2 with private List&lt;HardWorker&gt; processes = new ArrayList&lt;&gt;();</p>"
			},
			1
		);

		// Q60
		createQuestion(
			"<p>Why does Console readPassword() return a char array rather than a String?</p>",
			"<p>卷ch9-ex1 說明:</p><p>為什麼Console的readPassword()函數回傳的是字元陣列而不是字串?</p><p>B)It improves security </p><p>//它提高了安全性</p><p>String不可變，密碼內容可能在字串長量池或堆中保留較長的時間，char[]可以使用後主動覆蓋，例如Arrays，fill(password，’\\o’)，減少敏感訊息</p><p><br></p><p>聯想:</p><p>String像用油性筆寫密碼；寫下後不好擦除char[]像白板；使用完可以立即擦乾淨。</p><p><br></p><p>口訣:</p><p>【字串改掉(不可變)、陣列能擦掉(安全性高)】</p><p>【字串抹不掉、陣列隨時擦；記憶體不留痕，安全性才高】</p><p><br></p><p>速記表:</p><blockquote>關鍵字:Console.readPassword() -&gt; char[]</blockquote><blockquote>背後原因:可手動覆寫/清空記憶體(Overwriten in memory)</blockquote><blockquote>最終目的:提高安全性(Security)</blockquote><p><br></p><p>題庫延伸補充:(考試常見誘騙答選題)</p><blockquote>選項A(Performance): 錯，用char[]或String在這裡對效能幾乎沒有差異</blockquote><blockquote>選項C(Must be stored as...): 錯，語法上並沒有強制規定密碼【必須】如何儲存，而是為了安全性的【最佳實踐(Best Practice】</blockquote><blockquote>選項E(Encyption):錯，readPassword()只是讀取字元陣列，並沒有進行任何加密演算法(Encryption)，加密需要由開發者後續自行實作</blockquote><p><br></p>",
			30,
			new String[]{"<p>A. It improves performance</p>", "<p>B. It improves security</p>", "<p>C. Passwords must be stored as a char array</p>", "<p>D. String cannot hold the individual password characters</p>", "<p>E. It adds encryption</p>", "<p>F. None of the above</p>"},
			1
		);

		// Q61
		createQuestion(
			"<p>Fill in the blanks: Writer is a(n) ____________ that related stream classes _________</p>",
			"<p>卷ch9-ex2 說明:</p><p>填空：Writer 是個________，它與________有關。</p><p>B. abstract class,extend</p><p><br></p><p>Write is a(n) abstract class that related stream classes extend.</p><p>(Writer 是一個抽象類別，相關的串流類別會繼承它。)</p><p><br></p><p>java.io.Writer 是字元輸出流的抽象基本類別，FileWriter，BufferedWriter，StringWriter等具體類別繼承它</p><p><br></p><p>聯想:</p><p>Writer 是一位只制定【怎麼寫字】規則的抽象主管，真正負責不同寫入方式的是: FileWriter : 寫檔案；BufferedWriter : 加緩衝；String Writer :寫進字串緩衝區</p><p><br></p><p>考試口訣:</p><p>【四大家族皆抽象，class繼承用extend!】</p><p><br></p><p>解析:</p><p>1.四大家族皆抽象:Java 只要看到InputStream(位元輸入)/OutputStream(位元輸出)/</p><p>\tReader(字元輸入)/Writer(字元輸出) 這四個名字(I/O四大頂層基類別)</p><p>\t它們全部都是abstract class(抽象類別)，絕對不是interface</p><p><br></p><p>2.class 繼承用extend，既然是class，子類別跟其關係一定是extends!</p><p>{/ Reason: Offers logical next steps to deepen understanding of Java I/O concepts and auestion patterns. /} </p><p><br></p>",
			30,
			new String[]{"<p>A. concrete class,extend</p>", "<p>B. abstract class,extend</p>", "<p>C. abstract class,implement</p>", "<p>D. interface,extend</p>", "<p>E. interface,implement</p>", "<p>F. None of the above</p>"},
			1
		);

		// Q62
		createQuestion(
			"<p>Given:</p><p><br></p><p>class MyPersistenceData{</p><p>\t\t\tString str;</p><p>\t\t\tprivate void methodA(){</p><p>\t\t\t\t\tSystem.out.println(\"methodA\");</p><p>\t\t\t}</p><p>}</p><p><br></p><p>You want to implement the java.io.Serializable interface to the MyPersistenceData class.</p><p><br></p><p>Which method should be overridden?</p>",
			"<p>卷ch9-ex3 說明:</p><p>你想在 MyPersistenceData 類別中實作 java.io.Serializable 介面。</p><p>應該重寫哪個方法？</p><p>D)nothing</p><p>Serializable是標記介面，沒有抽象方法，類別只需要宣告中implements Serualizable，但不可序列化欄位，可能需要transient或自定義序列化邏輯</p><p><br></p><p>聯想:</p><p>Serializable 像在行李上貼一張；可運送的標籤。貼上後就表示物件可以被序列化，不需要額外填寫任何方法表單。</p><p><br></p><p>考題解析</p><p>口訣:</p><p>【S標記，空空如也(選Notthing); E外部，讀寫一對(選Both)】</p><blockquote>S(Serializable):是標記(Marker)，裡面空空如也，都不用寫-&gt;選Nothing.</blockquote><blockquote>E(Externalizable):是外部(External)控制，一定要讀寫一對(readExternal + writeExternal)→選Both/Read&amp;Write。</blockquote><p><br></p>",
			30,
			new String[]{"<p>A) the readExternal method</p>", "<p>B) the readExternal and writeExtenernal methd</p>", "<p>C) the writeExternal method</p>", "<p>D) nothing</p>"},
			3
		);

		// Q63
		createQuestion(
			"<p>Fill in the blanks: ________________means the state of an object cannot be changed, while __________________means that it can</p>",
			"<p>卷ch10-ex1 說明:</p><p>填空：_______________表示物件的狀態不能改變</p><p>而________表示物件的狀態可以改變。</p><p>A.封裝，工廠方法</p><p>B.不可變性，可變性</p><p>C.剛性，柔性</p><p>D.靜態，實例</p><p>E.緊耦合，鬆散耦合</p><p>F.以上都不是</p><p>答案: B)Immutability, mutability </p><p>//不可變性，可變性</p><p><br></p><p>Fill in the blanks: Immutability means the state of an object </p><p>cannot be changed,while mutability means that it can</p><p>填空: 不可變性 表示物件的狀態不能改變，而 可變性 表示物件的狀態可以改變</p><p><br></p><p>對應:</p><blockquote>Immutability(不可變性)指一物件被建立後，其內部狀態(欄位/屬性)完全無法被修改，如 String s =\"Hello\";</blockquote><blockquote>Mutability(可變性):指的是物件建立後，其狀態可以隨時被修改</blockquote><blockquote> 如:Java中的ArrayList，可隨時調用.add()改變物件的內容</blockquote><p><br></p><p>聯想:</p><p>immutable像已經封膜的商品；封膜會不能更改內容，mutable 像白板，內容可以一直擦寫。</p><p><br></p><p>口訣:</p><p>【M是 Modify(可修改)，Im就是In(不能改)；String不變是Immutable，Builder能變】</p><p><br></p><p>拆解記憶技巧（字首字根法）</p><blockquote>Mutate/Mutable --&gt;聯想Modify(修改/突變)--&gt;可以改變</blockquote><blockquote>Immutable --&gt;字首Im-代表否定(Not) --&gt;不能改變</blockquote><p><br></p><p><br></p>",
			30,
			new String[]{"<p>A. Encapsulation,factory method</p>", "<p>B. Immutability, mutability</p>", "<p>C. Regidity, flexibility</p>", "<p>D. Static, instance</p>", "<p>E. Tightly coupled, loosely coupled</p>", "<p>F. None of the above</p>"},
			1
		);

		// Q64
		createQuestion(
			"<p>How do you change the value of an instance variable in an immutable class?</p>",
			"<p>卷ch10-ex2 說明:</p><p>如何改變不可變類別中實例變數的值?</p><p>E.You can\'t</p><p><br></p><p>選項分析:</p><p>A.呼叫setter 方法 </p><p>//不可變類別嚴禁提供Setter方法，否則就破壞了不可變性</p><p><br></p><p>B.移除final 修飾符並直接設定實例變數 </p><p>//如果你改變別程式碼(移除final並修改變數)，</p><p>//這代表你將改寫成了【可變類別】，而不是【在不可變數】</p><p><br></p><p>C使用內部類別建立一個新實例</p><p>//建立一個新實例會產生一個全新的物件，</p><p>//原本那個物件內部的實例變數值依然不變</p><p><br></p><p>D使用A、B或C以外的其他方法</p><p>//既然定義上竟【無法改變】，</p><p>//就不存在其他合法修改該物件狀態的方法</p><p><br></p><p>E無法操作</p><p>//不可變類是指物件一旦被建立(Instantiated)之後，</p><p>//其內部狀態(實例變數/Instance Variables) </p><p>//就永遠無法被修改。 </p><p>//對同一個已經建立好的不可變物件來說，</p><p>//要改變其內部的實例數值是完全做不到的，</p><p><br></p><p>定義immutable class 的方法如下</p><p><br></p><p>1.不提供修改屬性內容的setter方法</p><p>2.所有的屬性宣告為private final 使其無法被修改</p><p>3.類別宣告為final class 使無法被繼承，方法無法被覆寫。或是把建構設為private，並改以工廠方法提供物件的實例。</p><p>4.若屬性為mutable object 應避免提供修改方法。讀取時提供複製物件而非原物件參照</p><p><br></p><p>聯想:</p><p>不可變物件像已經燒製完成的陶器，不能把同一個陶器重新捏成別的形狀</p><p><br></p><p>口訣:【類別封，欄位隱，終結設、無修改、深複製】</p><p><br></p><p>類別封 final class 類別需告為final，防止被繼承並覆寫方法 </p><p>欄位隱 private fields 實例變數設為private，封裝內部細節 </p><p>終結設 final fields 實例變數設為final，確保只能在建構子初始化一次 </p><p>無修改 No Setter 不提供任何setter方法 </p><p>深複製 Depp Copy/Defensive Copy\t若含有可變物件(如Date、List)回傳或傳入時必 須做防禦性複製(Defensive Copy)</p><p><br></p><p>速記心法:</p><p>【看到Immutable ，聯想String;看到修改值，直接選不可(You Cant\'t)。】</p><p>在Java考試中，只要看到Immutable，就想想String 的特性:</p><p>String s= \"Hello\"; s.concat(\"Word\"); 原本的s 依然是\"Hello\",永遠不會變!</p>",
			30,
			new String[]{"<p>A. Call the setter method</p>", "<p>B. Remove the final modifier and set the instance variable directly</p>", "<p>C. Create a new instance with an inner class</p>", "<p>D. Use a method other than Option A,B, or C.</p>", "<p>E. You can’t</p>"},
			4
		);

		// Q65
		createQuestion(
			"<p>Given</p><p><br></p><p>public class Foo{</p><p>\tpublic static String ALPHA=\"alpha\";</p><p>\tprotected String beta=\"beta\";</p><p>\tprivate final String delta;</p><p>\tpublic Foo(String d){</p><p>\t\tdelta=ALPHA+d;</p><p>\t}</p><p>\tpublic String foo(){</p><p>\t\treturn beta+=delta;</p><p>\t}</p><p>}</p><p><br></p><p>Which chage would make Foo more secure?</p>",
			"<p>卷ch10-ex3 說明:</p><p>哪項改變能使 Foo 更加安全?</p><p>D)public static final String APPHA=\"alpha\";</p><p>為何答案是D?</p><p>public static 變數如果沒加上 final 會造成極大的安全性漏洞</p><p>1.現狀漏洞問題: public static String ALPHA = \"alpha\";</p><blockquote>public :代表任何其他類別都可存取</blockquote><blockquote> static:代表其是類別共享變數(全域狀態)</blockquote><blockquote> 缺少 final 代表任何程式碼都可以隨意修改它的值</blockquote><p>2.加上final 提升安全性</p><blockquote>將變數改為public static final String ALPHA = \"alpha\"後，</blockquote><blockquote> ALPHA就會變成一個真正的常量(Constant)外部只能【讀取】，永遠無法修改</blockquote><p>3.其他選項可能也有封裝效果，但public static 缺乏final 是最嚴重的漏洞</p><p><br></p><p>聯想:</p><p>public static 像放在公共大廳的告示牌，沒有final時，任何人都能拿筆修改，加上final後，就像套上一層保護罩，能容就不能被重新指定。</p><p><br></p><p>口訣:</p><p>【靜態公開必加終，欄位私有加方法】</p><p><br></p><p>靜態公開必加終 (public static final): public static 變數一定要加final，使其 成為常量放止被全域竄改。</p><p><br></p><p>欄位私有加方法 (private fields + Getters): 實例變數盡量設為private，透過Geeter/Setter控制存取權限</p><p><br></p><p>3秒解題心法:</p><p>【看到 public static 沒加上<span style=\"color: rgb(207, 81, 72);\"> final</span> 就是嚴重的安全破口!答案找 public static<span style=\"color: rgb(207, 81, 72);\"> final</span> 準沒錯】 </p>",
			30,
			new String[]{"<p>A) private String delta;</p>", "<p>B) public String beta= “beta”;</p>", "<p>C) protected final String beta = “beta”;</p>", "<p>D) public static final String ALPHA= “alpha”</p>"},
			3
		);

		// Q66
		createQuestion(
			"<p>What is the most likely outcome of the this code if the bunnies table is empty?</p><p><br></p><p>var url=\"jdbc:derby:bunnies\";</p><p>var sql=\"insert into bunny(name,color)values(?,?)\";</p><p>try(var conn=DriverManager.getConnection(url);</p><p>var stmt=conn.createStatement()){</p><p>\tstmt.setString(1,\"Hoppy\");</p><p>\tstmt.setString(2,\"Brown\");</p><p>\tstmt.executeUpdate(sql);</p><p><br></p><p>}</p>",
			"<p>卷ch11-ex1 說明:</p><p>如果 bunnies 表為空，這段程式碼最可能的結果是什麼？</p><p>C)The code does not compile. </p><p>//無法編譯</p><p><br></p><p>1.try-with-resonurces 語法完全錯亂 </p><p>try(var conn==DriverManager.getConnection(url)); //這裡該用分號</p><p>var stmt= conn.createStatement(){ //這行完全不在 try 的括號裡，還多了一個 {</p><p><br></p><p>2.呼叫方法時，參數中間寫成【句點.】而不是【逗號,】 stmt.setString(1.\"Hoppy\"); ////參數間用逗號隔開 stmt.setString(2.\"Brown\");</p><p><br></p><p>3.對 Statement做了 PreparedStatement才會有的操作 var stmt = conn.createStatement(); stmt.setString(...); </p><p>//conn.createStatement() 建立的是普通的 Statement 物件, </p><p>//沒有 setString 方法！</p><p><br></p><p>聯想:</p><p>把JDBC想成去圖書館借書；Connection = 先進圖書館PreparedStatement=填寫書單executeQuery()=將給櫃檯查書，ResultSet = 拿有查詢結果。</p><p><br></p><p>口訣: 【號、點、Pre】</p><p>【號無分、點換逗、問號必Per】</p><blockquote>號無分 (try括號後面絕對不能有分號，會直接判定無法編譯</blockquote><blockquote>點換逗 (傳參數要用逗號，不能用句號)</blockquote><blockquote>問號必Pre (SQL有?號，必須用PreparedStatement)</blockquote><p><br></p><p><br></p>",
			30,
			new String[]{"<p>A. One row is in the table.</p>", "<p>B. Two rows are in the table.</p>", "<p>C. The code does not compile.</p>", "<p>D. The code throws a SQLException</p>"},
			2
		);

		// Q67
		createQuestion(
			"<p>What must be the first characters of a database URL?</p>",
			"<p>卷ch11-ex2 說明:</p><p>資料庫URL的前幾個字源不須是甚麼?</p><p>D)jdbc:</p><p>java 中連線資料庫，表準的JDBC URL前綴永遠必須是 jdbc:</p><p><br></p><p>以Mysql 為例: jdbc:mysql://localhost:3306/mydb</p><p>jdbc:(協定Protocol)</p><blockquote>固定不變。告訴Java API這是要給JDBC框架處理的資料庫連接字串</blockquote><blockquote> 注意結尾必須紹冒號: ，而非逗號，。</blockquote><p><br></p><p>口訣:</p><p>【java找庫，JDBC開頭帶冒號】</p><p>Java Database Connectivity 開頭必帶冒號 --&gt; jdbc:</p>",
			30,
			new String[]{"<p>A.db,</p>", "<p>B.db:</p>", "<p>C.jdbc,</p>", "<p>D.jdbc:</p>", "<p>E.None of the above</p>"},
			3
		);

		// Q68
		createQuestion(
			"<p>Assuming the user creduntials are correct, Which expression will create a Connection?</p>",
			"<p>卷ch11-ex3 說明:</p><p>假設使用者憑證正確,哪個表達式可以建立連線?</p><p>答案:</p><p>A)DriverManager.getConnection(\"jdbc:derby:com\")</p><p>JDBC連接資料庫時，核心原則:第一個參數必須是符合JDBC URL 格式</p><p><br></p><p>DriverManager.getConnection接收合法JDBC URL 並回傳Connection，實際Derby URL是否可以連接還取決於資料庫名，驅動與創建參數，但語法型式正確。</p><p><br></p><p>錯誤對比:</p><p>B)DriverManager.getConnection(\"jdbc.derby.com\") </p><p>//JDBC規定用冒號(:)分格，不能全用句號</p><p><br></p><p>C)DriverManager.getConnection(\"http://database.jdbc.com\",\"J_SMITH\",\"dt12%2f3\")</p><p>//JDBC連接不是HTTP網頁傳輸，不能用http:// 必須用jdbc:</p><p><br></p><p>D)DriverManager.getConnection(); </p><p>//缺少參數，不能留空</p><p><br></p><p>E)DriverManager.getConnection(\"J_SMITH\",\"dt12%2f3\") </p><p>//第一個參數用遠必須式URL</p><p><br></p><p>聯想:</p><p>把DniverManager想成車站站長，你拿著資料庫地址:jdbcderby.com取找站長:請給我一條Connection，站長呼叫:getConnection()建立通往資料庫的連線。</p><p><br></p><p>口訣:</p><p>【JDBC 開頭帶冒號，無URL跑不掉】</p><blockquote>開頭一律認準 jdbc:</blockquote><blockquote> 看第一個位置永遠是URL，不能只給帳號跟密碼</blockquote><p>C)The code does not compile. //無法編譯</p><p>createStatement()回傳Ststement，而參數佔位符和setString屬於PreparedStatement，應使用conn.prepareStatment(sql)，在設置參數並呼叫executeUpdate();</p><p><br></p><p>聯想:</p><p>把JDBC 想成去圖書館借書；Connection=先進圖書館，PreparedStatement =填寫借書單，executeQuery() = 交給櫃台查書，ResultSet = 拿到查詢結果。</p><p><br></p><p>口訣:</p><p>【先連線，再準備，再查詢，最後取資料】</p><p>完整流程: Connection → PreparedStatement→ executeQuery() → ResultSet</p><p><br></p><p>本題速記</p><p>問號必Pre : (SQL有?號，必須用PreparedStatement)</p><p><br></p><p><br></p>",
			30,
			new String[]{
				"<p>A)DriverManager.getConnection(\"jdbc:derby:com\")</p>",
				"<p>B)DriverManager.getConnection(\"jdbc.derby.com\")</p>",
				"<p>C)DriverManager.getConnection(\"http://database.jdbc.com\",\"J_SMITH\",\"dt12%2f3\")</p>",
				"<p>D)DriverManager.getConnection();</p>",
				"<p>E)DriverManager.getConnection(\"J_SMITH\",\"dt12%2f3\")</p>"
			},
			0
		);

		// Q69
		createQuestion(
			"<p>What must be the first characters of database URL?</p>",
			"<p>卷ch11-ex2</p><p>資料庫URL的前幾個字元必須是什麼?</p><p>答案: D. jdbc:</p><p><br></p><p>java 中連線資料庫，表準的JDBC URL前綴永遠必須是 jdbc:</p><p>以Mysql 為例: jdbc:mysql://localhost:3306/mydb</p><p><br></p><p>jdbc:(協定Protocol)</p><blockquote>固定不變。告訴Java API這是要給JDBC框架處理的資料庫連接字串</blockquote><blockquote>注意結尾必須紹冒號: ，而非逗號，。</blockquote><p><br></p><p>口訣:</p><p>【java找庫，JDBC開頭帶冒號】</p><p>Java Database Connectivity 開頭必帶冒號 --&gt; jdbc:</p>",
			30,
			new String[]{"<p>A. db,</p>", "<p>B. db:</p>", "<p>C. jdbc,</p>", "<p>D. jdbc:</p>", "<p>E. None of the above</p>"},
			3
		);

		// Q70
		createQuestion(
			"<p>Assuming the user credentials are correct, which expression will create a Connection?</p>",
			"<p>卷ch11-ex3 說明:</p><p>假設用戶與密碼正確，哪個表達式可以創建Connection?</p><p>A) DriverManager.getConnection(”jdbc:derby:com”);</p><p><br></p><p>DriverManager.getConnection接收合法JDBC URL並回傳Connection，實際Derby URL是否可以連線還取決於資料庫名稱，驅動與創建參數，但語法形式正確。</p><p><br></p><p>對比:</p><p>A)DriverManager.getConnection(\"jdbc:derby:com\") 正確</p><p>\tJDBC連接資料庫時，核心原則:第一個參數必須是符合JDBC URL 格式</p><p>B)DriverManager.getConnection(\"jdbc.derby.com\") 錯誤!</p><p>\tJDBC規定用冒號(:)分格，不能全用句號</p><p>C)DriverManager.getConnection</p><p>\t(\"http://database.jdbc.com\",\"J_SMITH\",\"dt12%2f3\") 錯誤!</p><p>\tJDBC連接不是HTTP網頁傳輸，不能用http:// 必須用jdbc:</p><p>D)DriverManager.getConnection();</p><p>\t缺少參數，不能留空</p><p><br></p><p>聯想:</p><p>DriverManager想成車站站長，你拿著資料庫地址: jdbc:derby:com去找站長說:請給我一條 Connection，站長呼叫 getConnection()建立通往資料庫的連線。</p><p><br></p><p>口訣:</p><p>【JDBC 開頭帶冒號，無URL跑不掉】</p><blockquote>開頭一律認準 jdbc:</blockquote><blockquote>看第一個位置永遠是URL，不能只給帳號跟密碼</blockquote><p><br></p>",
			30,
			new String[]{
				"<p>A) DriverManager.getConnection(”jdbc:derby:com”);</p>",
				"<p>B) DriverManager.getConnection(”jdbc.derby.com”);</p>",
				"<p>C)</p><p>DriverManager.getConnection(”http://database.jdbc.com”,”J_SMITH”,”dt12%2f3”);</p>",
				"<p>D) DriverManager.getConnection();</p>",
				"<p>E) DriverManager.getConnection(”J_SMITH”,”dt12%2f3”);</p>"
			},
			0
		);

		// Q71 (複選)
		createQuestionWithFlags(
			"<p>Which of the following are considered locales?(Choose three)</p>",
			"<p>卷ch12-ex1 說明:</p><p>下列哪些屬於在地化?(選三項)</p><p><br></p><p>Java API文件(java.util.Locale) 中對於Locale的官方和新定義:</p><p>\"A Locale object represents a specific geographical,political,or cultural region.”</p><p>(一個Locale物件代表一個特定的地理、政治或文化區域)</p><p><br></p><p>故答案是；</p><p>A)Culture region(文化區域):代表語言習慣、日期時間格式、數字或貨幣表示法(如zh 中國 或en 英文)</p><p>E)Political region(政治區域):代表行政或政治實體管轄，決定該區採用的標準(如TW 台灣 US 美國)</p><p>F)Geographical region(地理區域):代表地理上的國家或區域分布</p><p><br></p><p>錯誤因:</p><p>City(城市)範圍太小，Locale是大範圍區域設定</p><p>Time zone region(時區區域)，由java.util.TimeZone或java.time.ZoneId獨立處理</p><p><br></p><p>聯想:</p><p>你搬到一個新國家，會受到三種影響: 地理位置(Geographical region)、政治制度(Political region)、文化習俗(Culture region)</p><p><br></p><p>背誦口訣:</p><p>【G-P-C】</p><blockquote>Geographical:地理區域</blockquote><blockquote>Political region:政治區域</blockquote><blockquote>Culture region:文化區域</blockquote><p><br></p>",
			30,
			new String[]{"<p>A. Culture region</p>", "<p>B. Local address</p>", "<p>C. City</p>", "<p>D. Time zone region</p>", "<p>E. Political region</p>", "<p>F. Geographical region</p>"},
			new boolean[]{true, false, false, false, true, true}
		);

		// Q72
		createQuestion(
			"<p>When localizing an application, which type of data varies in presentation depending on locale?</p>",
			"<p>卷ch12-ex2 說明:</p><p>在地化應用程式時，哪些類型的資料會根據語言環境的不同而改變呈現方式?</p><p>答案: C.Both 兩者皆是</p><p><br></p><p>Java國際化與在地化(Localization,i18n/l10n)開發規則，Locale(地區設定)用來劃分文化與地理區域物件。 </p><p>不同Locale會直接影響資料的前端呈現給使用者時的格式與樣式</p><p>A.Currencies(貨幣):</p><blockquote>金額格式化</blockquote><blockquote> 符號位置與格式</blockquote><blockquote> Java 主要透過 java.text.NumberFormat.getCurrencyInstance(locale) 或 java.util.Currency 來處理。</blockquote><p>B.Dates(日期與時間)</p><blockquote>年月日順序:</blockquote><blockquote> 美國(en_US)</blockquote><blockquote> 台灣/中國(zh_TW)</blockquote><blockquote> 歐洲多數國家(en_GB)</blockquote><blockquote> 月分與星期文字:January(英文)va 一月(中文)。</blockquote><blockquote> Java 主要透過 java.time.format.DateTimeFormatter 或舊版的 java.text.SimpleDateFormat 搭配 Locale 來控制呈現方式。</blockquote><p>因此，貨幣(Currencies)與 日期(Dates) 的呈現方式都會隨著Locale而改變</p><p><br></p><p>聯想:</p><p>同一個生日，美國朋友看到是07/28/2026；台灣朋友看到的是2026/07/28；日本朋友看到是2026年7月28日都是同一天</p><p><br></p><p>口訣:</p><p>【地(Locale)動貨(貨幣)日(日期)驚，雙雙變格式!】</p><p>地動:提示Locale(地區設定)的變動 貨日經:貨幣(Currency)與 日期(Date)。 雙雙變格式: 兩者(Both)都需邀根據地區調適不同的顯示格式。</p><p><br></p>",
			30,
			new String[]{"<p>A. Currencies</p>", "<p>B. Dates</p>", "<p>C. Both</p>", "<p>D. Neither</p>"},
			2
		);

		// Q73
		createQuestion(
			"<p>Given the code fragment:</p><p><br></p><p>Locale locale = Locale.US;</p><p>//Line 1</p><p>double currency = 1_00.00;</p><p>System.out.println(formatter.format(currency));</p><p><br></p><p>You want to display value of currency as $100.00.</p><p>Which code inserted on line 1 will accomplish this?</p>",
			"<p>卷ch12-ex3 說明:</p><p>你將貨幣值顯示為$100.00。在第一行插入哪段程式碼可以實現這個目標?</p><p><br></p><p>答案:D)NumberFOrmate formatter = NumberFormat.getCurrencyInstance(locale);</p><p><br></p><p>分析:</p><p>1.目標是格式化為【貨幣(Currency)】 題目希望輸出帶有金錢富號的$100.00，因此必須調用專門處理貨幣格式的工廠方法 --getCurrencyInstance(...)</p><p><br></p><p>2.帶入地區設定(Locale.US) &gt;當傳入Locale.US時，Java會載入美國地區的貨幣規則:前墜加上$、保留兩位小數並使用，作為千分為分隔號 &gt;因此formatter.format(100.0)輸出結果就會是$100.00。</p><p><br></p><p>3.語法細節 &gt;NumberFormat 採用抽象工廠模式，不能用new NumberFormat()，必須經由靜態方法NumberFormat.get...Instance()取得實例。</p><p><br></p><p>聯想:</p><p>NumberFormat 有三兄弟:NumberPercentCurrency這題是第三個:Currency</p><p><br></p><p>口訣:</p><p>看到NumberFormat，關鍵看【題目要輸出什麼格式】，直接鎖定對應的getXXXXXInstance:</p><p><br></p><blockquote>貨幣有符號($100.0)-&gt;找 getCurrencyInstance</blockquote><blockquote> 百分比加%(50%)-&gt;找 getPercentInstance</blockquote><blockquote> 整數無小數(100)-&gt;找getIntegerInstance</blockquote><blockquote> 一般數字有千分為(100.00)-&gt;找 getInstance或 getNumberInstance</blockquote><p><br></p><p>速記: </p><p>題目出現$	或€ -&gt;關鍵字立即鎖定 Currency !</p><p><br></p><p><br></p>",
			30,
			new String[]{
				"<p>A) NumberFormat </p><p>formatter=NumberFormat.getInstance(locale).</p><p>getCurrency();</p>",
				"<p>B) NumberFormat formatter=</p><p>NumberFormat.getInstance(locale);</p>",
				"<p>C) NumberFormat formatter=</p><p>NumberFormat.getCurrency(locale);</p>",
				"<p>D) NumberFormat formatter=</p><p>NumberFormat.getCurrencyInstance(locale);</p>"
			},
			3
		);

		// Q74
		createQuestion(
			"<p>Which code fragment does a service use to load the service provider with a Print interface?</p>",
			"<p>卷ch12-ex4 說明:</p><p>服務使用哪段程式碼來載入有列印介面的服務提供者?</p><p>答案:A) private java.util.ServiceLoader&lt;Print&gt; loader = </p><p>ServiceLoader.load(Print.class);</p><p><br></p><p>1.什麼是ServiceLoader?</p><blockquote>java 提供了一個稱為SPI(Service Provider Interface)的機制，允許應用程式【動態尋找並載入】，實現了特定介面(如print)的服務提供者(Provider)，而不需要在程式碼中將實現類別編碼(Hard-code)</blockquote><blockquote>java.util.ServiceLoader 就是負責尋找與載入這些實作的核心類別</blockquote><p><br></p><p>2.為什麼其他選項是錯的?</p><blockquote>C錯誤:ServiceLoader的建構子是私有的(private)的，不能直接使用new ServiceLoader()。</blockquote><blockquote>B與D錯誤:直接使用getIntstance() 或 new PrintImpl()是傳統的硬編碼寫法，無法使用java的服務載入機制(Service Load)，破獲了解偶的原則。</blockquote><p>3.正解A的關鍵語法；</p><blockquote>ServiceLoader 採用靜態工廠方法，載入介面的標準寫法為；ServiceLoader.load(介面名稱.class)</blockquote><p><br></p><p>聯想:</p><p>把Java想成一間大型公司，有很多不同品牌的印表機:HPCanonEpsonBrother公司不知道今天要用哪一家。</p><p><br></p><p>口訣:</p><p>【服務載入別用 new , ServiceLoader點load 帶 class!】</p><p><br></p><p>速記:</p><p>1.看到關鍵字</p><p> Service Provider/Service Load -&gt; 鎖定 ServiceLoader。</p><p>2.記得不能直接 new (排除C)</p><p>3.載入服務要用靜態方法 ServiceLoader.Load(Xxx.class)(鎖定A)。</p><p><br></p><p><br></p>",
			30,
			new String[]{
				"<p>A) private java.util.ServiceLoader&lt;Print&gt;loader</p><p>=ServiceLoader.load(Print.class);</p>",
				"<p>B) private Print print</p><p>= com.service.Provider.getInstance();</p>",
				"<p>C) private java.util.ServiceLoader&lt;Print&gt; loader </p><p>= new java.ServiceLoader&lt;&gt;();</p>",
				"<p>D) private Print print</p><p>= new com.service.PrintImp();</p>"
			},
			0
		);

		// Q75
		createQuestion(
			"<p>What modify is used to mark an annotation element as optional?</p>",
			"<p>卷ch13-ex1 說明:</p><p>如何使用修改方式將註解標記為可選?</p><p>答案: B) default</p><p>定義java 註解元素(Annotiation Elements)時，若每有設定預設值，則使用該註解時必須(Mandatory) </p><p>傳入參數；相反，只要使用default關鍵字指定預設值，該元素就變成可選(Optional)</p><p><br></p><p>範例:</p><p>public @interface MyAnnotion{</p><p>\t//沒有 default ，屬於【必須(Required)】元素</p><p>\tString name();</p><p><br></p><p>\t//使用default 指定預設值，屬於【可選(Optional)】元素</p><p>\tint age() default 18;</p><p>}</p><p><br></p><p>使用此註解時</p><p><br></p><p>//合法 :age 有預設值 18，所以可依省略不寫(可選)</p><p>@MyAnnotation(name=\"Alice\")</p><p>public class User{}</p><p><br></p><p>// 合法:也可以自行覆蓋預設值</p><p>@MyAnnotation(name=\"Bob\",age=25)</p><p>public class Admin{}</p><p><br></p><p>//錯誤 : name 每有預設值，若省略會編譯失敗</p><p>@MyAnnotation</p><p>public class Guess{}</p><p>因此，default 關鍵字是用來賦予主解成員預設值，進而達到【標記可選(Optional)】的目的</p><p><br></p><p>選項解說:</p><p>A.optional //錯誤: java中沒有optional這個關鍵字</p><p>C.required //錯誤: java 註解沒有required 關鍵字</p><p>D.value //錯誤: value 是註解的【預設成員名稱】並非用來設定可選的修飾詞</p><p>E.case //錯誤: case 用於 switch-case 條件判斷，與註解無關</p><p><br></p><p>聯想:</p><p>老師發一張報名表: 姓名:____(必填)電話:_____(可不填，「電話」已經有預設值。所以: 可不填→default</p><p><br></p><p>口訣:</p><p>【註解要可選，戴上default免填寫；沒有default值，不傳參數就報錯】</p><p><br></p><p>記憶點: </p><p>default = 【預設】 = 【沒填寫就用預設值】 =【可選】(Optional)】</p>",
			30,
			new String[]{"<p>A. optional</p>", "<p>B. default</p>", "<p>C. required</p>", "<p>D. value</p>", "<p>E. case</p>", "<p>F. None of the above</p>"},
			1
		);

		// Q76
		createQuestion(
			"<p>Fill in the blank with the correct annotation usage that allows the code to compile without any warnings.</p><p><br></p><p>@Deprecated(since=”5.0”)</p><p>public class ProjectPlanner&lt;T&gt;{</p><p>\tProjectPlanner create(T t){return this;}</p><p>}</p><p><br></p><p>@SuppressWarnings(____________)</p><p>class System Planner{</p><p>\tProjectPlanner planner= </p><p>\tnew ProjectPlanner().create(”TPS”);</p><p>}</p>",
			"<p>卷ch13-ex2 說明:</p><p>請在空白處填寫正確的註解用法，已使程式碼能夠編譯而不產生警告?</p><p>答案: D. {”deprecation”,”unchecked”}</p><p><br></p><p>解析:</p><p>程式碼中有兩個會觸發警告的地方</p><p>1.deprecation警告</p><p>ProjcetPlanner 類別被標記了@Deprecated(since=\"5.0\"當宣告或創立其實體時，編譯器會發出使用了已過時類別警告</p><p>2.unchecked警告</p><p>new ProjectPlanner()使用原始型態(Raw Type)，未指定泛型型別&lt;T&gt;，呼叫泛型物件creat(\"TPS\")法法，編譯器會發出未檢查操作的警告</p><p>必須同時抑制這兩種警告</p><blockquote>@SuppressWarings 接收的參數是一個字串陣列 String[]。</blockquote><blockquote> 當需要傳入多個字串時，必須使用大擴號{}並用逗號隔開: {\"deprecation\",\"unchecked\"}</blockquote><p><br></p><p>選項分析:</p><p>A.value = ignoreAll //沒有ignreAll 這個警告名稱，且必須加雙引號。</p><p>B.value = \"deprecation\",\"unchekecd\" //多個參數缺少大擴號{}</p><p>C.\"unchecked\",\"deprecation\" // 沒有使用大括號{}會引發語法錯誤</p><p>E.\"deprecation\" // 只有抑制過時警告，仍會unchecked 泛型警告</p><p><br></p><p>聯想:</p><p>老師一次提醒你兩件事；第一句:這本教材已經過時! → deprecation；第二句:泛行沒有寫完整 → unchecked 所以你要一次把兩個提醒關掉:</p><p>@SuppressWarnings({ “”deprecation”,”unchecked”})</p><p><br></p><p>口訣:</p><p>【兩警告，大括號，雙引號，逗號隔開別拼錯】</p><blockquote>兩警告 :看程式碼一一用到過時類別(deprecation)+ 泛型未檢查(unchecked)，一定要填兩個值。</blockquote><blockquote> 大括號 :只要出現兩個(含)以上的警告，必須有{}(直接A、B、C、E都是錯的)</blockquote><p><br></p>",
			30,
			new String[]{"<p>A. value=ignoreAll</p>", "<p>B. value=”deprecation”,”unchecked”</p>", "<p>C. “unchecked”,”deprecation”</p>", "<p>D. {”deprecation”,”unchecked”}</p>", "<p>E. “deprecation”</p>", "<p>F. None of the above</p>"},
			3
		);

		// Q77 (複選)
		createQuestionWithFlags(
			"<p>Given the declaration</p><p><br></p><p>@interface Resource{</p><p>\tString[] value();</p><p>}</p><p>Examine the code fragment:</p><p>/** Loc1 **//* class ProcessOrders{…}</p><p><br></p><p>Which two annotation may be applied at Loca1 in the code fragment?</p>",
			"<p>卷ch13-ex3 說明</p><p>鑒於以下聲明；檢查以下程式碼片段，程式碼片段中 Loc1 可以套用哪兩個註解？</p><p>答案:</p><p>D) @Resource({”Customer1”,”Customer2”});</p><p>E) @Resource (”Customer1”)</p><p><br></p><p>解析:</p><p>@interface Resource{</p><p>String[] value(); }</p><p>代表@Resource擁有一個屬性名為value，其形態為字串陣列，</p><p> 套用這樣的註解有三個重要規則</p><p>1.value屬性的省略簡寫</p><p>\t當註解只是一個屬性，或設定屬性名稱正好是value，可省略value=不寫</p><p>\t@Resource(value=\"Customer1\")可以簡寫為</p><p>\t@Resource(\"Customer1\")</p><p>2.單一元素的陣列語法糖</p><p>\t當陣列屬性只傳入一個元素時，可以省略大括號{}。</p><p>\t答案E(@Resource(\"Customer1\"):傳入單個字串，</p><p>\t會自動視為長度為1的陣列{\"Customer1\"}，此為合法簡寫</p><p>3.多個元素陣列寫法</p><p>\t當傳入多個元素時，必須使用大括號{}括起來</p><p><br></p><p>答案D(@Resource({\"Customer1\",\"Customer2\"}):標準陣列傳值語法，正確</p><p><br></p><p>選項分析:</p><p>A) @Resource() 和 C)@Resource : Resource註解宣告String[] value()，但沒有給預設值，呼叫時須提供參數，以免報錯。</p><p>B) @Resource(value={{}}):value 是單一維度陣列String[]，{{}}是二維陣列寫法型態不符</p><p><br></p><p>口訣:</p><p>【value 可省略，單數免大括，複數加{}，無預設必填!】</p><blockquote>value可省略:寫@Resource(...)就等於 @Resource(value=...)。</blockquote><blockquote> 單數免大括:只有一個字串時，不用寫大括號{}(選E)</blockquote><blockquote> 複數加{}:兩個以上的字串，必須用{}包起來(選D)</blockquote><blockquote> 無預設必填:沒有default預設值，所以括號內不能為空(A、C錯誤)</blockquote><p><br></p><h3>考前重點速記:</h3><ol><li data-list=\"ordered\"><span class=\"ql-ui\" contenteditable=\"false\"></span>多載看編譯時參數型別 ； 覆寫看執行時實際對象。</li><li data-list=\"ordered\"><span class=\"ql-ui\" contenteditable=\"false\"></span>欄位不具有多態，欄位訪問取決於參考變數的編譯時型別。</li><li data-list=\"ordered\"><span class=\"ql-ui\" contenteditable=\"false\"></span>final欄位必須在沒調構造路徑上完成確定賦值。</li><li data-list=\"ordered\"><span class=\"ql-ui\" contenteditable=\"false\"></span>受檢例外必須捕捉或宣告； finally會在return前執行。</li><li data-list=\"ordered\"><span class=\"ql-ui\" contenteditable=\"false\"></span>集合迭代時不要直接結構性修改原集合，排除使用迭代器允許的方法或併行集合</li><li data-list=\"ordered\"><span class=\"ql-ui\" contenteditable=\"false\"></span>Stream 中間操作是惰性的， 只有中止操作才會觸發執行。</li><li data-list=\"ordered\"><span class=\"ql-ui\" contenteditable=\"false\"></span>List.of / Set.of 創建集合不可以修改</li></ol><p><br></p>",
			30,
			new String[]{"<p>A) @Resource()</p>", "<p>B) @Resouce(value={{}}}</p>", "<p>C) @Resouce</p>", "<p>D) @Resource{{”Customer1”,”Customer2”}}</p>", "<p>E) @Resource(”Customer1”)</p>"},
			new boolean[]{false, false, false, true, true}
		);
	}
	/*
	 * 封裝Question 與 Option 的雙向關聯建立與持久化
	 * 
	 * @param title            	題目敘述
	 * @param ezplanation      	解答說明 / 試題解析
	 * @param timeLimitSeconds 	作答時間限制(秒)
	 * @param optionTexts		選項文字陣列(長度為4)
	 * @param correctIndex 		正確答案的索引位置(0~3)
	 * */
	private void createQuestion(
		String title,
		String explanation,
		int timeLimitSeconds,
		String[] optionTexts,
		int correctIndex) {
		Question question = new Question();
		question.setTitle(title);
		question.setExplanation(explanation);
		question.setTimeLimitSeconds(timeLimitSeconds);
		
		//建立各個 Option並加入雙向關聯
		for(int i=0;i<optionTexts.length;i++) {
			Option option = new Option();
			option.setOptionText(optionTexts[i]);
			option.setIsCorrect(i==correctIndex);
			
			//使用Question 提供的 helper method 同步維護關聯雙方
			question.addOption(option);
		}
		
		//由於配置了 CascadeType.ALL，儲存Quesiton會自動連帶儲存內部的 Options
		questionRepository.save(question);
	}	
	/*
	 * 封裝 Question 與 Option 的雙向關聯建立與持久畫(支援單選/多選標記)
	 * */
	private void createQuestionWithFlags(
			String title,
			String explanation,
			int timeLimitSeconds,
			String[] optionTexts,
			boolean[] correctFlags) {
		Question question = new Question();
		question.setTitle(title);
		question.setExplanation(explanation);
		question.setTimeLimitSeconds(timeLimitSeconds);
		
		//建立各個Option並加入雙向關聯
		for(int i =0; i< optionTexts.length;i++) {
			Option option = new Option();
			option.setOptionText(optionTexts[i]);
			option.setIsCorrect(i< correctFlags.length && correctFlags[i]);
			
			//使用Question 提供的helper methos 同步維護關聯雙方
			question.addOption(option);
		}
		
		//透過 CascadeType.ALL 同步儲存Question與底下的Options
		questionRepository.save(question);
	}
	
}
