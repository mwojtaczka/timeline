package coma.maciej.wojtaczka.timeline.persitence;

import coma.maciej.wojtaczka.timeline.domain.TimelineRepository;
import coma.maciej.wojtaczka.timeline.domain.model.TimelineItem;
import coma.maciej.wojtaczka.timeline.persitence.entity.TimelineItemDbEntity;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

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
}
