package com.example.demo.modules.game.turing.model;

import jakarta.persistence.*;

@Entity
@Table(name = "turing_question_conditions") // 👈 獨立出圖靈專用的新明細表
public class TuringQuestionCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 🟢 這是對應驗證卡片的編號 (Card ID)
    @Column(name = "condition_id")
    private int conditionId;

    private String description;

    // 🟢 這是多對一關聯：對應回主表的題目
    @ManyToOne
    @JoinColumn(name = "question_id") // 資料庫裡用來關聯的外鍵欄位名稱
    private TuringQuestion turingQuestion;

    public TuringQuestionCondition() {}

    public TuringQuestionCondition(int conditionId, String description, TuringQuestion turingQuestion) {
        this.conditionId = conditionId;
        this.description = description;
        this.turingQuestion = turingQuestion;
    }
    
    private int subIndex;

    // --- Getters and Setters ---

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public int getConditionId() { return conditionId; }
    public void setConditionId(int conditionId) { this.conditionId = conditionId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TuringQuestion getQuestion() { return turingQuestion; }
    public void setQuestion(TuringQuestion turingQuestion) { this.turingQuestion = turingQuestion; }

	public int getSubIndex() {
		return subIndex;
	}

	public void setSubIndex(int subIndex) {
		this.subIndex = subIndex;
	}
}