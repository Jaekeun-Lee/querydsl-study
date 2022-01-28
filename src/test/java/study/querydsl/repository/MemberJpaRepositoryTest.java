package study.querydsl.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import study.querydsl.TestHelper;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Autowired
    TestHelper testHelper;

    @BeforeEach
    public void before() {
        testHelper.beforeSet();
    }
    @Test
    public void basicTest() {
        Member member = new Member("member1", 1);
        memberJpaRepository.save(member);

        Member findMember = memberJpaRepository.findById(member.getId()).get();
        assertEquals(member, findMember);

        List<Member> byUsername = memberJpaRepository.findByUsername("member1");
        assertTrue(byUsername.contains(member));

        List<Member> all = memberJpaRepository.findAll();
        assertTrue(all.contains(member));
    }

    @Test
    public void querydslTest() {
        Member member = new Member("member1", 1);
        memberJpaRepository.save(member);

        Member findMember = memberJpaRepository.findById_Querydsl(member.getId()).get();
        assertEquals(member, findMember);

        List<Member> byUsername = memberJpaRepository.findByUsername_Querydsl("member1");
        assertTrue(byUsername.contains(member));

        List<Member> all = memberJpaRepository.findAll_Querydsl();
        assertTrue(all.contains(member));
    }

    @Test
    public void searchTest() {
        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setTeamName("teamB");
        condition.setAgeLoe(40);
        condition.setAgeGoe(31);

        List<MemberTeamDto> memberTeamDtos = memberJpaRepository.searchByBuilder(condition);
        assertEquals(1, memberTeamDtos.size());

        MemberSearchCondition condition2 = new MemberSearchCondition();
        condition2.setTeamName("teamB");

        List<MemberTeamDto> memberTeamDtos2 = memberJpaRepository.searchByBuilder(condition2);
        assertEquals(2, memberTeamDtos2.size());

    }

    @Test
    public void search() {
        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setTeamName("teamB");
        condition.setAgeLoe(40);
        condition.setAgeGoe(31);

        List<MemberTeamDto> memberTeamDtos = memberJpaRepository.search(condition);
        assertEquals(1, memberTeamDtos.size());

        MemberSearchCondition condition2 = new MemberSearchCondition();
        condition2.setTeamName("teamB");

        List<MemberTeamDto> memberTeamDtos2 = memberJpaRepository.search(condition2);
        assertEquals(2, memberTeamDtos2.size());

    }

}