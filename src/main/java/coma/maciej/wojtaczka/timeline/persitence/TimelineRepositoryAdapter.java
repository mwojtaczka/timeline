package coma.maciej.wojtaczka.timeline.persitence;

import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import coma.maciej.wojtaczka.timeline.domain.TimelineRepository;
import coma.maciej.wojtaczka.timeline.domain.model.TimelineItem;
import coma.maciej.wojtaczka.timeline.persitence.entity.TimelineItemDbEntity;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal;

@Repository
public class TimelineRepositoryAdapter implements TimelineRepository {

	private final CassandraOperations cassandraOperations;

	public TimelineRepositoryAdapter(CassandraOperations cassandraOperations) {
		this.cassandraOperations = cassandraOperations;
	}

	@Override
	public void saveTimelineItems(List<TimelineItem> newTimelineItems) {
		List<TimelineItemDbEntity> entities = newTimelineItems.stream()
															  .map(TimelineItemDbEntity::from)
															  .collect(Collectors.toList());

		cassandraOperations.batchOps()
						   .insert(entities)
						   .execute();
	}

	@Override
	public List<TimelineItem> fetchTimelineItems(UUID userId, int limit) {

		SimpleStatement select = QueryBuilder.selectFrom("timeline", "timeline_item")
											 .all()
											 .whereColumn("owner_id").isEqualTo(literal(userId))
											 .limit(limit)
											 .build();

		return cassandraOperations.select(select, TimelineItemDbEntity.class).stream()
								  .map(TimelineItemDbEntity::toModel)
								  .collect(Collectors.toList());
	}

	@Override
	public List<TimelineItem> fetchTimelineItems(UUID userId, int limit, Instant olderThan) {
		SimpleStatement select = QueryBuilder.selectFrom("timeline", "timeline_item")
											 .all()
											 .whereColumn("owner_id").isEqualTo(literal(userId))
											 .whereColumn("creation_time").isLessThanOrEqualTo(literal(olderThan))
											 .limit(limit)
											 .build();

		return cassandraOperations.select(select, TimelineItemDbEntity.class).stream()
								  .map(TimelineItemDbEntity::toModel)
								  .collect(Collectors.toList());
	}
}
