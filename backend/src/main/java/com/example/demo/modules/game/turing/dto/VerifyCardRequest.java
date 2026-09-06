package com.example.demo.modules.game.turing.dto;

import com.example.demo.modules.game.turing.model.Code;

public class VerifyCardRequest {
    private Code proposal; 
    private Integer cardId;    // 💡 改用 Integer 避免 null 映射錯誤
    private Code secret;   
    private String roomId;

    public Code getProposal() { return proposal; }
    public void setProposal(Code proposal) { this.proposal = proposal; }
    
    public Integer getCardId() { return cardId; }
    public void setCardId(Integer cardId) { this.cardId = cardId; }
    
    public Code getSecret() { return secret; }
    public void setSecret(Code secret) { this.secret = secret; }
	public String getRoomId() {
		return roomId;
	}
	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}
}