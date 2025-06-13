package service;

import dao.MemberDAO;
import model.Member;
import org.mindrot.jbcrypt.BCrypt; // 用於密碼雜湊

public class MemberService {
    private final MemberDAO memberDAO = new MemberDAO();

    public void register(String email, String password, String birthDate) {
        // 使用 BCrypt 雜湊密碼
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        Member member = new Member(0, email, hashedPassword, birthDate);
        memberDAO.addMember(member);
    }

    public Member login(String email, String password) {
        Member member = memberDAO.getMemberByEmail(email);
        // 使用 BCrypt 驗證輸入密碼與資料庫中的雜湊值是否匹配
        if (member != null && BCrypt.checkpw(password, member.getPassword())) {
            return member;
        }
        return null;
    }
    
    /**
     * 透過會員ID查詢會員資料
     * @param uid 會員ID
     * @return 會員物件，如果找不到則返回null
     */
    public Member getMemberById(int uid) {
        return memberDAO.getMemberByUid(uid);
    }
}