package com.example.demo.modules.game.turing.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "turing_questions") // 👈 獨立出圖靈專用的新表
public class TuringQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ans_b")
    private int ansB;

    @Column(name = "ans_y")
    private int ansY;

    @Column(name = "ans_p")
    private int ansP;

    @Column(name = "k_count")
    private int kCount;

    // 🟢 這裡是一對多關聯：一個題目擁有多個條件
    // fetch = FetchType.EAGER 代表撈題目的同時，把條件也一起撈出來
    @OneToMany(mappedBy = "turingQuestion", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<TuringQuestionCondition> conditions = new ArrayList<>();

    public TuringQuestion() {}

    public TuringQuestion(int ansB, int ansY, int ansP, int kCount) {
        this.ansB = ansB;
        this.ansY = ansY;
        this.ansP = ansP;
        this.kCount = kCount;
    }

    // --- Getters and Setters ---

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public int getAnsB() { return ansB; }
    public void setAnsB(int ansB) { this.ansB = ansB; }
    public int getAnsY() { return ansY; }
    public void setAnsY(int ansY) { this.ansY = ansY; }
    public int getAnsP() { return ansP; }
    public void setAnsP(int ansP) { this.ansP = ansP; }
    public int getKCount() { return kCount; }
    public void setKCount(int kCount) { this.kCount = kCount; }
    public List<TuringQuestionCondition> getConditions() { return conditions; }
    public void setConditions(List<TuringQuestionCondition> conditions) { this.conditions = conditions; }
}