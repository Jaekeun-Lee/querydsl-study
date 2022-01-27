package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.util.StringUtils;
import study.querydsl.entity.Member;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
public class QuerydslTest {

    @Autowired
    EntityManager em;

    @Autowired
    TestHelper testHelper;
    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = testHelper.beforeSet();
    }

    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertEquals(1, result.size());
    }

    private List<Member> searchMember1(String usernameParamCond, Integer ageParamCond) {

        BooleanBuilder builder = new BooleanBuilder();
        if (StringUtils.hasText(usernameParamCond)) {
            builder.and(member.username.eq(usernameParamCond));
        }

        if (ageParamCond != null) {
            builder.and(member.age.eq(ageParamCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();

    }


    @Test
    public void dynamicQuery_WhereParam() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertEquals(1, result.size());
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
//                .where(usernameEq(usernameCond), ageEq(ageCond))
                .where(allEq(usernameCond, ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }


    @Test
    public void bulkUpdate() {

        long execute = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(29))
                .execute();

        // 벌크 연산은 영속성 컨텍스트 무시 후 DB 바로 접근
        em.flush();
        em.clear();

        System.out.println(execute);
    }

    @Test
    public void bulkAdd() {

        long execute = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();

        System.out.println(execute);
    }

    @Test
    public void bulkDelete() {

        long execute = queryFactory
                .delete(member)
                .where(member.age.gt(19))
                .execute();

        System.out.println(execute);
    }

    @Test
    public void sqlFunction() {
        List<String> fetch = queryFactory
                .select(
                        Expressions.stringTemplate("function('replace', {0}, {1}, {2})",
                                member.username, "member", "m")
                ).from(member)
                .fetch();

        fetch.forEach(System.out::println);
    }
}
