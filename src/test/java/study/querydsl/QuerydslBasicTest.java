package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.transaction.Transactional;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.select;
import static org.junit.jupiter.api.Assertions.*;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager entityManager;
    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(entityManager);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        entityManager.persist(teamA);
        entityManager.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        entityManager.persist(member1);
        entityManager.persist(member2);
        entityManager.persist(member3);
        entityManager.persist(member4);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    public void startJPQL() {
        // member1 찾기
        Member findMember = entityManager.createQuery(
                "select m " +
                        "from Member m " +
                        "where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertEquals(findMember.getUsername(), "member1");

    }

    @Test
    public void startQuerydsl() {
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertEquals(findMember.getUsername(), "member1");
    }

    @Test
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1")
                                .and(member.age.eq(10)))
                .fetchOne();

        assertEquals(findMember.getUsername(), "member1");
    }

    @Test
    public void resultFetch() {
//        List<Member> fetch = queryFactory
//                .selectFrom(member)
//                .fetch();
//
//        Member fetchOne = queryFactory
//                .selectFrom(member)
//                .fetchOne();
//
//        Member fetchFirst = queryFactory
//                .selectFrom(QMember.member)
//                .fetchFirst();

        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();

        results.getTotal();
        List<Member> results1 = results.getResults();

        List<Member> fetch = queryFactory.selectFrom(member).fetch();
        fetch.forEach(System.out::println);


    }


    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순 ( desc )
     * 2. 회원 이름 올림차순 ( asc )
     * 단, 2에서 회원 이름이 없으면 마지막에 출력 ( nulls last )
     */
    @Test
    public void sort() {
        Member member5 = new Member(null, 100);
        Member member6 = new Member("member6", 100);
        Member member7 = new Member("member7", 100);

        List<Member> fetch = queryFactory
                .selectFrom(member)
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        assertEquals("member6", fetch.get(0).getUsername());
        assertEquals("member7", fetch.get(1).getUsername());
        assertNull(fetch.get(fetch.size() - 1));
    }

    @Test
    public void paging1() {
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();
    }

    @Test
    public void aggregation() {
        Tuple tuple = queryFactory
                .select(
                        member.count(),
                        member.age.max(),
                        member.age.min(),
                        member.age.avg()
                )
                .from(member)
                .fetchOne();

        assertEquals(4, tuple.get(member.count()));
        assertEquals(40, tuple.get(member.age.max()));
        assertEquals(10, tuple.get(member.age.min()));
        assertEquals(25, tuple.get(member.age.avg()));


        System.out.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
        queryFactory.select(member).from(member).where(member.id.in(1, 2, 3)).fetch().forEach(System.out::println);

    }

    /**
     * 팀의 이름과 각 팀의 평균 연령
     */
    @Test
    public void group() {
        List<Tuple> fetch = queryFactory
                .select(
                        team.teamName,
                        member.age.avg()
                )
                .from(member)
                .join(member.team, team)
                .groupBy(team.teamName)
                .orderBy(team.teamName.asc())
                .fetch();

        Tuple teamA = fetch.get(0);
        Tuple teamB = fetch.get(1);

        assertEquals("teamA", teamA.get(team.teamName));
        assertEquals(15, teamA.get(member.age.avg()));
        assertEquals("teamB", teamB.get(team.teamName));
        assertEquals(35, teamB.get(member.age.avg()));
    }

    /**
     * 팀 A에 소속된 모든 회원
     */
    @Test
    public void join() {
        List<Member> teamA = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.teamName.eq("teamA"))
                .fetch();

        assertEquals(2, teamA.size());
    }


    @Test
    public void joinOnFilter() {
        // 회원과 팀을 조인하면서, 팀이름이 teamA인 팀만 조인, 회원은 모두 조회
        List<Tuple> results = queryFactory
                .select(member.username, team.teamName)
                .from(member)
                .leftJoin(member.team, team).on(team.teamName.eq("teamA"))
                .fetch();

        assertEquals(4, results.size());
        System.out.println(results);
        assertEquals("teamA", results.get(0).get(team.teamName));

//        assertEquals("teamA", results.get(member));
//        assertEquals("teamA", members.get(1).getTeam().getTeamName());
//        assertNull(members.get(2).getTeam().getTeamName());
//        assertNull(members.get(3).getTeam().getTeamName());
    }

    @Test
    public void joinOnNoRelation() {
        /*
        연관 관계가 없는 엔티티 외부조인
        회원의 이름이 팀 이름과 같은 대상 외부조인
         */

        List<Tuple> fetch = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.teamName))
                .fetch();

        fetch.forEach(System.out::println);
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoin() {
        entityManager.flush();
        entityManager.clear();

        Member noFetchJoin = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(noFetchJoin.getTeam());
        assertFalse(loaded);

        entityManager.flush();
        entityManager.clear();

        Member fetchJoin = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        assertTrue(emf.getPersistenceUnitUtil().isLoaded(fetchJoin.getTeam()));

    }

    @Test
    public void subQuery() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();
        assertEquals(40, result.get(0).getAge());
    }

    @Test
    public void subQueryGoe() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();
        assertEquals(30, result.get(0).getAge());
        assertEquals(40, result.get(1).getAge());
    }

    @Test
    public void subQueryIn() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.goe(20))
                ))
                .fetch();
        assertEquals(20, result.get(0).getAge());
        assertEquals(30, result.get(1).getAge());
        assertEquals(40, result.get(2).getAge());
    }

    @Test
    public void selectSubQuery() throws Exception {

        QMember memberSub = new QMember("memberSub");

        List<Tuple> fetch = queryFactory
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();


        System.out.println(fetch.size());
        Tuple tuple = fetch.get(0);
        System.out.println(tuple.get(member.username));
        System.out.println(tuple.get(memberSub.age.avg()));
//        System.out.println(tuple.get(memberSub.age.avg()));

    }

    @Test
    public void basicCase() {
        List<String> fetch = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        fetch.forEach(System.out::println);
    }

    @Test
    public void complexCase() {


        List<String> fetch = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 19)).then("청소년")
                        .when(member.age.gt(19)).then("성인")
                        .otherwise("외계인"))
                .from(member).fetch();

        fetch.forEach(System.out::println);
    }


    @Test
    public void constant() {
        List<Tuple> a = queryFactory.select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        a.forEach(System.out::println);
    }

    @Test
    public void concat() {

        Member testMem = new Member("testMem", 50);

        entityManager.persist(testMem);
        entityManager.flush();
        entityManager.clear();


        List<String> fetch = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("testMem"))
                .fetch();

        assertEquals("testMem_50", fetch.get(0));

    }
}
