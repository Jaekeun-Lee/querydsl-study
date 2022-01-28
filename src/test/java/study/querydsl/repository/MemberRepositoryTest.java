package study.querydsl.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import study.querydsl.TestHelper;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    TestHelper testHelper;

    @BeforeEach
    public void before() {
        testHelper.beforeSet();
    }

    @Test
    public void basicTest() {
        Member member = new Member("member1", 1);
        memberRepository.save(member);

        Member findMember = memberRepository.findById(member.getId()).get();
        assertEquals(member, findMember);

        List<Member> byUsername = memberRepository.findByUsername("member1");
        assertTrue(byUsername.contains(member));

        List<Member> all = memberRepository.findAll();
        assertTrue(all.contains(member));
    }

    @Test
    public void search() {
        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setTeamName("teamB");
        condition.setAgeLoe(40);
        condition.setAgeGoe(31);

        List<MemberTeamDto> memberTeamDtos = memberRepository.search(condition);
        assertEquals(1, memberTeamDtos.size());

        MemberSearchCondition condition2 = new MemberSearchCondition();
        condition2.setTeamName("teamB");

        List<MemberTeamDto> memberTeamDtos2 = memberRepository.search(condition2);
        assertEquals(2, memberTeamDtos2.size());

    }

    @Test
    public void searchPageSimple() {
        MemberSearchCondition condition = new MemberSearchCondition();

        PageRequest of = PageRequest.of(0, 3);
        Page<MemberTeamDto> memberTeamDtos = memberRepository.searchPageSimple(condition, of);

        assertEquals(3, memberTeamDtos.getSize());

    }

}