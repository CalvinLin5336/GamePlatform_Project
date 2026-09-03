package com.example.demo.modules.board.service;

import com.example.demo.modules.board.entity.Member;
import com.example.demo.modules.board.repository.MemberRepository;
import com.example.demo.modules.user.service.LoginSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoardSessionService {
    private final LoginSessionService sessions;
    private final MemberRepository members;
    private final PasswordEncoder encoder;

    @Transactional(readOnly = true)
    public Member requireMember(String authorization) {
        var user = sessions.requireUser(authorization);
        return members.findByPlatformUserId(user.id()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.CONFLICT, "請先重新整理 Board，完成會員連結"));
    }

    @Transactional
    public Member currentMember(String authorization) {
        var user = sessions.requireUser(authorization);
        Member member = members.findByPlatformUserId(user.id()).orElse(null);
        var sameAccount = members.findByAccount(user.account());
        if (sameAccount.isPresent() && (member == null || !sameAccount.get().getId().equals(member.getId())))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "此帳號與舊版組隊帳號同名，請聯絡管理員確認帳號歸屬後再使用");
        if (member == null) {
            member = new Member();
            member.setPlatformUserId(user.id());
            // 平台會員只使用 JWT 登入；不複製平台密碼，也不允許以 Board 舊密碼登入。
            member.setPassword(encoder.encode(UUID.randomUUID().toString()));
            member.setEmail("platform-" + UUID.randomUUID() + "@users.invalid");
        }
        member.setAccount(user.account());
        member.setNickname(user.username());
        member.setRole(user.role());
        return members.save(member);
    }
}
