package coma.maciej.wojtaczka.timeline.rest.controller;

import com.datastax.oss.driver.api.core.CqlSession;
import coma.maciej.wojtaczka.timeline.utils.UserFixture;
import org.cassandraunit.CQLDataLoader;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;
import java.util.UUID;

import static coma.maciej.wojtaczka.timeline.rest.controller.TimelineRestController.TIMELINES_URL;
import static java.time.Instant.parse;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@WebAppConfiguration
@AutoConfigureMockMvc
@DirtiesContext
class TimelineRestControllerTest {

	@Autowired
	private UserFixture $;

	@Autowired
	private MockMvc mockMvc;

	@BeforeAll
	static void startCassandra() throws IOException, InterruptedException {
		EmbeddedCassandraServerHelper.startEmbeddedCassandra();
		CqlSession session = EmbeddedCassandraServerHelper.getSession();
		new CQLDataLoader(session).load(new ClassPathCQLDataSet("schema.cql"));
	}

	@Test
	void shouldFetchFreshestAnnouncements() throws Exception {
		//given
		UUID followerId = UUID.randomUUID();
		UUID followeeId1 = UUID.randomUUID();
		UUID followeeId2 = UUID.randomUUID();

		$.userWithId(followerId)
		 .followsUserWithId(followeeId1)
		 .whoPublishedAnnouncement().atTime(parse("2007-12-03T10:15:30.00Z")).withContent("Hello 1 from user 1")
		 .andAlsoAnnouncement().atTime(parse("2007-12-04T10:15:30.00Z")).withContent("Hello 2 from user 1")
		 .andTheGivenUser()
		 .followsUserWithId(followeeId2)
		 .whoPublishedAnnouncement().atTime(parse("2007-12-03T11:15:30.00Z")).withContent("Hello 1 from user 2")
		 .andAlsoAnnouncement().atTime(parse("2007-12-04T11:15:30.00Z")).withContent("Hello 2 from user 2")
		 .andTheGivenUser().isDone();

		//when
		ResultActions result = mockMvc.perform(get(TIMELINES_URL + "/" + followerId)
													   .contentType(APPLICATION_JSON)
													   .accept(APPLICATION_JSON));

		//then
		result.andExpect(status().isOk())
			  .andExpect(jsonPath("$", hasSize(4)))
			  .andExpect(jsonPath("$[0].content", Matchers.equalTo("Hello 2 from user 2")))
			  .andExpect(jsonPath("$[1].content", Matchers.equalTo("Hello 2 from user 1")))
			  .andExpect(jsonPath("$[2].content", Matchers.equalTo("Hello 1 from user 2")))
			  .andExpect(jsonPath("$[3].content", Matchers.equalTo("Hello 1 from user 1")));

	}

	@Test
	void shouldFetchAnnouncementsOlderThenTheGiven() throws Exception {
		//given
		UUID followerId = UUID.randomUUID();
		UUID followeeId1 = UUID.randomUUID();
		UUID followeeId2 = UUID.randomUUID();

		$.userWithId(followerId)
		 .followsUserWithId(followeeId1)
		 .whoPublishedAnnouncement().atTime(parse("2007-12-03T10:15:30.00Z")).withContent("Hello 1 from user 1")
		 .andAlsoAnnouncement().atTime(parse("2007-12-04T10:15:30.00Z")).withContent("Hello 2 from user 1")
		 .andTheGivenUser()
		 .followsUserWithId(followeeId2)
		 .whoPublishedAnnouncement().atTime(parse("2007-12-03T11:15:30.00Z")).withContent("Hello 1 from user 2")
		 .andAlsoAnnouncement().atTime(parse("2007-12-04T11:15:30.00Z")).withContent("Hello 2 from user 2")
		 .andTheGivenUser().isDone();

		//when
		ResultActions result = mockMvc.perform(get(TIMELINES_URL + "/" + followerId)
													   .param("announcementAuthorID", followeeId1.toString())  //reference
													   .param("announcementCreationTime", "2007-12-04T10:15:30.00Z")  //reference
													   .contentType(APPLICATION_JSON)
													   .accept(APPLICATION_JSON));

		//then
		result.andExpect(status().isOk())
			  .andExpect(jsonPath("$", hasSize(2)))
			  .andExpect(jsonPath("$[0].content", Matchers.equalTo("Hello 1 from user 2")))
			  .andExpect(jsonPath("$[1].content", Matchers.equalTo("Hello 1 from user 1")));

	}



}
