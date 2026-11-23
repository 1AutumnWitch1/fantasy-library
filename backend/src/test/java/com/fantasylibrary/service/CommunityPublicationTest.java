package com.fantasylibrary.service;

import com.fantasylibrary.api.ApiResponse.Code;
import com.fantasylibrary.community.CommunityModels.PostOrder;
import com.fantasylibrary.community.CommunityProblem;
import com.fantasylibrary.persistence.mapper.CommunityMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommunityPublicationTest {
    @Test
    void closedForumStopsPublicReadsAndPublishingWhileAllowingReportReview() {
        var mapper = (CommunityMapper) Proxy.newProxyInstance(CommunityMapper.class.getClassLoader(),
                new Class<?>[]{CommunityMapper.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "forumOpen" -> false;
                    case "countReports" -> 0L;
                    default -> throw new AssertionError("Unexpected mapper call: " + method.getName());
                });
        var beans = new StaticListableBeanFactory();
        beans.addBean("communityMapper", mapper);
        var community = new CommunityService(beans.getBeanProvider(CommunityMapper.class), null, null, null, null);

        assertEquals(Code.COMMUNITY_CLOSED, assertThrows(CommunityProblem.class,
                () -> community.posts(null, null, 1, 12, PostOrder.RECENT)).code());
        assertEquals(Code.COMMUNITY_CLOSED, assertThrows(CommunityProblem.class, () -> community.post(1)).code());
        assertEquals(Code.COMMUNITY_CLOSED, assertThrows(CommunityProblem.class,
                () -> community.publish(1, null, List.of())).code());
        assertEquals(0, community.reports(1, 12).total());
    }
}
