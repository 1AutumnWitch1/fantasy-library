package com.fantasylibrary.persistence;

import com.fantasylibrary.catalog.model.Catalog.WorkType;
import com.fantasylibrary.catalog.model.CatalogQuery;
import com.fantasylibrary.persistence.mapper.CatalogMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogMapperTest {

    @Test
    void bindsFiltersAndPaginationAndSharesFiltersWithCount() throws Exception {
        var configuration = new Configuration();
        String resource = "mybatis/CatalogMapper.xml";
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertThat(input).isNotNull();
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }

        var query = new CatalogQuery(3, 5, "fantasy", "mystery", "  雾港%_  ", List.of("detective", "mystery"));
        var parameters = Map.of("type", WorkType.ANIME, "query", query);
        String namespace = CatalogMapper.class.getName();
        var page = configuration.getMappedStatement(namespace + ".findWorks").getBoundSql(parameters);
        var count = configuration.getMappedStatement(namespace + ".countWorks").getBoundSql(parameters);
        assertThat(count.getParameterMappings()).extracting(ParameterMapping::getProperty)
                .containsExactly("type", "query.category", "__frch_slug_0", "__frch_slug_1", "tagCount", "query.keyword", "query.keyword");
        assertThat(page.getParameterMappings()).extracting(ParameterMapping::getProperty)
                .containsExactly("type", "query.category", "__frch_slug_0", "__frch_slug_1", "tagCount", "query.keyword", "query.keyword",
                        "query.offset", "query.size");
        assertThat(page.getSql()).contains("ORDER BY w.release_date DESC NULLS LAST, w.id DESC",
                "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY").doesNotContain(query.keyword());
        assertThat(count.getAdditionalParameter("__frch_slug_0")).isEqualTo("mystery");
        assertThat(count.getAdditionalParameter("__frch_slug_1")).isEqualTo("detective");
        assertThat(count.getAdditionalParameter("tagCount")).isEqualTo(2);
        assertThat(page.getSql()).doesNotContain("detective", "mystery");
        var values = configuration.newMetaObject(parameters);
        assertThat(values.getValue("query.offset")).isEqualTo(10L);
        assertThat(values.getValue("query.keyword")).isEqualTo("雾港%_");
    }
}
