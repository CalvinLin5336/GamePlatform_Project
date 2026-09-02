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
        
        //2. 初始化 20題試卷題庫(含詳細解說與選項)
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
	 * 若資料庫沒有題目則建立20題內容
	 * */
	private void initQuestionsIfEmpty() {
		if(questionRepository.count()>0) {
			return;
		}
		
		// Q1
        createQuestion(
            "Spring Boot 預設內嵌的 Web 伺服器是什麼？",
            "Tomcat 是 Spring Boot (spring-boot-starter-web) 預設打包並啟動的 Servlet 容器；亦可透過抽換 Starter 換成 Jetty 或 Undertow。",
            30,
            new String[]{"Tomcat", "Jetty", "Undertow", "Netty"},
            0
        );

        // Q2
        createQuestion(
            "JPA 中用於標記資料表主鍵（Primary Key）的註解是？",
            "@Id 屬於 jakarta.persistence 套件，專門用來宣告實體的主鍵欄位；@Table 指定資料表名稱，@Column 設定欄位映射。",
            30,
            new String[]{"@Table", "@Column", "@Id", "@Entity"},
            2
        );

        // Q3
        createQuestion(
            "Java 中哪一個型態不是基本型態（Primitive Type）？",
            "Java 的 8 種基本型態為 byte, short, int, long, float, double, boolean, char；String 則是 java.lang 底下的物件參照型態。",
            30,
            new String[]{"int", "String", "boolean", "char"},
            1
        );

        // Q4
        createQuestion(
            "在關聯式資料庫中，用來唯一識別資料表中每一筆紀錄的欄位稱為？",
            "主鍵（Primary Key）具備唯一性且不能為 NULL，用於確保每筆記錄的實體完整性；外部鍵則是用於建立表與表之間的關聯。",
            30,
            new String[]{"外部鍵 (Foreign Key)", "主鍵 (Primary Key)", "候選鍵 (Candidate Key)", "複合鍵 (Composite Key)"},
            1
        );

        // Q5
        createQuestion(
            "HTTP 協定中，標準代表「請求成功」的狀態碼是？",
            "200 OK 代表客戶端請求已成功被伺服器接收、理解並處理；302 為重新導向，404 為找不到資源，500 為伺服器端錯誤。",
            30,
            new String[]{"200", "302", "404", "500"},
            0
        );

        // Q6
        createQuestion(
            "在 RESTful API 設計風格中，通常建議使用哪種 HTTP 方法來建立新資源？",
            "依照 RESTful 語意規範，POST 通常用於在伺服器上建立全新資源；GET 用於讀取，PUT 用於完整取代更新，DELETE 用於刪除。",
            30,
            new String[]{"GET", "POST", "PUT", "DELETE"},
            1
        );

        // Q7
        createQuestion(
            "TCP/IP 網路架構中，提供可靠、具流量控制與確保封包順序傳輸的協定是？",
            "TCP（傳輸控制協定）透過三次握手、確認應答與重傳機制提供可靠連線；UDP 則屬於非連線導向，速度快但不保證封包順序與到達。",
            30,
            new String[]{"IP", "UDP", "TCP", "ICMP"},
            2
        );

        // Q8
        createQuestion(
            "Git 版本控制中，將暫存區（Staging Area）的變更正式記錄到本地版本庫的指令是？",
            "git commit 會將索引/暫存區的檔案快照提交到本地儲存庫；git push 是推送到遠端，git pull 是拉取並合併，git checkout 是切換分支或復原檔案。",
            30,
            new String[]{"git pull", "git push", "git commit", "git checkout"},
            2
        );

        // Q9
        createQuestion(
            "下列哪一種資料結構遵循「後進先出」（LIFO, Last-In-First-Out）的運作原則？",
            "堆疊（Stack）的操作只發生在頂部，符合後進先出原則；佇列（Queue）則是先進先出（FIFO）。",
            30,
            new String[]{"佇列 (Queue)", "堆疊 (Stack)", "樹 (Tree)", "鏈結串列 (Linked List)"},
            1
        );

        // Q10
        createQuestion(
            "在已排序陣列中執行二元搜尋（Binary Search），最壞情況下的時間複雜度為？",
            "二元搜尋每次比對皆會排除一半的搜尋區間，因此在長度為 n 的資料集下，搜尋次數以對數方式成長，為 O(log n)。",
            30,
            new String[]{"O(1)", "O(n)", "O(log n)", "O(n²)"},
            2
        );

        // Q11
        createQuestion(
            "十進位整數 15 轉換為十六進位（Hexadecimal）時的表示方式為何？",
            "十六進位使用 0-9 以及 A-F（A=10, B=11, C=12, D=13, E=14, F=15），因此 15 在十六進位即為 F。",
            30,
            new String[]{"E", "F", "10", "A"},
            1
        );

        // Q12
        createQuestion(
            "在 Linux/Unix 作業系統環境中，用來印出目前工作目錄完整路徑的指令是？",
            "pwd 為「Print Working Directory」的縮寫，用來顯示當前所在路徑；cd 為變更目錄，ls 為列出目錄檔案，mkdir 為新建目錄。",
            30,
            new String[]{"cd", "ls", "pwd", "mkdir"},
            2
        );

        // Q13
        createQuestion(
            "下列哪一個不屬於物件導向程式設計（OOP）的三大（或四大）核心基本特性？",
            "物件導向的核心特性為：封裝（Encapsulation）、繼承（Inheritance）、多型（Polymorphism）以及抽象（Abstraction）；編譯為建置流程而非 OOP 核心特性。",
            30,
            new String[]{"封裝 (Encapsulation)", "繼承 (Inheritance)", "編譯 (Compilation)", "多型 (Polymorphism)"},
            2
        );

        // Q14
        createQuestion(
            "Spring 框架的核心理念中，除了依賴注入（DI）外，另一個最核心的機制是？",
            "控制反轉（Inversion of Control, IoC）將物件的生命週期與依賴管理交給 Spring Container 控制，DI 則是其實作方式之一。",
            30,
            new String[]{"控制反轉 (IoC)", "事件驅動 (EDA)", "面向介面 (OIP)", "領域驅動 (DDD)"},
            0
        );

        // Q15
        createQuestion(
            "在 MySQL 資料庫中，用來查看既有資料表的欄位定義與結構時常用的指令是？",
            "DESCRIBE 或簡寫 DESC table_name; 可快速顯示資料表內的所有欄位名稱、型態、Null 屬性與鍵值等結構資訊。",
            30,
            new String[]{"SELECT", "DESCRIBE", "UPDATE", "DROP"},
            1
        );

        // Q16
        createQuestion(
            "二進位數字 0010（十進位為 2）執行向左位移 1 位元（<< 1）後的運算結果為何？",
            "向左位移 1 位元等同於數值乘以 2，0010 左移後變成 0100（十進位的 4）。",
            30,
            new String[]{"0001", "0011", "0100", "1000"},
            2
        );

        // Q17
        createQuestion(
            "當 HTTP 用戶端收到狀態碼 404（Not Found）時，代表何種涵義？",
            "404 Not Found 表示伺服器可正常通訊，但找不到客戶端所請求的 URL 端點或指定資源路徑。",
            30,
            new String[]{"伺服器內部錯誤", "禁止存取 (Forbidden)", "找不到指定資源 (Not Found)", "未授權請求 (Unauthorized)"},
            2
        );

        // Q18
        createQuestion(
            "在 Java Collection Framework 中，哪一個集合實作不允許儲存重複的元素？",
            "Set 介面的實作類（如 HashSet、TreeSet）會透過 equals() 與 hashCode() 阻絕重複元素；List 家族（如 ArrayList）則允許重複儲存。",
            30,
            new String[]{"ArrayList", "LinkedList", "HashSet", "Vector"},
            2
        );

        // Q19
        createQuestion(
            "標準的 ASCII（American Standard Code for Information Interchange）編碼使用多少位元表示單一字元？",
            "標準 ASCII 編碼使用 7 個位元（bit）表示 128 個字元（0-127）；延伸 ASCII 則擴展至 8 個位元。",
            30,
            new String[]{"7", "8", "16", "32"},
            0
        );

        // Q20
        createQuestion(
            "在 OSI 七層模型中，負責端對端封包定址、路徑選擇（Routing）的是哪一層？",
            "網路層（Network Layer，如 IP 協定）主要負責邏輯定址與封包繞送（Routing）；資料連結層負責節點到節點傳輸，傳輸層負責端對端傳輸與流量控制。",
            30,
            new String[]{"實體層", "資料連結層", "網路層", "傳輸層"},
            2
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
}
