package coma.maciej.wojtaczka.timeline.domain;

import java.util.List;
import java.util.UUID;

public interface ConnectorService {

    List<UUID> fetchFollowers(UUID authorId);
}
