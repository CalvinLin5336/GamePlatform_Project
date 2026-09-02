package com.example.demo.modules.game.quiz.service;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.modules.game.quiz.dto.QuizReportResponse;
import com.example.demo.modules.game.quiz.dto.QuizSubmitRequest;
import com.example.demo.modules.game.quiz.model.Option;
import com.example.demo.modules.game.quiz.model.Question;
import com.example.demo.modules.game.quiz.repository.QuestionRepository;


/*
 * 題目與測驗核心業務邏輯類別
 * */
@Service
public class QuizService {

	//依賴注入:題目資料庫存取與玩家服務層
	private final QuestionRepository questionrepo;
	private final PlayerService playerService;
	
	/*
	 * 建構子注入(Constructor Injection)
	 * 具備不變性(final)且有利於單元測試
	 * */
	public QuizService(QuestionRepository questionrepo,PlayerService playerService) {
		this.questionrepo = questionrepo;
		this.playerService = playerService;
	}
	
	//隨機抽取測驗考題:記憶體洗牌防呆，避免SQL方言衝突或資料量不足越界
	//加上唯讀事務避免 LazyInitializationException
	@Transactional(readOnly=true)
	public List<Question> getRandomExamQuestions(int count){
		List<Question> allQuestions = questionrepo.findAll();
		if(allQuestions== null || allQuestions.isEmpty()) {
			return Collections.emptyList();
		}
		//複製一份清單避免影響快取，進行隨機洗牌
		List<Question> shuffled = new ArrayList<>(allQuestions);
		Collections.shuffle(shuffled);
		
		//使用 stream limit，即便題庫少於count題也部會拋出 IndexOutOfBoundsException
		List<Question> examQuestions = shuffled.stream()
				.limit(count)
				.collect(Collectors.toList());
		
		//明確觸發選項加載，確保序列化為JSON 時不中斷
		examQuestions.forEach(q->{
			if(q.getOptions()!=null) {
				q.getOptions().size();
			}
		});
		return examQuestions;
	}
	
	//查詢題庫中所有題目
	public List<Question> getAllQuestions(){
		return questionrepo.findAll();
	}
	
	//依ID取得單一題目
	public Question getQuestionById(Long id) {
		return questionrepo.findById(id)
				.orElseThrow(()->new RuntimeException("題目不存在"));
	}	
	
	//新增題目(包含其下所有選項)
	@Transactional
	public Question createQuestion(Question question) {
		//維護 JPA 雙向一對多(One-to-Many)關聯:讓每個Option實體指向所屬的Question
		if(question.getOptions()!=null) {
			question.getOptions().forEach(option-> option.setQuestion(question));
		}
		return questionrepo.save(question);
	}
	
	//更新題目與其選項內容
	@Transactional  //交易控制，若過程有錯誤會將改動自動回滾
	public Question updateQuestion(Long id, Question updateQuestion) {			
		Question existing = questionrepo.findById(id)
				.orElseThrow(()-> new RuntimeException("題目不存在"));
		//根據傳入的id去資料庫找出【舊題目資料(existing)】
		//找不到就拋出 RuntimeException("題目不存在")報作中斷
		
		//更新基本屬性
		existing.setTitle(updateQuestion.getTitle());
		existing.setExplanation(updateQuestion.getExplanation());
		existing.setTimeLimitSeconds(updateQuestion.getTimeLimitSeconds());
		//將傳入的新題目名稱(Title)和答題時限(Time Limit)設定給舊物件，覆蓋舊有的數值
		
		existing.getOptions().clear();
		//清空舊選項: 先把這題原本所有的選項從清單中移除。配合JPA的
		//orphanRemoval = true 設定時，資料庫中對應的舊選項紀錄會被自動刪除
		
		if(updateQuestion.getOptions()!=null) {
			for(Option opt : updateQuestion.getOptions()) {
				existing.addOption(opt);
			}
		}
		//>重新建立新的選項:如果傳入的新資料，包含選項清單，就用迴圈逐一透過
		//	existing.addOption(opt)加進去
		//>通常 addOption 是雙向關聯輔助方法，內部通常會自動執行
		//	opt.setQuestion(this)，確保新選項知道自己屬於哪一題
		
		return questionrepo.save(existing);
		//修改後存回資料庫，並回傳更新後的Question 物件
	}
	
	//依照ID刪除題目
	@Transactional
	public void deleteQuestion(Long id) {
		questionrepo.deleteById(id);
	}
	
	/*
	 * 核心邏輯:批改測驗、計算成績，更新玩家紀錄並產出明細報告
	 * @param request 前端傳入的交卷資料(包含玩家名稱與答題清單)
	 * @return 包含總分與美題對錯明細 QuizReportResponse
	 * */
	@Transactional
	public QuizReportResponse processQuizSubmission(QuizSubmitRequest request)
	{
		//防呆保護:避免request 為null 或 answer 為null
		if(request==null) {
			throw new IllegalArgumentException("交卷資料不能為空");
		}
		//防呆保護:若未提供答案列表則以空清單處理，避免NPE
				List<QuizSubmitRequest.AnswerItem> answers = request.getAnswers() !=null
						?request.getAnswers()
						:Collections.emptyList();
		
		int correctCount=0; //答對題數計數器
		List<QuizReportResponse.QuestionDetail> details = new ArrayList<>();
		
		//效能優化:一次性取出所有題目，避免迴圈多次查詢資料庫(解決N+1問題)
		List<Long> questionIds = answers.stream()
				.map(QuizSubmitRequest.AnswerItem::getQuestionId)
				.distinct()  //去除重複，防止題目重複引發的toMap例外
				.collect(Collectors.toList());
		
		//加上(existinkey，replacementKey) -> existingKey 避免重疊key崩潰
		Map<Long, Question> questionMap = questionIds.isEmpty()
				? Collections.emptyMap()
				: questionrepo.findAllById(questionIds).stream()
				  		.collect(Collectors.toMap(Question::getId, Function.identity(),(q1,q2)->q1));
		
		//逐題比對使用者答案與資料庫標準答案
		for(QuizSubmitRequest.AnswerItem answer : answers) {
			Question question = questionMap.get(answer.getQuestionId());
			if(question==null) {
				throw new RuntimeException("找不到該題目，ID: "+ answer.getQuestionId());
			}
			
			
			//取得使用者勾選的選項 ID 集合(若為null 則給予空Set，避免NPE)
			Set<Long> userSelections = answer.getSelectedOptionIds() != null
					? answer.getSelectedOptionIds()
					: Collections.emptySet();
			
			//從題目實體中過濾出所有【正確選項】的ID集合
			List<Option> options = question.getOptions() != null
					? question.getOptions()
					: Collections.emptyList();
			// 防呆處理: 使用Boolean.TRUE.equals避免isCorrect 為null 時噴出 NPE
			Set<Long> correctSelections =options.stream()
					.filter(opt -> Boolean.TRUE.equals(opt.getIsCorrect()))
					.map(Option::getId)
					.collect(Collectors.toSet());
			
			//集合對比:Set.equals()會比較元素是否完全相同(順序無關)，同時支援單選與多選
			boolean isCorrect = correctSelections.equals(userSelections);
			if(isCorrect) {
				correctCount++;
			}
			//建立單題明細資料
			details.add(QuizReportResponse.QuestionDetail.builder()
					.question(question)
					.userSelectedOptionIds(userSelections)
					.isCorrect(isCorrect)
					.build()
					);	
		}
		
		//動態計算比分制總分(若無題目則0分)
		int totolQuestions = answers.size();
		int score = totolQuestions > 0 ? (int) Math.round(((double) correctCount / totolQuestions) * 100) : 0;
		
		//若有填寫完加名稱，則呼叫PlayerService 嘗試更新排行榜與個人最高分
		if(request.getUsername() != null && !request.getUsername().isBlank()){
			playerService.updatePlayerScore(request.getUsername(), score);
		}
		
		//組裝並回傳完整測驗結果報告
		return QuizReportResponse.builder()
				.totalQuestions(answers.size())
				.correctCount(correctCount)
				.score(score)
				.details(details)
				.build();
	
	}
}
