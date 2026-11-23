package com.fantasylibrary.persistence.mapper;

import com.fantasylibrary.account.AccountModels.Profile;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AccountMapper {
    record LoginAccount(long id, String passwordHash, String accountRole) {
    }

    record SessionOwner(long id, String accountRole) {}

    @Insert("""
            INSERT INTO fl_account (username, password_hash, display_name)
            VALUES (#{username}, #{hash}, #{displayName})
            """)
    void create(@Param("username") String username, @Param("hash") String hash, @Param("displayName") String displayName);

    @Select("SELECT id, password_hash, account_role FROM fl_account WHERE username = #{username} AND is_deleted = 0")
    LoginAccount findLogin(String username);

    @Select("SELECT id, username, display_name, bio, account_role, avatar_key FROM fl_account WHERE id = #{userId} AND is_deleted = 0")
    Profile profile(long userId);

    @Select("SELECT id FROM fl_account WHERE id = #{userId} AND is_deleted = 0 FOR UPDATE")
    Long lockActive(long userId);

    @Select("SELECT avatar_key FROM fl_account WHERE id = #{userId} AND is_deleted = 0 FOR UPDATE")
    String lockAvatar(long userId);

    @Update("UPDATE fl_account SET avatar_key = #{key,jdbcType=VARCHAR} WHERE id = #{userId} AND is_deleted = 0")
    int updateAvatar(@Param("userId") long userId, @Param("key") String key);

    @Select("SELECT COUNT(*) FROM fl_account WHERE avatar_key = #{key}")
    int referencesAvatar(String key);

    @Update("UPDATE fl_account SET display_name = #{name}, bio = #{bio,jdbcType=VARCHAR} WHERE id = #{userId} AND is_deleted = 0")
    void updateProfile(@Param("userId") long userId, @Param("name") String name, @Param("bio") String bio);

    @Insert("""
            INSERT INTO fl_login_token (digest, user_id, expires_at)
            VALUES (#{digest}, #{userId}, SYSTIMESTAMP + NUMTODSINTERVAL(#{seconds}, 'SECOND'))
            """)
    void createToken(@Param("digest") String digest, @Param("userId") long userId, @Param("seconds") long seconds);

    @Select("""
            SELECT a.id, a.account_role FROM fl_login_token t JOIN fl_account a ON a.id = t.user_id
            WHERE t.digest = #{digest} AND t.expires_at > SYSTIMESTAMP AND a.is_deleted = 0
            """)
    SessionOwner authenticate(String digest);

    @Delete("DELETE FROM fl_login_token WHERE digest = #{digest}")
    void revoke(String digest);

    @Delete("DELETE FROM fl_login_token WHERE user_id = #{userId} AND expires_at <= SYSTIMESTAMP")
    void pruneExpired(long userId);
}
